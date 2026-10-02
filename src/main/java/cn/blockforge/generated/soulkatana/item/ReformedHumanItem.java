package cn.blockforge.generated.soulkatana.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 改造人：「魂刀·真人」的合成素材。
 * 雪屋地下室的宝箱固定刷新 4 个（位置随机，见 SoulKatanaEvents 的战利品注入）；
 * 不可堆叠（stacksTo(1)），因此 4 个会散落在 4 个随机格子里。
 */
public class ReformedHumanItem extends Item {

    public ReformedHumanItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.soulkatana.reformed_human")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
