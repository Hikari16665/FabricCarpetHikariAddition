package me.eventually.carpethikariaddition.mixin.item;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;

import net.minecraft.block.AbstractPlantStemBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.KelpBlock;
import net.minecraft.block.KelpPlantBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Hikari
 */
@Mixin(AbstractPlantStemBlock.class)
public class KelpGrowthPreventMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void kelpAttachModifying(
            BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!ExtensionOwnSettings.kelpPreventGrowth) {
            return;
        }
        boolean plantCastSuccess;
        // detect if 'this' represents KelpBlock class
        try {
            Class<? extends KelpPlantBlock> kelpPlantBlockClass = KelpPlantBlock.class;
            // if casting is successful, set castSuccess to true
            kelpPlantBlockClass.cast(this);
            plantCastSuccess = true;
        } catch (ClassCastException ignored) {
            plantCastSuccess = false;
        }
        boolean blockCastSuccess;
        try {
            Class<? extends KelpBlock> kelpBlockClass = KelpBlock.class;
            kelpBlockClass.cast(this);
            blockCastSuccess = true;
        } catch (ClassCastException ignored) {
            blockCastSuccess = false;
        }
        if (!plantCastSuccess && !blockCastSuccess) {
            return;
        }
        ci.cancel();
    }
}
