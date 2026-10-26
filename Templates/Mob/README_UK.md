# Шаблон істоти

## Куди що копіювати

| З папки Templates/Mob | У проєкт |
|---|---|
| `java/MyCreature.java`, `MyCreatureModel.java`, `MyCreatureRenderer.java` | `src/main/java/com/spiritform/` |
| `assets/spirit_form/textures/entity/my_creature.png` | `src/main/resources/assets/spirit_form/textures/entity/` |
| `assets/spirit_form/models/item/my_creature_spawn_egg.json` | `src/main/resources/assets/spirit_form/models/item/` |
| `PASTE_into_SpiritCreatures.txt` | блоки вставляються в `SpiritCreatures.java`, `SpiritCreaturesClient.java`, `SpiritItems.java` |
| `lang_lines.txt` | рядки додаються в обидва lang-файли |
| `data/spirit_form/biome_spawner_entry.json` | вміст вставляється в список `"monster"` (або `"creature"`) у файлі біома |

## Порядок

1. Змініть назви-заглушки (`MyCreature`, `my_creature`, `MY_CREATURE`) на свої, див. `Templates/README_UK.md`.
2. Скопіюйте файли за таблицею й вставте блоки з `PASTE_into_SpiritCreatures.txt`.
3. Намалюйте текстуру (64x64, порожній прозорий файл уже є). Куби моделі лежать так: голова зліва вгорі, тулуб під нею, ноги нижче.
4. Додайте запис про появу у файл біома виміру.
5. Запустіть гру, візьміть яйце у вкладці Spirit Form.

Характеристики змінюються в `createAttributes()`, поведінка в `registerGoals()`. Більше: `docs/CREATURES_UK.md`.
