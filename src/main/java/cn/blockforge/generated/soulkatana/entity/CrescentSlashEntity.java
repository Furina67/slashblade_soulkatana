package cn.blockforge.generated.soulkatana.entity;

import cn.blockforge.generated.soulkatana.SoulKatanaEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * [解] 的白色月牙波动：以约每 tick 0.85 格的速度向玩家视线方向推进（较快），
 * 总长约 11 格后消散。
 * 伤害采用"接触判定"：推进过程中每 tick 对月牙扫掠带（当前渲染宽度 ±1.5 格）
 * 内碰到的实体造成 8 点(4颗心)伤害，同一发月牙对每个实体只结算一次。
 * 不再使用施放瞬间的范围结算。
 */
public class CrescentSlashEntity extends Projectile {
    /** 每 tick 推进 0.85 格，约 10 tick 推完 8 格射程，再多飞 3 格余量收尾。 */
    public static final double SPEED = 0.85D;
    public static final double MAX_DISTANCE = 11.0D;
    /** 接触判定半径：与渲染器 HALF_WIDTH(1.5) 对齐，看到月牙扫到即判定命中。 */
    public static final double HIT_RADIUS = 1.5D;
    /** 接触伤害：8 点（4 颗心）。 */
    public static final float CONTACT_DAMAGE = 8.0F;

    private double travelled;
    /** 本发月牙已命中过的实体（服务端，去重用，不落盘）。 */
    private final Set<UUID> hitTargets = new HashSet<>();

    public CrescentSlashEntity(EntityType<? extends CrescentSlashEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public CrescentSlashEntity(EntityType<? extends CrescentSlashEntity> type, Level level,
                               Player owner, Vec3 direction) {
        this(type, level);
        setOwner(owner);
        faceDirection(direction);
        setDeltaMovement(direction.normalize().scale(SPEED));
    }

    /**
     * 把实体 yaw/pitch 设成"面片朝向施放者"（本包渲染器约定：面片法线沿实体视线方向）。
     * 取推进方向的反向量：施放者在月牙后方，正面朝回他才能看到按贴图原样、
     * 不左右镜像的月牙（此前法线朝飞行方向，玩家看到的是背面 → "月牙反过来的"）。
     * 与 DomainSlashEntity#faceViewer 同一套公式。
     */
    public void faceDirection(Vec3 direction) {
        Vec3 towardOwner = direction.normalize().scale(-1.0D);
        setYRot((float) Math.toDegrees(Math.atan2(-towardOwner.x, towardOwner.z)));
        setXRot((float) Math.toDegrees(Math.asin(Mth_clamp(towardOwner.y))));
    }

    private static double Mth_clamp(double v) {
        return Math.max(-1.0D, Math.min(1.0D, v));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            Vec3 from = position();
            Vec3 delta = getDeltaMovement();
            setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);
            travelled += delta.length();
            sweepDamage(from, position());
            if (travelled >= MAX_DISTANCE) {
                discard();
            }
        }
    }

    /**
     * 接触伤害：对本 tick 扫掠线段（from→to）距离 ≤ HIT_RADIUS 的活体实体
     * 造成 8 点(4颗心)投掷伤害（归属施放者），每个实体每发月牙至多命中一次。
     * 线段本身只有 0.85 格长，无需担心高速穿透漏判。
     */
    private void sweepDamage(Vec3 from, Vec3 to) {
        Entity owner = getOwner();
        AABB band = new AABB(from, to).inflate(HIT_RADIUS);
        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class, band,
                entity -> entity != owner && entity.isAlive()
                        && !entity.isSpectator() && !hitTargets.contains(entity.getUUID()));
        if (nearby.isEmpty()) {
            return;
        }
        DamageSource source = level().damageSources().thrown(owner == null ? this : owner, this);
        for (LivingEntity target : nearby) {
            if (distanceToSegment(from, to, target.getBoundingBox()) <= HIT_RADIUS) {
                hitTargets.add(target.getUUID());
                target.hurt(source, CONTACT_DAMAGE);
            }
        }
    }

    /** 线段到实体碰撞箱的最短距离（线段上离箱中心最近点 → 点到 AABB 距离）。 */
    private static double distanceToSegment(Vec3 from, Vec3 to, AABB box) {
        Vec3 ab = to.subtract(from);
        double len2 = ab.lengthSqr();
        if (len2 < 1.0E-9D) {
            return pointToBox(from, box);
        }
        Vec3 center = box.getCenter();
        double t = Mth.clamp(center.subtract(from).dot(ab) / len2, 0.0D, 1.0D);
        return pointToBox(from.add(ab.scale(t)), box);
    }

    private static double pointToBox(Vec3 p, AABB box) {
        double dx = Mth.clamp(p.x, box.minX, box.maxX) - p.x;
        double dy = Mth.clamp(p.y, box.minY, box.maxY) - p.y;
        double dz = Mth.clamp(p.z, box.minZ, box.maxZ) - p.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        travelled = tag.getDouble("Travelled");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putDouble("Travelled", travelled);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
