package me.eventually.carpethikariaddition.mixin.game;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkLightProvider.class)
public class LightUpdateMixin {
    @Inject(method = "doLightUpdates", at = @At("HEAD"), cancellable = true)
    private void doLightUpdates(CallbackInfoReturnable<Integer> cir) {
        if (!ExtensionOwnSettings.preventLightUpdate) {
            return;
        }
        cir.cancel();
    }
}
