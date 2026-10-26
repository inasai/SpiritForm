# Інструменти, зброя й обладунки: як створювати і налаштовувати

У моді вже є Коса (`scythe`) та комплект обладунків Привида (`spirit_helmet`, `spirit_chestplate`, `spirit_leggings`, `spirit_boots`). Усе, що нижче, показано на їхньому прикладі.

## Де що лежить

| Файл | Для чого |
|---|---|
| `SpiritItems.java` | Реєстрація всіх предметів, вкладка творчого режиму, шкода й швидкість Коси |
| `ScytheItem.java` | Клас Коси: сюди дописується особлива поведінка |
| `SpiritArmorMaterials.java` | Матеріал обладунків: захист, зачарування, звук, ремонт |
| `models/item/<назва>.json` | Модель предмета в руці й в інвентарі |
| `textures/item/<назва>.png` | Текстура предмета (16x16) |
| `textures/models/armor/spirit_layer_1.png`, `spirit_layer_2.png` | Текстури одягненого обладунка |
| `lang/uk_ua.json`, `lang/en_us.json` | Назви: `"item.spirit_form.<назва>"` |

## 1. Текстури

- **Предмет:** `textures/item/scythe.png`, `spirit_helmet.png` тощо, зазвичай 16x16 (можна більше, але тоді значок виглядає чіткішим лише за потреби).
- **Одягнений обладунок:** два файли розміром **64x32**. `spirit_layer_1.png` це шолом, нагрудник і чоботи, `spirit_layer_2.png` це штани. Розкладка така сама, як у ванільної шкіряної броні (`leather_layer_1.png` можна взяти як шаблон із застосунку Minecraft).
- Хочете лише розфарбувати ванільний вигляд: подивіться текстури `leather_layer_*.png` і перемалюйте.

## 2. Зброя й інструменти

### Характеристики Коси

У `SpiritItems.java`:

```java
.attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))
```

- `3` це додаткова шкода зверху бази. Підсумкова шкода = 1 (кулак) + бонус матеріалу (`Tiers.IRON` дає 2) + це число.
- `-2.4F` це швидкість атаки. Орієнтири: меч -2.4, сокира -3.0, кирка -2.8, лопата -3.0, ніж/кинджал -1.8. Чим менше число, тим повільніший удар.
- Міцність, швидкість копання й зачаровуваність беруться від `Tiers.IRON` у `ScytheItem` (`super(Tiers.IRON, properties)`).

### Свій матеріал (Tier) для зброї й інструментів

Створіть файл `SpiritTiers.java`:

```java
package com.spiritform;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public enum SpiritTiers implements Tier {
    SPIRIT(1800, 8.0F, 3.0F, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 15);

    private final int uses;
    private final float speed;
    private final float damage;
    private final TagKey<Block> incorrectBlocks;
    private final int enchantability;

    SpiritTiers(int uses, float speed, float damage, TagKey<Block> incorrectBlocks, int enchantability) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.incorrectBlocks = incorrectBlocks;
        this.enchantability = enchantability;
    }

    @Override public int getUses() { return uses; }                       // міцність
    @Override public float getSpeed() { return speed; }                   // швидкість копання
    @Override public float getAttackDamageBonus() { return damage; }      // бонус шкоди
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrectBlocks; } // що НЕ можна добути
    @Override public int getEnchantmentValue() { return enchantability; } // зачаровуваність
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; } // чим лагодиться
}
```

Орієнтири міцності: дерево 59, камінь 131, залізо 250, алмаз 1561, незерит 2031. Потім у `ScytheItem` замініть `Tiers.IRON` на `SpiritTiers.SPIRIT`, а в `SpiritItems` теж.

### Інші види інструментів

Кирка, сокира, лопата й мотика створюються так само, як Коса, але з відповідним класом:

Найпростіше зробити для кожного інструмента свій клас на кшталт `ScytheItem`:

```java
public class SpiritPickaxeItem extends PickaxeItem {
    public SpiritPickaxeItem(Item.Properties properties) {
        super(SpiritTiers.SPIRIT, properties.attributes(PickaxeItem.createAttributes(SpiritTiers.SPIRIT, 1.0F, -2.8F)));
    }
}
```

І зареєструйте в `SpiritItems`: `ITEMS.registerItem("spirit_pickaxe", SpiritPickaxeItem::new, new Item.Properties())`. Для сокири `AxeItem`, для лопати `ShovelItem`, для мотики `HoeItem` (усі мають `createAttributes(tier, шкода, швидкість)`).

### Своя поведінка Коси (удар, використання)

У `ScytheItem.java` перевизначайте методи:

```java
@Override
public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    // приклад: уражена істота вбирає слабкість на 3 секунди
    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));
    return super.hurtEnemy(stack, target, attacker);
}

@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    // дія на ПКМ; не забудьте повернути результат
    return super.use(level, player, hand);
}
```

Ще корисні: `inventoryTick` (щотіку, поки предмет в інвентарі), `onLeftClickEntity` (ЛКМ по істоті).

### Свої числа через атрибути

Можна не користуватися `createAttributes`, а задати все вручну, зокрема й дальність руки:

```java
new Item.Properties().attributes(ItemAttributeModifiers.builder()
    .add(Attributes.ATTACK_DAMAGE,
         new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"), 7.0, AttributeModifier.Operation.ADD_VALUE),
         EquipmentSlotGroup.MAINHAND)
    .add(Attributes.ATTACK_SPEED,
         new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"), -2.6, AttributeModifier.Operation.ADD_VALUE),
         EquipmentSlotGroup.MAINHAND)
    .add(Attributes.ENTITY_INTERACTION_RANGE,
         new AttributeModifier(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "scythe_reach"),
                 1.0, AttributeModifier.Operation.ADD_VALUE),
         EquipmentSlotGroup.MAINHAND)
    .build())
```

Ці два ідентифікатори (`base_attack_damage`, `base_attack_speed`) замінюють звичайні значення зброї. До бази гравця (шкода 1.0, швидкість 4.0) додається вказане число, тож шкода `7.0` дає підсумок 8, а швидкість `-2.6` дає 1.4 удару за секунду.

## 3. Обладунки

### Характеристики (`SpiritArmorMaterials.java`)

- `defense.put(...)`: захист кожної деталі (нагрудник 6 = 3 «щити»). Залізо: чоботи 2, штани 5, нагрудник 6, шолом 2. Алмаз: 3, 6, 8, 3. Незерит те саме, що алмаз, але з `toughness` 3.0 і стійкістю до відкидання.
- Друге число в `new ArmorMaterial(...)` (зараз `9`): зачаровуваність.
- `SoundEvents.ARMOR_EQUIP_IRON`: звук надягання (інші: `ARMOR_EQUIP_DIAMOND`, `ARMOR_EQUIP_LEATHER`, `ARMOR_EQUIP_NETHERITE`).
- `Ingredient.EMPTY`: чим лагодиться в ковадлі. Приклад: `Ingredient.of(Items.IRON_INGOT)`.
- `0.0F, 0.0F` наприкінці: `toughness` (зменшує великі удари) і `knockback_resistance` (0.1 це 10% стійкості).
- `DURABILITY_MULTIPLIER = 15`: міцність деталей. Залізо 15, алмаз 33, незерит 37. Шолом отримує 11 × множник, нагрудник 16 ×, штани 15 ×, чоботи 13 ×.

### Кілька комплектів

Для другого комплекту додайте в `SpiritArmorMaterials` ще один `ARMOR_MATERIALS.register("ashen", ...)` зі своїм шаром (`ResourceLocation ... "ashen"` означає файли `ashen_layer_1.png` / `ashen_layer_2.png`), а в `SpiritItems` допоможе метод `armor(...)`: додайте йому параметр матеріалу.

### Особливості повного комплекту (бонус за весь набір)

Ванільної підтримки «бонусу комплекту» нема, його роблять подією (клас на зразок `SpiritArmorEvents.java`):

```java
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritArmorEvents {
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.tickCount % 20 != 0 || player.level().isClientSide()) {
            return;
        }
        boolean full = player.getItemBySlot(EquipmentSlot.HEAD).is(SpiritItems.SPIRIT_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(SpiritItems.SPIRIT_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(SpiritItems.SPIRIT_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(SpiritItems.SPIRIT_BOOTS.get());
        if (full) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }
    }
}
```

### 3D-обладунок із Blockbench

Ванільний обладунок має вигляд, який задають лише текстури шарів. Якщо потрібна власна 3D-форма (шипи, плащ), потрібен код: клас, що відповідає `IClientItemExtensions` і повертає свою модель через `getHumanoidArmorModel`, та реєстрація через `RegisterClientExtensionsEvent`. Це складніше. Коли захочете, скажіть, я зроблю шаблон так само, як для істот.

## 4. Моделі предметів і анімації

- Предмет у руці й в інвентарі: `models/item/<назва>.json`. Для звичайного значка `"parent": "minecraft:item/generated"`, для зброї й інструментів `minecraft:item/handheld` (тримається як меч).
- **3D-модель із Blockbench** (Коса як справжня об'ємна модель): експортуйте як Java Block/Item (JSON), покладіть як `models/item/scythe.json`, а текстури в `textures/item/`. Налаштуйте положення в руці в розділі `display` (у Blockbench вкладка Display).
- **Анімації предметів** ванільна гра не підтримує. Є два шляхи: бібліотека GeckoLib (підключається як залежність і дає анімовані предмети) або власний рендерер. Це окрема робота, скажіть, коли дійдете до цього.

## 5. Зачарування (важливо в 1.21)

У версії 1.21 зачарування прив'язані до **тегів предметів**. Свої предмети вони не бачать, поки ви їх не додасте. Створіть файл, наприклад `data/minecraft/tags/item/enchantable/sword.json`:

```json
{ "replace": false, "values": ["spirit_form:scythe"] }
```

Так само для інших груп: `enchantable/sharp_weapon` (Гострість), `enchantable/weapon`, `enchantable/durability` (Міцність), `enchantable/head_armor`, `chest_armor`, `leg_armor`, `foot_armor`, `enchantable/armor` для всіх деталей обладунка. Без `"replace": false` ви замінили б усі ванільні предмети тега, тож не забудьте це поле.

## 6. Рецепти

Файли в `data/spirit_form/recipe/`. Приклад Коси:

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [ "II ", " S ", " S " ],
  "key": { "I": { "item": "minecraft:iron_ingot" }, "S": { "item": "minecraft:stick" } },
  "result": { "id": "spirit_form:scythe", "count": 1 }
}
```

Для покращення в ковадлі (`minecraft:smithing_transform`) вказуйте `template`, `base`, `addition` та `result`. Інші типи рецептів: `minecraft:crafting_shapeless`, `minecraft:smelting`.

## 7. Інші налаштування предмета (`Item.Properties`)

`.stacksTo(1)`, `.durability(число)`, `.fireResistant()` (не горить у лаві), `.rarity(Rarity.EPIC)` (колір назви), `.component(...)` для додаткових даних.

Щоб предмет з'явився в творчій вкладці, додайте його в `output.accept(...)` у `SpiritItems`.

## 8. Перевірка

- `/give @s spirit_form:scythe`: видати предмет.
- Фіолетова клітинка означає, що не знайдено текстуру або модель (шлях у `models/item/...json` і назва файла png мають збігатися).
- Назва виглядає як `item.spirit_form.scythe` означає, що не вистачає рядка в lang-файлі.
- Помилки читайте в `logs/latest.log`.
