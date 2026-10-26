package com.spiritform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * Розташування здібностей на трьох панелях і власні іконки здібностей (лише на клієнті).
 * Зберігається у файлі config/spirit_form-loadout.json.
 *
 * PASSIVE - колесо пасивних здібностей (T); AUX - цифри 1-9 у режимі здібностей;
 * MAIN - основні гарячі клавіші справа внизу.
 */
public final class SpiritLoadout {

    public enum Bar {
        PASSIVE(6),
        AUX(9),
        MAIN(3);

        public final int size;

        Bar(int size) {
            this.size = size;
        }

        public String key() {
            return name().toLowerCase();
        }
    }

    /** "Інстинкт Привида" не окрема здібність із покупкою, але теж живе на колесі. */
    public static final String INSTINCT = "instinct";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<Bar, String[]> SLOTS = new EnumMap<>(Bar.class);
    private static boolean loaded;

    private SpiritLoadout() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("spirit_form-loadout.json");
    }

    private static String[] pad(int size, String... ids) {
        String[] result = new String[size];
        Arrays.fill(result, "");
        for (int i = 0; i < ids.length && i < size; i++) {
            result[i] = ids[i];
        }
        return result;
    }

    private static void defaults() {
        SLOTS.put(Bar.PASSIVE, pad(Bar.PASSIVE.size, INSTINCT, "icy_touch", "presence", "gaze", "echo"));
        SLOTS.put(Bar.AUX, pad(Bar.AUX.size, "vanish", "possession", "delirium", "immunity", "life_gift", "scales", "scream", "summon", "sense"));
        SLOTS.put(Bar.MAIN, pad(Bar.MAIN.size, "touch", "step", "army"));
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        defaults();

        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();

            // Версія 2: основних здібностей три, решта в цифрах. Старе розташування скидаємо, іконки лишаємо.
            JsonElement version = root.get("version");
            boolean current = version != null && version.isJsonPrimitive() && version.getAsInt() >= 2;

            for (Bar bar : Bar.values()) {
                if (!current) {
                    break;
                }
                JsonElement element = root.get(bar.key());
                if (element == null || !element.isJsonArray()) {
                    continue;
                }
                JsonArray array = element.getAsJsonArray();
                String[] slots = pad(bar.size);
                for (int i = 0; i < bar.size && i < array.size(); i++) {
                    String id = array.get(i).getAsString();
                    if (candidates(bar).contains(id)) {
                        slots[i] = id;
                    }
                }
                SLOTS.put(bar, slots);
            }

            // Версія 3: з'явилися нові здібності, їх ставимо у вільні клітинки, решту не чіпаємо
            if (current && version.getAsInt() < 3) {
                addMissing("echo", "scream", "summon");
            }
            // Версія 4: Армія Потойбіччя
            if (current && version.getAsInt() < 4) {
                addMissing("army");
            }
            // Версія 5: Армія на N (основна), Чуття на цифрі 9. Лише якщо там ще стоїть старе типове розташування
            if (current && version.getAsInt() < 5) {
                String[] main = SLOTS.get(Bar.MAIN);
                String[] aux = SLOTS.get(Bar.AUX);
                if ("sense".equals(main[2]) && "army".equals(aux[8])) {
                    main[2] = "army";
                    aux[8] = "sense";
                }
                save();
            }
        } catch (Exception e) {
            LOGGER.warn("Spirit Form: could not read the ability loadout, using defaults", e);
            defaults();
        }
    }

    private static void addMissing(String... ids) {
        for (String id : ids) {
            Bar bar = candidates(Bar.PASSIVE).contains(id) ? Bar.PASSIVE : Bar.AUX;
            boolean placed = false;
            for (Bar other : sameGroup(bar)) {
                for (String slot : SLOTS.get(other)) {
                    placed |= slot.equals(id);
                }
            }
            for (Bar other : sameGroup(bar)) {
                if (placed) {
                    break;
                }
                String[] slots = SLOTS.get(other);
                for (int i = 0; i < slots.length && !placed; i++) {
                    if (slots[i].isEmpty()) {
                        slots[i] = id;
                        placed = true;
                    }
                }
            }
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 5);
        for (Bar bar : Bar.values()) {
            JsonArray array = new JsonArray();
            for (String id : SLOTS.get(bar)) {
                array.add(id);
            }
            root.add(bar.key(), array);
        }

        try {
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Spirit Form: could not save the ability loadout", e);
        }
    }

    /** Які здібності взагалі можна класти на цю панель. */
    public static List<String> candidates(Bar bar) {
        List<String> ids = new ArrayList<>();
        if (bar == Bar.PASSIVE) {
            ids.add(INSTINCT);
        }
        for (SpiritAbility ability : SpiritAbility.values()) {
            boolean passive = ability.kind == SpiritAbility.Kind.PASSIVE;
            if (passive == (bar == Bar.PASSIVE)) {
                ids.add(ability.id);
            }
        }
        return ids;
    }

    /** Ідентифікатор здібності в клітинці або порожній рядок. */
    public static String get(Bar bar, int index) {
        ensureLoaded();
        String[] slots = SLOTS.get(bar);
        return index >= 0 && index < slots.length ? slots[index] : "";
    }

    /** Кладе здібність у клітинку (порожній рядок очищає). Одна здібність стоїть лише в одному місці. */
    public static void set(Bar bar, int index, String id) {
        ensureLoaded();
        if (index < 0 || index >= bar.size) {
            return;
        }
        if (!id.isEmpty()) {
            if (!candidates(bar).contains(id)) {
                return;
            }
            for (Bar other : sameGroup(bar)) {
                String[] slots = SLOTS.get(other);
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i].equals(id)) {
                        slots[i] = "";
                    }
                }
            }
        }
        SLOTS.get(bar)[index] = id;
        save();
    }

    /** Активні здібності діляться між цифрами й основними клавішами, пасивні окремо. */
    private static Bar[] sameGroup(Bar bar) {
        return bar == Bar.PASSIVE ? new Bar[] {Bar.PASSIVE} : new Bar[] {Bar.AUX, Bar.MAIN};
    }

    public static SpiritAbility ability(String id) {
        return SpiritAbility.byId(id);
    }

    public static Component name(String id) {
        return INSTINCT.equals(id)
                ? Component.translatable("passive.spirit_form.instinct")
                : SpiritAbility.byId(id) == null ? Component.literal(id) : SpiritAbility.byId(id).displayName();
    }
}
