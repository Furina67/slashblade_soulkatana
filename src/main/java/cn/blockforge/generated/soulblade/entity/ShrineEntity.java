package cn.blockforge.generated.soulblade.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * Static, non-colliding 9x9x9 domain anchor. Its client renderer displays the
 * shrine texture; the entity itself is owned by one ActiveDomain instance.
 */
public final class ShrineEntity extends Entity {
    public ShrineEntity(EntityType<? extends ShrineEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /**
     * 神龛只属于"正在展开的领域"，而领域是纯内存状态、不会跨重启存在。
     * 让它进存档就必然留下清不掉的孤儿（界主掉线/服务端崩溃后神龛还杵在原地），
     * 所以直接声明不入库存：1.20.1 的 PersistentEntitySectionManager 写区块时
     * 会按这个返回值过滤实体。
     */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        // The shrine is a stationary visual anchor and must never acquire motion.
        setDeltaMovement(0.0D, 0.0D, 0.0D);
    }
}
