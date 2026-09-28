package cn.blockforge.generated.soulblade;

import cn.blockforge.generated.soulblade.client.DomainVisuals;
import cn.blockforge.generated.soulblade.domain.SoulDomain;
import cn.blockforge.generated.soulblade.skill.SoulSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 网络通道：
 *  C2S SkillPacket —— 左键（[解]，替换挖方块，只能客户端拦截后上报）与中键（领域展开开关）。
 *  S2C DomainVisualPacket —— 领域视觉同步（前摇/开启/关闭：雾色、暗化、色差、震颤）。
 */
public final class SoulBladeNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SoulBlade.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;

    /** 领域视觉同步：关闭。 */
    public static final int DOMAIN_VISUAL_OFF = 0;
    /** 领域视觉同步：前摇开始（1.5 秒渐染暗红）。 */
    public static final int DOMAIN_VISUAL_PRECAST = 1;
    /** 领域视觉同步：领域已开启（满额暗红 + 迷雾收缩 + 震颤/后处理）。 */
    public static final int DOMAIN_VISUAL_ACTIVE = 2;

    /** 技能动作：左键 [解]。 */
    public static final int ACTION_TATAYAKU = 1;
    /** 技能动作：鼠标中键 领域展开 开/关。 */
    public static final int ACTION_DOMAIN_TOGGLE = 2;
    /** 技能动作：右键 [捌]（按下即上报，目标由服务端宽松锁定；与服务端 RightClickItem 兜底靠冷却去重）。 */
    public static final int ACTION_KANESADA = 3;

    private SoulBladeNetwork() { }

    public static void init() {
        CHANNEL.registerMessage(nextId++, SkillPacket.class, SkillPacket::encode,
                SkillPacket::decode, SkillPacket::handle);
        CHANNEL.registerMessage(nextId++, DomainVisualPacket.class, DomainVisualPacket::encode,
                DomainVisualPacket::decode, DomainVisualPacket::handle);
    }

    public static void sendToServer(SkillPacket packet) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), packet);
    }

    public static void sendDomainVisual(ServerPlayer player, int mode) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DomainVisualPacket(mode));
    }

    public record SkillPacket(int action) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(action);
        }

        public static SkillPacket decode(FriendlyByteBuf buffer) {
            return new SkillPacket(buffer.readVarInt());
        }

        public static void handle(SkillPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                switch (packet.action()) {
                    case ACTION_TATAYAKU -> SoulSkill.castTatayaku(player);
                    case ACTION_KANESADA -> SoulSkill.castKanesada(player);
                    case ACTION_DOMAIN_TOGGLE -> SoulDomain.toggle(player);
                    default -> { }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record DomainVisualPacket(int mode) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(mode);
        }

        public static DomainVisualPacket decode(FriendlyByteBuf buffer) {
            return new DomainVisualPacket(buffer.readVarInt());
        }

        public static void handle(DomainVisualPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> DomainVisuals.applyDomainVisual(packet.mode())));
            context.setPacketHandled(true);
        }
    }
}
