package com.spiritform.entity;

import com.spiritform.form.SpiritBeyond;
import com.spiritform.herald.SpiritHeralds;
import com.spiritform.item.HeraldHeartItem;
import com.spiritform.registry.SpiritItems;
import com.spiritform.registry.SpiritParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * М'ятний дим Відлуння: з'являється на місці, де гравець активував Відлуння Смерті, і через 5 секунд зникає.
 * Поки він є, з ним можна взаємодіяти так, як раніше з блоком Відлуння Потойбіччя: ПКМ веде в Потойбіччя й назад,
 * а Доказ сили кличе Вісника Смерті. Серця Вісників діють, як і скрізь (ПКМ у повітрі). Саме Відлуння (місце, куди
 * повертає здібність) лишається в обліку й після того, як дим розвіявся.
 */
public class EchoSmokeEntity extends Entity {

    /** Тривалість диму в тактах (5 секунд). */
    public static final int LIFETIME = 100;

    private int age;

    public EchoSmokeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** Дим не зберігається у світі: після перезапуску його просто немає. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        age++;
        if (age >= LIFETIME) {
            discard();
            return;
        }
        if (age % 3 == 0) {
            server.sendParticles(SpiritParticles.MINT, getX(), getY() + 0.7, getZ(), 4, 0.3, 0.45, 0.3, 0.01);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = serverPlayer.getItemInHand(hand);
        // Серця Вісників працюють як завжди (їх використовують у повітрі)
        if (stack.getItem() instanceof HeraldHeartItem) {
            return InteractionResult.PASS;
        }
        if (stack.is(SpiritItems.PROOF_OF_STRENGTH.get()) && level() instanceof ServerLevel server) {
            SpiritHeralds.useProofOnEcho(serverPlayer, server, stack, position());
            return InteractionResult.CONSUME;
        }
        SpiritBeyond.use(serverPlayer, blockPosition());
        return InteractionResult.CONSUME;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
