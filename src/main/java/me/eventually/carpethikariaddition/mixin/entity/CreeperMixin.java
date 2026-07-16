package me.eventually.carpethikariaddition.mixin.entity;

import me.eventually.carpethikariaddition.ExtensionOwnSettings;
import me.eventually.carpethikariaddition.ai.goal.AvoidTntGoal;
import me.eventually.carpethikariaddition.ai.goal.CrowdSuicideGoal;
import me.eventually.carpethikariaddition.ai.goal.TntThrowGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public class CreeperMixin extends HostileEntity {
    protected CreeperMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("HEAD"), cancellable = true)
    public void creeperNewGoal(CallbackInfo ci) {
        if (!ExtensionOwnSettings.middleEastMode) {
            return;
        }

        CreeperEntity creeper = ((CreeperEntity) (Object) this);

        TntThrowGoal tntThrowGoal = new TntThrowGoal(creeper);

        creeper.goalSelector.add(1, new SwimGoal(creeper));
        creeper.goalSelector.add(1, new AvoidTntGoal(creeper));
        creeper.goalSelector.add(2, tntThrowGoal);
        creeper.goalSelector.add(3, new BreakDoorGoal(creeper, difficulty -> true));
        creeper.goalSelector.add(4, new CreeperIgniteGoal(creeper));
        creeper.goalSelector.add(5, new MeleeAttackGoal(creeper, (double)4.0F, false));
        creeper.goalSelector.add(6, new CrowdSuicideGoal(creeper, tntThrowGoal));
        creeper.goalSelector.add(7, new WanderAroundFarGoal(creeper, 1.6));
        creeper.goalSelector.add(7, new GoToVillageGoal(creeper, 512));
        creeper.goalSelector.add(8, new LookAtEntityGoal(creeper, LivingEntity.class, 32.0F));
        creeper.goalSelector.add(8, new LookAroundGoal(creeper));
        creeper.goalSelector.add(8, new MoveThroughVillageGoal(creeper, 1.6, false, 64, () -> true));

        creeper.targetSelector.add(1, new RevengeGoal(creeper));
        creeper.targetSelector.add(2, new ActiveTargetGoal<>(creeper, LivingEntity.class, false, (target, world) -> !(target instanceof CreeperEntity)));

        ci.cancel();
    }

    @ModifyConstant(method = "explode()V", constant = @Constant(floatValue = 2.0F))
    public float injectSuperExplodeMultiplier(float original) {
        return original * 8;
    }

    @ModifyConstant(method = "explode()V", constant = @Constant(floatValue = 1.0F))
    public float injectCommonExplodeMultiplier(float original) {
        return original * 4;
    }
}
