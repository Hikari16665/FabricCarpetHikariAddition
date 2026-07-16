package me.eventually.carpethikariaddition.ai.goal;

import net.minecraft.entity.TntEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

public class AvoidTntGoal extends Goal {
    private final CreeperEntity creeper;
    private int scanCooldown;

    public AvoidTntGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return false;
        }
        World world = this.creeper.getEntityWorld();
        List<TntEntity> tnts = world.getEntitiesByClass(
            TntEntity.class,
            this.creeper.getBoundingBox().expand(8.0),
            TntEntity::isAlive
        );
        return !tnts.isEmpty();
    }

    @Override
    public boolean shouldContinue() {
        World world = this.creeper.getEntityWorld();
        List<TntEntity> tnts = world.getEntitiesByClass(
            TntEntity.class,
            this.creeper.getBoundingBox().expand(8.0),
            TntEntity::isAlive
        );
        return !tnts.isEmpty();
    }

    @Override
    public void tick() {
        World world = this.creeper.getEntityWorld();
        List<TntEntity> tnts = world.getEntitiesByClass(
            TntEntity.class,
            this.creeper.getBoundingBox().expand(8.0),
            TntEntity::isAlive
        );

        if (tnts.isEmpty()) {
            return;
        }

        double avgX = 0.0;
        double avgZ = 0.0;
        for (TntEntity tnt : tnts) {
            avgX += tnt.getX();
            avgZ += tnt.getZ();
        }
        avgX /= tnts.size();
        avgZ /= tnts.size();

        double dx = this.creeper.getX() - avgX;
        double dz = this.creeper.getZ() - avgZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) {
            dx = 1.0;
            dz = 0.0;
            len = 1.0;
        }

        double retreatX = this.creeper.getX() + (dx / len) * 8.0;
        double retreatZ = this.creeper.getZ() + (dz / len) * 8.0;

        if (this.creeper.getNavigation().isIdle()) {
            this.creeper.getNavigation().startMovingTo(retreatX, this.creeper.getY(), retreatZ, 1.3);
        }
    }

    @Override
    public void start() {
        this.scanCooldown = 10;
    }

    @Override
    public void stop() {
        this.creeper.getNavigation().stop();
    }
}
