package com.spiritform;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Браслет Фентона. Діє, коли лежить в одній із рук або в слоті Curios "браслет".
 * Ефект завжди один, скільки б браслетів гравець не тримав: подвоює швидкість і силу (модифікатори
 * мають фіксований ідентифікатор і не складаються), а перезарядку здібностей скорочує вдвічі.
 * Інверсію кольорів скіна малює клієнт (див. FentonSkin).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class FentonBracelet {

    private static final ResourceLocation SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "fenton_speed");
    private static final ResourceLocation STRENGTH_ID =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "fenton_strength");
    private static final ResourceLocation JUMP_ID =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "fenton_jump");

    private FentonBracelet() {
    }

    private static boolean isBracelet(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SpiritItems.FENTONS_BRACELET.get());
    }

    private static boolean inHands(LivingEntity entity) {
        return isBracelet(entity.getItemInHand(InteractionHand.MAIN_HAND))
                || isBracelet(entity.getItemInHand(InteractionHand.OFF_HAND));
    }

    /** Чи діє браслет: у руці або в Curios (навіть якщо його сховано "оком"). */
    public static boolean isWearing(LivingEntity entity) {
        return inHands(entity)
                || CuriosCompat.isEquipped(entity, SpiritItems.FENTONS_BRACELET.get());
    }

    /** Чи видно браслет на персонажі (а значить, інвертуються кольори скіна): рука або видимий слот Curios. */
    public static boolean invertsSkin(LivingEntity entity) {
        return inHands(entity)
                || CuriosCompat.state(entity, SpiritItems.FENTONS_BRACELET.get()) == CuriosCompat.State.VISIBLE;
    }

    /** Чи малювати браслет на тілі: лежить у видимому слоті Curios і не тримається в руці (там він і так видний). */
    public static boolean shownOnBody(LivingEntity entity) {
        return !inHands(entity)
                && CuriosCompat.state(entity, SpiritItems.FENTONS_BRACELET.get()) == CuriosCompat.State.VISIBLE;
    }

    /** Намисто Потойбіччя діє: нагрудником або в слоті Curios "намисто" (навіть сховане). */
    public static boolean hasNecklace(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(SpiritItems.NECKLACE.get())
                || CuriosCompat.isEquipped(entity, SpiritItems.NECKLACE.get());
    }

    /** Чи видно намисто: нагрудник завжди, слот Curios лише з відкритим "оком". */
    public static boolean showsNecklace(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(SpiritItems.NECKLACE.get())
                || CuriosCompat.state(entity, SpiritItems.NECKLACE.get()) == CuriosCompat.State.VISIBLE;
    }

    /** Вартість здібності голодом: з Намистом Потойбіччя нуль. */
    public static int cost(Player player, int base) {
        return base > 0 && hasNecklace(player) ? 0 : base;
    }

    /** Перезарядка здібності в тіках: з Стигматом Фантома удвічі швидше (мінімум 1 тік, якщо вона була). */
    public static long cooldown(Player player, long baseTicks) {
        if (baseTicks <= 0 || !isWearing(player)) {
            return baseTicks;
        }
        return Math.max(1L, baseTicks / 2L);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean wearing = isWearing(player);
        apply(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, wearing, 0.5); // 50% faster
        apply(player.getAttribute(Attributes.ATTACK_DAMAGE), STRENGTH_ID, wearing, 2.0); // 100% stronger
        apply(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID, wearing, 1.0); // 100% higher jump
    }

    @SubscribeEvent
    public static void onPlayerFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isWearing(player)) {
                event.setCanceled(true); // Скасовуємо падіння і шкоду від нього
            }
        }
    }

    private static void apply(AttributeInstance attribute, ResourceLocation id, boolean on, double bonus) {
        if (attribute == null) {
            return;
        }
        if (on) {
            if (!attribute.hasModifier(id)) {
                attribute.addTransientModifier(
                        new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (attribute.hasModifier(id)) {
            attribute.removeModifier(id);
        }
    }
}
