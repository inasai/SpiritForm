# Вимір Потойбіччя: як змінювати світ і додавати біоми

Це документ про те, як влаштований вимір `spirit_form:beyond` і як його змінювати. Усе робиться JSON-файлами в папці `src/main/resources/data/spirit_form/`, код писати майже не потрібно.

## З чого складається вимір

| Файл | За що відповідає |
|---|---|
| `dimension_type/beyond.json` | Правила виміру: висота світу, світло, чи працюють ліжка, який вигляд неба |
| `dimension/beyond.json` | Як створюється ландшафт і які біоми де стоять |
| `worldgen/biome/beyond.json` | Єдиний біом: кольори, дерева й руди, істоти |
| `SpiritBeyond.java` | Вхід у вимір, повернення, точка появи (код, змінювати не обов'язково) |

Зараз вимір це «звичайний світ» (ландшафт Верхнього світу, `"settings": "minecraft:overworld"`), але весь заповнений одним біомом `spirit_form:beyond`.

## 1. Правила виміру (`dimension_type/beyond.json`)

Найкорисніші поля:

- `min_y` і `height`: нижня межа й висота світу. Мають бути кратні 16. Зараз `-64` і `384`, як у Верхньому світі. `logical_height` не більше за `height`.
- `has_skylight`: чи є денне світло. `false` дає темний вимір.
- `ambient_light`: мінімальне світло (0.0 повна темрява, 0.1 як в Нижньому світі).
- `fixed_time`: додайте, щоб час стояв на місці, наприклад `"fixed_time": 18000` (вічна ніч), `6000` (вічний полудень). Без цього поля доба йде як звичайно.
- `bed_works`, `respawn_anchor_works`: чи можна спати й чи працює якір відродження.
- `natural`: якщо `false`, компас і годинник працюють хаотично.
- `effects`: вигляд неба. `minecraft:overworld` це сонце, місяць і зорі; `minecraft:the_end` це «космос» Енду; `minecraft:the_nether` це туман без неба. Власне небо через JSON не робиться, для цього потрібен код.
- `monster_spawn_light_level`: за якого рівня світла з'являються монстри (0-7 означає у темряві).
- `coordinate_scale`: масштаб координат для порталів, у нас порталів немає.

Після зміни цього файлу **перезапустіть гру** (команда `/reload` його не перечитує).

## 2. Ландшафт і розміщення біомів (`dimension/beyond.json`)

Зараз:

```json
{
  "type": "spirit_form:beyond",
  "generator": {
    "type": "minecraft:noise",
    "settings": "minecraft:overworld",
    "biome_source": { "type": "minecraft:fixed", "biome": "spirit_form:beyond" }
  }
}
```

- `settings` визначає форму землі (гори, річки, печери). Варіанти без власного файлу: `minecraft:overworld`, `minecraft:amplified` (дуже високі гори), `minecraft:large_biomes`, `minecraft:caves`, `minecraft:floating_islands`, `minecraft:end`, `minecraft:nether`.
- `biome_source` визначає, який біом де стоїть.

### Кілька біомів: `multi_noise`

Замініть `biome_source` на такий (приклад із двома біомами):

```json
"biome_source": {
  "type": "minecraft:multi_noise",
  "biomes": [
    {
      "biome": "spirit_form:beyond",
      "parameters": {
        "temperature": [-1.0, 0.0], "humidity": [-1.0, 1.0],
        "continentalness": [-1.2, 1.0], "erosion": [-1.0, 1.0],
        "weirdness": [-1.0, 1.0], "depth": 0.0, "offset": 0.0
      }
    },
    {
      "biome": "spirit_form:ashen_fields",
      "parameters": {
        "temperature": [0.0, 1.0], "humidity": [-1.0, 1.0],
        "continentalness": [-1.2, 1.0], "erosion": [-1.0, 1.0],
        "weirdness": [-1.0, 1.0], "depth": 0.0, "offset": 0.0
      }
    }
  ]
}
```

Тут біоми розділені лише за температурою: перший займає холодну половину, другий теплу. `depth` 0.0 означає поверхню (печерні біоми мають `depth` від 0.2 до 0.9). Діапазони треба вибирати так, щоб вони покривали всі значення без дірок, інакше гра підставить найближчий за параметрами біом.

Якщо хочете вручну розставити біоми, «як намалювати карту», найпростіше використовувати `"type": "minecraft:checkerboard"` (шашкова дошка: біоми чергуються плитками; `scale` задає розмір плитки): `"biomes": ["spirit_form:beyond", "spirit_form:ashen_fields"], "scale": 4`.

### Власний ландшафт

- **Плоский світ:** `"generator": {"type": "minecraft:flat", "settings": {"biome": "spirit_form:beyond", "layers": [{"block": "minecraft:bedrock", "height": 1}, {"block": "minecraft:stone", "height": 60}, {"block": "minecraft:grass_block", "height": 1}], "features": false, "lakes": false, "structure_overrides": []}}`.
- **Свій noise-файл:** скопіюйте `noise_settings/overworld.json` із застосунку Minecraft (вона є в `client.jar` у `data/minecraft/worldgen/noise_settings/`) до `data/spirit_form/worldgen/noise_settings/beyond.json`, змініть її (висоту моря `sea_level`, блоки `default_block` / `default_fluid`, щільність, правила поверхні `surface_rule`) і вкажіть у `settings`: `"spirit_form:beyond"`. Це найпотужніший спосіб, але й найскладніший: файл дуже великий.

## 3. Новий біом

Створіть файл `worldgen/biome/<назва>.json` і додайте його до `biome_source`. Обов'язкові поля (копіюйте з `beyond.json`):

- `has_precipitation`, `temperature`, `downfall`: дощ, температура (від неї залежить, чи падає сніг: нижче 0.15 це сніг).
- `effects`: кольори `sky_color`, `fog_color`, `water_color`, `water_fog_color` (звичайні числа, десятковий запис RGB: наприклад, м'ятний `0x8CFFC8` це `9240520`). Необов'язково: `grass_color`, `foliage_color`, `ambient_sound`, `music`, `particle`.
- `carvers`, `features`, `spawners`, `spawn_costs`.

Приклад атмосферних частинок у біомі (попіл падає з неба), додайте у `effects`:

```json
"particle": { "probability": 0.005, "options": { "type": "minecraft:ash" } }
```

### Що росте й лежить (`features`)

`features` це 11 списків-етапів генерації (порядок важливий): печери, рідини, руди, ґрунт, дерева, рослини тощо. У нашому біомі заповнено руди (етап 7), джерела (9) і трава (10). Додавайте до потрібного етапу готові назви:

- дерева й рослини: `minecraft:trees_plains`, `minecraft:trees_birch_and_oak`, `minecraft:flower_default`, `minecraft:patch_grass_plain`, `minecraft:brown_mushroom_normal`;
- руди: `minecraft:ore_*`;
- озера й джерела: `minecraft:lake_lava_surface`, `minecraft:spring_water`.

Усі назви можна побачити в застосунку Minecraft (`data/minecraft/worldgen/placed_feature/`).

**Свої об'єкти (наприклад, дерево з вашим блоком):** потрібні два файли:

1. `worldgen/configured_feature/<назва>.json` описує, *що* ставити (структуру).
2. `worldgen/placed_feature/<назва>.json` описує, *де й як часто* (кількість, висота, біом).

Найпростіший приклад: купка блоків-руди (`minecraft:ore`):

```json
{ "type": "minecraft:ore", "config": { "size": 8, "discard_chance_on_air_exposure": 0.0,
  "targets": [ { "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" },
                 "state": { "Name": "minecraft:glowstone" } } ] } }
```

А це його `placed_feature`:

```json
{ "feature": "spirit_form:my_ore",
  "placement": [ { "type": "minecraft:count", "count": 12 }, { "type": "minecraft:in_square" },
    { "type": "minecraft:height_range", "height": { "type": "minecraft:uniform",
      "min_inclusive": { "absolute": -40 }, "max_inclusive": { "absolute": 60 } } },
    { "type": "minecraft:biome" } ] }
```

Потім додайте `"spirit_form:my_ore"` у відповідний список `features` біома.

### Істоти (`spawners`)

Списки за категоріями: `monster` (вороги), `creature` (мирні), `ambient` (кажани), `water_creature` та інші. Запис:

```json
{ "type": "spirit_form:template_creature", "weight": 8, "minCount": 1, "maxCount": 2 }
```

`weight`: частота (більше значить частіше). Як створити саму істоту: `docs/CREATURES_UK.md`.

`spawn_costs` обмежує густоту: `"spirit_form:wraith": { "energy_budget": 0.7, "charge": 0.15 }`.

## 4. Перевірка

- Новий світ або нові чанки: біоми й ландшафт застосовуються лише до **ще не створених** частин світу. Для перевірки полетіть далеко в новий район або заходьте в новий світ.
- `/execute in spirit_form:beyond run tp @s 0 200 0`: телепорт у вимір вручну.
- `/locate biome spirit_form:ashen_fields`: знайти біом.
- `/place feature spirit_form:my_ore`: поставити об'єкт перед собою.
- `/reload`: перечитує біоми, features і структури, але не `dimension_type` і не `dimension`.
- У разі помилки відкрийте `logs/latest.log`: шукайте рядки `Failed to load registries` або `Not a JSON object`. Найчастіші причини: зайва кома, пропущена лапка, невідома назва біома чи feature.

## 5. Вхід, вихід і точка появи

Правила (не змінюйте без потреби) лежать у `SpiritBeyond.java`:

- вхід можливий лише з досягненням «Король Потойбіччя» через блок Відлуння Потойбіччя при активному Відлунні Смерті;
- гравець з'являється у висоті, де закінчується твердий ґрунт (метод `surfaceY`), а вдруге там, де вийшов;
- вихід зберігає точку, звідки гравець зайшов.

Якщо після зміни ландшафту (наприклад, світ-острови) гравець з'являється в повітрі чи у воді, пишіть, я підправлю `surfaceY`.
