package com.spiritform;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * Станції здібності Самодостатність. Порядок не змінювати: за ним зберігається маска відкритих станцій.
 * Ідентифікатор у розташуванні (SpiritLoadout) має вигляд "station_<id>".
 *
 * Останні вісім значень (EXT1..EXT8) - порожні місця для станцій з інших модів: що саме в них лежить, задає
 * список блоків у даних гравця (його додають у меню розвитку). Поки запису немає, станція недоступна й ніде не показується.
 */
public enum Station {
    CRAFTING("crafting", Items.CRAFTING_TABLE, false),
    FURNACE("furnace", Items.FURNACE, true),
    ENCHANTING("enchanting", Items.ENCHANTING_TABLE, false),
    BREWING("brewing", Items.BREWING_STAND, true),
    SMOKER("smoker", Items.SMOKER, true),
    BLAST_FURNACE("blast_furnace", Items.BLAST_FURNACE, true),
    ANVIL("anvil", Items.ANVIL, false),
    SMITHING("smithing", Items.SMITHING_TABLE, false),
    STONECUTTER("stonecutter", Items.STONECUTTER, false),
    GRINDSTONE("grindstone", Items.GRINDSTONE, false),
    CARTOGRAPHY("cartography", Items.CARTOGRAPHY_TABLE, false),
    LOOM("loom", Items.LOOM, false),
    EXT1("ext1", null, false),
    EXT2("ext2", null, false),
    EXT3("ext3", null, false),
    EXT4("ext4", null, false),
    EXT5("ext5", null, false),
    EXT6("ext6", null, false),
    EXT7("ext7", null, false),
    EXT8("ext8", null, false);

    public static final String PREFIX = "station_";
    public static final int PER_PAGE = 4;
    /** Скільки сторінок можна зберегти (12 власних станцій + 8 зовнішніх). */
    public static final int PAGES = 5;
    /** Кількість власних станцій; далі йдуть зовнішні. */
    public static final int BUILTIN = 12;

    public final String id;
    private final ItemLike item;
    /** Станція виробляє предмети з часом і пам'ятає їх (печі, варильна стійка). */
    public final boolean persistent;

    Station(String id, ItemLike item, boolean persistent) {
        this.id = id;
        this.item = item;
        this.persistent = persistent;
    }

    /** Розібраний запис конфігу для зовнішньої станції. */
    public record External(Block block, Item icon) {
    }

    public boolean external() {
        return ordinal() >= BUILTIN;
    }

    /** Звідки клієнт бере список блоків зовнішніх станцій своєї гравця (виставляє клієнтська частина мода). */
    private static volatile java.util.function.Supplier<List<String>> source = List::of;

    public static void setSource(java.util.function.Supplier<List<String>> supplier) {
        source = supplier;
    }

    public static List<String> clientBlocks() {
        try {
            return source.get();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private External spec(List<String> blocks) {
        int index = ordinal() - BUILTIN;
        if (index < 0 || blocks == null || index >= blocks.size() || blocks.get(index).isBlank()) {
            return null;
        }
        ResourceLocation blockId = ResourceLocation.tryParse(blocks.get(index).trim());
        if (blockId == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
            return null;
        }
        return new External(block, block.asItem());
    }

    /** Блок зовнішньої станції (null для власних і порожніх комірок). */
    public Block externalBlock(List<String> blocks) {
        External spec = external() ? spec(blocks) : null;
        return spec == null ? null : spec.block();
    }

    /** Чи станція існує в цього гравця: власні завжди, зовнішні лише якщо комірку заповнено. */
    public boolean available(List<String> blocks) {
        return !external() || spec(blocks) != null;
    }

    public boolean available() {
        return available(clientBlocks());
    }

    /** Скільки сторінок колеса потрібно, щоб помістити всі наявні станції. */
    public static int pages(List<String> blocks) {
        int last = BUILTIN;
        for (Station station : values()) {
            if (station.external() && station.available(blocks)) {
                last = station.ordinal() + 1;
            }
        }
        return Math.max(3, Math.min(PAGES, (last + PER_PAGE - 1) / PER_PAGE));
    }

    public static int pages() {
        return pages(clientBlocks());
    }

    public int bit() {
        return 1 << ordinal();
    }

    /** Ідентифікатор у розташуванні. */
    public String loadoutId() {
        return PREFIX + id;
    }

    public ItemStack stack(List<String> blocks) {
        if (external()) {
            External spec = spec(blocks);
            return spec == null || spec.icon() == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(spec.icon());
        }
        return new ItemStack(item);
    }

    public ItemStack stack() {
        return stack(clientBlocks());
    }

    public Component displayName(List<String> blocks) {
        if (external()) {
            External spec = spec(blocks);
            return spec == null ? Component.literal(id) : spec.block().getName();
        }
        return stack(blocks).getHoverName();
    }

    public Component displayName() {
        return displayName(clientBlocks());
    }

    public int defaultPage() {
        return ordinal() / PER_PAGE;
    }

    public static Station byLoadoutId(String loadoutId) {
        if (loadoutId == null || !loadoutId.startsWith(PREFIX)) {
            return null;
        }
        String id = loadoutId.substring(PREFIX.length());
        for (Station station : values()) {
            if (station.id.equals(id)) {
                return station;
            }
        }
        return null;
    }

    public static Station byIndex(int index) {
        Station[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }
}
