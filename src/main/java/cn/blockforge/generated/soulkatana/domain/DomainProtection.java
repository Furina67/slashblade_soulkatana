package cn.blockforge.generated.soulkatana.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 封闭式领域【自闭圆顿裹】的保护层（需求文档第 6 条）：
 *
 * <ul>
 *   <li>{@link BlockEvent.BreakEvent}：领域范围（半径 16 球体）与 Y=303 存储区的方块禁挖；</li>
 *   <li>{@link BlockEvent.EntityPlaceEvent}：同一区域禁放（含玩家、发射器、末影人等一切来源）；</li>
 *   <li>{@link ExplosionEvent.Detonate}：爆炸波及名单里的方块直接剔除——TNT/苦力怕炸掉存储区
 *       会直接毁掉被搬走的原始地形，这是比挖方块更隐蔽的数据丢失路径。</li>
 * </ul>
 *
 * <p>结界/光明方块本身已是不可破坏（strength -1），这里再拦一道，同时也把「领域范围内
 * 玩家自己放/挖方块」一并挡住，避免还原时把标记的位置改花。判定入口是
 * {@link RealPersonDomain#isProtected(Level, BlockPos)}，没有活动领域时零开销。
 */
@Mod.EventBusSubscriber(modid = "soulkatana")
public final class DomainProtection {

    private DomainProtection() { }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (RealPersonDomain.isProtected(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (RealPersonDomain.isProtected(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<BlockPos> affected = event.getAffectedBlocks();
        affected.removeIf(pos -> RealPersonDomain.isProtected(level, pos));
    }
}
