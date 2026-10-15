package cn.blockforge.generated.soulkatana;

import cn.blockforge.generated.soulkatana.block.DomainBarrierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 领域方块注册：封闭式领域【自闭圆顿裹】运行时批量放置的结界外壳。
 *
 * <p>本模组只注册 {@link #DOMAIN_BARRIER} 一种方块：纯黑、有整格碰撞、不可破坏的空心半球壳，
 * 领域存续期间由服务端批量放置，关闭时按映射表原样还原地形。碰撞箱是"关人"的关键：
 * 域内的玩家/生物撞在壳上出不去，"走出领域自动关闭"只留作异常手段的兜底。脚下那层圆盘
 * 也保留为本方块，因为内部填充用的原版光明方块没有碰撞箱。
 *
 * <p>填充球体用的「光明方块」不再自建：那本来就是原版的调试方块
 * {@code minecraft:light}（{@code Blocks.LIGHT}，默认亮度 15、无形状、不可见、不可破坏），
 * 直接用原版即可，避免与游戏本体重复注册。见 {@code RealPersonDomain.lightState()}。
 */
public final class SoulKatanaBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SoulKatana.MODID);

    /** 结界方块 #000000FF：空心半球外壳（半径 16），同时是领域效果触发区。 */
    public static final RegistryObject<Block> DOMAIN_BARRIER =
            BLOCKS.register("domain_barrier", DomainBarrierBlock::new);

    private SoulKatanaBlocks() { }
}
