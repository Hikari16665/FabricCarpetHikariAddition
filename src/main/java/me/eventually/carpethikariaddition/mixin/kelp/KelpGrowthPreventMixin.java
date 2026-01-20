package me.eventually.carpethikariaddition.mixin.kelp;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import net.minecraft.block.KelpBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KelpBlock.class)
public class KelpGrowthPreventMixin {


    @Inject(method = "getGrowthLength", at = @At("HEAD"), cancellable = true)
    private void preventKelpGrowth(CallbackInfoReturnable<Integer> cir) {
        if (ExtensionOwnSettings.kelpPreventGrowth) {
            cir.setReturnValue(0);
        }
    }
}
