package com.spiritform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

/** Власні іконки душ (лише на клієнті): душа -> предмет. Файл config/spirit_form-army-icons.json. */
public final class SpiritArmyIcons {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, String> ICONS = new HashMap<>();
    private static boolean loaded;

    private SpiritArmyIcons() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("spirit_form-army-icons.json");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                try {
                    ICONS.put(UUID.fromString(entry.getKey()), entry.getValue().getAsString());
                } catch (IllegalArgumentException ignored) {
                    // пошкоджений запис пропускаємо
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Spirit Form: could not read the army icons", e);
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<UUID, String> entry : ICONS.entrySet()) {
            root.addProperty(entry.getKey().toString(), entry.getValue());
        }
        try {
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Spirit Form: could not save the army icons", e);
        }
    }

    /** Власна іконка душі або порожній стек, якщо її не вибрано. */
    public static ItemStack get(UUID soul) {
        ensureLoaded();
        String key = ICONS.get(soul);
        ResourceLocation id = key == null ? null : ResourceLocation.tryParse(key);
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Ставить іконку-предмет; null повертає стандартну. */
    public static void set(UUID soul, Item item) {
        ensureLoaded();
        if (item == null) {
            ICONS.remove(soul);
        } else {
            ICONS.put(soul, BuiltInRegistries.ITEM.getKey(item).toString());
        }
        save();
    }
}
