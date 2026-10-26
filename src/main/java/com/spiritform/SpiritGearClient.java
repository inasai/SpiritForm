package com.spiritform;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Клієнтська частина обладунків Потойбіччя: "Погляд з порожнечі" (здоров'я й ефекти істоти, на яку дивитеся)
 * і описи властивостей у підказках предметів.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritGearClient {

    private static final double GAZE_RANGE = 48.0;

    private SpiritGearClient() {
    }

    private static LivingEntity lookedAt(Minecraft minecraft, LocalPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(GAZE_RANGE));
        // Стіни закривають огляд
        HitResult block = minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double limit = block.getType() == HitResult.Type.MISS ? GAZE_RANGE : block.getLocation().distanceTo(start);
        Vec3 limited = start.add(look.scale(limit));
        AABB box = player.getBoundingBox().expandTowards(look.scale(limit)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, limited, box,
                e -> !e.isSpectator() && e.isPickable() && e instanceof LivingEntity && e != player, limit * limit);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static String time(int ticks) {
        int seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui
                || !SpiritReaper.hasCrown(player)) {
            return;
        }
        LivingEntity target = lookedAt(minecraft, player);
        if (target == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = graphics.guiWidth() / 2 + 14;
        int y = graphics.guiHeight() / 2 - 12;

        List<Component> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add(target.getDisplayName());
        colors.add(0x8CFFC8);
        lines.add(Component.translatable("screen.spirit_form.gaze.health",
                String.format(Locale.ROOT, "%.1f", target.getHealth()),
                String.format(Locale.ROOT, "%.1f", target.getMaxHealth())));
        colors.add(0xFF7070);
        if (target.getAbsorptionAmount() > 0.0F) {
            lines.add(Component.translatable("screen.spirit_form.gaze.absorption",
                    String.format(Locale.ROOT, "%.1f", target.getAbsorptionAmount())));
            colors.add(0xFFD060);
        }
        int shown = 0;
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (shown++ >= 8) {
                break;
            }
            Component level = effect.getAmplifier() > 0
                    ? Component.literal(" " + (effect.getAmplifier() + 1)) : Component.empty();
            String duration = effect.isInfiniteDuration() ? "∞" : time(effect.getDuration());
            lines.add(Component.empty().append(effect.getEffect().value().getDisplayName()).append(level)
                    .append(Component.literal(" (" + duration + ")")));
            colors.add(effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL ? 0xFF8080 : 0x90FF90);
        }

        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }
        int height = lines.size() * 10 + 4;
        graphics.fill(x - 3, y - 3, x + width + 3, y + height - 1, 0x90101018);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(minecraft.font, lines.get(i), x, y + i * 10, colors.get(i), true);
        }
    }

    /** Описи властивостей обладунків у підказках. */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (stack.is(SpiritItems.SPIRIT_HELMET.get())) {
            tooltip.add(Component.translatable("tooltip.spirit_form.crown"));
        } else if (stack.is(SpiritItems.SPIRIT_CHESTPLATE.get())) {
            tooltip.add(Component.translatable("tooltip.spirit_form.cloak", Config.FORM_EDGE_COOLDOWN_SECONDS.get()));
        } else if (stack.is(SpiritItems.SPIRIT_LEGGINGS.get())) {
            tooltip.add(Component.translatable("tooltip.spirit_form.trousers", Config.GEAR_PERK_SECONDS.get()));
        } else if (stack.is(SpiritItems.SPIRIT_BOOTS.get())) {
            tooltip.add(Component.translatable("tooltip.spirit_form.boots", Config.GEAR_PERK_SECONDS.get()));
        } else {
            return;
        }
        tooltip.add(Component.translatable("tooltip.spirit_form.set"));
    }
}
