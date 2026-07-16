package me.eventually.carpethikariaddition.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

public class TntThrowGoal extends Goal {
    private final CreeperEntity creeper;
    private int tntCount;
    private int throwCooldown;

    public TntThrowGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.tntCount = 16;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.tntCount <= 0) {
            return false;
        }
        return findAndSetTarget() != null;
    }

    @Override
    public boolean shouldContinue() {
        if (this.tntCount <= 0) {
            return false;
        }
        LivingEntity target = this.creeper.getTarget();
        if (target == null || !target.isAlive() || target instanceof CreeperEntity) {
            return false;
        }
        return this.creeper.squaredDistanceTo(target) <= 625.0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.creeper.getTarget();
        if (target == null) {
            return;
        }

        this.creeper.getLookControl().lookAt(target, 30.0F, (float) this.creeper.getMaxLookPitchChange());

        double dist = this.creeper.squaredDistanceTo(target);

        if (dist < 16.0) {
            this.creeper.getNavigation().stop();
            double dx = this.creeper.getX() - target.getX();
            double dz = this.creeper.getZ() - target.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.01) {
                dx /= len;
                dz /= len;
            } else {
                dx = 1.0;
                dz = 0.0;
            }
            double retreatX = this.creeper.getX() + dx * 6.0;
            double retreatZ = this.creeper.getZ() + dz * 6.0;
            this.creeper.getNavigation().startMovingTo(retreatX, this.creeper.getY(), retreatZ, 1.2);
            return;
        }

        if (this.throwCooldown > 0) {
            this.throwCooldown--;
        } else {
            throwTnt(target);
        }

        if (this.creeper.getNavigation().isIdle()) {
            this.creeper.getNavigation().startMovingTo(target, 0.6);
        }
    }

    private void throwTnt(LivingEntity target) {
        World world = this.creeper.getWorld();
        var rand = this.creeper.getRandom();

        double startX = this.creeper.getX();
        double startY = this.creeper.getY() + 1.2;
        double startZ = this.creeper.getZ();

        double offsetX = (rand.nextDouble() - 0.5) * 12.0;
        double offsetZ = (rand.nextDouble() - 0.5) * 12.0;

        double dx = target.getX() + offsetX - startX;
        double dz = target.getZ() + offsetZ - startZ;

        double vy = 1.0 + rand.nextDouble() * 0.6;
        double flightTicks = vy / 0.04 * 2.0;

        double vx = dx / flightTicks;
        double vz = dz / flightTicks;

        TntEntity tnt = new TntEntity(world, startX, startY, startZ, this.creeper);
        tnt.setVelocity(vx, vy, vz);
        tnt.setFuse((int) (flightTicks + 10));
        world.spawnEntity(tnt);

        this.tntCount--;
        this.throwCooldown = 5 + rand.nextInt(10);
    }

    private LivingEntity findAndSetTarget() {
        LivingEntity existing = this.creeper.getTarget();
        if (existing != null && existing.isAlive() && !(existing instanceof CreeperEntity)) {
            double dist = this.creeper.squaredDistanceTo(existing);
            if (dist >= 9.0 && dist <= 625.0) {
                return existing;
            }
        }

        World world = this.creeper.getWorld();
        Box searchBox = this.creeper.getBoundingBox().expand(25.0);

        PlayerEntity player = world.getClosestPlayer(this.creeper, 25.0);
        if (player != null && player.isAlive()) {
            this.creeper.setTarget(player);
            return player;
        }

        List<LivingEntity> entities = world.getEntitiesByClass(
            LivingEntity.class, searchBox,
            e -> e.isAlive() && !(e instanceof CreeperEntity) && !(e instanceof PlayerEntity)
        );
        if (!entities.isEmpty()) {
            LivingEntity chosen = entities.get(0);
            if (this.creeper.squaredDistanceTo(chosen) >= 9.0) {
                this.creeper.setTarget(chosen);
                return chosen;
            }
        }

        return null;
    }

    @Override
    public void stop() {
        this.creeper.getNavigation().stop();
    }

    public int getTntCount() {
        return tntCount;
    }
}
