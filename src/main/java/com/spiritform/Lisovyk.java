package com.spiritform;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Лісовик: покритий мохом велетень на кордоні Лісу Потойбіччя. Захищає ліс: нападає на ворожих мобів
 * (крім тих, що живуть у Лісі Потойбіччя). Гравця не чіпає, поки той не вдарить його.
 */
public class Lisovyk extends PathfinderMob {

    public Lisovyk(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.2);
    }

    /** Чи місцевий мешканець Лісу Потойбіччя (його Лісовик не чіпає). */
    static boolean isForestNative(LivingEntity e) {
        return e instanceof Lisovyk || e instanceof Player || Mavka.isForestKin(e)
                || e instanceof Chuhaister || e instanceof Zlydeny || e instanceof Povitrulia
                || e instanceof Perelisnyk || e instanceof Nichnytsia || e instanceof Potercha
                || e instanceof Vodianyk || e instanceof Vodianytsia || e instanceof TemplateCreature;
    }

    /**
     * Чи стоїть точка в лісі поруч із його межею (щонайменше з одного боку за 24 блоки вже не ліс).
     * Біоми беремо в генератора, а не в області генерації чанка: під час створення чанка сусідні чанки ще недоступні.
     */
    static boolean atForestBorder(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.core.BlockPos pos) {
        net.minecraft.server.level.ServerLevel server = level.getLevel();
        net.minecraft.server.level.ServerChunkCache cache = server.getChunkSource();
        net.minecraft.world.level.biome.BiomeSource source = cache.getGenerator().getBiomeSource();
        net.minecraft.world.level.levelgen.RandomState random = cache.randomState();
        if (!biomeAt(source, random, pos.getX(), pos.getY(), pos.getZ()).is(SpiritBiomes.FOREST_LIKE)) {
            return false;
        }
        int[][] offsets = {{24, 0}, {-24, 0}, {0, 24}, {0, -24}, {17, 17}, {-17, 17}, {17, -17}, {-17, -17}};
        for (int[] o : offsets) {
            if (!biomeAt(source, random, pos.getX() + o[0], pos.getY(), pos.getZ() + o[1]).is(SpiritBiomes.FOREST_LIKE)) {
                return true;
            }
        }
        return false;
    }

    private static net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeAt(
            net.minecraft.world.level.biome.BiomeSource source, net.minecraft.world.level.levelgen.RandomState random,
            int x, int y, int z) {
        return source.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x), net.minecraft.core.QuartPos.fromBlock(y),
                net.minecraft.core.QuartPos.fromBlock(z), random.sampler());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, LivingEntity.class, 28.0, false,
                e -> e instanceof Enemy && !isForestNative(e)));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }
}
