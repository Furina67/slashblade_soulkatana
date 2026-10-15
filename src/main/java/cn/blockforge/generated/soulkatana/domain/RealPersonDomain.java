package cn.blockforge.generated.soulkatana.domain;

import cn.blockforge.generated.soulkatana.SoulKatana;
import cn.blockforge.generated.soulkatana.SoulKatanaBlocks;
import cn.blockforge.generated.soulkatana.SoulKatanaEffects;
import cn.blockforge.generated.soulkatana.SoulKatanaIdentity;
import cn.blockforge.generated.soulkatana.SoulKatanaSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * [领域展开 · 自闭圆顿裹]（服务端·封闭式领域）：主手持「魂刀·真人」时按鼠标中键触发。
 *
 * <p><b>一、领域效果（保持原样，未改动）</b>
 * <ol>
 *   <li>台词闸门：完整播完 self_embodiment_of_perfection_voiceline（服务端解析 OGG 真实时长，
 *       实测约 150 tick），播完的当 tick 才展开；收刀/死亡/下线停音中止；</li>
 *   <li>展开后对半球内除界主外的一切活物挂 [触及灵魂]，界主每次发起攻击（含空挥）对全领域内
 *       所有 [触及灵魂] 目标各结算一次 dealDomainHit；</li>
 *   <li>再按一次中键关闭，界主死亡/换维度/下线自动收尾，关闭后 30 秒共用冷却。</li>
 * </ol>
 *
 * <p><b>二、结界实现（本轮按需求文档重写）</b>
 * <ol>
 *   <li>以展开者脚下坐标为球心，涟漪状向外替换方块，速度 5 方块/s（每 4 tick 一层）：
 *       先在脚下平面铺出半径 16 的圆，再向上包成半径 16 的空心半球（{@code DOMAIN_BARRIER}，
 *       纯黑 #000000FF、整格碰撞）；该半球同时就是领域效果的触发区；</li>
 *   <li>随后填充半径 15 的光明方块半球（dy ≥ 1），用原版调试方块「光明方块」
 *       {@code minecraft:light}（{@code Blocks.LIGHT}，默认亮度 15、无形状、不可见、不可破坏）。
 *       球心平面（dy = 0）那一层不铺光明方块：它必须留给有碰撞的结界方块当实体地板，
 *       否则无碰撞的光明方块会把地板顶掉，域内实体直接从脚下漏出结界；</li>
 *   <li><b>物理快照</b>：被替换的方块绝不进内存，而是搬到「建筑高度上限 - 16」的 Y=303 平面
 *       一带做物理存储；Y=303 被占就向上/向下找空位，内存只留「原坐标 -&gt; 存储坐标」映射表
 *       （{@link DomainSnapshot} 落盘，服务端崩溃后下次启动自动还原）；</li>
 *   <li>领域关闭时按生成的反方向逐层还原：先清该层的结界/光明方块，再把存储区的原方块搬回原位；</li>
 *   <li>展开期间强制加载领域与存储区的区块（{@link ForgeChunkManager}），并拦截
 *       破坏/放置/爆炸（见 {@link DomainProtection}）。</li>
 * </ol>
 *
 * <p>为什么内部填充刻意用原版 {@code minecraft:light}：半径 15 的实心球若带碰撞箱，界主会被
 * 直接封死在球体内（窒息且无法移动），领域就没法打了。原版光明方块本身没有形状、渲染不可见、
 * 不窒息，正好只负责把球体空间点亮，战斗照常进行；同时它是原版调试方块，无需重复注册。
 * 真正负责"关人"的是外壳 {@link SoulKatanaBlocks#DOMAIN_BARRIER}：它已注册整格碰撞，
 * 脚下那层圆盘也保留为结界方块，整个半球是密封的实体壳。
 */
@Mod.EventBusSubscriber(modid = "soulkatana")
public final class RealPersonDomain {

    /** 结界半径（空心半球，同时也是领域效果触发区半径）：文档锁定 16。 */
    public static final int BARRIER_RADIUS = 16;
    /** 光明球半径：文档锁定 15（填充体是原版 minecraft:light 方块）。 */
    public static final int LIGHT_RADIUS = 15;
    /** 兼容旧引用。 */
    public static final int RADIUS_BLOCKS = BARRIER_RADIUS;
    /** 领域效果触发半径 = 结界半径（文档：结界同时是触发区）。 */
    private static final double EFFECT_RADIUS = BARRIER_RADIUS;
    /** 发音时长（tick），0 = 未解析。 */
    private static int voicelineTicks;
    /** 解析失败兜底：台词实测 7.499s × 20 = 150 tick。 */
    private static final int VOICELINE_FALLBACK_TICKS = 150;
    /** 兼容旧引用；实际冷却时长与写入逻辑统一在 {@link SoulDomain}（两把刀共享一条领域冷却）。 */
    public static final int DOMAIN_COOLDOWN_TICKS = SoulDomain.DOMAIN_COOLDOWN_TICKS;
    /** [触及灵魂] 维持时长：界主每 tick 续一次，领域收尾时显式摘除。 */
    private static final int SOUL_TOUCH_MAINTAIN_TICKS = 40;
    /** 边界环约每 3 格一个粒子（贴着结界底圈）。 */
    private static final int RING_POINTS = (int) Math.ceil(2.0D * Math.PI * BARRIER_RADIUS / 3.0D);
    /** 暗红 0xA01010 的尘粒子。 */
    private static final DustParticleOptions DOMAIN_RING = new DustParticleOptions(
            new Vector3f(0xA0 / 255.0F, 0x10 / 255.0F, 0x10 / 255.0F), 0.2F);

    // ------------------------------------------------------------------ 结界几何 / 调度
    /** 每 4 tick 推进一层 = 5 方块/s。 */
    private static final int STEP_TICKS = 4;
    /** 第一阶段：脚下平面的圆，r = 0..16，共 17 层。 */
    private static final int DISC_STEPS = BARRIER_RADIUS + 1;
    /** 第二阶段：空心半球，dy = 1..16，共 16 层。 */
    private static final int SHELL_STEPS = BARRIER_RADIUS;
    /** 第三阶段的起始层号。 */
    private static final int LIGHT_START_STEP = DISC_STEPS + SHELL_STEPS;
    /** 第三阶段：实心光明球，r = 0..15，共 16 层。 */
    private static final int LIGHT_STEPS = LIGHT_RADIUS + 1;
    private static final int TOTAL_STEPS = LIGHT_START_STEP + LIGHT_STEPS;
    /** 关闭时每 tick 还原的层数（限流，避免 1.6 万方块一次性回填造成卡顿）。 */
    private static final int CLOSE_STEPS_PER_TICK = 4;
    /** 存储区向下搜寻的深度上限。 */
    private static final int STORAGE_BELOW_LIMIT = 96;

    private static final Map<UUID, ActiveDomain> ACTIVE_DOMAINS = new HashMap<>();
    private static final Map<UUID, ChargingDomain> CHARGING_DOMAINS = new HashMap<>();
    /** 跨领域共用的存储槽位占用表（防止两个领域把方块叠进同一格）。 */
    private static final Set<BlockPos> GLOBAL_STORAGE = new HashSet<>();
    /** 防事件重入：全域结算进行中不再触发第二次结算（见 onOwnerAttack）。 */
    private static boolean resolving = false;

    private RealPersonDomain() { }

    // ==================================================================================
    // A. 触发 / 台词闸门
    // ==================================================================================

    /** 真人刀中键入口（SoulKatanaNetwork 按手持刀分流到这里）。 */
    public static void toggle(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive() || !isHoldingRealPerson(player)) {
            return;
        }
        UUID id = player.getUUID();
        ActiveDomain active = ACTIVE_DOMAINS.get(id);
        if (active != null) {
            close(player);
            return;
        }
        if (CHARGING_DOMAINS.containsKey(id) || SoulDomain.isDomainCoolingDown(player)) {
            return;
        }
        beginCharge(player);
    }

    public static boolean isHoldingRealPerson(Player player) {
        return SoulKatanaIdentity.isRealPerson(player.getMainHandItem());
    }

    /** 台词闸门开始：登记读条（时长 = 台词 OGG 完整时长），并在界主位置播台词。 */
    private static void beginCharge(ServerPlayer player) {
        ServerLevel server = (ServerLevel) player.level();
        ChargingDomain charging = new ChargingDomain(server.getGameTime());
        CHARGING_DOMAINS.put(player.getUUID(), charging);
        charging.musicNotified.addAll(playDomainSound(server, player.position(),
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get(), 1.6F));
    }

    /** self_embodiment_of_perfection_voiceline.ogg 播完所需 tick 数（服务端解析 OGG）。 */
    private static int voicelineDurationTicks() {
        if (voicelineTicks <= 0) {
            voicelineTicks = SoulDomain.readOggDurationTicks(
                    "/assets/soulkatana/sounds/self_embodiment_of_perfection_voiceline.ogg",
                    VOICELINE_FALLBACK_TICKS);
        }
        return voicelineTicks;
    }

    // ==================================================================================
    // B. 展开
    // ==================================================================================

    /** 展开领域（台词已完整播完的当 tick 调用）：登记领域、强载区块、建立物理快照索引。 */
    public static void cast(ServerPlayer player, Set<UUID> voicelineListeners) {
        ServerLevel server = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        ActiveDomain domain = new ActiveDomain(server, center, player.getId(), player.getUUID());
        ACTIVE_DOMAINS.put(player.getUUID(), domain);
        if (voicelineListeners != null) {
            domain.musicNotified.addAll(voicelineListeners);
        }
        forceChunks(server, center, player.getUUID(), true);
        domain.snapshotData = DomainSnapshot.get(server);
        domain.snapshot = domain.snapshotData.createEntry(player.getUUID(), center);
        server.sendParticles(ParticleTypes.ENCHANT, center.getX() + 0.5D, center.getY() + 1.0D,
                center.getZ() + 0.5D, 32, 1.0D, 1.0D, 1.0D, 0.2D);
        domain.musicNotified.addAll(playDomainSound(server, domain.centerVec,
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION.get(), 1.2F));
        maintainSoulTouch(domain, player);
        sendBoundary(server, center, player);
        player.displayClientMessage(Component.translatable("message.soulkatana.real_domain_started"), false);
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

    /**
     * 服务端启动：把上次崩溃留下的领域快照全部还原（地形搬回原位、结界/光明方块清空）。
     * 这是 Y=303 物理快照映射表落盘的唯一用途——正常运行中领域关闭会即时还原并删除记录。
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            DomainSnapshot snapshot = DomainSnapshot.get(level);
            if (snapshot.isEmpty()) {
                continue;
            }
            List<DomainSnapshot.Entry> pending = new ArrayList<>(snapshot.entries());
            for (DomainSnapshot.Entry entry : pending) {
                restoreFromSnapshot(level, entry);
            }
            snapshot.clear();
        }
    }

    /** 维护每个玩家的前台词闸门与领域生命周期。 */
    private static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        ServerLevel server = (ServerLevel) player.level();

        ChargingDomain charging = CHARGING_DOMAINS.get(id);
        if (charging != null) {
            if (!player.isAlive() || !isHoldingRealPerson(player)) {
                cancelCharging(player);
            } else if (server.getGameTime() - charging.startedAt >= voicelineDurationTicks()) {
                CHARGING_DOMAINS.remove(id);
                cast(player, charging.musicNotified);
            }
        }

        ActiveDomain domain = ACTIVE_DOMAINS.get(id);
        if (domain == null) {
            return;
        }
        // 界主死亡 / 换维度 / 换实体：不再有后续 tick 驱动的机会，立即同步还原
        if (domain.level != player.level() || !player.isAlive()
                || domain.ownerEntityId != player.getId()) {
            ACTIVE_DOMAINS.remove(id);
            evacuatePlayers(domain);
            restoreAll(domain);
            releaseSoulTouch(domain);
            stopDomainMusic(domain.level, domain.musicNotified,
                    SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION.get(),
                    SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get());
            return;
        }
        if (domain.closing) {
            advanceClose(domain);
            return;
        }
        // 兜底：结界外壳已注册碰撞、地板也是实体方块，正常情况下界主根本走不出去；
        // 只有传送、穿模等异常手段才会把人甩到圈外。真出了领域就立刻收尾，
        // 避免出现"人已在外、领域还挂着"的僵尸结界。
        if (player.distanceToSqr(domain.centerVec) > (double) (BARRIER_RADIUS * BARRIER_RADIUS)) {
            close(player);
            return;
        }
        if (!domain.finished && server.getGameTime() >= domain.nextStepTick) {
            advanceBuild(domain);
        }
        if (!domain.finished) {
            applyBuildSafety(domain);
        }
        maintainSoulTouch(domain, player);
        if (domain.finished && server.getGameTime() % 10L == 0L) {
            sendBoundary(server, domain.center, player);
        }
    }

    private static void cancelCharging(ServerPlayer player) {
        ChargingDomain charging = CHARGING_DOMAINS.remove(player.getUUID());
        if (charging != null && player.level() instanceof ServerLevel server) {
            stopDomainMusic(server, charging.musicNotified,
                    SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get());
        }
    }

    /** 中键关闭：进入反向还原流程（每 tick 限流还原若干层），冷却与停音立即生效。 */
    private static void close(ServerPlayer player) {
        cancelCharging(player);
        ActiveDomain existing = ACTIVE_DOMAINS.get(player.getUUID());
        if (existing == null || existing.closing) {
            return;
        }
        existing.closing = true;
        existing.closingStep = existing.progress - 1;
        evacuatePlayers(existing);
        releaseSoulTouch(existing);
        stopDomainMusic(existing.level, existing.musicNotified,
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION.get(),
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get());
        SoulDomain.startDomainCooldown(player, existing.level.getGameTime());
    }

    /** 界主下线的收尾：不依赖后续 tick，直接把还没还原的层全部同步还原。 */
    private static void onOwnerDisconnect(ServerPlayer player) {
        cancelCharging(player);
        ActiveDomain domain = ACTIVE_DOMAINS.remove(player.getUUID());
        if (domain == null) {
            return;
        }
        evacuatePlayers(domain);
        restoreAll(domain);
        releaseSoulTouch(domain);
        stopDomainMusic(domain.level, domain.musicNotified,
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION.get(),
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get());
    }

    // ==================================================================================
    // C. 结界搭建（涟漪 → 空心半球 → 实心光明球）
    // ==================================================================================

    private static void advanceBuild(ActiveDomain domain) {
        if (domain.progress >= TOTAL_STEPS) {
            domain.finished = true;
            return;
        }
        int step = domain.progress;
        BlockState target = step < LIGHT_START_STEP ? barrierState() : lightState();
        for (BlockPos pos : layerPositions(domain.center, step)) {
            placeDomainBlock(domain, pos, target);
        }
        domain.progress++;
        domain.nextStepTick = domain.level.getGameTime() + STEP_TICKS;
        if (domain.progress >= TOTAL_STEPS) {
            domain.finished = true;
        }
        // 映射表每推进 4 层落一次 dirty，崩溃时最多丢 4 层的索引（那几层的方块仍可被反查清掉）
        if (domain.snapshotData != null && domain.progress % 4 == 0) {
            domain.snapshotData.setDirty();
        }
    }

    /**
     * 把 pos 变成结界/光明方块；若原位是普通方块，先把它物理搬到 Y=303 存储区并登记映射。
     * 基岩一类不可破坏方块（destroySpeed &lt; 0）留在原地不搬，球体上留个小孔即可。
     */
    private static void placeDomainBlock(ActiveDomain domain, BlockPos pos, BlockState target) {
        ServerLevel level = domain.level;
        if (!level.hasChunkAt(pos)) {
            return;
        }
        BlockState old = level.getBlockState(pos);
        if (old.is(target.getBlock())) {
            domain.placed.add(pos.immutable());
            return;
        }
        if (!isDomainBlock(old) && !old.isAir()) {
            if (old.getDestroySpeed(level, pos) < 0.0F) {
                return;
            }
            BlockPos slot = findStorageSlot(domain, pos);
            if (slot == null) {
                return;
            }
            moveToStorage(domain, pos, slot, old);
        }
        level.setBlock(pos, target, Block.UPDATE_CLIENTS);
        domain.placed.add(pos.immutable());
    }

    /** 物理搬运：方块状态与方块实体 NBT 一并搬到存储槽（箱子内容不丢）。 */
    private static void moveToStorage(ActiveDomain domain, BlockPos origin, BlockPos slot, BlockState state) {
        ServerLevel level = domain.level;
        BlockEntity blockEntity = level.getBlockEntity(origin);
        CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;
        level.setBlock(slot, state, Block.UPDATE_CLIENTS);
        if (tag != null && !tag.isEmpty()) {
            BlockEntity moved = level.getBlockEntity(slot);
            if (moved != null) {
                moved.load(tag);
            }
        }
        BlockPos originKey = origin.immutable();
        BlockPos slotKey = slot.immutable();
        domain.moved.put(originKey, slotKey);
        domain.storageUsed.add(slotKey);
        GLOBAL_STORAGE.add(slotKey);
        if (domain.snapshot != null) {
            domain.snapshot.add(originKey, slotKey);
        }
    }

    /** Y=303 平面起找空位：先向上（304、305…），再向下（302、301…）；避开任何领域的球体。 */
    private static BlockPos findStorageSlot(ActiveDomain domain, BlockPos origin) {
        ServerLevel level = domain.level;
        int floorY = Math.max(storagePlane(level), domain.center.getY() + BARRIER_RADIUS + 2);
        int maxY = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), floorY, origin.getZ());
        for (int y = floorY; y <= maxY; y++) {
            cursor.setY(y);
            if (isFreeStorageSlot(domain, cursor)) {
                return cursor.immutable();
            }
        }
        int minY = Math.max(level.getMinBuildHeight(), floorY - STORAGE_BELOW_LIMIT);
        for (int y = floorY - 1; y >= minY; y--) {
            cursor.setY(y);
            if (isFreeStorageSlot(domain, cursor)) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static boolean isFreeStorageSlot(ActiveDomain domain, BlockPos pos) {
        if (domain.storageUsed.contains(pos) || GLOBAL_STORAGE.contains(pos)) {
            return false;
        }
        if (isInsideAnyDomain(pos)) {
            return false;
        }
        ServerLevel level = domain.level;
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    /** 存储平面 = 建筑高度上限 - 17（1.20.1 为 320-17 = Y=303，与文档一致）。 */
    private static int storagePlane(ServerLevel level) {
        return level.getMaxBuildHeight() - 17;
    }

    private static boolean isInsideAnyDomain(BlockPos pos) {
        for (ActiveDomain domain : ACTIVE_DOMAINS.values()) {
            double dx = pos.getX() + 0.5D - domain.centerVec.x;
            double dy = pos.getY() + 0.5D - domain.centerVec.y;
            double dz = pos.getZ() + 0.5D - domain.centerVec.z;
            if (dx * dx + dy * dy + dz * dz <= (BARRIER_RADIUS + 1.0) * (BARRIER_RADIUS + 1.0)) {
                return true;
            }
        }
        return false;
    }

    /** 展开期间给领域内活物挂缓降：脚下地形被搬空时会下坠，缓降保证不摔伤。 */
    private static void applyBuildSafety(ActiveDomain domain) {
        for (LivingEntity target : domain.level.getEntitiesOfClass(LivingEntity.class,
                domainAABB(domain, BARRIER_RADIUS), LivingEntity::isAlive)) {
            if (!inDome(domain.centerVec, target)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
        }
    }

    /**
     * 生成第 step 层要处理的坐标：
     * <ul>
     *   <li>[0, 17) 脚下平面的圆环，r = step；</li>
     *   <li>[17, 33) 空心半球，按高度 dy = 1..16 取球面（到球心距离四舍五入 = 16）；</li>
     *   <li>[33, 49) 光明半球（dy ≥ 1），按半径 r = 0..15 逐层填充；dy = 0 留空给实体地板。</li>
     * </ul>
     */
    private static List<BlockPos> layerPositions(BlockPos center, int step) {
        List<BlockPos> out = new ArrayList<>();
        if (step < DISC_STEPS) {
            int r = step;
            int rSq = r * r;
            int inner = (r - 1) * (r - 1);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int d = dx * dx + dz * dz;
                    if (d <= rSq && (r == 0 || d > inner)) {
                        out.add(center.offset(dx, 0, dz));
                    }
                }
            }
        } else if (step < LIGHT_START_STEP) {
            int dy = step - DISC_STEPS + 1;
            for (int dx = -BARRIER_RADIUS; dx <= BARRIER_RADIUS; dx++) {
                for (int dz = -BARRIER_RADIUS; dz <= BARRIER_RADIUS; dz++) {
                    int d = dx * dx + dy * dy + dz * dz;
                    if (Math.round(Math.sqrt((double) d)) == BARRIER_RADIUS) {
                        out.add(center.offset(dx, dy, dz));
                    }
                }
            }
        } else {
            int r = step - LIGHT_START_STEP;
            int rSq = r * r;
            int inner = (r - 1) * (r - 1);
            // dy 从 1 起：dy = 0 的圆盘保留为结界方块（有碰撞）当实体地板，
            // 光明方块没有碰撞箱，铺上去会让域内实体从脚下漏出结界。
            for (int dy = 1; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        int d = dx * dx + dy * dy + dz * dz;
                        if (d <= rSq && (r == 0 || d > inner)) {
                            out.add(center.offset(dx, dy, dz));
                        }
                    }
                }
            }
        }
        return out;
    }

    private static Set<BlockPos> layerSet(BlockPos center, int step) {
        return new HashSet<>(layerPositions(center, step));
    }

    // ==================================================================================
    // D. 关闭 / 还原
    // ==================================================================================

    /** 每 tick 限流还原若干层（反向：先撤光明球外壳，最后撤脚下的圆）。 */
    private static void advanceClose(ActiveDomain domain) {
        int budget = CLOSE_STEPS_PER_TICK;
        while (budget-- > 0 && domain.closingStep >= 0) {
            restoreLayer(domain, domain.closingStep--);
        }
        if (domain.closingStep < 0) {
            cleanupDomain(domain);
        }
    }

    /** 还原一层：先清该层的结界/光明方块，再把该层搬走的方块从存储区搬回原位。 */
    private static void restoreLayer(ActiveDomain domain, int step) {
        ServerLevel level = domain.level;
        Set<BlockPos> layer = layerSet(domain.center, step);
        for (BlockPos pos : layer) {
            BlockState state = level.getBlockState(pos);
            if (isDomainBlock(state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                domain.placed.remove(pos);
            }
        }
        if (domain.moved.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, BlockPos>> it = domain.moved.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, BlockPos> entry = it.next();
            if (!layer.contains(entry.getKey())) {
                continue;
            }
            restoreBlock(level, entry.getValue(), entry.getKey());
            GLOBAL_STORAGE.remove(entry.getValue());
            domain.storageUsed.remove(entry.getValue());
            it.remove();
        }
    }

    /** 立刻把剩余层一次性还原（死亡/换维度/下线时不能再等 tick）。 */
    private static void restoreAll(ActiveDomain domain) {
        while (domain.closingStep >= 0) {
            restoreLayer(domain, domain.closingStep--);
        }
        cleanupDomain(domain);
    }

    /** 收尾：兜底清干净残留的领域方块与映射，解除区块强载，删除存档索引。 */
    private static void cleanupDomain(ActiveDomain domain) {
        ServerLevel level = domain.level;
        for (BlockPos pos : new ArrayList<>(domain.placed)) {
            BlockState state = level.getBlockState(pos);
            if (isDomainBlock(state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        domain.placed.clear();
        for (Map.Entry<BlockPos, BlockPos> entry : domain.moved.entrySet()) {
            restoreBlock(level, entry.getValue(), entry.getKey());
            GLOBAL_STORAGE.remove(entry.getValue());
        }
        domain.moved.clear();
        domain.storageUsed.clear();
        forceChunks(level, domain.center, domain.owner, false);
        if (domain.snapshotData != null && domain.snapshot != null) {
            domain.snapshotData.removeEntry(domain.snapshot);
            domain.snapshot = null;
        }
        domain.finished = true;
        ACTIVE_DOMAINS.remove(domain.owner);
    }

    /** 单个方块从存储槽搬回原坐标（含方块实体 NBT）。 */
    private static void restoreBlock(ServerLevel level, BlockPos storage, BlockPos origin) {
        if (!level.hasChunkAt(storage) || !level.hasChunkAt(origin)) {
            return;
        }
        BlockState state = level.getBlockState(storage);
        if (state.isAir()) {
            return;
        }
        if (isDomainBlock(state)) {
            level.setBlock(storage, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(storage);
        CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;
        level.setBlock(origin, state, Block.UPDATE_CLIENTS);
        if (tag != null && !tag.isEmpty()) {
            BlockEntity restored = level.getBlockEntity(origin);
            if (restored != null) {
                restored.load(tag);
            }
        }
        level.setBlock(storage, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** 崩溃恢复：按存档里的映射表一次性还原。 */
    private static void restoreFromSnapshot(ServerLevel level, DomainSnapshot.Entry entry) {
        BlockPos center = entry.centerPos();
        forceChunks(level, center, entry.owner, true);
        for (BlockPos pos : geometryPositions(center)) {
            BlockState state = level.getBlockState(pos);
            if (isDomainBlock(state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (Map.Entry<BlockPos, BlockPos> moved : entry.movedMap().entrySet()) {
            restoreBlock(level, moved.getValue(), moved.getKey());
        }
        forceChunks(level, center, entry.owner, false);
    }

    /** 全领域几何（圆 + 半球 + 光明球），崩溃恢复时用来扫地式清除领域方块。 */
    private static List<BlockPos> geometryPositions(BlockPos center) {
        List<BlockPos> out = new ArrayList<>();
        for (int step = 0; step < TOTAL_STEPS; step++) {
            out.addAll(layerPositions(center, step));
        }
        return out;
    }

    /**
     * 关闭前把领域内的玩家抬回球心（原展开点），避免地形还原后把人埋进方块里。
     * 球心平面（dy = 0）是实体结界地板，所以落在它<b>上面</b>一格，人正好站在地板上；
     * 地板最后才还原，还原后脚下就是原本的地面。
     */
    private static void evacuatePlayers(ActiveDomain domain) {
        for (ServerPlayer target : domain.level.getEntitiesOfClass(ServerPlayer.class,
                domainAABB(domain, BARRIER_RADIUS + 3.0D), Player::isAlive)) {
            if (target.distanceToSqr(domain.centerVec)
                    <= (BARRIER_RADIUS + 3.0D) * (BARRIER_RADIUS + 3.0D)) {
                target.teleportTo(domain.center.getX() + 0.5D, domain.center.getY() + 1.0D,
                        domain.center.getZ() + 0.5D);
            }
        }
    }

    private static AABB domainAABB(ActiveDomain domain, double radius) {
        return new AABB(domain.centerVec.x - radius, domain.centerVec.y - radius,
                domain.centerVec.z - radius, domain.centerVec.x + radius,
                domain.centerVec.y + radius, domain.centerVec.z + radius);
    }

    // ==================================================================================
    // E. 领域效果层（保持原实现：触及灵魂 + 全域必中）
    // ==================================================================================

    /** 领域关闭/消散：显式摘掉所有目标身上的 [触及灵魂]。 */
    private static void releaseSoulTouch(ActiveDomain domain) {
        for (LivingEntity touched : domain.touched.values()) {
            if (touched != null && !touched.isRemoved()) {
                touched.removeEffect(SoulKatanaEffects.SOUL_TOUCH.get());
            }
        }
        domain.touched.clear();
    }

    /**
     * 半球扫描：给领域内（除界主）的活物挂/续 [触及灵魂]，出半球或死亡的即时摘除。
     * 每 tick 由 tick() 调用一次，纯标记效果、不产生任何视觉。
     */
    private static void maintainSoulTouch(ActiveDomain domain, ServerPlayer owner) {
        AABB search = domainAABB(domain, EFFECT_RADIUS);
        Map<UUID, LivingEntity> insideNow = new HashMap<>();
        for (LivingEntity target : domain.level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive())) {
            if (!inDome(domain.centerVec, target)) {
                continue;
            }
            insideNow.put(target.getUUID(), target);
            target.addEffect(new MobEffectInstance(SoulKatanaEffects.SOUL_TOUCH.get(),
                    SOUL_TOUCH_MAINTAIN_TICKS, 0, false, false, false));
        }
        Iterator<Map.Entry<UUID, LivingEntity>> it = domain.touched.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = it.next();
            if (insideNow.containsKey(entry.getKey())) {
                continue;
            }
            LivingEntity gone = entry.getValue();
            if (gone != null && !gone.isRemoved()) {
                gone.removeEffect(SoulKatanaEffects.SOUL_TOUCH.get());
            }
            it.remove();
        }
        domain.touched.putAll(insideNow);
    }

    /** 半球判定：dx²+dy²+dz² ≤ R² 且目标不低于球心平面（dy ≥ -0.5 容忍半格高差）。 */
    private static boolean inDome(Vec3 center, Entity target) {
        double dx = target.getX() - center.x;
        double dy = target.getY() - center.y;
        double dz = target.getZ() - center.z;
        return dy >= -0.5D && dx * dx + dy * dy + dz * dz <= EFFECT_RADIUS * EFFECT_RADIUS;
    }

    /**
     * 全域必中的总入口：界主"发起攻击"（含对空气/方块空挥）时对半球内所有
     * [触及灵魂] 实体各结算一次 dealDomainHit。
     */
    public static void onOwnerAttack(ServerPlayer owner) {
        onOwnerAttack(owner, null);
    }

    /** @param directHit 本击已由原版近战结算的实体（可为 null），全域结算时跳过防重复。 */
    public static void onOwnerAttack(ServerPlayer owner, Entity directHit) {
        if (resolving) {
            return;
        }
        ActiveDomain domain = ACTIVE_DOMAINS.get(owner.getUUID());
        if (domain == null || domain.closing || domain.level != owner.level()
                || !isHoldingRealPerson(owner)) {
            return;
        }
        // 同一次挥刀只结算一轮：空挥上报与实体命中事件同 tick 撞车时按 tick 去重
        if (domain.lastBurstTick == domain.level.getGameTime()) {
            return;
        }
        domain.lastBurstTick = domain.level.getGameTime();
        List<LivingEntity> targets = new ArrayList<>(domain.touched.values());
        resolving = true;
        try {
            for (LivingEntity target : targets) {
                if (target == directHit || target.isRemoved() || !target.isAlive()) {
                    continue;
                }
                if (!inDome(domain.centerVec, target)) {
                    continue;
                }
                dealDomainHit(owner, target);
            }
        } finally {
            resolving = false;
        }
        domain.touched.entrySet().removeIf(entry ->
                entry.getValue().isRemoved() || !entry.getValue().isAlive());
    }

    /** 左键命中实体时原版路径也会发起一次全域结算（directHit 不重复吃）。 */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        onOwnerAttack(player, event.getTarget());
    }

    /**
     * 必中结算：伤害公式逐项复刻原版 Player.attack()（与界主普通攻击打同一目标的结果一致）。
     * 强制扣血：先把 invulnerableTime/hurtTime 清零再走 hurt()，死亡动画/不死图腾/掉落全部保留；
     * 护甲/抗性/吸收的作废由事件层保证（SoulKatanaEvents 对 [触及灵魂] 目标还原真实伤害）。
     */
    public static void dealDomainHit(Player attacker, LivingEntity target) {
        ItemStack weapon = attacker.getMainHandItem();
        float base = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float enchantBonus = EnchantmentHelper.getDamageBonus(weapon, target.getMobType());
        float scale = attacker.getAttackStrengthScale(0.5F);
        base *= 0.2F + scale * scale * 0.8F;
        enchantBonus *= scale;
        boolean critical = scale > 0.9F && attacker.fallDistance > 0.0F && !attacker.onGround()
                && !attacker.onClimbable() && !attacker.isInWater()
                && !attacker.hasEffect(MobEffects.BLINDNESS) && !attacker.isPassenger()
                && !attacker.isSprinting();
        if (critical) {
            base *= 1.5F;
        }
        float damage = base + enchantBonus;
        if (damage <= 0.0F) {
            return;
        }
        DamageSource source = attacker.damageSources().playerAttack(attacker);
        if (target.isInvulnerableTo(source)) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        if (target.hurt(source, damage)) {
            target.invulnerableTime = 0;
            int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon);
            if (fire > 0) {
                target.setSecondsOnFire(fire * 4);
            }
        }
    }

    // ==================================================================================
    // F. 保护 / 区块强载 / 演出
    // ==================================================================================

    /** 领域保护判定：结界球体、已放置方块与 Y=303 存储槽位一律不可破坏/放置/被爆炸波及。 */
    public static boolean isProtected(Level level, BlockPos pos) {
        if (level == null) {
            return false;
        }
        for (ActiveDomain domain : ACTIVE_DOMAINS.values()) {
            if (domain.level != level) {
                continue;
            }
            if (domain.placed.contains(pos) || domain.storageUsed.contains(pos)) {
                return true;
            }
            double dx = pos.getX() + 0.5D - domain.centerVec.x;
            double dy = pos.getY() + 0.5D - domain.centerVec.y;
            double dz = pos.getZ() + 0.5D - domain.centerVec.z;
            if (dx * dx + dy * dy + dz * dz <= (BARRIER_RADIUS + 1.0) * (BARRIER_RADIUS + 1.0)) {
                return true;
            }
        }
        return false;
    }

    /** 强制加载领域与存储区所在的 3×3 区块，并立即同步载入（Y=303 写入前必须已加载）。 */
    private static void forceChunks(ServerLevel level, BlockPos center, UUID owner, boolean add) {
        int minCX = (center.getX() - BARRIER_RADIUS) >> 4;
        int maxCX = (center.getX() + BARRIER_RADIUS) >> 4;
        int minCZ = (center.getZ() - BARRIER_RADIUS) >> 4;
        int maxCZ = (center.getZ() + BARRIER_RADIUS) >> 4;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                ForgeChunkManager.forceChunk(level, SoulKatana.MODID, owner, cx, cz, add, true);
                if (add) {
                    level.getChunkAt(new BlockPos(cx << 4, center.getY(), cz << 4));
                }
            }
        }
    }

    /** 演出音：播放一次并广播，返回真正收到这段音乐的玩家名单（半径按 SoundEvent#getRange）。 */
    private static Set<UUID> playDomainSound(ServerLevel server, Vec3 pos, SoundEvent sound, float volume) {
        server.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, 1.0F);
        Set<UUID> heard = new HashSet<>();
        double range = (double) sound.getRange(volume);
        double rangeSquared = range * range;
        for (ServerPlayer target : server.players()) {
            if (target.distanceToSqr(pos.x, pos.y, pos.z) <= rangeSquared) {
                heard.add(target.getUUID());
            }
        }
        return heard;
    }

    /** 停播音乐：按名单补发 ClientboundStopSoundPacket（全发会误伤宿傩领域的演出音）。 */
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

    /** 边界环：贴着结界底圈画暗红尘粒子；普通粒子广播只达 32 格，界主另走逐玩家通道。 */
    private static void sendBoundary(ServerLevel server, BlockPos center, ServerPlayer owner) {
        double ringY = center.getY() + 0.1D;
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = i * Math.PI * 2.0D / RING_POINTS;
            double x = center.getX() + 0.5D + Math.cos(angle) * BARRIER_RADIUS;
            double z = center.getZ() + 0.5D + Math.sin(angle) * BARRIER_RADIUS;
            server.sendParticles(DOMAIN_RING, x, ringY, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (owner != null) {
                server.sendParticles(owner, DOMAIN_RING, true, x, ringY, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static BlockState barrierState() {
        return SoulKatanaBlocks.DOMAIN_BARRIER.get().defaultBlockState();
    }

    private static BlockState lightState() {
        // 原版调试方块「光明方块」：默认 LEVEL=15、无形状、不可见，直接拿来当实心球填充体
        return Blocks.LIGHT.defaultBlockState();
    }

    private static boolean isDomainBlock(BlockState state) {
        return state.is(SoulKatanaBlocks.DOMAIN_BARRIER.get()) || state.is(Blocks.LIGHT);
    }

    // ==================================================================================
    // G. 状态存储
    // ==================================================================================

    /** 台词闸门阶段。 */
    private static final class ChargingDomain {
        private final long startedAt;
        private final Set<UUID> musicNotified = new HashSet<>();

        private ChargingDomain(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    /** 领域登记：球心固定在展开瞬间界主脚下，维护结界/存储映射与 [触及灵魂] 名单。 */
    private static final class ActiveDomain {
        private final ServerLevel level;
        private final BlockPos center;
        private final Vec3 centerVec;
        private final int ownerEntityId;
        private final UUID owner;
        private final Set<UUID> musicNotified = new HashSet<>();
        /** 半球内挂了 [触及灵魂] 的目标（不含界主），每 tick 维护。 */
        private final Map<UUID, LivingEntity> touched = new HashMap<>();
        /** 本领域放置过的结界/光明方块坐标。 */
        private final Set<BlockPos> placed = new HashSet<>();
        /** 原坐标 -&gt; Y=303 存储坐标。 */
        private final Map<BlockPos, BlockPos> moved = new HashMap<>();
        /** 本领域占用的存储槽位。 */
        private final Set<BlockPos> storageUsed = new HashSet<>();
        private DomainSnapshot snapshotData;
        private DomainSnapshot.Entry snapshot;
        /** 已完成的层数（0..TOTAL_STEPS）。 */
        private int progress;
        private long nextStepTick;
        private boolean finished;
        /** 关闭还原：从 progress-1 往 0 逐层回退。 */
        private boolean closing;
        private int closingStep = -1;
        /** 上一轮全域必中结算的 tick（空挥上报与命中事件同 tick 去重）。 */
        private long lastBurstTick = Long.MIN_VALUE;

        private ActiveDomain(ServerLevel level, BlockPos center, int ownerEntityId, UUID owner) {
            this.level = level;
            this.center = center.immutable();
            this.centerVec = Vec3.atBottomCenterOf(center);
            this.ownerEntityId = ownerEntityId;
            this.owner = owner;
            this.nextStepTick = level.getGameTime();
        }
    }
}
