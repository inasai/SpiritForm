package com.spiritform.entity;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Проста ціль для лісових істот: шукає найближчу живу істоту певного виду, яку дозволяє фільтр.
 * Власний клас, щоб не залежати від зміни підписів NearestAttackableTargetGoal між версіями.
 */
public class ForestTargetGoal<T extends LivingEntity> extends TargetGoal {

    private final Class<T> type;
    private final Predicate<LivingEntity> filter;
    private final double range;
    private LivingEntity found;

    public ForestTargetGoal(Mob mob, Class<T> type, double range, boolean mustSee, Predicate<LivingEntity> filter) {
        super(mob, mustSee);
        this.type = type;
        this.range = range;
        this.filter = filter;
    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextInt(8) != 0) {
            return false;
        }
        List<T> list = this.mob.level().getEntitiesOfClass(this.type, this.mob.getBoundingBox().inflate(range, range / 2.0, range),
                e -> e != this.mob && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && this.mob.canAttack(e) && this.filter.test(e));
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = this.mob.distanceToSqr(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        this.found = best;
        return best != null;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.found);
        super.start();
    }
}
