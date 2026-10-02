package cn.blockforge.generated.soulkatana.skill;

import cn.blockforge.generated.soulkatana.SoulKatanaEntities;
import cn.blockforge.generated.soulkatana.SoulKatanaSounds;
import cn.blockforge.generated.soulkatana.entity.CrescentSlashEntity;
import cn.blockforge.generated.soulkatana.entity.DomainSlashEntity;
import cn.blockforge.generated.soulkatana.SoulKatanaIdentity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 魂刀特殊攻击与[冷却]。
 *
 * [冷却]：施放任一特殊攻击（[解]/[捌]/[无为转变]）后 2 秒（40 tick）内，
 *         [解]/[捌]/[无为转变]无法放出；左右键的普通攻击完全不受影响。
 * [解]：放出白色月牙波动（CrescentSlashEntity），伤害为接触判定——
 *       月牙碰到实体即造成 8 点(4颗心)伤害，同一发对每个实体只结算一次；
 *       不再使用施放瞬间的"横3×竖2×长8"范围结算。
 * [捌]：右键朝向前方实体施放。首次对某实体施放，减少其最大生命 10%（进一法取整）；
 *       对该实体再次施放则造成 8 点(4颗心)近战伤害。
 *       生效时在目标身上爆出原版暴击粒子（白色星芒）作为确认信号。
 */
public final class SoulSkill {
    public static final int SKILL_COOLDOWN_TICKS = 40;
    private static final String COOLDOWN_KEY = "SoulbladeSkillCd";
    private static final String CLEAVE_MARKS_KEY = "SoulbladeCleaveMarks";
    /** [捌]"再次命中"分支的近战伤害：8 点（4 颗心）。[解] 的接触伤害见 CrescentSlashEntity。 */
    private static final float KANESADA_DAMAGE = 8.0F;
    /** [捌] 宽松锁定：最大距离与半锥角余弦（≈31°），准星大致的朝前目标即可命中。 */
    private static final double KANESADA_RANGE = 6.5D;
    private static final double KANESADA_CONE_COS = 0.85D;

    private SoulSkill() { }

    public static boolean isOnCooldown(Player player) {
        return player.getPersistentData().getLong(COOLDOWN_KEY) > player.level().getGameTime();
    }

    /** 剩余冷却 tick 数（不在冷却中返回 0）。 */
    public static long cooldownRemainingTicks(Player player) {
        long remain = player.getPersistentData().getLong(COOLDOWN_KEY) - player.level().getGameTime();
        return Math.max(0L, remain);
    }

    public static void startCooldown(Player player) {
        player.getPersistentData().putLong(COOLDOWN_KEY, player.level().getGameTime() + SKILL_COOLDOWN_TICKS);
    }

    public static boolean isHoldingSoulKatana(Player player) {
        return SoulKatanaIdentity.isSoulKatana(player.getMainHandItem());
    }

    /**
     * [解]：左键落空时由客户端上报，服务端放出月牙波动实体。
     * 伤害已改为接触判定（见 CrescentSlashEntity#sweepDamage），
     * 这里不再做"横3×竖2×长8"的瞬间范围结算。
     */
    public static void castTatayaku(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel server) || !player.isAlive()
                || isOnCooldown(player) || !isHoldingSoulKatana(player)) {
            return;
        }
        startCooldown(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoulKatanaSounds.SUKUNA_SLASH.get(), SoundSource.PLAYERS, 0.8F, 1.0F);

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        CrescentSlashEntity wave = new CrescentSlashEntity(
                SoulKatanaEntities.CRESCENT_SLASH.get(), server, player, look);
        wave.setPos(eye.x + look.x * 0.6D, eye.y + look.y * 0.6D, eye.z + look.z * 0.6D);
        server.addFreshEntity(wave);
        spawnWhiteCrescentParticles(server, player, look);
    }

    /**
     * 白色粒子月牙：沿视线方向在 1.5/3.5/5.5/7.5 格四个截面上，
     * 各布一道月牙弧（横三格宽），白雾尘粒子，配合月牙实体推进。
     * 注意 up = forward×right 实际指向世界下方，故"中间 y 大"渲染出来是中间低的 ◡ 弧，
     * 与 crescent.png 的月牙朝向一致。
     */
    private static void spawnWhiteCrescentParticles(ServerLevel server, ServerPlayer player, Vec3 look) {
        double lookLen = look.length();
        if (lookLen < 1.0E-4D) {
            return;
        }
        Vec3 forward = new Vec3(look.x / lookLen, look.y / lookLen, look.z / lookLen);
        // 屏幕右向量（水平）与局部"上"向量
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double rightLen = right.length();
        if (rightLen < 1.0E-4D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = new Vec3(right.x / rightLen, 0.0D, right.z / rightLen);
        }
        Vec3 up = forward.cross(right);
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        double[] slices = {1.5D, 3.5D, 5.5D, 7.5D};
        for (double d : slices) {
            Vec3 base = origin.add(forward.scale(d));
            for (int i = 0; i <= 8; i++) {
                double x = -1.4D + 0.35D * i;
                // 中间 y 大 + up 朝下 → 世界坐标中间低、两端翘的 ◡ 弧（与 crescent.png 同向）
                double y = 0.9D - 0.46D * (x / 1.4D) * (x / 1.4D);
                Vec3 pos = base.add(right.scale(x)).add(up.scale(y - 0.45D));
                server.sendParticles(WHITE_DUST, pos.x, pos.y, pos.z, 1,
                        0.06D, 0.06D, 0.06D, 0.0D);
            }
        }
    }

    private static final DustParticleOptions WHITE_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.15F);

    /**
     * [捌]：右键命中实体时施放。首次削 10% 最大生命（进一法），再次命中打 8 点近战。
     * 返回 true 表示已生效（并进入冷却）；找不到目标/冷却中/未持刀则不消耗冷却。
     */
    public static boolean castKanesada(ServerPlayer player) {
        return castKanesada(player, findKanesadaTarget(player));
    }

    /** [捌]（指定目标版）：客户端上报与服务端右键兜底共用，靠冷却天然去重。 */
    public static boolean castKanesada(ServerPlayer player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel server) || !player.isAlive()
                || isOnCooldown(player) || !isHoldingSoulKatana(player) || target == null) {
            return false;
        }
        startCooldown(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoulKatanaSounds.SUKUNA_SLASH.get(), SoundSource.PLAYERS, 0.8F, 0.9F);

        // 命中位置贴一发斩击帧（复用领域斩击实体，面向界主）
        DomainSlashEntity spark = new DomainSlashEntity(SoulKatanaEntities.DOMAIN_SLASH.get(), server);
        Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        spark.setPos(at.x, at.y, at.z);
        spark.setVisualOnly(0.18D, Math.max(1.6F, (float) target.getBbWidth() * 1.6F));
        spark.setSlashStyle(server.getRandom().nextInt(3));
        spark.setSlashAngle(server.getRandom().nextFloat() * (float) Math.PI * 2.0F);
        spark.faceViewer(player.getEyePosition());
        server.addFreshEntity(spark);

        // 生效确认：目标身上爆出原版暴击粒子（白色星芒）
        spawnCritBurst(server, target);

        if (hasCleaveMark(player, target)) {
            // 再次使用：8 点(4颗心)近战伤害
            target.hurt(player.damageSources().playerAttack(player), KANESADA_DAMAGE);
        } else {
            // 第一次使用：减少目标最大生命 10% 的血量（进一法取整）。
            // 只有真正见到掉血才登记"已被捌过"：旧版先登记后掉血，首刀若被吞，
            // 该实体就永远只剩 8 点分支——"减 10% 血量依然不生效"的第二个根因。
            float cut = (float) Math.ceil(target.getMaxHealth() * 0.1D);
            if (applyMaxHealthCut(player, target, cut)) {
                addCleaveMark(player, target);
            }
        }
        return true;
    }

    /** 原版暴击粒子（ParticleTypes.CRIT，白色星芒）：[捌] 生效的可见信号。 */
    private static void spawnCritBurst(ServerLevel server, LivingEntity target) {
        double w = Math.max(0.6D, target.getBbWidth());
        double h = target.getBbHeight();
        server.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + h * 0.5D, target.getZ(),
                18, w * 0.45D, h * 0.4D, w * 0.45D, 0.3D);
    }

    /**
     * [捌]首刀的"减 10% 血量"：先走正常 hurt 管线（红闪/音效/仇恨/击杀归属，
     * 间接魔法无视护甲）；若这一刀被格挡、减免或事件吞掉，直接按血量补足差额。
     * 1.20.1 的血量存在同步实体数据里（getHealth/setHealth 走 entityData），
     * 补刀会自动同步到客户端，血条立刻可见地少一截；保底留半心，致命一律交给 hurt 结算。
     * 返回 true 表示确实见血（掉满配额或目标因此死亡），此时才登记首刀标记。
     */
    private static boolean applyMaxHealthCut(ServerPlayer player, LivingEntity target, float cut) {
        DamageSource source = player.damageSources().indirectMagic(player, player);
        float before = target.getHealth();
        target.invulnerableTime = 0;
        target.hurt(source, cut);
        if (!target.isAlive()) {
            return true;
        }
        float want = Math.max(before - cut, Math.min(0.5F, before));
        if (target.getHealth() > want) {
            target.setHealth(want);
        }
        return before - target.getHealth() >= Math.min(cut, before) - 1.0E-3D;
    }

    /** 准星射线精确命中的活体目标（近战触及范围+3格；服务端与客户端通用）。 */
    public static LivingEntity pickLivingEntity(Player player) {
        double reach = player.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get()) + 3.0D;
        net.minecraft.world.phys.HitResult hit = player.pick(reach, 0.0F, false);
        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
            Entity entity = ((net.minecraft.world.phys.EntityHitResult) hit).getEntity();
            if (entity instanceof LivingEntity living && entity != player && entity.isAlive()) {
                return living;
            }
        }
        return null;
    }

    /**
     * [捌] 的目标锁定：先按准星精确射线取，取不到再退化为"面朝前方 6.5 格、
     * 半锥角约 31°"内最贴近视线的一条活体——只要准星大致朝着实体就能施放。
     * 上一版要求客户端与服务端两次精确射线都命中，任一刻的视角差都会静默失败，
     * 这正是 [捌] "依然不生效"的头号嫌疑。
     */
    public static LivingEntity findKanesadaTarget(ServerPlayer player) {
        LivingEntity exact = pickLivingEntity(player);
        if (exact != null) {
            return exact;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB search = player.getBoundingBox().inflate(4.0D)
                .move(look.scale(KANESADA_RANGE / 2.0D));
        LivingEntity best = null;
        double bestScore = -1.0D;
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 rel = entity.position().add(0.0D, entity.getBbHeight() / 2.0D, 0.0D).subtract(eye);
            double dist = rel.length();
            if (dist < 0.3D || dist > KANESADA_RANGE) {
                continue;
            }
            double cos = rel.scale(1.0D / dist).dot(look);
            if (cos < KANESADA_CONE_COS) {
                continue;
            }
            if (blockBlocksView(player.serverLevel(), eye, entity.position().add(0.0D,
                    entity.getBbHeight() / 2.0D, 0.0D), dist)) {
                continue; // 有墙挡着：右键的是方块，不是它背后的实体
            }
            double score = cos * 10.0D - dist;
            if (score > bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        return best;
    }

    /** 视线是否被方块截断（眼→目标中心连线中途撞到实体方块，且明显比目标近）。 */
    private static boolean blockBlocksView(ServerLevel server, Vec3 eye, Vec3 targetCenter, double dist) {
        net.minecraft.world.phys.BlockHitResult hit = server.clip(new net.minecraft.world.level.ClipContext(
                eye, targetCenter, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, null));
        return hit != null
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && hit.getLocation().distanceToSqr(eye) < (dist - 1.0D) * (dist - 1.0D);
    }

    /** 该实体是否已被此界主 [捌] 过（首刀标记，UUID 名单，随玩家持久化）。 */
    private static boolean hasCleaveMark(Player player, LivingEntity target) {
        ListTag marks = player.getPersistentData().getList(CLEAVE_MARKS_KEY, Tag.TAG_STRING);
        String id = target.getStringUUID();
        for (int i = 0; i < marks.size(); i++) {
            if (id.equals(marks.getString(i))) {
                return true;
            }
        }
        return false;
    }

    /** 登记首刀标记（仅在确认掉血后调用）。 */
    private static void addCleaveMark(Player player, LivingEntity target) {
        ListTag marks = player.getPersistentData().getList(CLEAVE_MARKS_KEY, Tag.TAG_STRING);
        marks.add(StringTag.valueOf(target.getStringUUID()));
        if (marks.size() > 512) {
            marks.remove(0);
        }
        player.getPersistentData().put(CLEAVE_MARKS_KEY, marks);
    }
}
