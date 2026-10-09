package cn.blockforge.generated.soulkatana;

import cn.blockforge.generated.soulkatana.client.DomainVisuals;
import cn.blockforge.generated.soulkatana.client.IdentityMenuClient;
import cn.blockforge.generated.soulkatana.domain.RealPersonDomain;
import cn.blockforge.generated.soulkatana.domain.SoulDomain;
import cn.blockforge.generated.soulkatana.skill.SoulSkill;
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
 *  S2C OpenIdentityMenuPacket —— [无为转变] 让客户端弹出 Identity 的化身选择界面。
 *  S2C CloseIdentityMenuPacket —— [无为转变] 选择完毕后让客户端自动关闭该界面。
 */
public final class SoulKatanaNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SoulKatana.MODID, "main"),
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
    /** 动作：真人领域存续期间的左键空挥（MISS/BLOCK）——服务端据此结算"全域必中"。 */
    public static final int ACTION_DOMAIN_ATTACK = 4;

    private SoulKatanaNetwork() { }

    public static void init() {
        CHANNEL.registerMessage(nextId++, SkillPacket.class, SkillPacket::encode,
                SkillPacket::decode, SkillPacket::handle);
        CHANNEL.registerMessage(nextId++, DomainVisualPacket.class, DomainVisualPacket::encode,
                DomainVisualPacket::decode, DomainVisualPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenIdentityMenuPacket.class, OpenIdentityMenuPacket::encode,
                OpenIdentityMenuPacket::decode, OpenIdentityMenuPacket::handle);
        CHANNEL.registerMessage(nextId++, CloseIdentityMenuPacket.class, CloseIdentityMenuPacket::encode,
                CloseIdentityMenuPacket::decode, CloseIdentityMenuPacket::handle);
    }

    public static void sendToServer(SkillPacket packet) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), packet);
    }

    public static void sendDomainVisual(ServerPlayer player, int mode) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DomainVisualPacket(mode));
    }

    /** [无为转变]：让客户端弹出 Identity 自带的化身选择界面。 */
    public static void sendOpenIdentityMenu(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenIdentityMenuPacket());
    }

    /** [无为转变]：选择已被消费（应用/取消），让客户端关掉化身选择界面。 */
    public static void sendCloseIdentityMenu(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseIdentityMenuPacket());
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
                    // 中键按手持刀分流（显式白名单：只有本模组两把已知刀才进入对应领域）：
                    //   魂刀·真人     → 自闭圆顿裹（RealPersonDomain）
                    //   魂刀·两面宿傩 → 伏魔御厨子（SoulDomain）
                    //   其余任何刀（含未写领域的自定义魂刀）→ 不进入任何领域
                    case ACTION_DOMAIN_TOGGLE -> {
                        if (SoulKatanaIdentity.isRealPerson(player.getMainHandItem())) {
                            RealPersonDomain.toggle(player);
                        } else if (SoulKatanaIdentity.isSoulKatana(player.getMainHandItem())) {
                            SoulDomain.toggle(player);
                        }
                        // else: 既不是真人刀也不是两面宿傩刀 —— 静默忽略，不触发任何领域
                    }
                    case ACTION_DOMAIN_ATTACK -> RealPersonDomain.onOwnerAttack(player);
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

    /** 无负载 S2C：客户端反射打开 IdentityScreen（Identity 缺席时静默）。 */
    public record OpenIdentityMenuPacket() {
        public void encode(FriendlyByteBuf buffer) {
        }

        public static OpenIdentityMenuPacket decode(FriendlyByteBuf buffer) {
            return new OpenIdentityMenuPacket();
        }

        public static void handle(OpenIdentityMenuPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> IdentityMenuClient::openIdentityMenu));
            context.setPacketHandled(true);
        }
    }

    /** 无负载 S2C：选完/取消后关闭化身选择界面（仅当它正开着时生效）。 */
    public record CloseIdentityMenuPacket() {
        public void encode(FriendlyByteBuf buffer) {
        }

        public static CloseIdentityMenuPacket decode(FriendlyByteBuf buffer) {
            return new CloseIdentityMenuPacket();
        }

        public static void handle(CloseIdentityMenuPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> IdentityMenuClient::closeIdentityMenuIfOpen));
            context.setPacketHandled(true);
        }
    }
}
