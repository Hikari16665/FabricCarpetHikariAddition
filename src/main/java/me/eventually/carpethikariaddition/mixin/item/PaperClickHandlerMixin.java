package me.eventually.carpethikariaddition.mixin.item;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import me.eventually.carpethikariaddition.utils.ItemsUtil;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author Hikari
 */
@Mixin(Item.class)
public abstract class PaperClickHandlerMixin {

    @Unique
    private static long getTotalResultCount(
            List<ItemStack> craftingInput, long shulkerBoxCount, ItemStack resultStack) {
        int itemsPerCraft = 0;
        for (ItemStack recipeStack : craftingInput) {
            if (!recipeStack.isEmpty()) {
                itemsPerCraft++;
            }
        }

        if (itemsPerCraft == 0) {
            itemsPerCraft = 1;
        }

        // 获取每个物品的最大堆叠数量
        int itemsPerStack = resultStack.getMaxCount();

        // 计算总合成次数：
        // 每个潜影盒有27组物品，每组数量为物品的最大堆叠数量
        // 总物品数量 = shulkerBoxCount * 27 * 物品最大堆叠数量
        // 总合成次数 = 总物品数量 / 每个合成需要的物品数量
        long totalItems = shulkerBoxCount * 27 * itemsPerStack;
        long totalCrafts = totalItems / itemsPerCraft;

        // 计算总结果数量：每次合成产生resultStack.getCount()个结果
        return totalCrafts * resultStack.getCount();
    }

    /** 处理提示纸的点击事件 */
    @Inject(method = "onStackClicked", at = @At("HEAD"), cancellable = true)
    private void onStackClicked(
            ItemStack stack,
            Slot slot,
            ClickType clickType,
            PlayerEntity player,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ExtensionOwnSettings.shulkerBoxBulkCrafting) {
            return;
        }
        if (stack.isOf(Items.PAPER) && stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
            if (name == null) {
                return;
            }
            if ("您已触发潜影盒批量合成".equals(name.getString())) {
                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    // 从Lore中读取批量合成信息
                    if (stack.contains(DataComponentTypes.LORE)) {
                        int paperStackCount = stack.getCount();
                        LoreComponent lore = stack.get(DataComponentTypes.LORE);
                        if (lore != null) {
                            for (Text loreText : lore.lines()) {
                                String loreString = loreText.getString();
                                if (loreString.startsWith("BulkCraftingPaper:")) {
                                    String bulkData =
                                            loreString.substring("BulkCraftingPaper:".length());
                                    int firstColonIndex = bulkData.indexOf(":");
                                    if (firstColonIndex != -1) {
                                        int shulkerBoxCount =
                                                Integer.parseInt(
                                                        bulkData.substring(0, firstColonIndex));
                                        // 剩余部分就是完整的配方信息
                                        String recipeData = bulkData.substring(firstColonIndex + 1);
                                        if (player.currentScreenHandler
                                                instanceof
                                                CraftingScreenHandler craftingScreenHandler) {
                                            boolean recipeValid = false;
                                            List<ItemStack> inputItems = new ArrayList<>();
                                            boolean hasShulkerBoxInCrafting = false;
                                            for (int i = 1; i < 10; i++) {
                                                Slot inputSlot = craftingScreenHandler.getSlot(i);
                                                if (inputSlot != null
                                                        && inputSlot.hasStack()
                                                        && ItemsUtil.isShulkerBox(
                                                                inputSlot.getStack())) {
                                                    hasShulkerBoxInCrafting = true;
                                                    Optional<ItemStack> contentOpt =
                                                            getShulkerBoxContent(
                                                                    inputSlot.getStack());
                                                    contentOpt.ifPresent(inputItems::add);
                                                }
                                            }
                                            List<ItemStack> craftingInput =
                                                    new ArrayList<>(
                                                            List.of(
                                                                    ItemStack.EMPTY,
                                                                    ItemStack.EMPTY,
                                                                    ItemStack.EMPTY,
                                                                    ItemStack.EMPTY,
                                                                    ItemStack.EMPTY,
                                                                    ItemStack.EMPTY,
                                                                    ItemStack.EMPTY,
                                                                    ItemStack.EMPTY,
                                                                    ItemStack.EMPTY));
                                            String decodedRecipeData = recipeData;
                                            try {
                                                // 尝试解码Base64数据
                                                byte[] decodedBytes =
                                                        Base64.getDecoder().decode(recipeData);
                                                decodedRecipeData =
                                                        new String(
                                                                decodedBytes,
                                                                StandardCharsets.UTF_8);
                                            } catch (IllegalArgumentException e) {
                                                // 如果解码失败，说明是旧格式，直接使用原始数据
                                                // 不做任何处理，使用原始数据
                                                decodedRecipeData = recipeData;
                                            }

                                            // 解析配方信息
                                            String[] recipeItems = decodedRecipeData.split(",");
                                            if (recipeItems.length == 9) {
                                                // 使用解析出的完整配方
                                                for (int i = 0; i < recipeItems.length; i++) {
                                                    String recipeItem = recipeItems[i];
                                                    if (!"empty".equals(recipeItem)) {
                                                        Identifier itemId =
                                                                Identifier.tryParse(recipeItem);
                                                        if (itemId != null) {
                                                            Item item = Registries.ITEM.get(itemId);
                                                            if (item != null) {
                                                                craftingInput.set(
                                                                        i, new ItemStack(item));
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // 兼容旧格式
                                                if (inputItems.isEmpty()) {
                                                    // 旧格式：只有一个物品类型
                                                    Identifier itemId =
                                                            Identifier.tryParse(decodedRecipeData);
                                                    if (itemId != null) {
                                                        Item item = Registries.ITEM.get(itemId);
                                                        ItemStack itemStack = new ItemStack(item);
                                                        inputItems.add(itemStack);
                                                    }
                                                }

                                                // 将潜影盒中的物品填充到合成输入中
                                                int index = 0;
                                                for (ItemStack itemStack : inputItems) {
                                                    if (index < 9) {
                                                        craftingInput.set(index, itemStack);
                                                        index++;
                                                    }
                                                }
                                            }
                                            if (!craftingInput.isEmpty()) {
                                                CraftingRecipeInput recipeInput =
                                                        CraftingRecipeInput.create(
                                                                3, 3, craftingInput);

                                                // 查询Minecraft注册表，找到对应的合成配方
                                                Optional<
                                                                net.minecraft.recipe.RecipeEntry<
                                                                        net.minecraft.recipe
                                                                                .CraftingRecipe>>
                                                        recipeEntryOpt = Optional.empty();
                                                try {
                                                    recipeEntryOpt =
                                                            serverWorld
                                                                    .getRecipeManager()
                                                                    .getFirstMatch(
                                                                            net.minecraft.recipe
                                                                                    .RecipeType
                                                                                    .CRAFTING,
                                                                            recipeInput,
                                                                            serverWorld);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                if (recipeEntryOpt.isPresent()) {
                                                    net.minecraft.recipe.CraftingRecipe recipe =
                                                            recipeEntryOpt.get().value();
                                                    // 获取合成结果
                                                    ItemStack resultStack = ItemStack.EMPTY;
                                                    try {
                                                        resultStack =
                                                                recipe.craft(
                                                                        recipeInput,
                                                                        serverWorld
                                                                                .getRegistryManager());
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }

                                                    if (!resultStack.isEmpty()) {
                                                        recipeValid = true;
                                                        // 计算每个合成需要的物品数量
                                                        // 简化实现：假设每个合成需要的物品数量等于工作台中非空槽位的数量
                                                        long totalResultCount =
                                                                getTotalResultCount(
                                                                        craftingInput,
                                                                        shulkerBoxCount,
                                                                        resultStack);

                                                        // 计算需要的潜影盒数量：每个潜影盒可以装27组物品，每组数量为物品的最大堆叠数量
                                                        int slotsPerShulkerBox = 27;
                                                        int itemsPerStack =
                                                                resultStack.getMaxCount();

                                                        // 使用物品的实数量
                                                        long totalStacksNeeded =
                                                                (((totalResultCount
                                                                                + itemsPerStack) * paperStackCount)
                                                                                - 1)
                                                                        / itemsPerStack;
                                                        long shulkerBoxNeeded =
                                                                (totalStacksNeeded
                                                                                + slotsPerShulkerBox
                                                                                - 1)
                                                                        / slotsPerShulkerBox;

                                                        // 获取第一个潜影盒作为模板
                                                        ItemStack firstShulkerBox = null;
                                                        for (int i = 1; i < 10; i++) {
                                                            Slot inputSlot =
                                                                    craftingScreenHandler.getSlot(
                                                                            i);
                                                            if (inputSlot != null
                                                                    && inputSlot.hasStack()
                                                                    && ItemsUtil.isShulkerBox(
                                                                            inputSlot.getStack())) {
                                                                firstShulkerBox =
                                                                        inputSlot.getStack();
                                                                break;
                                                            }
                                                        }

                                                        // 生成对应数量的成品潜影盒
                                                        long remainingItems = totalResultCount;
                                                        for (long i = 0;
                                                                i < shulkerBoxNeeded;
                                                                i++) {
                                                            // 使用与输入相同颜色的潜影盒
                                                            ItemStack shulkerBox =
                                                                    firstShulkerBox != null
                                                                            ? firstShulkerBox.copy()
                                                                            : new ItemStack(
                                                                                    Items
                                                                                            .SHULKER_BOX);

                                                            // 创建装满物品的DefaultedList
                                                            DefaultedList<ItemStack> items =
                                                                    DefaultedList.ofSize(
                                                                            slotsPerShulkerBox,
                                                                            ItemStack.EMPTY);
                                                            int currentSlot = 0;

                                                            // 填充当前潜影盒
                                                            while (remainingItems > 0
                                                                    && currentSlot
                                                                            < slotsPerShulkerBox) {
                                                                // 计算当前槽位可以装多少物品（最多64个）
                                                                long itemsInSlot =
                                                                        Math.min(
                                                                                itemsPerStack,
                                                                                remainingItems);
                                                                // 创建物品堆叠
                                                                ItemStack stackedItem =
                                                                        resultStack.copy();
                                                                stackedItem.setCount(
                                                                        (int) itemsInSlot);
                                                                items.set(currentSlot, stackedItem);
                                                                // 更新剩余物品数量和当前槽位
                                                                remainingItems -= itemsInSlot;
                                                                currentSlot++;
                                                            }

                                                            // 直接构造ContainerComponent
                                                            ContainerComponent container =
                                                                    new ContainerComponent(items);
                                                            shulkerBox.set(
                                                                    DataComponentTypes.CONTAINER,
                                                                    container);

                                                            System.out.println(
                                                                    "Generated shulker box: "
                                                                            + shulkerBox
                                                                            + " (剩余物品: "
                                                                            + remainingItems
                                                                            + ")");

                                                            // 掉落物品
                                                            player.dropItem(shulkerBox, true);
                                                        }
                                                    }
                                                }
                                            }

                                            // 只有在配方有效时，才清空输入槽和输出槽，并清除玩家手中的提示纸
                                            if (recipeValid) {
                                                // 清空输入槽
                                                for (int i = 1; i < 10; i++) {
                                                    Slot inputSlot =
                                                            craftingScreenHandler.getSlot(i);
                                                    if (inputSlot != null
                                                            && inputSlot.hasStack()
                                                            && ItemsUtil.isShulkerBox(
                                                                    inputSlot.getStack())) {
                                                        inputSlot.setStack(ItemStack.EMPTY);
                                                    }
                                                }

                                                // 清空输出槽
                                                Slot outputSlot =
                                                        craftingScreenHandler.getOutputSlot();
                                                outputSlot.setStack(ItemStack.EMPTY);
                                                if (player
                                                        instanceof
                                                        ServerPlayerEntity serverPlayer) {
                                                    // 发送槽位更新数据包
                                                    ScreenHandler handler =
                                                            player.currentScreenHandler;
                                                    serverPlayer.networkHandler.sendPacket(
                                                            new ScreenHandlerSlotUpdateS2CPacket(
                                                                    handler.syncId,
                                                                    handler.nextRevision(),
                                                                    outputSlot.id,
                                                                    ItemStack.EMPTY));
                                                }

                                                // 清除玩家手中的提示纸，防止无限刷物品
                                                if (player
                                                        instanceof
                                                        ServerPlayerEntity serverPlayer) {
                                                    // 检查玩家手中是否是提示纸
                                                    ItemStack heldStack =
                                                            serverPlayer.currentScreenHandler
                                                                    .getCursorStack();
                                                    if (heldStack.isOf(Items.PAPER)
                                                            && heldStack.contains(
                                                                    DataComponentTypes
                                                                            .CUSTOM_NAME)) {
                                                        Text heldName =
                                                                heldStack.get(
                                                                        DataComponentTypes
                                                                                .CUSTOM_NAME);
                                                        if ("您已触发潜影盒批量合成"
                                                                .equals(heldName.getString())) {
                                                            serverPlayer.currentScreenHandler
                                                                    .setCursorStack(
                                                                            ItemStack.EMPTY);
                                                        }
                                                    }
                                                }

                                                cir.setReturnValue(true);
                                            } else {
                                                // 配方无效
                                                cir.setReturnValue(false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** 获取潜影盒中的物品 */
    @Unique
    private Optional<ItemStack> getShulkerBoxContent(ItemStack shulkerBox) {
        if (!shulkerBox.contains(DataComponentTypes.CONTAINER)) {
            return Optional.empty();
        }
        ContainerComponent container = shulkerBox.get(DataComponentTypes.CONTAINER);
        Iterator<ItemStack> iterator = null;
        if (container != null) {
            iterator = container.iterateNonEmpty().iterator();
        }
        if (iterator != null && !iterator.hasNext()) {
            return Optional.empty();
        }
        ItemStack firstItem = null;
        if (iterator != null) {
            firstItem = iterator.next();
        }
        // 检查是否所有物品都相同
        if (iterator != null) {
            while (iterator.hasNext()) {
                ItemStack nextItem = iterator.next();
                // 检查物品是否相同
                if (!ItemStack.areItemsAndComponentsEqual(nextItem, firstItem)) {
                    return Optional.empty();
                }
            }
        }
        // 不需要检查是否有27个物品，只要物品都相同就可以
        return Optional.of(firstItem);
    }
}
