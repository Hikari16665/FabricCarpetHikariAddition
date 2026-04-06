package me.eventually.carpethikariaddition.mixin.block;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import me.eventually.carpethikariaddition.utils.ItemsUtil;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hikari
 */
@Mixin(BlockItem.class)
public class ShulkerBoxBlockItemPlaceMixin {
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
    private void blockItemPlaceMixin(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!ExtensionOwnSettings.shulkerBoxBlockPlace) {
            // 如果设置关闭，让原始方法继续执行
            return;
        }

        if (context.getPlayer() == null) {
            // 如果不是玩家在操作，让原始方法继续执行
            return;
        }

        // 先检查下是不是潜影盒
        ItemStack stack = context.getStack();
        if (!ItemsUtil.isShulkerBox(stack)) {
            // 如果不是潜影盒，让原始方法继续执行
            return;
        }

        // 找找这潜影盒有没有物品栏
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) {
            // 如果没有容器组件，让原始方法继续执行（放置潜影盒）
            return;
        }

        // 这个container可以stream转list
        List<ItemStack> itemStacks = new ArrayList<>(container.stream().toList());

        if (itemStacks.isEmpty()) {
            // 如果没有物品，让原始方法继续执行（放置空潜影盒）
            return;
        }

        // 遍历找一下里面有没有任何方块物品,如果有的话处理一下
        int firstStackIndex = -1;
        for (int i = 0; i < itemStacks.size(); i++) {
            ItemStack itemStack = itemStacks.get(i);
            if (itemStack.getItem() instanceof BlockItem) {
                firstStackIndex = i;
                break;
            }
        }
        if (firstStackIndex == -1) {
            // 完全没找到方块物品，让原始方法继续执行（放置含非方块物品的潜影盒）
            return;
        }

        // 验证放置上下文
        BlockItem blockItem = (BlockItem) (Object) this;
        if (!blockItem.getBlock().isEnabled(context.getWorld().getEnabledFeatures())) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        if (!context.canPlace()) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        ItemPlacementContext itemPlacementContext = blockItem.getPlacementContext(context);
        if (itemPlacementContext == null) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        BlockState blockState = blockItem.getPlacementState(itemPlacementContext);
        if (blockState == null) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        // 我们找到了第一个BlockItem，现在执行自定义放置逻辑
        ItemStack firstStack = itemStacks.get(firstStackIndex);
        BlockItem firstBlockItem = (BlockItem) firstStack.getItem();
        Block firstBlock = firstBlockItem.getBlock();

        // 获取目标位置和世界
        BlockPos pos = itemPlacementContext.getBlockPos();
        World world = itemPlacementContext.getWorld();
        PlayerEntity player = itemPlacementContext.getPlayer();
        ItemStack originalStack = itemPlacementContext.getStack();

        if (player != null && player.isSneaking()) {
            return;
        }

        // 执行方块放置
        BlockState targetBlockState = firstBlock.getDefaultState();

        // 检查是否可以放置
        if (!world.getBlockState(pos).canReplace(itemPlacementContext) ||
                !targetBlockState.canPlaceAt(world, pos)) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        // 设置方块状态
        if (world.setBlockState(pos, targetBlockState, Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD)) {
            // 播放放置声音
            BlockSoundGroup blockSoundGroup = targetBlockState.getSoundGroup();
            world.playSound(player, pos, firstBlockItem.getPlaceSound(targetBlockState),
                    SoundCategory.BLOCKS,
                    (blockSoundGroup.getVolume() + 1.0F) / 2.0F,
                    blockSoundGroup.getPitch() * 0.8F);

            // 发射游戏事件
            world.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(player, targetBlockState));

            // 更新潜影盒中的物品
            ItemStack updatedFirstStack = firstStack.copy();
            updatedFirstStack.decrement(1);

            // 创建新的容器组件，将修改后的物品列表放回去
            DefaultedList<ItemStack> newContainerList = DefaultedList.ofSize(itemStacks.size(), ItemStack.EMPTY);
            for (int i = 0; i < itemStacks.size(); i++) {
                if (i == firstStackIndex) {
                    // 如果是被修改的那个物品槽，使用修改后的物品
                    newContainerList.set(i, updatedFirstStack);
                } else {
                    // 否则复制原来的物品
                    newContainerList.set(i, itemStacks.get(i).copy());
                }
            }

            ContainerComponent newContainerComponent = new ContainerComponent(newContainerList);

            // 将新容器组件设置回原潜影盒
            ItemStack updatedStack = originalStack.copy();
            updatedStack.set(DataComponentTypes.CONTAINER, newContainerComponent);

            // 更新玩家的物品栏
            if (player != null) {
                PlayerInventory playerInventory = player.getInventory();

                // 找到玩家手中对应的物品槽位
                int slotId = playerInventory.getSelectedSlot();

                // 检查当前选中槽位的物品是否与我们要更新的物品相同
                if (ItemStack.areItemsAndComponentsEqual(playerInventory.getStack(slotId), originalStack)) {
                    // 减少潜影盒的数量
                    playerInventory.removeOne(originalStack);

                    // 如果潜影盒没有被完全消耗（内部还有物品），则更新为修改后的潜影盒
                    if (!updatedStack.isEmpty()) {
                        // 检查当前选中槽位是否为空，如果不为空则设置更新后的潜影盒
                        if (playerInventory.getStack(slotId).isEmpty()) {
                            playerInventory.setStack(slotId, updatedStack);
                        }
                    }
                } else {
                    // 如果当前选中的不是这个物品，遍历整个库存找到匹配项
                    for (int i = 0; i < playerInventory.size(); i++) {
                        if (ItemStack.areItemsAndComponentsEqual(playerInventory.getStack(i), originalStack)) {
                            // 减少潜影盒的数量
                            playerInventory.removeOne(originalStack);

                            // 如果潜影盒没有被完全消耗（内部还有物品），则更新为修改后的潜影盒
                            if (!updatedStack.isEmpty() && playerInventory.getStack(i).isEmpty()) {
                                playerInventory.setStack(i, updatedStack);
                            }
                            break;
                        }
                    }
                }
                playerInventory.markDirty();

                // 如果是服务器玩家，发送统计信息
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    Criteria.INVENTORY_CHANGED.trigger(serverPlayer, playerInventory, updatedStack);
                }
            }

            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel(); // 取消原始方法的执行
        } else {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}