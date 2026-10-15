package cn.blockforge.generated.soulkatana.domain;

import cn.blockforge.generated.soulkatana.SoulKatanaEntities;
import cn.blockforge.generated.soulkatana.SoulKatanaNetwork;
import cn.blockforge.generated.soulkatana.SoulKatanaSounds;
import cn.blockforge.generated.soulkatana.entity.DomainSlashEntity;
import cn.blockforge.generated.soulkatana.entity.ShrineEntity;
import cn.blockforge.generated.soulkatana.skill.SoulSkill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * [领域展开 · 伏魔御厨子]（服务端）：手持魂刀时按鼠标中键触发。
 *  0. 吟唱闸门：按下中键先完整播完 malevolent_shrine.ogg（伏魔御厨子咏唱），
 *     音尾落地后才开始后续程序；吟唱中再按中键无效，收刀/死亡则中止并停音；
 *  1. 30 tick 前摇：周围实体近乎定身，圈内玩家视野 1.5 秒渐染暗红，播前摇音；
 *  2. 展开：界主身后 4 格生成神龛（嘴部对准背部），以展开瞬间界主坐标为球心、
 *     半径 32 格的【半球体】领域成立（不生成结界方块，界主移动不改变球心），
 *     播"伏魔御厨子"、画半球边界（底圈 + 经线）、圈内玩家满额暗红 + 迷雾收缩 + 震颤 + 暗化/色差后处理；
 *  3. 领域内每 2 tick 落下数十道幻影斩（贴图斩击），并持续对界主之外的一切
 *     活物造成 1.3 × 面板攻击 的必中伤害；
 *  4. 再按一次中键关闭（关闭是即时收尾，不走吟唱）；界主死亡/换维度/走出领域自动收尾；
 *     关闭后 30 秒冷却。
 * 领域不破坏地形（防卡服、防误删存档），视觉与斩击按宿傩参考包移植。
 */
@Mod.EventBusSubscriber(modid = "soulkatana")
public final class SoulDomain {
    /** 开放式领域：无结界方块，半径 32 的半球体即作用范围（球心锁定展开时的坐标）。 */
    public static final int RADIUS_BLOCKS = 32;
    private static final double RADIUS = RADIUS_BLOCKS;
    private static final int CHARGE_TICKS = 30;
    private static final int PRECAST_RADIUS_BLOCKS = 32;
    private static final double PRECAST_RADIUS = PRECAST_RADIUS_BLOCKS;
    private static final double DAMAGE_MULTIPLIER = 1.3D;
    public static final int DOMAIN_COOLDOWN_TICKS = 600;
    private static final String DOMAIN_COOLDOWN_KEY = "SoulbladeDomainCd";
    /** 边界环约每 3 格一个粒子。 */
    private static final int RING_POINTS = (int) Math.ceil(2.0D * Math.PI * RADIUS / 3.0D);
    /** 半球经线：8 条自底圈升到顶点的弧，每条 10 个采样点（半球体的边界可视化）。 */
    private static final int MERIDIAN_COUNT = 8;
    private static final int MERIDIAN_STEPS = 10;
    /** 每 2 tick 一波斩击风暴。 */
    private static final int SLASH_STORM_INTERVAL_TICKS = 2;
    private static final int SLASHES_PER_STORM = 60;
    private static final double SLASH_STORM_LIFETIME_SECONDS = 0.15D;
    private static final double SHRINE_INNER_RADIUS = 20.0D;
    private static final double SHRINE_NEAR_CHANCE = 0.65D;
    /** 斩击在神龛水平半径内时，贴近神龛高度的概率（其余按半球高度随机）。 */
    private static final double STORM_SHRINE_HEIGHT_CHANCE = 0.6D;
    private static final double STORM_ABOVE_RANGE = 28.0D;
    private static final int SLASH_STORM_MIN_LENGTH = 15;
    private static final int SLASH_STORM_MAX_LENGTH = 45;
    private static final Map<UUID, ActiveDomain> ACTIVE_DOMAINS = new HashMap<>();
    private static final Map<UUID, IncantingDomain> INCANTING_DOMAINS = new HashMap<>();
    private static final Map<UUID, ChargingDomain> CHARGING_DOMAINS = new HashMap<>();
    /** malevolent_shrine.ogg 的完整时长（tick）；0 = 未解析。 */
    private static int shrineChantTicks;
    /** 解析失败时的兜底：按当前音频实测 4.574s × 20 ≈ 92 tick。 */
    private static final int SHRINE_CHANT_FALLBACK_TICKS = 92;
    /** 本会话内所有由领域生成的神龛，关闭时按表清理（不做全维度 AABB 查询，防卡死）。 */
    private static final Set<ShrineEntity> LIVE_SHRINES = new HashSet<>();
    private static final DustParticleOptions DOMAIN_RING = new DustParticleOptions(
            new Vector3f(0.12F, 0.015F, 0.02F), 0.2F);

    private SoulDomain() { }

    /**
     * 创造模式豁免：创造模式无视领域冷却，随时可以展开。
     * 两把刀（宿傩 / 真人）共用同一枚冷却键，所以豁免放在这里一处生效。
     */
    public static boolean hasDomainCooldownExemption(Player player) {
        return player.isCreative();
    }

    public static boolean isDomainCoolingDown(Player player) {
        if (hasDomainCooldownExemption(player)) {
            return false;
        }
        return player.getPersistentData().getLong(DOMAIN_COOLDOWN_KEY) > player.level().getGameTime();
    }

    /**
     * 领域收尾时写入冷却。创造模式豁免者直接清掉残留计时，
     * 这样从创造切回生存不会带着上一段没有意义的冷却。
     */
    public static void startDomainCooldown(Player player, long gameTime) {
        if (hasDomainCooldownExemption(player)) {
            player.getPersistentData().remove(DOMAIN_COOLDOWN_KEY);
            return;
        }
        player.getPersistentData().putLong(DOMAIN_COOLDOWN_KEY, gameTime + DOMAIN_COOLDOWN_TICKS);
    }

    /** 中键：领域开启中 → 关闭；吟唱/前摇中、冷却中 → 无效；否则先完整播完咏唱音。 */
    public static void toggle(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive() || !SoulSkill.isHoldingSoulKatana(player)) {
            return;
        }
        UUID id = player.getUUID();
        if (ACTIVE_DOMAINS.containsKey(id)) {
            close(player);
            return;
        }
        if (INCANTING_DOMAINS.containsKey(id) || CHARGING_DOMAINS.containsKey(id)
                || isDomainCoolingDown(player)) {
            return;
        }
        beginIncantation(player);
    }

    /**
     * 吟唱闸门开始：以界主为中心广播播放 malevolent_shrine（伏魔御厨子咏唱），
     * 并登记 tick 倒计时。音频完整播完后才进入后续程序（hold → 30 tick 前摇 → 展开）；
     * 期间死亡/收刀会停音中止（cancelIncantation）。
     */
    private static void beginIncantation(ServerPlayer player) {
        ServerLevel server = (ServerLevel) player.level();
        IncantingDomain incanting = new IncantingDomain(server.getGameTime());
        incanting.musicNotified.addAll(playDomainSound(server, player.position(),
                SoulKatanaSounds.MALEVOLENT_SHRINE.get(), 1.8F));
        INCANTING_DOMAINS.put(player.getUUID(), incanting);
    }

    /** 吟唱中断（死亡/收刀/下线）：停掉未播完的咏唱音；正常播完不走这里（音已自然结束）。 */
    private static void cancelIncantation(ServerPlayer player) {
        IncantingDomain incanting = INCANTING_DOMAINS.remove(player.getUUID());
        if (incanting != null && player.level() instanceof ServerLevel server) {
            stopDomainMusic(server, incanting.musicNotified, SoulKatanaSounds.MALEVOLENT_SHRINE.get());
        }
    }

    /** malevolent_shrine.ogg 播完所需 tick 数（服务端解析 OGG，与真实音频严格对齐）。 */
    private static int shrineChantTicks() {
        if (shrineChantTicks <= 0) {
            shrineChantTicks = readOggDurationTicks(
                    "/assets/soulkatana/sounds/malevolent_shrine.ogg", SHRINE_CHANT_FALLBACK_TICKS);
        }
        return shrineChantTicks;
    }

    /**
     * 从 OGG(Vorbis) 容器解析音频时长：
     *  - 采样率在首页第 1 个包（identification header，0x01+"vorbis"）偏移 12 处（小端 4 字节）；
     *  - 总样本数 = 全流最后一页的 granule position（小端 8 字节，-1 的纯包头页跳过）；
     *  - 秒 = granule / rate，tick = ceil(秒 × 20)。
     * 任何异常都退回 fallback，保证领域永远只是"稍早/稍晚几 tick"，不会卡死流程。
     */
    static int readOggDurationTicks(String resource, int fallbackTicks) {
        try (InputStream in = SoulDomain.class.getResourceAsStream(resource)) {
            if (in == null) {
                return fallbackTicks;
            }
            byte[] data = in.readAllBytes();
            if (data.length < 35 || !(data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S')) {
                return fallbackTicks;
            }
            long granule = -1L;
            int sampleRate = 0;
            boolean firstPage = true;
            int pos = 0;
            while (pos + 27 <= data.length
                    && data[pos] == 'O' && data[pos + 1] == 'g'
                    && data[pos + 2] == 'g' && data[pos + 3] == 'S') {
                int segCount = data[pos + 26] & 0xFF;
                if (pos + 27 + segCount > data.length) {
                    break;
                }
                int body = 0;
                for (int i = 0; i < segCount; i++) {
                    body += data[pos + 27 + i] & 0xFF;
                }
                if (firstPage) {
                    int base = pos + 27 + segCount;
                    if (base + 16 <= data.length && data[base] == 0x01
                            && data[base + 1] == 'v' && data[base + 2] == 'o' && data[base + 3] == 'r'
                            && data[base + 4] == 'b' && data[base + 5] == 'i' && data[base + 6] == 's') {
                        sampleRate = readLeInt(data, base + 12);
                    }
                    firstPage = false;
                }
                long g = readLeLong(data, pos + 6);
                if (g > granule) {
                    granule = g;
                }
                pos += 27 + segCount + body;
            }
            if (sampleRate > 0 && granule > 0L) {
                double seconds = (double) granule / (double) sampleRate;
                int ticks = (int) Math.ceil(seconds * 20.0D);
                return Math.max(1, Math.min(ticks, 20 * 60 * 10)); // 上限 10 分钟，防坏文件卡流程
            }
        } catch (Throwable ignored) {
            // 资源缺失/格式异常：用兜底 tick
        }
        return fallbackTicks;
    }

    private static int readLeInt(byte[] data, int off) {
        return (data[off] & 0xFF) | (data[off + 1] & 0xFF) << 8
                | (data[off + 2] & 0xFF) << 16 | (data[off + 3] & 0xFF) << 24;
    }

    private static long readLeLong(byte[] data, int off) {
        long value = 0L;
        for (int i = 7; i >= 0; i--) {
            value = (value << 8) | (data[off + i] & 0xFFL);
        }
        return value;
    }

    /** 前摇开始：读条登记 + 定身 + 渐染 + 前摇音。 */
    private static void hold(ServerPlayer player) {
        ServerLevel server = (ServerLevel) player.level();
        ChargingDomain charging = new ChargingDomain(server.getGameTime());
        CHARGING_DOMAINS.put(player.getUUID(), charging);
        applyPrecastSlow(server, player, charging);
        broadcastVisual(server, player.position(), RADIUS, SoulKatanaNetwork.DOMAIN_VISUAL_PRECAST);
        charging.musicNotified.addAll(playDomainSound(server, player.position(),
                SoulKatanaSounds.DOMAIN_CHARGE.get(), 1.2F));
    }

    /** 展开领域：神龛在界主身后 4 格生成，嘴部正对界主背部。 */
    public static void cast(ServerPlayer player) {
        ServerLevel server = (ServerLevel) player.level();
        Vec3 center = player.position();
        Vec3 shrineAnchor = shrineAnchor(player, center);
        ActiveDomain old = ACTIVE_DOMAINS.remove(player.getUUID());
        if (old != null) {
            clearAllShrines(old.level, old);
            stopDomainMusic(old.level, old.musicNotified,
                    SoulKatanaSounds.DOMAIN_EXPAND.get(), SoulKatanaSounds.MALEVOLENT_SHRINE.get());
            broadcastVisualOff(old.level, old, player);
        } else {
            clearAllShrines(server, null);
        }
        ActiveDomain domain = new ActiveDomain(server, center, player.getId());
        domain.spawnShrine(shrineAnchor, player.getYRot());
        ACTIVE_DOMAINS.put(player.getUUID(), domain);
        server.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 1.0D, center.z,
                32, 1.0D, 1.0D, 1.0D, 0.2D);
        // 「魔虚宫/伏魔御厨子」咏唱音已前移到中键的吟唱闸门（beginIncantation），
        // 这里不再重复播放，只保留展开音；停音名单仍带上咏唱音，防中键连点时残留。
        domain.musicNotified.addAll(playDomainSound(server, center,
                SoulKatanaSounds.DOMAIN_EXPAND.get(), 1.2F));
        sendBoundary(server, center, player);
        broadcastVisual(server, center, RADIUS, SoulKatanaNetwork.DOMAIN_VISUAL_ACTIVE, domain);
        player.displayClientMessage(Component.translatable("message.soulkatana.domain_started"), false);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onOwnerDisconnect(player);
        }
    }

    /** 维护每个玩家的咏唱、读条与领域。 */
    private static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        IncantingDomain incanting = INCANTING_DOMAINS.get(id);
        if (incanting != null) {
            ServerLevel server = (ServerLevel) player.level();
            if (!player.isAlive() || !SoulSkill.isHoldingSoulKatana(player)) {
                cancelIncantation(player);
                return;
            }
            if (server.getGameTime() - incanting.startedAt >= shrineChantTicks()) {
                // malevolent_shrine.ogg 已完整播完：摘除吟唱，进入后续程序（前摇 → 展开）。
                INCANTING_DOMAINS.remove(id);
                if (player.isAlive() && SoulSkill.isHoldingSoulKatana(player)
                        && !isDomainCoolingDown(player) && !ACTIVE_DOMAINS.containsKey(id)
                        && !CHARGING_DOMAINS.containsKey(id)) {
                    hold(player);
                }
            }
        }

        ChargingDomain charging = CHARGING_DOMAINS.get(id);
        if (charging != null) {
            ServerLevel server = (ServerLevel) player.level();
            if (!player.isAlive() || !SoulSkill.isHoldingSoulKatana(player)) {
                cancelCharging(player);
            } else if (server.getGameTime() - charging.startedAt >= CHARGE_TICKS) {
                CHARGING_DOMAINS.remove(id);
                clearPrecastSlow(server, player, charging);
                stopDomainMusic(server, charging.musicNotified, SoulKatanaSounds.DOMAIN_CHARGE.get());
                if (!isDomainCoolingDown(player)) {
                    cast(player);
                } else {
                    broadcastVisualOff(server, null, player);
                }
            } else {
                applyPrecastSlow(server, player, charging);
            }
        }

        ActiveDomain domain = ACTIVE_DOMAINS.get(id);
        if (domain == null) {
            return;
        }
        if (domain.level != player.level() || !player.isAlive()
                || domain.ownerEntityId != player.getId()) {
            clearAllShrines(domain.level, domain);
            stopDomainMusic(domain.level, domain.musicNotified,
                    SoulKatanaSounds.DOMAIN_EXPAND.get(), SoulKatanaSounds.MALEVOLENT_SHRINE.get());
            ACTIVE_DOMAINS.remove(id);
            broadcastVisualOff(domain.level, domain, player);
            return;
        }
        // 界主走出半球范围（3D 距离；球心固定在展开瞬间的坐标，界主移动不会拖着领域走）
        if (player.distanceToSqr(domain.center) > RADIUS * RADIUS) {
            close(player);
            return;
        }
        ServerLevel server = domain.level;
        damageEntities(server, player, domain.center);
        if (server.getGameTime() % SLASH_STORM_INTERVAL_TICKS == 0L) {
            spawnSlashStorm(server, domain);
        }
        if (server.getGameTime() % 4L == 0L) {
            server.playSound(null, domain.center.x, domain.center.y, domain.center.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F,
                    0.75F + server.getRandom().nextFloat() * 0.2F);
        }
        if (server.getGameTime() % 10L == 0L) {
            sendBoundary(server, domain.center, player);
            syncDomainAura(server, domain);
        }
    }

    private static void cancelCharging(ServerPlayer player) {
        ChargingDomain charging = CHARGING_DOMAINS.remove(player.getUUID());
        if (charging != null && player.level() instanceof ServerLevel server) {
            clearPrecastSlow(server, player, charging);
            stopDomainMusic(server, charging.musicNotified, SoulKatanaSounds.DOMAIN_CHARGE.get());
            broadcastVisualOff(server, null, player);
        }
    }

    private static void close(ServerPlayer player) {
        cancelCharging(player);
        ActiveDomain existing = ACTIVE_DOMAINS.remove(player.getUUID());
        if (existing == null) {
            return;
        }
        clearAllShrines(existing.level, existing);
        stopDomainMusic(existing.level, existing.musicNotified,
                SoulKatanaSounds.DOMAIN_EXPAND.get(), SoulKatanaSounds.MALEVOLENT_SHRINE.get());
        startDomainCooldown(player, existing.level.getGameTime());
        broadcastVisualOff(existing.level, existing, player);
    }

    /** 界主下线的收尾：按 close 流程完整关掉，避免"僵尸领域"把玩家卡在滤镜里。 */
    private static void onOwnerDisconnect(ServerPlayer player) {
        ActiveDomain domain = ACTIVE_DOMAINS.remove(player.getUUID());
        ChargingDomain charging = CHARGING_DOMAINS.remove(player.getUUID());
        if (charging != null && player.level() instanceof ServerLevel server) {
            clearPrecastSlow(server, player, charging);
            stopDomainMusic(server, charging.musicNotified, SoulKatanaSounds.DOMAIN_CHARGE.get());
        }
        if (player.level() instanceof ServerLevel server) {
            IncantingDomain incanting = INCANTING_DOMAINS.remove(player.getUUID());
            if (incanting != null) {
                stopDomainMusic(server, incanting.musicNotified,
                        SoulKatanaSounds.MALEVOLENT_SHRINE.get());
            }
        }
        if (domain == null) {
            return;
        }
        clearAllShrines(domain.level, domain);
        stopDomainMusic(domain.level, domain.musicNotified,
                SoulKatanaSounds.DOMAIN_EXPAND.get(), SoulKatanaSounds.MALEVOLENT_SHRINE.get());
        broadcastVisualOff(domain.level, domain, player);
    }

    /** 关闭领域时清除该维度所有无人引用的神龛（含崩溃遗留的孤儿）。 */
    private static void clearAllShrines(ServerLevel server, ActiveDomain closing) {
        if (closing != null) {
            closing.removeShrine();
        }
        Iterator<ShrineEntity> it = LIVE_SHRINES.iterator();
        while (it.hasNext()) {
            ShrineEntity shrine = it.next();
            if (shrine.isRemoved()) {
                it.remove();
                continue;
            }
            if (shrine.level() != server || ownsShrine(shrine)) {
                continue;
            }
            shrine.discard();
            it.remove();
        }
    }

    private static boolean ownsShrine(ShrineEntity shrine) {
        for (ActiveDomain domain : ACTIVE_DOMAINS.values()) {
            if (domain.shrine == shrine) {
                return true;
            }
        }
        return false;
    }

    /** 神龛生成点：界主身后 4 格；俯视/仰头水平分量过小时退回 yaw。 */
    private static Vec3 shrineAnchor(ServerPlayer player, Vec3 center) {
        Vec3 look = player.getLookAngle();
        double hx = look.x;
        double hz = look.z;
        double horizontal = Math.sqrt(hx * hx + hz * hz);
        if (horizontal < 1.0E-4D) {
            double radians = Math.toRadians(player.getYRot());
            hx = -Math.sin(radians);
            hz = Math.cos(radians);
            horizontal = 1.0D;
        }
        return new Vec3(center.x - hx / horizontal * 4.0D, center.y, center.z - hz / horizontal * 4.0D);
    }

    private static void broadcastVisual(ServerLevel server, Vec3 center, double radius, int mode) {
        broadcastVisual(server, center, radius, mode, null);
    }

    /** 以 center 为圆心把领域视觉模式广播给半球内的玩家；ACTIVE 时登记名单用于精确回收。 */
    private static void broadcastVisual(ServerLevel server, Vec3 center, double radius, int mode,
                                        ActiveDomain domain) {
        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (ServerPlayer target : server.getEntitiesOfClass(ServerPlayer.class, box, Player::isAlive)) {
            if (inDome(center, target)) {
                SoulKatanaNetwork.sendDomainVisual(target, mode);
                if (domain != null && mode == SoulKatanaNetwork.DOMAIN_VISUAL_ACTIVE) {
                    domain.visualNotified.add(target.getUUID());
                }
            }
        }
    }

    /** 按名单回收视觉（跨维度也发），再单独发给界主——他就是"按圈回收"最易漏的人。 */
    private static void broadcastVisualOff(ServerLevel server, ActiveDomain domain, ServerPlayer owner) {
        if (domain != null) {
            for (UUID target : domain.visualNotified) {
                ServerPlayer player = server.getServer().getPlayerList().getPlayer(target);
                if (player != null) {
                    SoulKatanaNetwork.sendDomainVisual(player, SoulKatanaNetwork.DOMAIN_VISUAL_OFF);
                }
            }
            domain.visualNotified.clear();
        }
        SoulKatanaNetwork.sendDomainVisual(owner, SoulKatanaNetwork.DOMAIN_VISUAL_OFF);
    }

    /** 每 0.5 秒对账：半球内玩家幂等收 ACTIVE，出圈的补 OFF 并摘除。 */
    private static void syncDomainAura(ServerLevel server, ActiveDomain domain) {
        AABB box = new AABB(domain.center.x - RADIUS, domain.center.y - RADIUS,
                domain.center.z - RADIUS, domain.center.x + RADIUS,
                domain.center.y + RADIUS, domain.center.z + RADIUS);
        List<ServerPlayer> inside = new ArrayList<>();
        for (ServerPlayer target : server.getEntitiesOfClass(ServerPlayer.class, box, Player::isAlive)) {
            if (inDome(domain.center, target)) {
                inside.add(target);
            }
        }
        for (ServerPlayer target : inside) {
            SoulKatanaNetwork.sendDomainVisual(target, SoulKatanaNetwork.DOMAIN_VISUAL_ACTIVE);
            domain.visualNotified.add(target.getUUID());
        }
        domain.visualNotified.removeIf(target -> {
            if (inside.stream().anyMatch(player -> player.getUUID().equals(target))) {
                return false;
            }
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(target);
            if (player != null) {
                SoulKatanaNetwork.sendDomainVisual(player, SoulKatanaNetwork.DOMAIN_VISUAL_OFF);
            }
            return true;
        });
    }

    private static void applyPrecastSlow(ServerLevel server, ServerPlayer owner, ChargingDomain charging) {
        Vec3 center = owner.position();
        AABB search = new AABB(center.x - PRECAST_RADIUS, center.y - PRECAST_RADIUS,
                center.z - PRECAST_RADIUS, center.x + PRECAST_RADIUS,
                center.y + PRECAST_RADIUS, center.z + PRECAST_RADIUS);
        double radiusSquared = PRECAST_RADIUS * PRECAST_RADIUS;
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive())) {
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            if (dx * dx + dz * dz > radiusSquared) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 6, false, false, false));
            target.setDeltaMovement(target.getDeltaMovement().scale(0.01D));
            charging.slowedEntities.add(target.getUUID());
        }
    }

    private static void clearPrecastSlow(ServerLevel server, ServerPlayer owner, ChargingDomain charging) {
        if (charging.slowedEntities.isEmpty()) {
            return;
        }
        Vec3 center = owner.position();
        AABB search = new AABB(center.x - PRECAST_RADIUS, center.y - PRECAST_RADIUS,
                center.z - PRECAST_RADIUS, center.x + PRECAST_RADIUS,
                center.y + PRECAST_RADIUS, center.z + PRECAST_RADIUS);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, search,
                entity -> charging.slowedEntities.contains(entity.getUUID()))) {
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        charging.slowedEntities.clear();
    }

    /** 领域内的必中伤害：对半球体内、界主之外的一切活物持续结算。 */
    private static void damageEntities(ServerLevel server, ServerPlayer owner, Vec3 center) {
        AABB search = new AABB(center.x - RADIUS, center.y - RADIUS, center.z - RADIUS,
                center.x + RADIUS, center.y + RADIUS, center.z + RADIUS);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive()
                        && !(entity instanceof Player player && player.getAbilities().instabuild))) {
            if (!inDome(center, target)) {
                continue;
            }
            double heldDamage = Math.max(2.0D, owner.getAttributeValue(Attributes.ATTACK_DAMAGE));
            float damage = (float) (heldDamage * DAMAGE_MULTIPLIER);
            target.invulnerableTime = 0;
            target.hurtTime = 0;
            if (target.hurt(server.damageSources().playerAttack(owner), damage)) {
                target.invulnerableTime = 0;
                ItemStack held = owner.getMainHandItem();
                int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, held);
                if (fire > 0) {
                    target.setSecondsOnFire(fire * 4);
                }
            }
        }
    }

    /** 半球判定：球心锁定展开坐标，只取 caster 脚下平面以上的半球（dy ≥ -0.5 容忍半格高差）。 */
    static boolean inDome(Vec3 center, Entity target) {
        double dx = target.getX() - center.x;
        double dy = target.getY() - center.y;
        double dz = target.getZ() - center.z;
        return dy >= -0.5D && dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS;
    }

    /** 每 2 tick 一波幻影斩：落点以神龛为锚，全部收在半径 32 的半球体内，三种样式随机，面片对准界主。 */
    private static void spawnSlashStorm(ServerLevel server, ActiveDomain domain) {
        Entity owner = server.getEntity(domain.ownerEntityId);
        Vec3 viewerEye = owner != null ? owner.getEyePosition() : domain.center.add(0.0D, 1.5D, 0.0D);
        Vec3 shrinePos = domain.shrine != null && !domain.shrine.isRemoved()
                ? domain.shrine.position() : domain.center;
        double innerRadiusSquared = SHRINE_INNER_RADIUS * SHRINE_INNER_RADIUS;
        for (int i = 0; i < SLASHES_PER_STORM; i++) {
            double x;
            double z;
            double angle;
            double distance;
            if (server.getRandom().nextDouble() < SHRINE_NEAR_CHANCE) {
                angle = server.getRandom().nextDouble() * Math.PI * 2.0D;
                distance = Math.sqrt(server.getRandom().nextDouble()) * SHRINE_INNER_RADIUS;
                x = shrinePos.x + Math.cos(angle) * distance;
                z = shrinePos.z + Math.sin(angle) * distance;
            } else {
                angle = server.getRandom().nextDouble() * Math.PI * 2.0D;
                distance = Math.sqrt(server.getRandom().nextDouble()) * (RADIUS - 4.0D);
                x = domain.center.x + Math.cos(angle) * distance;
                z = domain.center.z + Math.sin(angle) * distance;
            }
            // 半球内取 Y：底平面 = 展开坐标的脚下高度，向上到球面为止；
            // 神龛水平范围内有较高概率贴神龛高度（保留原版"从神龛喷出斩击"的观感）。
            double dx = x - domain.center.x;
            double dz = z - domain.center.z;
            double planarSq = dx * dx + dz * dz;
            double maxAbove = Math.sqrt(Math.max(1.0D, RADIUS * RADIUS - planarSq));
            double y;
            if (planarSq < innerRadiusSquared
                    && server.getRandom().nextDouble() < STORM_SHRINE_HEIGHT_CHANCE) {
                y = shrinePos.y;
            } else {
                y = domain.center.y + server.getRandom().nextDouble()
                        * Math.min(maxAbove, STORM_ABOVE_RANGE);
            }
            y = Mth.clamp(y, domain.center.y, domain.center.y + maxAbove);
            y = Mth.clamp(y, server.getMinBuildHeight() + 1, server.getMaxBuildHeight() - 1);
            BlockPos probe = BlockPos.containing(x, y, z);
            if (!server.hasChunkAt(probe)) {
                continue;
            }
            int length = SLASH_STORM_MIN_LENGTH
                    + server.getRandom().nextInt(SLASH_STORM_MAX_LENGTH - SLASH_STORM_MIN_LENGTH + 1);
            DomainSlashEntity slash = new DomainSlashEntity(SoulKatanaEntities.DOMAIN_SLASH.get(), server);
            slash.setPos(x, y, z);
            slash.setVisualOnly(SLASH_STORM_LIFETIME_SECONDS, (float) length);
            slash.setSlashStyle(server.getRandom().nextInt(3));
            slash.setSlashAngle(server.getRandom().nextFloat() * (float) Math.PI * 2.0F);
            slash.faceViewer(viewerEye);
            server.addFreshEntity(slash);
        }
    }

    /**
     * 演出音：播放一次并广播，返回真正收到这段音乐的玩家名单——
     * 半径用 SoundEvent#getRange，与服务端内部广播判定一致。
     */
    private static Set<UUID> playDomainSound(ServerLevel server, Vec3 pos, SoundEvent sound, float volume) {
        server.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, 1.0F);
        Set<UUID> heard = new HashSet<>();
        double rangeSquared = (double) sound.getRange(volume) * sound.getRange(volume);
        for (ServerPlayer target : server.players()) {
            if (target.distanceToSqr(pos.x, pos.y, pos.z) <= rangeSquared) {
                heard.add(target.getUUID());
            }
        }
        return heard;
    }

    /**
     * 停播音乐：按名单补发 ClientboundStopSoundPacket（全发会误伤别的领域）。
     * 一个领域的 musicNotified 名单是共享的（前置音+展开音都往里加），
     * 所以支持一次传多个音效逐个发停音包，最后统一清空名单。
     */
    private static void stopDomainMusic(ServerLevel server, Set<UUID> listeners, SoundEvent... sounds) {
        if (listeners.isEmpty()) {
            return;
        }
        for (SoundEvent sound : sounds) {
            ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(sound.getLocation(), SoundSource.PLAYERS);
            for (UUID listener : listeners) {
                ServerPlayer target = server.getServer().getPlayerList().getPlayer(listener);
                if (target != null) {
                    target.connection.send(stop);
                }
            }
        }
        listeners.clear();
    }

    /**
     * 半球边界：底圈 + 8 条经线（自底圈升到顶点）。
     * 普通粒子广播只达 32 格，故界主另走逐玩家通道，其他人贴边时也能看到。
     */
    private static void sendBoundary(ServerLevel server, Vec3 center, ServerPlayer owner) {
        double ringY = center.y + 0.1D;
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = i * Math.PI * 2.0D / RING_POINTS;
            double x = center.x + Math.cos(angle) * RADIUS;
            double z = center.z + Math.sin(angle) * RADIUS;
            ringParticle(server, owner, x, ringY, z);
        }
        for (int m = 0; m < MERIDIAN_COUNT; m++) {
            double phi = m * Math.PI * 2.0D / MERIDIAN_COUNT;
            for (int k = 1; k <= MERIDIAN_STEPS; k++) {
                double theta = k * (Math.PI * 0.5D) / (MERIDIAN_STEPS + 1);
                double horizontal = Math.cos(theta) * RADIUS;
                double x = center.x + Math.cos(phi) * horizontal;
                double z = center.z + Math.sin(phi) * horizontal;
                double y = center.y + Math.sin(theta) * RADIUS;
                ringParticle(server, owner, x, y, z);
            }
        }
    }

    private static void ringParticle(ServerLevel server, ServerPlayer owner, double x, double y, double z) {
        server.sendParticles(DOMAIN_RING, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        if (owner != null) {
            server.sendParticles(owner, DOMAIN_RING, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 吟唱闸门阶段：malevolent_shrine.ogg 完整播完前挂在这里，播完才转 hold()。 */
    private static final class IncantingDomain {
        private final long startedAt;
        private final Set<UUID> musicNotified = new HashSet<>();

        private IncantingDomain(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    private static final class ChargingDomain {
        private final long startedAt;
        private final Set<UUID> slowedEntities = new HashSet<>();
        private final Set<UUID> musicNotified = new HashSet<>();

        private ChargingDomain(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    private static final class ActiveDomain {
        private final ServerLevel level;
        private final Vec3 center;
        private final int ownerEntityId;
        private ShrineEntity shrine;
        private final Set<UUID> visualNotified = new HashSet<>();
        private final Set<UUID> musicNotified = new HashSet<>();

        private ActiveDomain(ServerLevel level, Vec3 center, int ownerEntityId) {
            this.level = level;
            this.center = center;
            this.ownerEntityId = ownerEntityId;
        }

        private void spawnShrine(Vec3 position, float yaw) {
            shrine = new ShrineEntity(SoulKatanaEntities.SHRINE.get(), level);
            shrine.setPos(position.x, position.y, position.z);
            // 嘴部在模型 -Z（实体朝向）面：yaw 与界主一致即面向界主背部
            shrine.setYRot(yaw);
            shrine.setXRot(0.0F);
            LIVE_SHRINES.add(shrine);
            level.addFreshEntity(shrine);
        }

        private void removeShrine() {
            if (shrine != null) {
                LIVE_SHRINES.remove(shrine);
                shrine.discard();
                shrine = null;
            }
        }
    }
}
