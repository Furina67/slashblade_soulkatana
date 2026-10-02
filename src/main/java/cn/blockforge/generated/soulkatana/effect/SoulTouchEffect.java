package cn.blockforge.generated.soulkatana.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * [触及灵魂]——真人领域【自闭圆顿裹】的标记效果：
 * 纯标记，没有任何视觉、没有任何提示（isDurationEffectTick 恒 false，tick 逻辑为空）。
 * 它的唯一作用：让事件层认出"这个目标此刻在领域内，对它的一切减伤（护甲/抗性/吸收）全部作废"。
 */
public class SoulTouchEffect extends MobEffect {
    public SoulTouchEffect() {
        super(MobEffectCategory.NEUTRAL, 0xA01010);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 纯标记：什么都不做
    }
}
