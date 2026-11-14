package com.spiritform.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/** Кристалізована Душа: матеріал для рецептів. Пам'ятає, чия душа в ній (для підказки). */
public class CrystalSoulItem extends Item {

    private static final String KEY = "CrystalSoul";

    public CrystalSoulItem(Properties properties) {
        super(properties);
    }

    /** Записує в предмет тип моба, чию душу кристалізовано. */
    public static void remember(ItemStack stack, String mobId) {
        CompoundTag soul = new CompoundTag();
        soul.putString("id", mobId);
        CompoundTag root = new CompoundTag();
        root.put(KEY, soul);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.spirit_form.crystal_soul.tooltip").withStyle(ChatFormatting.GRAY));
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(KEY, 10)) {
            return;
        }
        String id = root.getCompound(KEY).getString("id");
        EntityType.byString(id).ifPresent(type -> tooltip.add(
                Component.translatable("item.spirit_form.crystal_soul.of", type.getDescription())
                        .withStyle(ChatFormatting.DARK_AQUA)));
    }
}
