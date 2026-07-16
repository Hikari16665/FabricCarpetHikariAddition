package me.eventually.carpethikariaddition.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

public class CrowdSuicideGoal extends Goal {
    private final CreeperEntity creeper;
    private final TntThrowGoal tntThrowGoal;
    private int scanCooldown;

    public CrowdSuicideGoal(CreeperEntity creeper, TntThrowGoal tntThrowGoal) {
        this.creeper = creeper;
        this.tntThrowGoal = tntThrowGoal;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.tntThrowGoal.getTntCount() > 0) {
            return false;
        }

        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return false;
        }

        LivingEntity target = this.creeper.getTarget();
        if (target != null && target.isAlive() && !(target instanceof CreeperEntity)) {
            return false;
        }

        return findBestTarget() != null;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = this.creeper.getTarget();
        if (target == null || !target.isAlive() || target instanceof CreeperEntity) {
            return false;
        }
        return this.creeper.squaredDistanceTo(target) > 9.0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.creeper.getTarget();
        if (target == null) {
            return;
        }

        this.creeper.getLookControl().lookAt(target, 30.0F, (float) this.creeper.getMaxLookPitchChange());

        if (this.creeper.getNavigation().isIdle()) {
            this.creeper.getNavigation().startMovingTo(target, 1.0);
        }
    }

    private LivingEntity findBestTarget() {
        World world = this.creeper.getEntityWorld();
        Box searchBox = this.creeper.getBoundingBox().expand(32.0);

        List<LivingEntity> entities = world.getEntitiesByClass(
            LivingEntity.class, searchBox,
            e -> e.isAlive() && !(e instanceof CreeperEntity)
        );

        if (entities.isEmpty()) {
            return null;
        }

        LivingEntity best = null;
        int bestNeighbors = -1;
        double bestDist = Double.MAX_VALUE;

        for (LivingEntity candidate : entities) {
            int neighbors = 0;
            for (LivingEntity other : entities) {
                if (candidate != other && candidate.squaredDistanceTo(other) < 25.0) {
                    neighbors++;
                }
            }
            double dist = this.creeper.squaredDistanceTo(candidate);
            if (neighbors > bestNeighbors || (neighbors == bestNeighbors && dist < bestDist)) {
                bestNeighbors = neighbors;
                bestDist = dist;
                best = candidate;
            }
        }

        if (best != null) {
            this.creeper.setTarget(best);
        }

        return best;
    }

    @Override
    public void start() {
        this.scanCooldown = 60;
    }

    @Override
    public void stop() {
        this.creeper.getNavigation().stop();
    }
}
