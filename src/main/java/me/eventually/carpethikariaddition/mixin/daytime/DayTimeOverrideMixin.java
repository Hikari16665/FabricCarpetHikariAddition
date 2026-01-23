package me.eventually.carpethikariaddition.mixin.daytime;

import java.util.function.BooleanSupplier;
import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Hikari
 */
@Mixin(ServerWorld.class)
public class DayTimeOverrideMixin {
  @Inject(method = "tick", at = @At("TAIL"))
  private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
    if (!ExtensionOwnSettings.enableDayTimeOverride) {
      return;
    }
    ServerWorld world = (ServerWorld) (Object) this;
    world.setTimeOfDay(ExtensionOwnSettings.dayTimeOverride);
  }
}
