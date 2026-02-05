package me.eventually.carpethikariaddition.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ItemsUtil {
    public static boolean isShulkerBox(ItemStack stack) {
        net.minecraft.item.Item item = stack.getItem();
        return stack.isOf(Items.SHULKER_BOX)
                || stack.isOf(Items.BLACK_SHULKER_BOX)
                || stack.isOf(Items.BLUE_SHULKER_BOX)
                || stack.isOf(Items.BROWN_SHULKER_BOX)
                || stack.isOf(Items.CYAN_SHULKER_BOX)
                || stack.isOf(Items.GRAY_SHULKER_BOX)
                || stack.isOf(Items.GREEN_SHULKER_BOX)
                || stack.isOf(Items.LIGHT_BLUE_SHULKER_BOX)
                || stack.isOf(Items.LIGHT_GRAY_SHULKER_BOX)
                || stack.isOf(Items.LIME_SHULKER_BOX)
                || stack.isOf(Items.MAGENTA_SHULKER_BOX)
                || stack.isOf(Items.ORANGE_SHULKER_BOX)
                || stack.isOf(Items.PINK_SHULKER_BOX)
                || stack.isOf(Items.PURPLE_SHULKER_BOX)
                || stack.isOf(Items.RED_SHULKER_BOX)
                || stack.isOf(Items.WHITE_SHULKER_BOX)
                || stack.isOf(Items.YELLOW_SHULKER_BOX);
    }
}
