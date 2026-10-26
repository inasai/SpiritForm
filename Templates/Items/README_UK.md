# Шаблон зброї й обладунків

## Куди що копіювати

| З папки Templates/Items | У проєкт |
|---|---|
| `java/MyTier.java`, `MyWeaponItem.java` | `src/main/java/com/spiritform/` |
| `java/MyArmorEvents.java` (необов'язково: бонус за повний комплект) | `src/main/java/com/spiritform/` |
| `PASTE_into_SpiritItems_and_Materials.txt` | блоки вставляються в `SpiritArmorMaterials.java` і `SpiritItems.java` |
| `assets/spirit_form/models/item/*.json` | `src/main/resources/assets/spirit_form/models/item/` |
| `assets/spirit_form/textures/item/*.png` (прозорі 16x16) | `.../textures/item/`; намалюйте самі |
| `assets/spirit_form/textures/models/armor/my_armor_layer_1.png`, `_layer_2.png` (64x32) | `.../textures/models/armor/` (шар 1: шолом, нагрудник, чоботи; шар 2: штани) |
| `data/spirit_form/recipe/*.json` (приклади: верстак, ковадло) | `src/main/resources/data/spirit_form/recipe/` |
| `data/minecraft/tags/item/enchantable/*.json` | `src/main/resources/data/minecraft/tags/item/enchantable/`; без них предмети не приймають зачарування |
| `lang_lines.txt` | рядки додаються в обидва lang-файли |

## Порядок

1. Замініть назви: `MyWeapon`, `my_weapon`, `MyArmor`, `my_armor`, `MY_ARMOR`, `MyTier`, `MY_TIER` на свої (див. `Templates/README_UK.md`).
2. Скопіюйте файли й вставте блоки з `PASTE_into_SpiritItems_and_Materials.txt`.
3. Підставте свої числа: шкода й швидкість у `MyWeaponItem`, міцність і швидкість копання в `MyTier`, захист і звук у блоці матеріалу.
4. Намалюйте текстури й видаліть непотрібні рецепти й теги.
5. Перевірка: `/give @s spirit_form:my_weapon`.

Для інструмента (кирка, сокира тощо) скопіюйте `MyWeaponItem` під новою назвою і змініть батьківський клас. Докладно: `docs/TOOLS_ARMOR_UK.md`.
