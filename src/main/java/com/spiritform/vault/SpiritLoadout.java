package com.spiritform.vault;

import com.spiritform.ability.SpiritAbility;
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
 * PASSIVE - колесо здібностей (T), має кілька сторінок по 6 клітинок (їх можна додавати й видаляти);
 * AUX - цифри 1-9 у режимі здібностей; MAIN - основні гарячі клавіші справа внизу.
 * На будь-яку панель можна покласти будь-яку здібність: пасивна там перемикається, активна запускається.
 * Винятки: утримувані здібності (Доторк, Дарування Життя, Терези) не працюють із колеса, а Інстинкт живе лише на колесі.
 * Кожна здібність стоїть лише в одному місці.
 */
public final class SpiritLoadout {

    public enum Bar {
        PASSIVE(6),
        AUX(9),
        MAIN(3),
        /** Станції Самодостатності: 3 сторінки по 4 комірки. */
        STATION(4);

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

    /** Скільки сторінок колеса можна мати найбільше. */
    public static final int MAX_PAGES = 10;

    /** Цифри й основні клавіші (PASSIVE тут не використовується, його сторінки окремо). */
    private static final Map<Bar, String[]> SLOTS = new EnumMap<>(Bar.class);
    private static final List<String[]> PASSIVE_PAGES = new ArrayList<>();
    private static final List<String[]> STATION_PAGES = new ArrayList<>();
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
        PASSIVE_PAGES.clear();
        PASSIVE_PAGES.add(pad(Bar.PASSIVE.size, INSTINCT, "icy_touch", "presence", "gaze", "echo", "energy"));
        STATION_PAGES.clear();
        for (int page = 0; page < Station.PAGES; page++) {
            String[] slots = pad(Bar.STATION.size);
            for (Station station : Station.values()) {
                if (station.available() && station.defaultPage() == page) {
                    slots[station.ordinal() % Station.PER_PAGE] = station.loadoutId();
                }
            }
            STATION_PAGES.add(slots);
        }
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

            if (current) {
                java.util.Set<String> seen = new java.util.HashSet<>();

                // Колесо: масив сторінок (версія 7) або одна плоска сторінка (старі файли)
                JsonElement passive = root.get(Bar.PASSIVE.key());
                if (passive != null && passive.isJsonArray()) {
                    JsonArray array = passive.getAsJsonArray();
                    List<String[]> pages = new ArrayList<>();
                    boolean nested = array.size() > 0 && array.get(0).isJsonArray();
                    if (nested) {
                        for (JsonElement page : array) {
                            if (page.isJsonArray() && pages.size() < MAX_PAGES) {
                                pages.add(readSlots(Bar.PASSIVE, page.getAsJsonArray(), seen));
                            }
                        }
                    } else {
                        pages.add(readSlots(Bar.PASSIVE, array, seen));
                    }
                    if (!pages.isEmpty()) {
                        PASSIVE_PAGES.clear();
                        PASSIVE_PAGES.addAll(pages);
                    }
                }

                JsonElement stations = root.get(Bar.STATION.key());
                if (stations != null && stations.isJsonArray()) {
                    List<String[]> pages = new ArrayList<>();
                    for (JsonElement page : stations.getAsJsonArray()) {
                        if (page.isJsonArray() && pages.size() < Station.PAGES) {
                            pages.add(readSlots(Bar.STATION, page.getAsJsonArray(), new java.util.HashSet<>()));
                        }
                    }
                    while (pages.size() < Station.PAGES) {
                        pages.add(pad(Bar.STATION.size));
                    }
                    STATION_PAGES.clear();
                    STATION_PAGES.addAll(pages);
                }

                for (Bar bar : new Bar[] {Bar.AUX, Bar.MAIN}) {
                    JsonElement element = root.get(bar.key());
                    if (element != null && element.isJsonArray()) {
                        SLOTS.put(bar, readSlots(bar, element.getAsJsonArray(), seen));
                    }
                }
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
            // Версія 6: Енергія Потойбіччя (пасивна) у вільну клітинку колеса
            if (current && version.getAsInt() < 6) {
                addMissing("energy");
                save();
            }
            // Версія 8: Самодостатність. Кладемо у вільну клітинку, якщо така є
            if (current && version.getAsInt() < 8) {
                addMissing("self_sufficiency");
                save();
            }
        } catch (Exception e) {
            LOGGER.warn("Spirit Form: could not read the ability loadout, using defaults", e);
            defaults();
        }
    }

    /** Читає одну панель із файлу: лише дозволені здібності, без повторів. */
    private static String[] readSlots(Bar bar, JsonArray array, java.util.Set<String> seen) {
        String[] slots = pad(bar.size);
        for (int i = 0; i < bar.size && i < array.size(); i++) {
            String id = array.get(i).getAsString();
            if (!id.isEmpty() && allowed(bar, id) && seen.add(id)) {
                slots[i] = id;
            }
        }
        return slots;
    }

    private static boolean placed(String id) {
        for (String[] page : PASSIVE_PAGES) {
            if (Arrays.asList(page).contains(id)) {
                return true;
            }
        }
        return Arrays.asList(SLOTS.get(Bar.AUX)).contains(id) || Arrays.asList(SLOTS.get(Bar.MAIN)).contains(id);
    }

    private static boolean putInFirstFree(String[] slots, String id) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) {
                slots[i] = id;
                return true;
            }
        }
        return false;
    }

    /** Нові здібності з'являються у вільній клітинці: пасивні на колесі, активні на цифрах. */
    private static void addMissing(String... ids) {
        for (String id : ids) {
            if (placed(id)) {
                continue;
            }
            SpiritAbility ability = SpiritAbility.byId(id);
            boolean passive = ability != null && ability.kind == SpiritAbility.Kind.PASSIVE;
            if (passive) {
                for (String[] page : PASSIVE_PAGES) {
                    if (putInFirstFree(page, id)) {
                        break;
                    }
                }
            } else if (!putInFirstFree(SLOTS.get(Bar.AUX), id)) {
                putInFirstFree(SLOTS.get(Bar.MAIN), id);
            }
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 8);
        JsonArray pages = new JsonArray();
        for (String[] page : PASSIVE_PAGES) {
            JsonArray array = new JsonArray();
            for (String id : page) {
                array.add(id);
            }
            pages.add(array);
        }
        root.add(Bar.PASSIVE.key(), pages);
        JsonArray stationPages = new JsonArray();
        for (String[] page : STATION_PAGES) {
            JsonArray array = new JsonArray();
            for (String id : page) {
                array.add(id);
            }
            stationPages.add(array);
        }
        root.add(Bar.STATION.key(), stationPages);
        for (Bar bar : new Bar[] {Bar.AUX, Bar.MAIN}) {
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

    /**
     * Зовнішні станції гравця потрапляють на колесо самі: нова стає в першу вільну клітинку (з перших сторінок),
     * а знята зі списку зникає з розташування.
     */
    public static void syncExternal(List<String> blocks) {
        ensureLoaded();
        boolean changed = false;
        int pages = Math.min(STATION_PAGES.size(), Station.pages(blocks));
        for (Station station : Station.values()) {
            if (!station.external()) {
                continue;
            }
            String id = station.loadoutId();
            boolean placed = false;
            for (String[] page : STATION_PAGES) {
                for (String slot : page) {
                    placed |= slot.equals(id);
                }
            }
            if (station.available(blocks)) {
                if (placed) {
                    continue;
                }
                search:
                for (int page = 0; page < pages; page++) {
                    String[] slots = STATION_PAGES.get(page);
                    for (int i = 0; i < slots.length; i++) {
                        if (slots[i].isEmpty()) {
                            slots[i] = id;
                            changed = true;
                            break search;
                        }
                    }
                }
            } else if (placed) {
                for (String[] page : STATION_PAGES) {
                    clear(page, id);
                }
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    /** Чи можна класти цю здібність на панель. */
    public static boolean allowed(Bar bar, String id) {
        if (Station.byLoadoutId(id) != null) {
            return bar == Bar.STATION;
        }
        if (bar == Bar.STATION) {
            return false;
        }
        if (INSTINCT.equals(id)) {
            return bar == Bar.PASSIVE;
        }
        SpiritAbility ability = SpiritAbility.byId(id);
        if (ability == null) {
            return false;
        }
        // Утримувані здібності потребують клавіші, яку колесо дати не може
        return bar != Bar.PASSIVE || !ability.hold;
    }

    /** Які здібності можна класти на цю панель (показуються в нижньому ряду редактора). */
    public static List<String> candidates(Bar bar) {
        List<String> ids = new ArrayList<>();
        if (bar == Bar.STATION) {
            for (Station station : Station.values()) {
                if (station.available()) {
                    ids.add(station.loadoutId());
                }
            }
            return ids;
        }
        if (bar == Bar.PASSIVE) {
            ids.add(INSTINCT);
        }
        for (SpiritAbility ability : SpiritAbility.values()) {
            if (allowed(bar, ability.id)) {
                ids.add(ability.id);
            }
        }
        return ids;
    }

    // ---------- сторінки колеса ----------

    public static int passivePages() {
        ensureLoaded();
        return PASSIVE_PAGES.size();
    }

    /** Додає порожню сторінку колеса й повертає її номер (або -1, якщо досягнуто межі). */
    public static int addPassivePage() {
        ensureLoaded();
        if (PASSIVE_PAGES.size() >= MAX_PAGES) {
            return -1;
        }
        PASSIVE_PAGES.add(pad(Bar.PASSIVE.size));
        save();
        return PASSIVE_PAGES.size() - 1;
    }

    /** Видаляє сторінку колеса (остання лишається); її здібності стають вільними. */
    public static void removePassivePage(int page) {
        ensureLoaded();
        if (PASSIVE_PAGES.size() <= 1 || page < 0 || page >= PASSIVE_PAGES.size()) {
            return;
        }
        PASSIVE_PAGES.remove(page);
        save();
    }

    // ---------- клітинки ----------

    private static String[] slots(Bar bar, int page) {
        if (bar == Bar.PASSIVE) {
            int index = Math.max(0, Math.min(PASSIVE_PAGES.size() - 1, page));
            return PASSIVE_PAGES.get(index);
        }
        if (bar == Bar.STATION) {
            return STATION_PAGES.get(Math.max(0, Math.min(STATION_PAGES.size() - 1, page)));
        }
        return SLOTS.get(bar);
    }

    /** Ідентифікатор здібності в клітинці першої сторінки або порожній рядок. */
    public static String get(Bar bar, int index) {
        return get(bar, 0, index);
    }

    /** Ідентифікатор здібності в клітинці (для цифр і основних сторінка не береться до уваги) або порожній рядок. */
    public static String get(Bar bar, int page, int index) {
        ensureLoaded();
        String[] slots = slots(bar, page);
        return index >= 0 && index < slots.length ? slots[index] : "";
    }

    public static void set(Bar bar, int index, String id) {
        set(bar, 0, index, id);
    }

    /** Кладе здібність у клітинку (порожній рядок очищає). Одна здібність стоїть лише в одному місці. */
    public static void set(Bar bar, int page, int index, String id) {
        ensureLoaded();
        if (index < 0 || index >= bar.size) {
            return;
        }
        if (!id.isEmpty()) {
            if (!allowed(bar, id)) {
                return;
            }
            if (bar == Bar.STATION) {
                for (String[] page2 : STATION_PAGES) {
                    clear(page2, id);
                }
            } else {
                for (String[] page2 : PASSIVE_PAGES) {
                    clear(page2, id);
                }
                clear(SLOTS.get(Bar.AUX), id);
                clear(SLOTS.get(Bar.MAIN), id);
            }
        }
        slots(bar, page)[index] = id;
        save();
    }

    private static void clear(String[] slots, String id) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].equals(id)) {
                slots[i] = "";
            }
        }
    }

    public static SpiritAbility ability(String id) {
        return SpiritAbility.byId(id);
    }

    public static Component name(String id) {
        Station station = Station.byLoadoutId(id);
        if (station != null) {
            return station.displayName();
        }
        return INSTINCT.equals(id)
                ? Component.translatable("passive.spirit_form.instinct")
                : SpiritAbility.byId(id) == null ? Component.literal(id) : SpiritAbility.byId(id).displayName();
    }
}
