package me.eventually.carpethikariaddition.mixin.entity;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public class VillagerFastGrowMixin {
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void fastGrow(ServerWorld world, CallbackInfo ci) {
        if (!ExtensionOwnSettings.villagerFastGrow) {
            return;
        }
        VillagerEntity villager;
        try {
            villager = (VillagerEntity) (Object) this;
        } catch (ClassCastException e) {
            return;
        }
        if (villager.isBaby()) {
            villager.setBreedingAge(0);
        }

    }
}
