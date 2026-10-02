package cn.blockforge.generated.soulkatana.integration;

import cn.blockforge.generated.soulkatana.SoulKatana;
import cn.blockforge.generated.soulkatana.SoulKatanaConfig;
import cn.blockforge.generated.soulkatana.SoulKatanaIdentity;
import cn.blockforge.generated.soulkatana.skill.SoulSkill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [无为转变]（魂刀·真人 的右键技能）。
 *
 * 规则（魂刀_真人.txt + 平衡追加）：
 *  - 主手持「魂刀·真人」右键击中生物：
 *      · 装载 Identity 且审核钩子挂接成功 → 弹出 Identity 自带的化身选择界面，
 *        玩家选中的化身被施加到"被击中的实体"上（借道 IdentitySwapCallback，见 IdentityInterop）；
 *        单人 / 局域网 / 专用服务器一律走这条路径。
 *      · 未装载 Identity（或钩子挂接失败）→ 被击中的实体随机变成一种生物（boss 类除外）。
 *  - 对 boss 与傀儡（铁傀儡/雪傀儡/铜傀儡及各模组自称 golem 的傀儡）不生效。
 *  - **平衡：每个实体一生只能被转变一次。**转变成功后新实体身上打持久标记
 *    （ForgeData，随存档保存），再对着它（或其转变产物）右键会被拒绝。
 *    取消选择/目标消失不算用过，仍可对原实体再施。
 *  - 与宿傩刀共用 2 秒特殊攻击冷却；冷却只锁特殊攻击，不影响普通挥斩。
 *
 * 本类只依赖原版类与 Forge 实体持久数据，Identity 缺席时随机转变路径照常工作。
 */
public final class IdleTransformation {
    private static final Logger LOGGER = LoggerFactory.getLogger("SoulKatana|IdleTransformation");

    /** 选择挂起有效期：60 秒（超时后玩家再点化身界面按普通变身处理）。 */
    private static final long PENDING_TTL_TICKS = 20L * 60L;

    /** 实体"已被无为转变过"的持久标记键（写在实体的 ForgeData 里，随存档保存）。 */
    private static final String REFORMED_KEY = "SoulkatanaReformed";

    private record Pending(UUID targetId, long expiresAtGameTime) { }

    /** 玩家 UUID → 等待化身选择的被击中实体。 */
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /** 随机转变候选池（进入世界时重建；首次使用时兜底构建）。 */
    private static volatile List<EntityType<?>> randomPool;

    /**
     * 玩家进入世界时重建候选池：遍历【当前注册表里所有已注册生物】
     * （含各模组后加的实体），保证模组生物都能被[无为转变]顾及。
     * 只在服务端线程调用（PlayerLoggedInEvent）。
     */
    public static void rebuildPool(ServerLevel server) {
        if (server == null) {
            return;
        }
        randomPool = buildPool(server);
    }

    private IdleTransformation() { }

    /**
     * 右键击中实体入口。返回 true 表示这次交互已被本技能占用（需要取消原交互）。
     */
    public static boolean tryCast(ServerPlayer player, Entity hit) {
        if (!(hit instanceof LivingEntity target) || target instanceof Player || !target.isAlive()) {
            return false;
        }
        // 【闸门】未持「魂刀·真人」→ 整个[无为转变]作废：不弹选择界面、不挂起、不扣冷却、静默无提示。
        // 注意：这里先于 isTransformed/isImmune/冷却 判断，确保没刀时连任何提示都不出现。
        if (SoulKatanaConfig.identityRequireSoulBlade()
                && !SoulKatanaIdentity.isRealPerson(player.getMainHandItem())) {
            return true; // 已占用本次右键，避免主模组顺手蓄力
        }
        if (isTransformed(target)) {
            // 平衡：一个实体一生只能被无为转变一次（取消选择过的不算）
            player.displayClientMessage(Component.translatable("message.soulkatana.idle_reformed"), true);
            return true;
        }
        if (isImmune(target)) {
            player.displayClientMessage(Component.translatable("message.soulkatana.idle_immune"), true);
            return true; // 不生效，但也不让主模组顺手蓄力
        }
        if (SoulSkill.isOnCooldown(player)) {
            long remain = SoulSkill.cooldownRemainingTicks(player);
            player.displayClientMessage(Component.translatable("message.soulkatana.cooldown",
                    String.format(java.util.Locale.ROOT, "%.1f", remain / 20.0D)), true);
            return true;
        }
        SoulSkill.startCooldown(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel server = player.serverLevel();
        server.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                6, 0.25D, 0.35D, 0.25D, 0.0D);

        // Identity 联动可用 → 走"选择已解锁化身"界面（单人/局域网/专用服务器一致）；
        // 未装 Identity 或钩子挂接失败 → 随机转变兜底
        if (IdentityInterop.isAvailable()) {
            PENDING.put(player.getUUID(),
                    new Pending(target.getUUID(), server.getGameTime() + PENDING_TTL_TICKS));
            IdentityInterop.openIdentityMenu(player);
            player.displayClientMessage(Component.translatable("message.soulkatana.idle_select"), true);
        } else {
            EntityType<?> type = randomLivingType(server);
            if (type == null) {
                LOGGER.warn("[魂刀] 随机转变候选池为空，放弃本次[无为转变]。");
                return true;
            }
            Entity fresh = createQuietly(type, server);
            if (fresh instanceof LivingEntity living) {
                transformTo(server, target, living);
                player.displayClientMessage(Component.translatable("message.soulkatana.idle_done"), true);
            }
        }
        return true;
    }

    /** 该玩家是否有挂起的化身选择（IdentityInterop 在 swap 回调里询问）。 */
    public static boolean hasPending(ServerPlayer player) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return false;
        }
        if (player.serverLevel().getGameTime() > pending.expiresAtGameTime()) {
            PENDING.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void clearPending(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    /**
     * 玩家在 Identity 化身选择界面点中了某个化身：swap 回调把已构造好的化身实例
     * （identity 的 IdentityType.create 产物，含变体数据）转交到这里，施加给被击中的实体。
     */
    public static void applyPending(ServerPlayer caster, LivingEntity formEntity) {
        Pending pending = PENDING.remove(caster.getUUID());
        if (pending == null) {
            return;
        }
        ServerLevel server = caster.serverLevel();
        if (server.getGameTime() > pending.expiresAtGameTime()) {
            return;
        }
        Entity raw = server.getEntity(pending.targetId());
        if (raw instanceof LivingEntity target && target.isAlive() && !isImmune(target)) {
            if (isTransformed(target)) {
                caster.displayClientMessage(Component.translatable("message.soulkatana.idle_reformed"), true);
                return;
            }
            transformTo(server, target, formEntity);
            caster.displayClientMessage(Component.translatable("message.soulkatana.idle_done"), true);
        } else {
            caster.displayClientMessage(Component.translatable("message.soulkatana.idle_lost"), true);
        }
    }

    // ------------------------------------------------------------------
    // 转变执行
    // ------------------------------------------------------------------

    /** 用 fresh 顶替 old：同位置同朝向，保留自定义名，爆出灵魂粒子，并打"一生一次"标记。 */
    private static void transformTo(ServerLevel server, LivingEntity old, Entity fresh) {
        fresh.setPos(old.getX(), old.getY(), old.getZ());
        fresh.setXRot(old.getXRot());
        fresh.setYRot(old.getYRot());
        if (old.hasCustomName()) {
            fresh.setCustomName(old.getCustomName());
            fresh.setCustomNameVisible(old.isCustomNameVisible());
        }
        markTransformed(fresh);
        server.sendParticles(ParticleTypes.SOUL,
                old.getX(), old.getY() + old.getBbHeight() * 0.5D, old.getZ(),
                14, 0.3D, 0.4D, 0.3D, 0.05D);
        old.discard();
        server.addFreshEntity(fresh);
    }

    /** 该实体是否已被无为转变塑造过（含其转变产物；标记写在实体 NBT，随存档保存）。 */
    public static boolean isTransformed(Entity entity) {
        return entity.getPersistentData().getBoolean(REFORMED_KEY);
    }

    private static void markTransformed(Entity entity) {
        entity.getPersistentData().putBoolean(REFORMED_KEY, true);
    }

    /** boss 与傀儡免疫（玩家与未死亡生物的判断在调用方做）。 */
    private static boolean isImmune(Entity entity) {
        return isBoss(entity) || isGolem(entity);
    }

    /**
     * boss 类生物：原版末影龙/凋灵/远古守卫者，以及"注册在 boss 包下 / id 含 boss"的模组生物。
     * （原版 Warden 不算 boss，按文档可被转变。）
     */
    private static boolean isBoss(Entity entity) {
        if (entity instanceof EnderDragon || entity instanceof WitherBoss
                || entity instanceof ElderGuardian) {
            return true;
        }
        if (entity.getClass().getName().contains(".boss.")) {
            return true;
        }
        ResourceLocation id = id(entity.getType());
        return id != null && id.getPath().contains("boss");
    }

    /** 傀儡：铁/雪傀儡（AbstractGolem 子类）、铜傀儡等注册名或类名带 golem 的模组傀儡。 */
    private static boolean isGolem(Entity entity) {
        if (entity instanceof AbstractGolem) {
            return true;
        }
        if (entity.getClass().getSimpleName().contains("Golem")) {
            return true;
        }
        ResourceLocation id = id(entity.getType());
        return id != null && id.getPath().contains("golem");
    }

    private static ResourceLocation id(EntityType<?> type) {
        return ForgeRegistries.ENTITY_TYPES.getKey(type);
    }

    /** 随机候选池：所有能实例化的生物（排除玩家、boss、傀儡与 MISC 类投射物）。 */
    private static EntityType<?> randomLivingType(ServerLevel server) {
        List<EntityType<?>> pool = randomPool;
        if (pool == null) {
            synchronized (IdleTransformation.class) {
                pool = randomPool;
                if (pool == null) {
                    pool = buildPool(server);
                    randomPool = pool;
                }
            }
        }
        return pool.isEmpty() ? null : pool.get(server.getRandom().nextInt(pool.size()));
    }

    private static List<EntityType<?>> buildPool(ServerLevel server) {
        List<EntityType<?>> pool = new ArrayList<>();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            if (type == EntityType.PLAYER || type.getCategory() == MobCategory.MISC) {
                continue;
            }
            ResourceLocation id = id(type);
            if (id != null && SoulKatana.MODID.equals(id.getNamespace())) {
                continue; // 本模组的技能实体不参与
            }
            Entity probe = createQuietly(type, server);
            if (probe instanceof LivingEntity living
                    && !(living instanceof Player) && !isImmune(living)) {
                pool.add(type);
            }
        }
        LOGGER.info("[魂刀] [无为转变] 随机候选生物 {} 种。", pool.size());
        return List.copyOf(pool);
    }

    /** create() 对个别模组实体可能抛异常，逐个吞掉，保证池子构建不中断。 */
    private static Entity createQuietly(EntityType<?> type, ServerLevel server) {
        try {
            return type.create(server);
        } catch (Throwable expectedForWeirdTypes) {
            return null;
        }
    }
}
