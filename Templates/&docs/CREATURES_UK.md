# Як створити нову істоту Потойбіччя

У моді вже є робочий шаблон: істота **TemplateCreature** («Шаблонна істота»). Вона живе лише у вимірі Потойбіччя, має яйце виклику у творчій вкладці Spirit Form й використовує тимчасову текстуру. Нову істоту найлегше зробити, скопіювавши шаблон.

## Із чого складається істота

| Файл | Для чого |
|---|---|
| `TemplateCreature.java` | Сама істота: характеристики, поведінка, звуки |
| `TemplateCreatureModel.java` | Модель (форма з кубів і анімація кроків) |
| `TemplateCreatureRenderer.java` | З'єднує модель і текстуру |
| `SpiritCreatures.java` | Реєстрація, характеристики, місце появи (усі істоти в одному файлі) |
| `SpiritCreaturesClient.java` | Яку модель і відображення має кожна істота |
| `textures/entity/template_creature.png` | Текстура |
| `worldgen/biome/beyond.json` | Де й як часто істота з'являється |
| `lang/uk_ua.json`, `lang/en_us.json` | Назви істоти та яйця |
| `models/item/template_creature_spawn_egg.json` | Модель яйця |

## Покроково (приклад: істота «Wraith»)

1. **Скопіюйте три файли** `TemplateCreature`, `TemplateCreatureModel`, `TemplateCreatureRenderer`. В IntelliJ це найзручніше робити так: ПКМ по класу → Refactor → Copy Class, нова назва `Wraith`. Назви моделі й відображення зробіть `WraithModel` та `WraithRenderer`. Усередині `WraithModel` змініть `"template_creature"` на `"wraith"` у `LAYER`, а в `WraithRenderer` шлях текстури на `textures/entity/wraith.png`.
2. **У `SpiritCreatures.java`** скопіюйте блок «ІСТОТА: TemplateCreature» і замініть назви на `Wraith` / `"wraith"`. Те саме зробіть у двох методах нижче (`onAttributes` і `onSpawnPlacements`): додайте по одному рядку для нової істоти.
3. **У `SpiritCreaturesClient.java`** додайте по одному рядку в `onLayers` та `onRenderers`.
4. **Текстура:** покладіть `wraith.png` у `src/main/resources/assets/spirit_form/textures/entity/`.
5. **Назви:** додайте в обидва lang-файли рядки
   `"entity.spirit_form.wraith"` та `"item.spirit_form.wraith_spawn_egg"`.
6. **Яйце виклику:** скопіюйте `models/item/template_creature_spawn_egg.json` у `wraith_spawn_egg.json`, а в `SpiritItems.java` в `displayItems` додайте `output.accept(SpiritCreatures.WRAITH_EGG.get());`.
7. **Місце появи:** у `beyond.json` у списку `"monster"` додайте ще один запис
   `{ "type": "spirit_form:wraith", "weight": 8, "minCount": 1, "maxCount": 2 }`.
   `weight` це частота (більше число означає частіше). Для мирних істот використовуйте список `"creature"`, а в `SpiritCreatures` замість `MobCategory.MONSTER` поставте `MobCategory.CREATURE`.

Істота з'являється лише у біомах, де її вписано, тому у звичайному світі вона не зустрінеться.

## Характеристики та поведінка

- `createAttributes()` у файлі істоти: здоров'я, швидкість, шкода, дальність помічання цілі, броня.
- `registerGoals()`: список дій. Щоб істота була мирною, приберіть `MeleeAttackGoal` і рядки в `targetSelector`. Щоб тікала, додайте `PanicGoal`. Готові класи шукайте у `net.minecraft.world.entity.ai.goal`.

## Власна модель з Blockbench

1. У Blockbench створіть проєкт **Modded Entity** і зробіть модель.
2. Експорт: File → Export → **Export Java Entity** (маппінги Mojang).
3. З отриманого класу перенесіть тіло методу `createBodyLayer()` в `createBodyLayer()` вашої моделі, а код анімації в `setupAnim()`. Назви частин, до яких конструктор моделі звертається через `root.getChild("...")`, мають збігатися з назвами в Blockbench.
4. Розмір текстури в кінці `LayerDefinition.create(mesh, ширина, висота)` має збігатися з розміром png.

## Перевірка

Запустіть гру, у творчому режимі візьміть яйце виклику з вкладки Spirit Form і поставте істоту (або зайдіть у вимір Потойбіччя й почекайте ночі).
