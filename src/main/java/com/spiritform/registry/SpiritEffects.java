package com.spiritform.registry;

import com.spiritform.SpiritFormMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Власні ефекти: Задуха (Водяник) і Заманювання (Водяниці).
 */
public final class SpiritEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, SpiritFormMod.MODID);

    public static final DeferredHolder<MobEffect, SuffocationEffect> SUFFOCATION =
            EFFECTS.register("suffocation", SuffocationEffect::new);

    public static final DeferredHolder<MobEffect, LureEffect> LURE =
            EFFECTS.register("lure", LureEffect::new);

    /** Інверсія: керування дзеркальне (вправо-вліво, камера вгору-вниз). Саму інверсію робить InversionClient. */
    public static final DeferredHolder<MobEffect, InversionEffect> INVERSION =
            EFFECTS.register("inversion", InversionEffect::new);

    private SpiritEffects() {
    }

    /**
     * Задуха: бульбашки повітря зникають, як під водою. Повного запасу (300) вистачає рівно на 5 секунд,
     * тож один удар Водяника забирає всі бульбашки. Коли повітря немає, а ефект ще діє, кожну секунду йде шкода.
     */
    public static class SuffocationEffect extends MobEffect {

        public SuffocationEffect() {
            super(MobEffectCategory.HARMFUL, 0x3A6F9E);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.canBreatheUnderwater() || entity.hasEffect(MobEffects.WATER_BREATHING)) {
                return true;
            }
            // Ванільне відновлення додає 4 за тік, тож сумарно запас спадає на 3 за тік: 300 за 100 тіків
            int air = entity.getAirSupply() - 7 * (amplifier + 1);
            if (air <= 0) {
                air = 0;
                if (entity.tickCount % 20 == 0) {
                    entity.hurt(entity.damageSources().drown(), 2.0F);
                }
            }
            entity.setAirSupply(air);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }
    }

    /**
     * Заманювання: гравця тягне до найближчої води, а стрибати він не може (сила стрибка зведена до нуля).
     */
    public static class LureEffect extends MobEffect {

        private static final String TARGET_KEY = "SpiritLureTarget";
        private static final int SEARCH = 32;

        public LureEffect() {
            super(MobEffectCategory.HARMFUL, 0x4FA38A);
            this.addAttributeModifier(Attributes.JUMP_STRENGTH,
                    ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "effect.lure_no_jump"),
                    -1.0, AttributeModifier.Operation.ADD_VALUE);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.isSpectator()) {
                return true;
            }
            if (entity.isInWater()) {
                // У воді випливати не можна: гравця тягне на дно
                Vec3 m = entity.getDeltaMovement();
                if (m.y > -0.1) {
                    entity.setDeltaMovement(m.x, -0.1, m.z);
                    if (entity instanceof ServerPlayer) {
                        entity.hurtMarked = true;
                    }
                }
                return true;
            }
            Level level = entity.level();
            BlockPos target;
            if (entity.tickCount % 20 == 0 || !entity.getPersistentData().contains(TARGET_KEY)) {
                target = findWater(level, entity.blockPosition());
                if (target == null) {
                    entity.getPersistentData().remove(TARGET_KEY);
                    return true;
                }
                entity.getPersistentData().putLong(TARGET_KEY, target.asLong());
            } else {
                target = BlockPos.of(entity.getPersistentData().getLong(TARGET_KEY));
            }
            double dx = target.getX() + 0.5 - entity.getX();
            double dz = target.getZ() + 0.5 - entity.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.8) {
                return true; // вже на краю води
            }
            double pull = 0.07 * (amplifier + 1);
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x + dx / dist * pull, motion.y, motion.z + dz / dist * pull);
            if (entity instanceof ServerPlayer) {
                entity.hurtMarked = true; // щоб клієнт отримав новий рух
            }
            return true;
        }

        /** Найближча вода (за горизонтальною відстанню) у межах SEARCH блоків від істоти. */
        private static BlockPos findWater(Level level, BlockPos origin) {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int dx = -SEARCH; dx <= SEARCH; dx++) {
                for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                    double d = dx * dx + dz * dz;
                    if (d >= bestDist) {
                        continue;
                    }
                    for (int dy = -3; dy <= 1; dy++) {
                        p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (level.getFluidState(p).is(FluidTags.WATER)) {
                            best = p.immutable();
                            bestDist = d;
                            break;
                        }
                    }
                }
            }
            return best;
        }
    }

    public static class InversionEffect extends MobEffect {
        public InversionEffect() {
            super(MobEffectCategory.HARMFUL, 0x5A3A8C);
        }
    }
}
