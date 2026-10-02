package cn.blockforge.generated.soulkatana.item;

import cn.blockforge.generated.soulkatana.SoulKatana;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 两面宿傩的手指：紫色稀有度、可食用，食用后立刻死亡。
 * 死因使用数据包 damage_type「soulkatana:sukuna_finger」（带 bypasses_invulnerability，
 * 不死图腾也救不了），死亡消息为「*玩家名*不配当诅咒之王的受肉体」。
 */
public class SukunaFingerItem extends Item {

    public static final ResourceKey<DamageType> SUKUNA_FINGER_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, SoulKatana.prefix("sukuna_finger"));

    public SukunaFingerItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)
                .food(new FoodProperties.Builder().nutrition(4).saturationMod(0.1F).alwaysEat()
                        .effect(() -> new MobEffectInstance(MobEffects.POISON, 60, 0), 1.0F)
                        .build()));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            try {
                DamageSource source = new DamageSource(
                        level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolderOrThrow(SUKUNA_FINGER_DAMAGE), entity);
                entity.hurt(source, Float.MAX_VALUE);
            } catch (IllegalStateException missingDatapack) {
                // damage_type 数据缺失（资源包被裁剪）时退回原版即死，至少不留活口
                entity.hurt(entity.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
        }
        return result;
    }
}
