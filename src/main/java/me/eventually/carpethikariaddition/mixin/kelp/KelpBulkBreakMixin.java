package me.eventually.carpethikariaddition.mixin.kelp;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Hikari
 */
@Mixin(Block.class)
public class KelpBulkBreakMixin {
    @Inject(method = "onBreak", at = @At("HEAD"))
    public void afterBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<BlockState> cir) {
        if (!ExtensionOwnSettings.kelpBulkBreaking) {
            return;
        }
        boolean bulkBreakDrop = ExtensionOwnSettings.kelpBulkBreakingDrop;
        if (state.getBlock() != Blocks.KELP && state.getBlock() != Blocks.KELP_PLANT) {
            return;
        }
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        mutablePos.set(pos);
        while (mutablePos.getY() > world.getBottomY()) {
            mutablePos.move(0, -1, 0);
            BlockState belowState = world.getBlockState(mutablePos);
            if (belowState.getBlock() != Blocks.KELP && belowState.getBlock() != Blocks.KELP_PLANT) {
                mutablePos.move(0, 1, 0);
                break;
            }
        }
        while (mutablePos.getY() < world.getTopY(Heightmap.Type.MOTION_BLOCKING, mutablePos.toImmutable())) {
            BlockState currentBlockState = world.getBlockState(mutablePos);
            if (currentBlockState.getBlock() == Blocks.KELP || currentBlockState.getBlock() == Blocks.KELP_PLANT) {
                world.breakBlock(mutablePos.toImmutable(), bulkBreakDrop, player);
                mutablePos.move(0, 1, 0);
            } else {
                break;
            }
        }
    }
}
