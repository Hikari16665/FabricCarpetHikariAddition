package me.eventually.carpethikariaddition.mixin.entity;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Hikari
 */
@Mixin(FallingBlockEntity.class)
public class FallingAnvilMixin {
  @Inject(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/entity/FallingBlockEntity;applyGravity()V",
              shift = At.Shift.AFTER))
  private void applyAdditionalGravity(CallbackInfo ci) {
    if (!ExtensionOwnSettings.fastFallingAnvil) {
      return;
    }
    boolean isAnvil;
    FallingBlockEntity entity = null;
    try {
      Class<? extends FallingBlockEntity> fallingBlockEntityClass = FallingBlockEntity.class;
      entity = fallingBlockEntityClass.cast(this);
      isAnvil = true;
    } catch (ClassCastException ignored) {
      isAnvil = false;
    }
    if (!isAnvil) {
      return;
    }
    var block = entity.getBlockState().getBlock();
    if (!(block == Blocks.ANVIL
        || block == Blocks.CHIPPED_ANVIL
        || block == Blocks.DAMAGED_ANVIL)) {
      return;
    }
    entity.setVelocity(
        entity.getVelocity().x, entity.getVelocity().y * -999, entity.getVelocity().z);
  }
}
