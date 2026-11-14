package com.spiritform.herald;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.phys.AABB;

/** Ціль за правилом: найближча жива істота, що підходить під умову, у заданому радіусі. */
public class HeraldTargetGoal extends TargetGoal {

    private final double range;
    private final Predicate<LivingEntity> filter;
    private LivingEntity found;

    public HeraldTargetGoal(Mob mob, double range, Predicate<LivingEntity> filter) {
        super(mob, false);
        this.range = range;
        this.filter = filter;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (mob.getRandom().nextInt(8) != 0) {
            return false;
        }
        AABB box = mob.getBoundingBox().inflate(range);
        List<LivingEntity> list = mob.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != mob && e.isAlive() && filter.test(e));
        found = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : list) {
            double d = candidate.distanceToSqr(mob);
            if (d < best) {
                best = d;
                found = candidate;
            }
        }
        return found != null;
    }

    @Override
    public void start() {
        mob.setTarget(found);
        super.start();
    }
}
