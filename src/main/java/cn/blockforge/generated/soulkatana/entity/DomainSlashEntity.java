package cn.blockforge.generated.soulkatana.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

/**
 * 斩击贴图实体（纯视觉）：领域斩击风暴里飞出的每一刀，以及 [捌] 命中时的火花。
 * 移植自宿傩参考包 DismantleProjectile 的"视觉斩"模式：短寿命、按样式帧动画播放、
 * 面片朝向观察者、绕视线轴旋转 slashAngle。三种样式：
 *  0 = 原斩击（slash_frame 1→2→3→4）
 *  1 = 红边斩击（red_edge_frame 1→2→3）
 *  2 = 红色斩击（red_frame 1→2→3）
 */
public class DomainSlashEntity extends Projectile {
    private static final EntityDataAccessor<Float> VISUAL_LIFETIME = SynchedEntityData.defineId(
            DomainSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VISUAL_LENGTH = SynchedEntityData.defineId(
            DomainSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SLASH_ANGLE = SynchedEntityData.defineId(
            DomainSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SLASH_STYLE = SynchedEntityData.defineId(
            DomainSlashEntity.class, EntityDataSerializers.INT);

    public DomainSlashEntity(EntityType<? extends DomainSlashEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
    }

    /** lifetimeSeconds 内播完样式帧动画；length 为该刀的刀长（格）。 */
    public void setVisualOnly(double lifetimeSeconds, float length) {
        entityData.set(VISUAL_LIFETIME, (float) (lifetimeSeconds * 20.0D));
        entityData.set(VISUAL_LENGTH, length);
    }

    public float getVisualLifetime() {
        return entityData.get(VISUAL_LIFETIME);
    }

    public float getVisualLength() {
        return entityData.get(VISUAL_LENGTH);
    }

    public void setSlashAngle(float angle) {
        entityData.set(SLASH_ANGLE, angle);
    }

    public float getSlashAngle() {
        return entityData.get(SLASH_ANGLE);
    }

    public void setSlashStyle(int style) {
        entityData.set(SLASH_STYLE, style);
    }

    public int getSlashStyle() {
        return entityData.get(SLASH_STYLE);
    }

    /** 让斩击面片法线指向观察者（面片永远正面完整）。 */
    public void faceViewer(Vec3 viewerEye) {
        Vec3 facing = position().subtract(viewerEye);
        if (facing.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 normalized = facing.normalize();
        setYRot((float) Math.toDegrees(Math.atan2(-normalized.x, normalized.z)));
        setXRot((float) Math.toDegrees(Math.asin(Math.max(-1.0D, Math.min(1.0D, normalized.y)))));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= Math.max(1.0F, getVisualLifetime())) {
            discard();
        }
    }

    /** 客户端：当前该播第几帧（按寿命均分），以及是否还该渲染。 */
    public boolean shouldRenderVisual() {
        return tickCount < getVisualLifetime();
    }

    public int frameFor(int frameCount) {
        float lifetime = Math.max(1.0F, getVisualLifetime());
        int frame = (int) (tickCount * (float) frameCount / lifetime);
        return Math.max(0, Math.min(frameCount - 1, frame));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(VISUAL_LIFETIME, 3.0F);
        entityData.define(VISUAL_LENGTH, 10.0F);
        entityData.define(SLASH_ANGLE, 0.0F);
        entityData.define(SLASH_STYLE, 0);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        entityData.set(VISUAL_LIFETIME, tag.getFloat("VisualLifetime"));
        entityData.set(VISUAL_LENGTH, tag.getFloat("VisualLength"));
        entityData.set(SLASH_ANGLE, tag.getFloat("SlashAngle"));
        entityData.set(SLASH_STYLE, tag.getInt("SlashStyle"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putFloat("VisualLifetime", getVisualLifetime());
        tag.putFloat("VisualLength", getVisualLength());
        tag.putFloat("SlashAngle", getSlashAngle());
        tag.putInt("SlashStyle", getSlashStyle());
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
