package cn.blockforge.generated.soulkatana.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * 结界方块（色号 #000000FF）：封闭式领域【自闭圆顿裹】的结界外壳。
 *
 * <p>属性按需求文档：
 * <ul>
 *   <li>纯黑（{@link MapColor#COLOR_BLACK}）；</li>
 *   <li><b>注册碰撞</b>（默认整格碰撞箱）——结界壳是实体墙，域内的玩家/生物撞在壳上出不去，
 *       "走出领域自动关闭"只留作传送、穿模一类异常手段的兜底；</li>
 *   <li>不遮挡（{@code noOcclusion}）——从内部/外部都能看到另一侧；</li>
 *   <li>不窒息（{@code isSuffocating(...) == false}）——结界铺到脚下/头顶时不会把人闷死；</li>
 *   <li>不可破坏（{@code strength(-1)} + {@code noLootTable}），配合领域保护事件双保险。</li>
 * </ul>
 * 方块本身不存任何状态：领域几何、映射表全部由服务端的 {@code RealPersonDomain} 管理，
 * 这样 1.6 万个结界方块也只占注册表里一个 BlockState 的开销。
 *
 * <p>注意：内部填充用的原版光明方块没有碰撞箱，所以脚下那层圆盘必须保留为本方块，
 * 否则域内实体直接从地板漏出去（见 {@code RealPersonDomain.layerPositions}）。
 */
public class DomainBarrierBlock extends Block {

    public DomainBarrierBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .noLootTable()
                .noParticlesOnBreak()
                .pushReaction(PushReaction.BLOCK)
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false));
    }
}
