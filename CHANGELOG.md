# Changelog

Усі помітні зміни в цьому проєкті будуть документуватися в цьому файлі.

Формат базується на [Keep a Changelog](https://keepachangelog.com/uk/1.0.0/),
і цей проєкт дотримується [Семантичного Версіонування (SemVer)](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added для нових функцій
- 

### Changed для змін в існуючих функціях
- 

### Deprecated для функцій, які будуть видалені
- 

### Removed для видалених функцій
- 

### Fixed для виправлення помилок
- 

### Security для вразливостей
- 

---

## [2.6.a,m] - 2026-10-11

### Added
- New playstyles that can be configured under Gameplay->General->Existence Form:
Overworld – the standard mode, set by default.
Nether – is a spirit form available only after death, in which you cannot mine blocks or deal damage. To regain your physical body, you must possess a mob using the Possession ability.
Beyond – in the Overworld, the duration of the spirit form is limited to 1 minute, whereas in the Beyond, there are no such limits. You can still take damage even while in Ghost form.
- The "Icy Touch" ability negates fire damage.

### Changed
- Configuration Menu.

---

## [2.5.1] - 2026-10-10

### Added
- A new feature for the Delirium ability: you can now steal the appearance of mobs. To do this, possess a mob using the Possession ability, then activate the Delirium ability—the mob's appearance will become yours. To change your appearance, hold down the Delusion ability for 5 seconds.

---

## [2.4.1] - 2026-10-09

### Added
- Soul development system. You can improve your souls' stats using Crystallized Soul.

---

## [2.3.1] - 2026-10-09

### Added
- Biome in the Beyond dimension: Heatland.
- Creatures of the Beyond: Klunnyk, Bahatko, Phoenix.

### Changed
- "Self-Sufficiency" ability: you can now add external stations. This is an experimental feature; it was tested with the ProjectE mod, and everything worked. However, please proceed with caution.

---

## [2.2.2] - 2026-10-08

### Added
- The "Spirit Sense" ability now leads to a Soul Shard, if one exists.
- A command (/spirit_shard) to recover items from a Soul Shard—in case it fails to appear or disappears for any reason.

### Changed
- Mob information window with the Crown of the Beyond equipped and Spirit Sense enabled.

---

## [2.2.1] - 2026-10-08

### Added
- The "Self-Sufficiency" ability, which replaces station blocks (crafting table, furnace, enchanting table, etc.) with a convenient HUD.

---

## [2.1.4] - 2026-10-08

### Added
- The passive abilities menu now has pages, and more abilities can be placed there.

### Changed
- Screen appearance with the Spirit Sense ability active.

---

## [2.1.3] - 2026-10-07

### Added
- The "Spirit Sense" ability now highlights storage containers.

### Changed
- The "Vanishing" ability no longer has a duration.

### Removed
- Dependency on the Curios API mod.

### Fixed
- The Spirit Form: the character is now much more visible when standing inside blocks.

---

## [2.1.2] - 2026-10-07

### Added
- Armor in the Gear Vault is now displayed on the character and takes precedence over the armor currently equipped.
- The "Scythe of the Beyond" and the "Soul Catcher," when equipped via the Vault, can now trigger their special effects.

### Changed
- Jump height reduced to 3 blocks.

---

## [2.1.1] - 2026-10-07

### Added
- Storage system. In the progression menu (H key), you can now equip artifacts and armor in place of Curios slots.

---

## [2.0.6] - 2026-10-06

### Fixed
- Radial menus fixed. Performance should be better now.

---

## [2.0.5] - 2026-10-06

### Changed
- HUD upgrade panel: style of passive abilities, active abilities, and the Army of the Beyond ability.
- Night vision reworked.
- Models of two items.

---

## [2.0.4] - 2026-10-05

### Added
- Artifact of the Beyond: "Soul Pull", which draws items and experience orbs within a 16-block radius toward the player.
- Artifact of the Beyond: "Soul Memory", which restores the durability of items.

### Changed
- HUD upgrade panel: Ability icons now feature a cooldown indicator.

---

## [2.0.3] - 2026-10-04

### Added
- Artifact of the Beyond: "Gloves of the Living".

### Changed
- HUD upgrade panel.
- Spirit Form: flight activates only if Spirit Instincts are enabled and the character is in the air; otherwise, flight must be activated manually (by double-clicking the jump action).

---

## [2.0.2] - 2026-11-04

### Added
- The "/gamerule spiritFormOverworldMode" rule. When enabled, the Spirit Form level is set to the maximum. Disabled by default.

### Changed
- The progression panel interface.
- Spirit Form: double-pressing the jump key toggles flight, while double-pressing the crouch key toggles the ability to pass through walls.

---

## [2.0.1] - 2026-11-03

### Added
- "Scythe of the Beyond" is a new weapon.
- Armor of the Beyond is a new set of armor.
- Artifacts: "Mirror of Souls", "Shackles of the Edge";
- Pursuit - now each trip to the Beyond brings the breakthrough of otherworldly creatures closer to the dimension of the living.
- Heralds of the Beyond – 3 new bosses that arrive in the Overworld when the Pursuit reaches its maximum.
- "Herald of Death" is a new boss that is summoned by the Proof of Strength item.
- Herald Hearts - allow you to reduce the scale of Persecution.
- "Heart of Death" - absorbing it unlocks a new ability.
- Ability "Energy of the Beyond" - increases the player's health and the power of his souls.
- Bioms of the Beyond: "Forest of the Beyond", "Mist Forest", "Night Forest", "Beyond River", "Beyond Plains".
- Creatures of the Beyond: "Mavka", "Chuhaister", "Hukha", "Zlydeny", "Bisyk", "Skarbnyk", "Lisovyk", "Vodianyk", "Vodianytsia", "Povitrulia", "Perelisnyk", "Nichnytsia", "Potercha", "Polovyk", "Vovkulaka", "Chort".
- Guide "Bestiary of the Beyond'.

### Changed
- HUD upgrade panel.

---

## [1.14.3] - 2026-10-26

### Added
- Recipe added to the Diary.
- "Fury" command: mobs attack the same targets as the player, as well as those attacking the player.

### Fixed
- The "Possession" icon.
- Ability positions.

---

## [1.14.2] - 2026-10-26

### Added
- The ability to change the icon for the souls.

### Fixed
- The behavior of the Ender Dragon, the Wither, and the Guardian.

---

## [1.14.1] - 2026-10-26

### Added
- The "Army of the Beyond" ability, which allows you to assemble your own army.

---

## [1.13.2] - 2026-10-25

### Added
- The "Spirit Touch" ability can now interact with blocks.

### Fixed
- Camera freezes during Possession.
- Spirit Form.

---

## [1.13.1] - 2026-10-25

### Added
- The "Beyond" dimension has been added.

---

## [1.12.2] - 2026-10-25

### Changed
- Ability's icons have been replaced.
- The Echo of Death model.

---

## [1.12.1] - 2026-10-25

### Added
- The number of Soul Shards can now be upgraded.
- Now you can reset abilities.
- The ability "Echo of Death" (passive, Path of Darkness 3): allows you to save your current location upon activation and teleport back to it upon deactivation.
- The ability "Spirit Scream" (active, Path of Hunger 3): causes nearby mobs to protect you.
- The ability "Summon" (active, Path of Movement 3): teleports the mob you are looking at to your location.

---

## [1.11.3] - 2026-10-24

### Added
- Support for Patchouli.

### Changed
- The abilities and advancements icons.
- Ability cost reduced.
- The "Necklace of the Beyond" item now supports the Curios API.
- The item "Fenton's Bracelet" renamed to "Phantom's Stigma".
- "Phantom's Stigma" now reduces ability cooldowns instead of hunger.

---

## [1.11.2] - 2026-10-24

### Added
- A crafting recipe for "Necklace of the Beyond".

### Changed
- Achievements and abilities icons.

---

## [1.11.1] - 2026-10-24

### Added
- Achievements.
- The item "Necklace of the Beyond".

---

## [1.10.1] - 2026-10-23

### Added
- The entity/ability "Soul Shard," which keeps your items after death.

---

## [1.9.3] - 2026-10-23

### Added
- Compatibility with Curios API.
- "Soul Catcher" can now be placed as a block, and if you have caught a mob, it will glow.
- "Soul Catcher" and "Fenton's Bracelet" can now be crafted.
- Fenton's Bracelet can now be placed in Curios API slots.

---

## [1.9.2] - 2026-10-23

### Changed
- "Soul Catcher" and "Fenton’s Bracelete" models and textures.

---

## [1.9.1] - 2026-10-23

### Added
- The items "Soul Catcher" and "Fenton's Bracelet".

### Fixed
- The ability "Spirit Gaze".

---

## [1.8.1] - 2026-10-22

### Added
- The passive ability "Spirit Gaze," which scares away hostile mobs and attracts peaceful ones.
- Now there are only 3 Main Abilities.

### Changed
- Requirements for some abilities.

### Fixed
- Wither effect while using abilities "Gift of Life" and "Scales".

---

## [1.7.1] - 2026-10-22

### Added
- The active ability "Spirit Immunity," which removes all effects (positive and negative).
- The active ability "Gift of Life," which grants your hearts to other mobs.
- The active ability "Scales," which removes the same number of hearts from a mob as it does from a player.

### Changed
- Requirements for certain abilities.
- Upgrade panel.
- Ability panel.
- The ability "Icy Touch" now freezes lava.

---

## [1.6.2] - 2026-10-21

### Added
- Now, when using the "Spirit Touch" ability, mobs can be moved further away or brought closer using the mouse wheel.

### Changed
- The stance has been removed when using the "Delirium" ability. It has been replaced with smoke particles.

---

## [1.6.1] - 2026-10-21

### Added
- Passive and active abilities are now accessed via a menu - using the T and R keys, respectively.
- The ability "Spirit Touch".
- The ability "Delirium".
- The ability "Icy Touch".
- The ability "Presence".

### Changed
- The abilities menu.
- The "Possession" ability also absorbs the victims' hunger

---

## [1.5.1] - 2026-10-20

### Added
- Now you can possess flying mobs.
- Now the possessed witch, ghast, skeleton and some others have a special attack.

---

## [1.4.14] - 2026-10-19

### Added
- If Spirit Instincts is enabled, the Vanish ability is automatically used after absorbing the mob's health.
- Spirit Instincts cost you 3 health.
- A possessed mob now emits smoke particles.

---

## [1.4.13] - 2026-10-19

### Added
- Obsession costs 3 hunger marks.
- If you exit the Ghost Form while in the Possession state, health will be stolen from the mob.

---

## [1.4.12] - 2026-10-18

### Added
- Sounds added when using abilities.
- Night Vision added in Spirit Form.
- Ability upgrades changed.
- The "Spirit Instincts" ability, which saves the player from death if they have sufficient saturation.
- The "Possession" ability, which allows you to inhabit mobs.

---

## [1.3] - 2026-10-18

### Added
- Now Highlight is activated by the N key.
- Now, if you have 0 hunger units, Ghost mode turns off.

### Fixed
- Teleportation ability.

---

## [1.2] - 2026-10-18

### Added
- Mob highlighting is now toggled using the V key.
- The panel's appearance has been changed.
- The cost for each upgrade level can now be configured.
- The Teleportation ability has been improved. Several bugs have been fixed.

---

## [1.0.1] - 2026-10-17

### Added
- Перший офіційний реліз проєкту!

[Unreleased]: https://github.com/username/repository/compare/v1.1.0...HEAD


[1.0.1]: https://github.com/username/repository/compare/v1.0.0...v1.0.1
[1.0.1]: https://github.com/username/repository/releases/tag/v1.0.0