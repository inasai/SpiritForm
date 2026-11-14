# Структура коду Spirit Form (з оновлення 149)

Усі класи лежать у `src/main/java/com/spiritform/`. Файли ресурсів (`assets`, `data`) не чіпали: їхня структура задана самою грою.

Видалено порожню заглушку `SpiritArmyMode.java` (у ній лишався лише коментар, що файл не потрібен).

## Пакети

### (корінь)
Точка входу мода: SpiritFormMod і Config. Класів: 2.

`Config`, `SpiritFormMod`

### client
Усе, що працює лише на клієнті: обробники, клавіші, колесо, ефекти, підсвітка. Класів: 27.

`BeyondClientEffects`, `FentonSkin`, `SpiritAbilityMode`, `SpiritAbilityUse`, `SpiritAnim`, `SpiritArmyClient`, `SpiritClientEntities`, `SpiritClientHighlight`, `SpiritClientItems`, `SpiritClientReload`, `SpiritClientRender`, `SpiritContainerHighlight`, `SpiritCooldownClient`, `SpiritCreaturesClient`, `SpiritCullFix`, `SpiritDisguiseClient`, `SpiritFormClient`, `SpiritGearClient`, `SpiritHeraldClient`, `SpiritHeraldClientMod`, `SpiritKeyMappings`, `SpiritPossessionClient`, `SpiritSenseVision`, `SpiritShardTrail`, `SpiritTransparency`, `SpiritVaultClient`, `SpiritVisualEffects`

### client.screen
Екрани й панелі (вікно H, колеса, станції, душі Армії). Класів: 10.

`ArmyIconScreen`, `ArmySoulsScreen`, `ArmyWheelScreen`, `SpiritExternalPanel`, `SpiritLoadoutEditor`, `SpiritScreenBase`, `SpiritStationScreen`, `SpiritUpgradeScreen`, `SpiritVaultPanel`, `SpiritWheelScreen`

### client.gui
Кнопки та допоміжні елементи інтерфейсу, іконки. Класів: 6.

`IconButton`, `SpiritArmyIcons`, `SpiritButton`, `SpiritIcons`, `SpiritUi`, `SpiritWheelUi`

### client.render
Рендерери істот, шари на гравці (браслет, намисто, кайдани, броня). Класів: 11.

`BahatkoRenderer`, `BraceletLayer`, `ForestRenderers`, `HeraldRenderer`, `KlunnykRenderer`, `NecklaceLayer`, `PhoenixRenderer`, `ShacklesLayer`, `SoulMirrorLayer`, `SpiritArmorLayer`, `TemplateCreatureRenderer`

### client.model
Моделі істот і аксесуарів. Класів: 19.

`AccessoryModels`, `BahatkoModel`, `BisykModel`, `ChortModel`, `ChuhaisterModel`, `HeraldModel`, `LisovykModel`, `MavkaModel`, `NecklaceModel`, `PerelisnykModel`, `PhoenixModel`, `PolovykModel`, `PotercharModel`, `PovitruliaModel`, `SkarbnykModel`, `TemplateCreatureModel`, `VodianykModel`, `VodianytsiaModel`, `VovkulakaModel`

### network
Усі пакети (Payload) і SpiritNetworking. Класів: 18.

`AbilityPayload`, `ArmyActionPayload`, `ArmySyncPayload`, `CooldownPayload`, `ExternalStationPayload`, `HeraldSyncPayload`, `InvisibilityPayload`, `PossessActionPayload`, `PossessInputPayload`, `PossessPayload`, `SpiritNetworking`, `StationActionPayload`, `TeleportPayload`, `ToggleSpiritPayload`, `TouchDistancePayload`, `UpgradeSpiritPayload`, `VaultActionPayload`, `VaultHandPayload`

### data
Дані, що зберігаються: вкладення гравця, записи, SavedData. Класів: 12.

`DisguiseData`, `HeraldStore`, `PhoenixNests`, `ShardMarks`, `SoulShardIndex`, `SpiritArmyStore`, `SpiritAttachments`, `SpiritData`, `SpiritEchoIndex`, `StationData`, `VaultData`, `VodianykIndex`

### registry
Реєстри: блоки, предмети, істоти, ефекти, частинки, біоми, ігрові правила. Класів: 11.

`SpiritArmorMaterials`, `SpiritBiomes`, `SpiritBlocks`, `SpiritCreatures`, `SpiritEffects`, `SpiritEntities`, `SpiritGameRules`, `SpiritHeraldEntities`, `SpiritItems`, `SpiritParticles`, `SpiritRecipes`

### form
Форма Привида, режими існування, Потойбіччя, прокачка, баланс. Класів: 11.

`ExistenceMode`, `SpiritBalance`, `SpiritBeyond`, `SpiritBranch`, `SpiritCharge`, `SpiritExistence`, `SpiritForm`, `SpiritFormState`, `SpiritLord`, `SpiritRange`, `SpiritUpgrades`

### ability
Здібності: телепорт, невидимість, Доторк, Одержимість, Марення тощо. Класів: 14.

`SpiritAbility`, `SpiritAuras`, `SpiritCooldowns`, `SpiritDelirium`, `SpiritDisguise`, `SpiritEcho`, `SpiritImmunity`, `SpiritInvisibility`, `SpiritPossession`, `SpiritScream`, `SpiritSummon`, `SpiritTeleport`, `SpiritTouch`, `SpiritTouchBlock`

### army
Армія Потойбіччя. Класів: 2.

`ArmySoulStats`, `SpiritArmy`

### vault
Сховище, спорядження, аксесуари, станції Самодостатності. Класів: 6.

`SpiritAccessories`, `SpiritLoadout`, `SpiritStations`, `SpiritVault`, `SpiritVaultHands`, `Station`

### herald
Вісники та їхня шкала переслідування. Класів: 10.

`ColdHerald`, `DeathHerald`, `DeathHeraldDefeatedEvent`, `HeraldBase`, `HeraldTargetGoal`, `HeraldType`, `HungerHerald`, `SpiritHeraldWorld`, `SpiritHeralds`, `StormHerald`

### entity
Істоти мода, Уламки й вогники душ, цілі ШІ. Класів: 23.

`Bahatko`, `Bisyk`, `Chort`, `Chuhaister`, `ForestTargetGoal`, `Hukha`, `Klunnyk`, `Lisovyk`, `Mavka`, `Nichnytsia`, `Perelisnyk`, `Phoenix`, `Polovyk`, `Potercha`, `Povitrulia`, `Skarbnyk`, `SoulShardEntity`, `SoulWispEntity`, `TemplateCreature`, `Vodianyk`, `Vodianytsia`, `Vovkulaka`, `Zlydeny`

### item
Предмети й артефакти та їхня логіка. Класів: 22.

`CrystalSoulItem`, `DeathHeartItem`, `FentonBracelet`, `FentonBraceletItem`, `FernPearlItem`, `GlovesOfTheLiving`, `GlovesOfTheLivingItem`, `HeraldHeartItem`, `NecklaceItem`, `ProofOfStrengthItem`, `ScytheItem`, `ShacklesItem`, `SoulCatcherItem`, `SoulMemory`, `SoulMemoryItem`, `SoulPlunderItem`, `SoulPull`, `SoulPullItem`, `SoulShardTools`, `SpiritAppleItem`, `SpiritGuide`, `SpiritReaper`

### block
Блоки й блок-сутності. Класів: 11.

`EchoBlock`, `GlowFernBlock`, `HeraldHeadBlock`, `MavkaFlowerBlock`, `PhoenixAshBlock`, `PoludnytsiaBlock`, `PottedMavkaFlowerBlock`, `RichnytsiaBlock`, `SoulCatcherBlock`, `SoulCatcherBlockEntity`, `SpiritLeavesBlock`

### worldgen
Генерація світу: дерева, поляни, структури. Класів: 13.

`GlowFernFeature`, `HeatlandSites`, `HeatlandTerrainFeature`, `MavkaTreeFeature`, `MavkaTrees`, `NotNearMavkaTreeFilter`, `PlainsSites`, `PoludnytsiaRingsFeature`, `SpiritLeavesFeature`, `SpiritWorldgen`, `TitanRootsFeature`, `TitanTreeFeature`, `TitanTrees`

### recipe
Спеціальні рецепти. Класів: 2.

`CrystalRepairRecipe`, `CrystalSoulRecipe`

### command
Команди. Класів: 3.

`SpiritLocateCommand`, `SpiritShardCommand`, `SpiritUnlockCommand`

### event
Обробники подій: сервер, досягнення, Бестіарій, події істот. Класів: 10.

`BahatkoEvents`, `BestiaryEvents`, `ForestEvents`, `KlunnykEvents`, `PerunWrath`, `PhoenixEvents`, `RiverEvents`, `SoulShardEvents`, `SpiritAdvancements`, `SpiritServerEvents`

### mixin
Міксини (пакет не змінювався, на нього вказує spirit_form.mixins.json)

## Де що шукати
- Нова здібність: `ability`, її кнопки й вікна: `client.screen`, пакет з мережі: `network`.
- Нова істота: `entity` + `client.model` + `client.render`, реєстрація в `registry/SpiritCreatures`.
- Новий предмет: `item`, реєстрація в `registry/SpiritItems`.
- Новий параметр конфігу: `Config` (у відповідному розділі).
