package me.eventually.carpethikariaddition.mixin.screen;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import me.eventually.carpethikariaddition.utils.ItemsUtil;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author Hikari
 */
@Mixin(CraftingScreenHandler.class)
public abstract class ShulkerBoxBulkCraftingMixin extends ScreenHandler {
    protected ShulkerBoxBulkCraftingMixin() {
        super(null, 0);
    }

    // 保存玩家对象的字段
    @Unique private PlayerEntity cachedPlayer;

    /** 注入到CraftingScreenHandler的构造方法，保存玩家对象 */
    @Inject(
            method =
                    "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V",
            at = @At("RETURN"))
    private void init(
            int syncId,
            PlayerInventory playerInventory,
            ScreenHandlerContext context,
            CallbackInfo ci) {
        this.cachedPlayer = playerInventory.player;
    }

    /** 检测工作台中的潜影盒，当满足条件时生成提示纸 */
    @Inject(method = "onContentChanged", at = @At("HEAD"), cancellable = true)
    private void onContentChanged(CallbackInfo ci) {
        if (!ExtensionOwnSettings.shulkerBoxBulkCrafting) {
            return;
        }
        // 检查是否有至少一个潜影盒
        boolean hasShulkerBox = false;
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

        // 收集所有潜影盒中的物品
        int shulkerBoxCount = 0;
        for (int i = 1; i < 10; i++) {
            Slot slot = slots.get(i);
            if (slot.hasStack() && ItemsUtil.isShulkerBox(slot.getStack())) {
                hasShulkerBox = true;
                shulkerBoxCount++;
                Optional<ItemStack> contentOpt = getShulkerBoxContent(slot.getStack());
                if (contentOpt.isPresent()) {
                    craftingInput.set(i - 1, contentOpt.get());
                }
            }
        }

        if (hasShulkerBox) {
            boolean hasValidContent = false;
            for (ItemStack itemStack : craftingInput) {
                if (!itemStack.isEmpty()) {
                    hasValidContent = true;
                    break;
                }
            }

            if (hasValidContent && cachedPlayer != null) {
                // 创建合成输入，用于验证配方
                CraftingRecipeInput recipeInput = CraftingRecipeInput.create(3, 3, craftingInput);

                // 获取世界对象，用于验证配方
                World world = cachedPlayer.getWorld();
                if (world instanceof ServerWorld serverWorld) {
                    // 验证是否存在有效的合成配方
                    Optional<RecipeEntry<CraftingRecipe>> recipeEntryOpt =
                            serverWorld
                                    .getRecipeManager()
                                    .getFirstMatch(RecipeType.CRAFTING, recipeInput, serverWorld);

                    // 只有当存在有效的配方时，才生成提示纸
                    if (recipeEntryOpt.isPresent()) {
                        // 生成提示纸
                        ItemStack paperStack = new ItemStack(Items.PAPER);
                        // 使用组件系统设置名称和自定义数据
                        paperStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("您已触发潜影盒批量合成"));

                        // 保存完整的配方信息到Lore中，格式：BulkCraftingPaper:<shulkerBoxCount>:<Base64编码的配方>
                        StringBuilder recipeBuilder = new StringBuilder();
                        // 保存完整的3x3配方，每个物品用逗号分隔
                        for (int i = 0; i < craftingInput.size(); i++) {
                            ItemStack itemStack = craftingInput.get(i);
                            if (!itemStack.isEmpty()) {
                                recipeBuilder.append(itemStack.getItem().toString());
                            } else {
                                recipeBuilder.append("empty");
                            }
                            if (i < craftingInput.size() - 1) {
                                recipeBuilder.append(",");
                            }
                        }

                        // 对配方数据进行Base64编码
                        String recipeString = recipeBuilder.toString();
                        System.out.println(recipeString);
                        String encodedRecipe =
                                Base64.getEncoder()
                                        .encodeToString(
                                                recipeString.getBytes(StandardCharsets.UTF_8));

                        // 构建最终的Lore字符串
                        StringBuilder loreBuilder = new StringBuilder("BulkCraftingPaper:");
                        loreBuilder.append(shulkerBoxCount).append(":").append(encodedRecipe);

                        paperStack.set(
                                DataComponentTypes.LORE,
                                new LoreComponent(List.of(Text.literal(loreBuilder.toString()))));

                        // 获取输出槽并设置物品
                        Slot outputSlot = slots.get(0);
                        outputSlot.setStack(paperStack);
                        ci.cancel();
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
            // 获取所有非空的物品
            iterator = container.iterateNonEmpty().iterator();
        } else {
            return Optional.empty();
        }
        if (!iterator.hasNext()) {
            // 如果没有物品，则返回空
            return Optional.empty();
        }
        ItemStack firstItem = iterator.next();
        // 检查是否所有物品都相同
        while (iterator.hasNext()) {
            ItemStack nextItem = iterator.next();
            // 检查物品是否相同
            if (!ItemStack.areItemsAndComponentsEqual(nextItem, firstItem)) {
                return Optional.empty();
            }
            //  检查物品是否都满了
            if (firstItem.getCount() != firstItem.getMaxCount() || nextItem.getCount() != nextItem.getMaxCount()) {
                return Optional.empty();
            }
        }
        // 简单检查：如果有27个物品，认为是满的
        int count = 0;
        for (ItemStack ignored : container.iterateNonEmpty()) {
            count++;
        }
        if (count != 27) {
            return Optional.empty();
        }
        return Optional.of(firstItem);
    }
}
