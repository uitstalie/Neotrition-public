# Nutrition

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1-blue)](https://neoforged.net)
[![Modrinth](https://img.shields.io/badge/dynamic/json?label=Modrinth&query=version&url=https://api.modrinth.com/v2/project/nutrition-uitstalie)](https://modrinth.com/mod/nutrition-uitstalie)

A Minecraft mod that adds a comprehensive nutrition system — track 15 nutrient groups, trigger potion effects and attribute modifiers based on your diet.

## Features

- **15 Nutrition Groups** — fruits, vegetables, grains, proteins, fish, eggs, dairy, mushrooms, nuts, sugars, honey, wine, coffee, salt, trace elements
- **13 Effect Rules** — potion effects (regeneration, speed, haste, night vision, water breathing, luck, saturation) and attribute modifiers (health, armor, knockback resistance) triggered by meeting nutrition thresholds
- **Combined Conditions** — OR/AND logic across multiple nutrition groups (e.g., armor bonus when fish + veg + grain + egg + protein all above 50%)
- **Arc Progress Bars** — 260° arc rendering with 64-segment tessellation around item icons in the GUI
- **BFS Auto-Propagation** — recipe chain analysis automatically assigns food items to nutrition groups at world load
- **Configurable Decay** — per-group decay value, frequency, and logarithmic pressure scaling
- **F3+H Tooltips** — nutrition group tags displayed on food items
- **Data Pack Driven** — all configuration (groups, items, effects) via JSON data packs

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1+
- Java 21+
- [UitstalieLibrary](https://github.com/uitstalie/UitstalieLibrary) (bundled via jar-in-jar)

## Installation

1. Download the `.jar` from [Releases](https://github.com/uitstalie/Nutrition/releases)
2. Place it in your `mods/` folder
3. Ensure UitstalieLibrary is also in `mods/` (or it will be auto-bundled)

## Quick Start

1. **Open the GUI** — press `N` key
2. **Info tab** — see all 15 nutrition groups with arc progress bars showing current levels
3. **Effects tab** — see active and inactive effects with their trigger conditions
4. **Eat food** — nutrition values increase, decay starts after a configured delay

## Commands

```
/nutrition set <group> <value>     — Set nutrition value (0-100000)
/nutrition get <group>             — Get current nutrition value
/nutrition list                    — List all groups and their values
/nutrition autogen                 — Run BFS auto-generation
/nutrition find-seeds              — Find minimal item marking set
```

## Nutrition Groups

| ID | Group | Default Icon | Decay |
|----|-------|-------------|-------|
| `fruits` | Fruits | Apple | Every 6s |
| `vegetables` | Vegetables | Carrot | Every 6s |
| `grains` | Grains | Bread | Every 6s |
| `proteins` | Proteins | Cooked Beef | Every 6s |
| `fishs` | Fish | Cooked Cod | Every 7s |
| `eggs` | Eggs | Egg | Every 7s |
| `milks` | Dairy | Milk Bucket | Every 6s |
| `mushrooms` | Mushrooms | Red Mushroom | Every 7s |
| `nuts` | Nuts | Cocoa Beans | Every 7s |
| `sugars` | Sugars | Sugar | Every 6s |
| `honeys` | Honey | Honey Bottle | Every 8s |
| `wines` | Wine | Sweet Berries | Every 8s |
| `coffee` | Coffee | Cocoa Beans | Every 7s |
| `salt` | Salt | Firework Star | Every 8s |
| `trace_elements` | Trace Elements | Iron Ingot | Every 8s |

## Effect Rules

| # | Effect | Condition | Threshold |
|---|--------|-----------|-----------|
| 1 | +Health | fruits OR vegetables OR grains | ≥ 75k |
| 2 | +Health | proteins AND eggs AND milks | ≥ 75k |
| 3 | +Armor | fishs AND vegetables AND grains AND eggs AND proteins | ≥ 50k |
| 4 | +Armor +KnockbackRes | nuts AND salts AND mushrooms | ≥ 60k |
| 5 | Water Breathing | fishs | ≥ 25k |
| 6 | Night Vision | vegetables | ≥ 50k |
| 7 | Luck | sugars OR honeys OR wines | ≥ 20k |
| 8 | Haste | nuts OR salts OR coffee | ≥ 30k |
| 9 | Speed | coffee | ≥ 40k |
| 10 | Regeneration | mushrooms OR eggs OR milks | ≥ 50k |
| 11 | Speed II | nuts AND honeys AND sugars | ≥ 60k |
| 12 | Saturation | grains AND proteins AND vegetables | ≥ 70k |
| 13 | Luck II | honeys AND wines AND coffees | ≥ 60k |

## Data Pack Customization

Create custom data packs to add or modify:

```
data/nutrition/config/config.json    — Global settings (frequency, value formula)
data/nutrition/groups/<group>.json   — Nutrition group definitions
data/nutrition/items/<group>.json    — Item-to-group bindings with manual values
data/nutrition/effects/default.json  — Effect rules with OR/AND conditions
```

See [Wiki](https://github.com/uitstalie/Nutrition/wiki) for detailed configuration reference.

## Building

```bash
cd source
./gradlew build          # Compile
./gradlew runClient      # Run client
./gradlew runData        # Generate data packs
```

## License

All Rights Reserved © uitstalie

## Credits

- Inspired by [Diet](https://github.com/TheIllusiveC4/Diet) and [AppleSkin](https://github.com/squeek502/AppleSkin)
- Commissioned by [uitstalie](https://github.com/uitstalie)
