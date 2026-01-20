package me.eventually.carpethikariaddition.mixin.item;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import net.minecraft.block.*;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.dimension.NetherPortal;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(FireChargeItem.class)
public class FireChargeMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void preventFirePlacement(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {

        if (!ExtensionOwnSettings.fireChargeNoFireBlock) {
            return;
        }

        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);

        boolean canLightBlock = CampfireBlock.canBeLit(blockState)
                || CandleBlock.canBeLit(blockState)
                || CandleCakeBlock.canBeLit(blockState);

        if (canLightBlock) {
            world.playSound(null, blockPos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F,
                    world.getRandom().nextFloat() * 0.4F + 0.8F);

            // 点燃方块
            world.setBlockState(blockPos, blockState.with(Properties.LIT, true));
            world.emitGameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, blockPos);

            // 减少物品数量
            context.getStack().decrement(1);

            cir.cancel();
            cir.setReturnValue(ActionResult.SUCCESS);
        } else {
            BlockPos offsetPos = blockPos.offset(context.getSide());
            if (AbstractFireBlock.shouldLightPortalAt(world, offsetPos, context.getSide().getOpposite())) {
                world.playSound(null, offsetPos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F,
                        world.getRandom().nextFloat() * 0.4F + 0.8F);
                context.getStack().decrement(1);
                Optional<NetherPortal> optional = NetherPortal.getNewPortal(world, offsetPos, Direction.Axis.X);
                if (optional.isPresent()) {
                    optional.get().createPortal(world);
                    return;
                }

                cir.cancel();
                cir.setReturnValue(ActionResult.SUCCESS);
            } else {
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
        cir.setReturnValue(ActionResult.FAIL);
    }


}
