# Шаблон виміру з біомом

## Куди що копіювати

Усе з `data/spirit_form/` копіюється в `src/main/resources/data/spirit_form/` (структура папок та сама).

| Файл | Що це |
|---|---|
| `dimension_type/my_dimension.json` | Правила виміру: висота, світло, небо, ліжка |
| `dimension/my_dimension.json` | Генератор: ландшафт і джерело біомів (зараз один біом) |
| `worldgen/biome/my_biome.json` | Порожній біом: кольори є, дерев, руд й істот ще немає |
| `worldgen/configured_feature/my_feature.json` та `placed_feature/my_feature.json` | Приклад об'єкта: купки блоків під землею. Щоб працював, додайте `"spirit_form:my_feature"` у `features` біома (7-й список, етап підземних руд) |
| `alternatives/biome_source_multi_noise.json` | Замінює `biome_source` для кількох біомів |
| `alternatives/generator_flat.json` | Замінює весь `generator` для плоского світу |
| `java/MyDimension.java` | Код: ключ виміру й телепорт у нього та назад. У `src/main/java/com/spiritform/` |

## Порядок

1. Змініть `my_dimension`, `my_biome`, `my_feature` на свої назви (у назвах файлів і всередині файлів: `type`, `biome`, `feature`).
2. Скопіюйте файли. Для кількох біомів: скопіюйте `my_biome.json` під новою назвою й підставте `biome_source` з `alternatives/`.
3. Заповніть біом: кольори в `effects` (десяткові числа RGB), список `features` (11 етапів, порядок у `docs/DIMENSION_UK.md`), `spawners` для істот.
4. Вирішіть, як гравець потрапляє у вимір, і викличте `MyDimension.enter(player, x, y, z)`. Для швидкої перевірки: `/execute in spirit_form:my_dimension run tp @s 0 120 0`.
5. Перезапустіть гру (новий `dimension_type` читається при старті), створіть новий світ або заходьте в нові чанки.

Докладно: `docs/DIMENSION_UK.md`.
