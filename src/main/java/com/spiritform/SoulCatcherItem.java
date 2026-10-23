package com.spiritform;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Ловець Душ.
 * ПКМ по мобу: ловить його (порожній ловець). Коротке ПКМ із мобом усередині випускає його туди, куди дивишся.
 * Якщо затиснути ПКМ на 5 секунд, гравець поглинає моба: здоров'я йде гравцю, а залишок перетворюється на голод.
 * Босів і мобів із понад 50 сердечками (100 здоров'я) ловити не можна.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public class SoulCatcherItem extends BlockItem {

    public static final int HOLD_TICKS = 100;
    public static final float MAX_CATCH_HEALTH = 100.0F;
    private static final String KEY = "SoulCatcher";

    public SoulCatcherItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Блоком ставиться лише з Shift (Shift + ПКМ). Без Shift ПКМ по блоку веде до випускання/поглинання. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            return super.useOn(context);
        }
        return InteractionResult.PASS;
    }

    // ---------- зберігання моба в предметі ----------

    public static CompoundTag stored(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag root = data.copyTag();
        return root.contains(KEY, 10) ? root.getCompound(KEY) : null;
    }

    public static boolean isFull(ItemStack stack) {
        return stored(stack) != null;
    }

    public static void store(ItemStack stack, CompoundTag mob) {
        CompoundTag root = new CompoundTag();
        root.put(KEY, mob);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static void clear(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    // ---------- ловля ----------

    private static boolean canCatch(LivingEntity target) {
        if (!(target instanceof Mob mob) || target instanceof Player) {
            return false;
        }
        if (target instanceof EnderDragon || target instanceof WitherBoss
                || target.getType().is(Tags.EntityTypes.BOSSES)) {
            return false;
        }
        if (target.getMaxHealth() > MAX_CATCH_HEALTH) {
            return false;
        }
        return !SpiritPossession.isPossessed(mob) && !SpiritTouch.isGrabbed(mob) && !SpiritDelirium.isDecoy(mob);
    }

    /** ПКМ по сутності: ловець бере на себе взаємодію (торгівля чи годування не заважають). */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SoulCatcherItem)) {
            return;
        }

        event.setCanceled(true);
        if (isFull(stack) || !(event.getTarget() instanceof LivingEntity target)) {
            // Повний ловець: далі спрацює утримування (випустити чи поглинути)
            event.setCancellationResult(InteractionResult.PASS);
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            catchMob(stack, player, target);
        }
    }

    private static void catchMob(ItemStack stack, ServerPlayer player, LivingEntity target) {
        if (!canCatch(target)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.catcher.cannot"), true);
            return;
        }

        if (target.isPassenger()) {
            target.stopRiding();
        }
        if (target instanceof Mob mob) {
            mob.dropLeash(true, true);
        }

        CompoundTag tag = new CompoundTag();
        if (!target.save(tag)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.catcher.cannot"), true);
            return;
        }

        store(stack, tag);
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() / 2,
                    target.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.5F);
        }
        target.discard();
        player.displayClientMessage(Component.translatable("message.spirit_form.catcher.caught",
                target.getType().getDescription()), true);
        SpiritAdvancements.grant(player, target instanceof Enemy ? "behind_bars" : "dominion");
    }

    // ---------- випускання й поглинання ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isFull(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // Залежно від версії API метод має одну з двох форм: обидві повертають однакову тривалість
    public int getUseDuration(ItemStack stack) {
        return isFull(stack) ? HOLD_TICKS : 0;
    }

    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getUseDuration(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int used = HOLD_TICKS - remaining;
        // Перші кілька тіків це ще "клац", тож поглинання показуємо, лише коли гравець затиснув кнопку
        if (used > 6 && used % 4 == 0) {
            server.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.4, 0.6, 0.4, 0.02);
        }
        if (used > 6 && used % 10 == 0) {
            player.displayClientMessage(Component.translatable("message.spirit_form.catcher.absorbing",
                    String.valueOf(used * 100 / HOLD_TICKS)), true);
        }
    }

    /** Кнопку відпущено раніше за 5 секунд: моб випускається. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag mob = stored(stack);
        if (mob == null) {
            return;
        }
        if (releaseMob(player, mob)) {
            clear(stack);
        }
    }

    private static boolean releaseMob(ServerPlayer player, CompoundTag stored) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        Vec3 spot;
        HitResult hit = player.pick(6.0, 1.0F, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            spot = Vec3.atBottomCenterOf(block.getBlockPos().relative(block.getDirection()));
        } else {
            spot = player.getEyePosition().add(player.getLookAngle().scale(2.5));
        }

        CompoundTag tag = stored.copy();
        tag.remove("UUID");
        Optional<EntityType<?>> type = EntityType.by(tag);
        if (type.isEmpty()) {
            return false;
        }
        Entity entity = type.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.load(tag);
        entity.moveTo(spot.x, spot.y, spot.z, player.getYRot() + 180.0F, 0.0F);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        level.addFreshEntity(entity);

        level.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 0.5, spot.z, 16, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.8F);
        return true;
    }

    /** Кнопка втримана 5 секунд: гравець поглинає моба (здоров'я йому, залишок голодом). */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        CompoundTag mob = stored(stack);
        if (mob == null || level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return stack;
        }

        float health = mob.getFloat("Health");
        float need = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
        float heal = Math.min(need, health);
        if (heal > 0.0F) {
            player.heal(heal);
        }

        float leftover = health - heal;
        int food = player.getFoodData().getFoodLevel();
        int gain = Math.max(0, Math.min(20 - food, Math.round(leftover)));
        if (gain > 0) {
            player.getFoodData().setFoodLevel(food + gain);
        }

        clear(stack);

        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 0.4, 0.7, 0.4, 0.08);
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        player.displayClientMessage(Component.translatable("message.spirit_form.catcher.absorbed",
                String.valueOf(Math.round(heal / 2.0F * 10.0F) / 10.0F), String.valueOf(gain)), true);
        return stack;
    }

    // ---------- вигляд ----------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spirit_form.artifact.movement").withStyle(ChatFormatting.GOLD));
        CompoundTag mob = stored(stack);
        if (mob == null) {
            tooltip.add(Component.translatable("tooltip.spirit_form.catcher.empty").withStyle(ChatFormatting.GRAY));
        } else {
            Component name = EntityType.byString(mob.getString("id"))
                    .map(EntityType::getDescription)
                    .orElse(Component.literal(mob.getString("id")));
            tooltip.add(Component.translatable("tooltip.spirit_form.catcher.full", name).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.spirit_form.catcher.health",
                    String.valueOf(Math.round(mob.getFloat("Health") / 2.0F * 10.0F) / 10.0F))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
