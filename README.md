# KnowledgeBound

**KnowledgeBound** is a server-side RPG skill progression and balancing mod for Minecraft (Fabric 1.21.1). It replaces standard vanilla progression with a time-and-action-based knowledge system.

Players start knowing nothing — they will fail to gather resources, craft poor-quality tools, and deal very little damage in combat. As they perform actions over time, they level up their "Knowledge" in various categories, unlocking full vanilla capabilities and eventually mastering their craft.

Because KnowledgeBound is **server-side**, clients do not need to install the mod to play on a KnowledgeBound server. However, installing it on the client enables a **Knowledge HUD overlay** (press `K`) showing all skill tiers and progress bars in real time.

---

## 📋 Table of Contents

- [Core Concepts & Progression](#-core-concepts--progression)
- [Job Categories](#-job-categories)
- [Gathering Mechanics](#-gathering-mechanics)
- [Crafting Mechanics (Material Jobs)](#️-crafting-mechanics-material-jobs)
- [Crafting Mechanics (Class Jobs)](#-crafting-mechanics-class-jobs)
- [Carpentry](#-carpentry)
- [Masonry](#-masonry)
- [Beekeeping](#-beekeeping)
- [Stonecutter](#-stonecutter)
- [Combat & Armor Mechanics](#️-combat--armor-mechanics)
- [Fishing](#-fishing)
- [Proficiency Limits](#-proficiency-limits)
- [Blocked Items](#-blocked-items)
- [Client-Side HUD](#-client-side-hud)
- [Server-Side HUD & GUI](#-server-side-hud--gui)
- [Configuration Reference](#️-full-configuration-reference)
- [Commands](#-commands)
- [Datapack Compatibility](#-datapack-compatibility)
- [Current Limitations](#-current-limitations)
- [Credits](#-credits)

---

## 🌟 Core Concepts & Progression

Unlike traditional RPG mods that give XP per block broken or mob killed, KnowledgeBound grants XP based on **real-time minutes spent performing an activity**.

- You gain 1 "minute" of XP per real-time minute that you are actively using the skill (breaking blocks, crafting items, dealing damage, etc.).
- There is an internal cooldown — you can only gain 1 minute per real-time minute per knowledge, preventing spam.
- You must perform the right action with the right tool tier (e.g., breaking stone with a stone pickaxe when leveling from Tier 1 to Tier 2 in Mining).
- XP is tracked using **real-time** (`System.currentTimeMillis()`), not game ticks, so pausing the game or TPS lag won't affect progression.

---

## 📂 Job Categories

Knowledge categories are split into four types, each with different progression rules:

### Material Jobs (5-Tier) — *Cannot jump tiers*
These jobs track the material you're working with. If your tier is lower than the item's required tier, crafting **always fails** (100%). No tier jumping allowed.

| Job | Max Tier | XP Source |
|:---|:---:|:---|
| **Toolsmithing** | 5 | Crafting tools (pickaxes, axes, shovels, hoes) |
| **Weaponsmithing** | 5 | Crafting weapons (swords, bows, crossbows) |
| **Armouring** | 5 | Crafting armor pieces |

### Class Jobs (3-Tier) — *Can jump tiers with reduced success*
These jobs use the standard tier-difference table. You can attempt to craft items above your tier, but you'll have a high fail chance.

| Job | Max Tier | XP Source |
|:---|:---:|:---|
| **Carpentry** | 3 | Crafting wooden items |
| **Masonry** | 3 | Crafting stone/brick items, using stonecutter |
| **Beekeeping** | 3 | Harvesting honey/honeycomb from beehives |
| **Smelting** | 3 | Smelting ore in furnaces and blast furnaces |
| **Cooking** | 3 | Cooking food in furnaces, smokers, and campfires |
| **Husbandry** | 3 | Taming, breeding, shearing, milking, and riding animals |
| **Jeweller** | 3 | Crafting jewelry, applying armor trims, socketing gems |
| **Slaughtering** | 3 | Dissecting mob corpses with an axe or cleaver |

### Gathering (5-Tier)
| Job | Max Tier | XP Source |
|:---|:---:|:---|
| **Forestry** | 5 | Breaking logs and wood |
| **Mining** | 5 | Breaking stone, ores, deepslate |
| **Digging** | 5 | Breaking dirt, sand, gravel, clay |
| **Farming** | 5 | Harvesting mature crops |

### Combat (5-Tier)
| Job | Max Tier | XP Source |
|:---|:---:|:---|
| **Melee Combat** | 5 | Dealing damage with swords |
| **Ranged Combat** | 5 | Dealing damage with bows/crossbows |

### Other
| Job | Max Tier | XP Source |
|:---|:---:|:---|
| **Fishing** | 3 | Catching fish with a fishing rod |

### Default Time Requirements

**Material Jobs & Gathering (5-Tier):**

| Tier | Minutes | Real Time |
|:---|:---:|:---|
| Tier 0 → 1 | 60 | 1 hour |
| Tier 1 → 2 | 120 | 2 hours |
| Tier 2 → 3 | 240 | 4 hours |
| Tier 3 → 4 | 480 | 8 hours |
| Tier 4 → 5 | 960 | 16 hours |

**Class Jobs & Fishing (3-Tier):**

| Tier | Minutes | Real Time |
|:---|:---:|:---|
| Tier 0 → 1 | 60 | 1 hour |
| Tier 1 → 2 | 120 | 2 hours |
| Tier 2 → 3 | 240 | 4 hours |

All times are multiplied by the global `minutesMultiplier` config value (default `1.0`).

---

## 🪓 Gathering Mechanics

At low knowledge tiers, players will struggle to gather basic resources.

When breaking a block associated with a gathering skill (e.g., Logs for Forestry, Stone/Ores for Mining, Dirt/Sand for Digging, Mature Crops for Farming), there is a chance the block will break but **drop nothing**.

**Default Fail Chances:**

| Tier | Forestry, Mining, Digging | Farming |
|:---|:---:|:---:|
| Tier 0 | 40% | 30% |
| Tier 1 | 25% | 20% |
| Tier 2 | 10% | 10% |
| Tier 3 | 5% | 5% |
| Tier 4+ | 2% | 2% |

> **Note:** For Farming, only fully mature crops yield XP and are subject to the fail chance. Breaking seeds or un-grown crops does nothing.

**Tool Tier Progression:** To gain XP, you must use the correct tool tier for your current knowledge tier. For example, to progress from Mining Tier 1 to Tier 2, you must use a stone pickaxe.

---

## 🛠️ Crafting Mechanics (Material Jobs)

**Material jobs (Toolsmithing, Weaponsmithing, Armouring) cannot jump tiers.** If you attempt to craft an item whose tier is higher than your knowledge tier, the craft **always fails** and ingredients are lost. You'll see the message: *"You don't have enough [Knowledge] knowledge to work with these materials."*

When crafting at or below your tier, success depends on the **tier difference**:

`Difference = (Player Tier) - (Item Tier)`

### Crafting Results
- **Fail:** The item is destroyed. Ingredients are lost forever.
- **Poor Quality:** The item is crafted with only **10% of max durability**.
- **Normal:** The item is crafted perfectly.

**Default Tier-Difference Chances:**

| Diff | Fail | Poor | Normal | Example |
|:---|:---:|:---:|:---:|:---|
| ≤ −3 | 100% | 0% | 0% | T0 player → Netherite (T4) |
| −2 | 85% | 12% | 3% | T1 → Diamond (T3) |
| −1 | 45% | 35% | 20% | T2 → Diamond (T3) |
| 0 | 10% | 15% | 75% | T3 → Diamond (T3) |
| +1 | 0% | 8% | 92% | T4 → Diamond (T3) |
| ≥ +2 | 0% | 0% | 100% | T5 → Diamond (T3) |

### Default Vanilla Item Tiers (Material Jobs)

| Tier | Tools | Weapons | Armor |
|:---|:---|:---|:---|
| **0** | Wooden | Wooden Sword | Leather |
| **1** | Stone | Stone/Gold Sword, Bow | Chainmail, Gold |
| **2** | Iron | Iron Sword, Crossbow | Iron, Turtle Helmet |
| **3** | Diamond | Diamond Sword | Diamond |
| **4** | Netherite | Netherite Sword | Netherite |

---

## 🪵 Crafting Mechanics (Class Jobs)

**Class jobs (Carpentry, Masonry) can jump tiers** using the same tier-difference table above. This means a Tier 0 Carpenter can attempt to craft a Tier 2 item, but will have an 85% fail chance.

---

## 🪚 Carpentry

Carpentry covers all wooden craftable items. Items are assigned to tiers based on complexity:

| Tier | Items |
|:---|:---|
| **0** | All planks, sticks, torches |
| **1** | Banners, bookshelves, bowls, buttons, campfires, doors, trapdoors, fences, levers, stairs, slabs, trapped chests |
| **2** | Bamboo mosaic, beds, beehives, chiseled bookshelves, crafting tables, cartography/fletching tables, fence gates, signs, hanging signs, pressure plates, paintings, item frames, full wood blocks |
| **3** | Chests, barrels, armor stands, composters, jukeboxes, lecterns, looms, scaffolding, note blocks, ladders |

### Bamboo +1 Tier Rule
All bamboo variants are **+1 tier** compared to their normal wood equivalent. For example, a normal oak door is Tier 1 but a bamboo door is Tier 2.

---

## 🧱 Masonry

Masonry covers all stone, brick, and mineral crafting:

| Tier | Items |
|:---|:---|
| **1** | Stone stairs/slabs/buttons/pressure plates, levers, furnaces, smokers, concrete (all colors) |
| **2** | Bricks (all types), blast furnaces, flower pots, decorated pots, glass (all types), glass panes, tinted glass, glowstone, stonecutters |
| **3** | Dispensers, droppers, observers, pistons, sticky pistons |

### Shared Items
The **lever** is registered in both Carpentry (Tier 1) and Masonry (Tier 1). Crafting one grants XP for whichever knowledge has a rule registered (Masonry takes priority since it's registered second).

---

## 🐝 Beekeeping

Beekeeping governs interactions with beehives and bee nests.

### Honey/Honeycomb Harvesting
When using a **glass bottle** (honey) or **shears** (honeycomb) on a full beehive, the beekeeping system activates:

**Harvest Fail Chance (angers bees even with campfire):**

| Tier | Fail Chance |
|:---|:---:|
| Tier 0 | 50% |
| Tier 1 | 30% |
| Tier 2 | 10% |
| Tier 3+ | 0% |

On failure, bees become angry regardless of whether a campfire is underneath. XP is still granted on failure.

### Better Honey
When harvesting with a glass bottle, there is a chance to receive **"Royal Honey"** — a honey bottle with custom potion effects:

| Tier | Better Honey Chance |
|:---|:---:|
| Tier 1 | 10% |
| Tier 2 | 25% |
| Tier 3 | 25% |

**Default Royal Honey effects:**
- Regeneration II (10 seconds)
- Saturation I (5 seconds)

All effects, the item name, and color are fully configurable in `knowledgebound.json`.

### Silk Touch Beehive Moving
Moving beehives with Silk Touch requires **Beekeeping Tier 3** (configurable via `silkTouchBeehiveMinTier`). Lower-tier players attempting to break a beehive with Silk Touch will be blocked.

---

## 🪨 Stonecutter

The stonecutter is tied to the **Masonry** knowledge:

- **Requires Masonry Tier 1** to use at all.
- **Cutting damage chance:** At Tier 1, there's a 10% chance to take 1 heart of damage when using the stonecutter. This decreases by 5% per tier:
  - Tier 1: 10%
  - Tier 2: 5%
  - Tier 3: 0%
- Grants **Masonry XP** on each use.

---

## 🍳 Smelting & Cooking

Smelting and Cooking are **supervised jobs**:

- **Active Supervision Required:** You must stay within 16 blocks of the furnace/smoker/blast furnace/campfire while it operates.
- **Grace Period:** If you leave the area, a grace period timer starts. If you don't return before it expires, the job fails, destroying the items and producing smoke.
- **Collection Window:** Once smelting/cooking finishes, you have a limited collection window to take the items before they ruin/burn.
- **Better Honey/Cooking Special Outputs:** High-tier cooking offers a chance for special bonus outputs or higher nutritional yields.

---

## 🌾 Husbandry

Husbandry manages all advanced animal interactions, locking them behind knowledge tiers:

- **Taming & Breeding:** Requires specific Husbandry tiers depending on the animal type. Fails at lower tiers (with configurable item consumption/cooldowns).
- **Milking & Shearing:** Requires Husbandry knowledge. Failing to shear can damage shears; failing to milk wastes the attempt.
- **Mount Riding:** Riding horses, donkeys, or camels without sufficient knowledge causes the mount to buck and kick you off periodically.
- **Egg Hatching Override:** Thrown eggs can be configured to prevent baby chicken spawns to encourage proper breeding.

---

## 💎 Jeweller

Jeweller governs delicate crafting and ornamentation:

- **Jewelry Crafting:** Items registered in `jewellerCraftingItems` require specific Jeweller tiers to craft.
- **Smithing Table Gating:** Applying armor trims or socketing gems at the smithing table requires Jeweller knowledge. Attempting without knowledge blocks the action.

---

## 🥩 Slaughtering

Slaughtering replaces standard mob drops with a realistic dissection mechanic:

- **Corpse Spawning:** Killing an allowed mob with a cleaver spawns an inert, persistent corpse entity lying on the ground.
- **Critical Hit Penalty:** Finishing a mob with a critical hit ruins the corpse due to excessive force, preventing corpse spawning.
- **Dissection (Right-Click):** Use an axe or cleaver on the corpse to dissect it.
  - **Fail Tier:** Based on slaughtering tier (80% beginner, 50% T1, 20% T2, 5% T3). Failing ruins the corpse and drops 1 rotten flesh.
  - **Success Qualities:** Rolls between Poor (0.5× loot), Normal (1.0× loot), and Excellent (2.0× loot). Cleavers grant significantly higher excellent chances than axes.
- **Non-Cleaver Kills:** Killing mobs without a cleaver yields only a configurable chance (default 30%) to drop vanilla loot.

---

## ⚔️ Combat & Armor Mechanics

### Combat Damage Scaling
Players deal reduced damage until they level up their combat knowledge.

**Default Damage Multipliers:**

| Combat Tier | Damage Output |
|:---|:---:|
| Tier 0 | 40% |
| Tier 1 | 55% |
| Tier 2 | 70% |
| Tier 3 | 85% |
| Tier 4+ | 100% |

**Melee Combat** XP is gained by dealing damage with swords. **Ranged Combat** XP is gained by dealing projectile damage. Both require hitting a target and dealing > 0 damage.

### Armor Equip Restrictions
Armor is locked behind your **highest combat tier** (the greater of Melee or Ranged).

**Default Armor Requirements:**

| Combat Tier | Armor Allowed |
|:---|:---|
| 0 | Leather |
| 1 | Chainmail |
| 2 | Iron |
| 3 | Gold |
| 4 | Diamond |
| 5 | Netherite |

If a player equips armor they don't have the tier for, it automatically drops off them.

---

## 🎣 Fishing

Fishing has a simplified 3-tier progression:

| Tier | Minutes Required | Fail Chance |
|:---|:---:|:---:|
| Tier 0 | 60 min | 40% |
| Tier 1 | 120 min | 25% |
| Tier 2 | 240 min | 10% |
| Tier 3 | Maxed | 5% |

### Fishing Fail Mechanics
When reeling in a successful bite, there is a chance the fish gets away based on your Fishing tier. If you fail, the bobber retracts normally and you see the message *"The fish got away..."* but you still gain Fishing XP for the attempt.

### Custom Fishing Rods (Datapack Integration)
Two advanced fishing rods exist via datapack integration and are gated behind Toolsmithing knowledge:
- **Good Fishing Rod (`good_rod`)**: Requires Toolsmithing Tier 1 (Stone/Copper tier) to craft. Reduces fishing fail chance by 50% (e.g., T0 fail goes from 40% → 20%).
- **Super Fishing Rod (`super_rod`)**: Requires Toolsmithing Tier 2 (Iron tier) to craft. Reduces fishing fail chance by 75% (e.g., T0 fail goes from 40% → 10%).

---

## ☕ Planned Features

### Tea Brewing & Alcohol Brewing
Upcoming versions will introduce dedicated progression and supervision mechanics for **Tea Brewing** and **Alcohol Brewing**, expanding the culinary and apothecary systems with custom potables, aging barrels, and unique status effects.

---

## 🔒 Proficiency Limits

Players cannot master everything. Proficiency limits cap how many jobs a player can max out:

| Limit | Default | Description |
|:---|:---:|:---|
| `maxMasterMaterial` | 1 | Max Material jobs (5-tier) at Tier 5 |
| `maxTier4Material` | 3 | Max Material jobs at Tier 4 or higher |
| `maxMasterClass` | 1 | Max Class jobs (3-tier) at Tier 3 |

When a player hits a proficiency limit, they see: *"You've reached your proficiency limit for [Knowledge]. You cannot advance further."*

Set any limit to `-1` to disable it.

---

## 🚫 Blocked Items

Some items are completely blocked from crafting. Attempting to craft them will consume ingredients and yield nothing.

**Default blocked items:** All boats and chest boats (oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo raft, and all chest variants).

Additional items can be blocked via the `blockedCraftingItems` config array.

---

## 🖥️ Client-Side HUD

If the mod is installed on the client, pressing **K** (default, rebindable) toggles a Knowledge HUD overlay in the top-left corner showing:

- All 18 knowledge categories
- Current tier and max tier (e.g., `T3/5`)
- Progress bar toward the next tier
- Minutes remaining (e.g., `45/120m`)
- Blue bar for maxed-out knowledges

The HUD syncs automatically with the server on join, respawn, and every XP tick.

---

## 📊 Server-Side HUD & GUI

Even without the client mod, players have two ways to view their knowledge:

### Scoreboard Sidebar (`/kb hud`)

Toggle a scoreboard-based sidebar showing all knowledge tiers. This is fully server-side using the vanilla scoreboard system — no client mod needed. When disabled, any previous sidebar objective is restored.

### Knowledge Chest GUI (`/kb gui`)

Opens a read-only double chest GUI displaying all 18 knowledges:

- Each knowledge is represented by a named item with its current tier and progress
- Progress bars made from green/gray stained glass panes show how close you are to the next tier
- Mastered knowledges display an **enchantment glint** on their icon
- The GUI is server-side and read-only — players cannot take items from it

---

## ⚙️ Full Configuration Reference

The mod generates `knowledgebound.json` in your server's `config/` directory. All values can be hot-reloaded with `/kb reload`.

### Global XP Tuning

| Key | Default | Description |
|:---|:---|:---|
| `baseMinutesPerTier` | `[60, 120, 240, 480, 960]` | Minutes per tier for 5-tier jobs (index 0 = T1) |
| `classJobBaseMinutes` | `[60, 120, 240]` | Minutes per tier for 3-tier class jobs |
| `fishingBaseMinutes` | `[60, 120, 240]` | Minutes per tier for fishing |
| `minutesMultiplier` | `1.0` | Global multiplier. `0.5` = 2× faster, `2.0` = 2× slower |

### Proficiency Limits

| Key | Default | Description |
|:---|:---|:---|
| `maxMasterMaterial` | `1` | Max material jobs at max tier. `-1` = disabled |
| `maxTier4Material` | `3` | Max material jobs at T4+. `-1` = disabled |
| `maxMasterClass` | `1` | Max class jobs at max tier. `-1` = disabled |

### Combat Damage Scaling

| Key | Default | Description |
|:---|:---|:---|
| `combatDamageScale` | `[0.40, 0.55, 0.70, 0.85, 1.0, 1.0]` | Damage multiplier per combat tier (index 0 = T0) |

### Gathering Fail Chances

Each gathering skill has its own `GatherFailConfig` with `tier0` through `tier4` values:

| Key | T0 | T1 | T2 | T3 | T4 |
|:---|:---:|:---:|:---:|:---:|:---:|
| `forestryGatherFail` | 0.40 | 0.25 | 0.10 | 0.05 | 0.02 |
| `miningGatherFail` | 0.40 | 0.25 | 0.10 | 0.05 | 0.02 |
| `diggingGatherFail` | 0.40 | 0.25 | 0.10 | 0.05 | 0.02 |
| `farmingGatherFail` | 0.30 | 0.20 | 0.10 | 0.05 | 0.02 |

### Crafting Tier-Difference Chances

`craftingDiffChances` is an array of 6 entries (diff ≤ −3 through diff ≥ +2). Each entry has `failChance`, `poorChance`, `normalChance`. Values auto-normalize if they don't sum to 1.0.

| Index | Diff | Fail | Poor | Normal |
|:---:|:---:|:---:|:---:|:---:|
| 0 | ≤ −3 | 1.00 | 0.00 | 0.00 |
| 1 | −2 | 0.85 | 0.12 | 0.03 |
| 2 | −1 | 0.45 | 0.35 | 0.20 |
| 3 | 0 | 0.10 | 0.15 | 0.75 |
| 4 | +1 | 0.00 | 0.08 | 0.92 |
| 5 | ≥ +2 | 0.00 | 0.00 | 1.00 |

### Crafting Quality

| Key | Default | Description |
|:---|:---|:---|
| `poorDurabilityFraction` | `0.10` | Fraction of max durability for poor-quality items (0.10 = 10%) |

### Blocked Crafting

| Key | Default | Description |
|:---|:---|:---|
| `blockBoats` | `true` | Whether vanilla boats/rafts are blocked from crafting |

### Stonecutter Settings

| Key | Default | Description |
|:---|:---|:---|
| `stonecutterMinTier` | `1` | Minimum Masonry tier required to use the stonecutter |
| `stonecutterCutChanceTier1` | `0.10` | Chance to take damage at Masonry T1 |
| `stonecutterCutReductionPerTier` | `0.05` | Reduction per tier above 1 |
| `stonecutterCutDamage` | `2.0` | Damage dealt when cutting yourself (in half-hearts) |

### Beekeeping Settings

| Key | Default | Description |
|:---|:---|:---|
| `beekeepingHarvestFail` | `{0.50, 0.30, 0.10, 0.0, 0.0}` | Harvest fail chance per tier |
| `betterHoneyChance` | `[0.0, 0.10, 0.25]` | Better honey chance per tier (T1-T3) |
| `silkTouchBeehiveMinTier` | `3` | Min beekeeping tier to Silk Touch beehives |

### Better Honey Config

| Key | Default | Description |
|:---|:---|:---|
| `betterHoney.itemId` | `"minecraft:honey_bottle"` | Base item for better honey |
| `betterHoney.customName` | `"Royal Honey"` | Display name |
| `betterHoney.nameColor` | `"gold"` | Minecraft formatting color name |
| `betterHoney.effects` | Regen II 10s, Saturation I 5s | Array of `{effectId, durationTicks, amplifier}` |

### Smelting & Cooking Settings

| Key | Default | Description |
|:---|:---|:---|
| `smeltingEnabled` / `cookingEnabled` | `true` | Master toggles for supervision systems |
| `smeltingGraceTimeTicks` | `600` (30s) | Grace period before unattended smelting fails |
| `smeltingCollectionWindowTicks` | `1200` (60s) | Time window to collect smelted items |
| `cookingGraceTimeTicks` | `400` (20s) | Grace period before unattended cooking fails |
| `cookingCollectionWindowTicks` | `600` (30s) | Time window to collect cooked food |
| `cookingAppliesToCampfire` | `true` | Whether campfires require supervision |

### Husbandry Settings

| Key | Default | Description |
|:---|:---|:---|
| `husbandryEnabled` | `true` | Master toggle for husbandry features |
| `husbandryBreedingEnabled` / `husbandryTamingEnabled` | `true` | Toggles for breeding and taming tier checks |
| `husbandryBreedingConsumeItemOnFail` | `true` | Whether breeding items are lost on failure |
| `husbandryBreedingCooldownEnabled` | `true` | Whether breeding failure incurs a cooldown |
| `husbandryRidingEnabled` | `true` | Toggles for mount riding tier checks |
| `husbandryRidingKickOffChance` | `0.25` | Chance to be kicked off per check interval |
| `husbandryRidingCheckIntervalTicks` | `100` (5s) | Ticks between kick-off checks |
| `husbandryDisableEggChickenSpawn` | `true` | Whether thrown eggs are prevented from hatching |

### Jeweller Settings

| Key | Default | Description |
|:---|:---|:---|
| `jewellerEnabled` | `true` | Master toggle for jeweller system |
| `jewellerSmithingEnabled` | `true` | Gate smithing table trims/gems behind Jeweller |
| `jewellerCraftingItems` | Maps item IDs to tier | Gated jewelry items |

### Slaughtering Settings

| Key | Default | Description |
|:---|:---|:---|
| `slaughteringEnabled` | `true` | Master toggle for slaughtering system |
| `slaughteringAllMobsByDefault` | `true` | Allow all mobs to be slaughtered unless blacklisted |
| `slaughteringMobBlacklist` / `Whitelist` | `[...]` | Blacklist/whitelist for allowed mobs |
| `slaughteringCorpseDespawnTicks` | `6000` (5 min) | Time before a corpse entity despawns |
| `slaughteringCleaverCustomModelData` | `2` | CustomModelData for Butcher's Cleaver |
| `slaughteringFailChancePerTier` | `[0.80, 0.50, 0.20, 0.05]` | Fail chance per tier [T0, T1, T2, T3] |
| `slaughteringAxeDissectionChances` | `[0.20, 0.50, 0.30]` | Axe quality chances [poor, normal, excellent] |
| `slaughteringCleaverDissectionChances` | `[0.10, 0.40, 0.50]` | Cleaver quality chances [poor, normal, excellent] |
| `slaughteringLootMultipliers` | `[0.5, 1.0, 2.0]` | Loot multiplier per quality [poor, normal, excellent] |
| `slaughteringNonCleaverLootChance` | `0.3` | Chance for vanilla loot when killed without cleaver |
| `slaughteringBaseMinutes` | `[30, 60, 120]` | Minutes required per tier [T1, T2, T3] |

### Extra Item Lists

| Key | Description |
|:---|:---|
| `extraToolItems` | Item IDs to add to the Toolsmithing rule |
| `extraArmorItems` | Item IDs to add to the Armouring rule |
| `extraWeaponItems` | Item IDs to add to the Weaponsmithing rule |
| `extraCarpentryItems` | Item IDs to add to the Carpentry rule |
| `extraMasonryItems` | Item IDs to add to the Masonry rule |
| `blockedCraftingItems` | Item IDs to completely block from crafting |
| `itemCraftingTierOverrides` | Map of `"item_id": tier` to override any item's required tier |

### Gathering Block Extensions

| Key | Description |
|:---|:---|
| `extraForestryBlocks` | Block IDs to add to Forestry gathering |
| `extraMiningBlocks` | Block IDs to add to Mining gathering |
| `extraDiggingBlocks` | Block IDs to add to Digging gathering |
| `extraFarmingBlocks` | Block IDs to add to Farming gathering |

### Armor Tier Requirements

The `armorTiers` section lets you customize which combat tier is needed to equip each armor type, and add modded armor via `extraItemTiers`.

---

## 📣 Commands

All commands use the `/kb` prefix. Legacy alias `/checkxp` is also available for self-check. All `<knowledge>` arguments support tab auto-completion.

### Player Commands (Everyone)

| Command | Description |
|:---|:---|
| `/kb` | Show your own knowledge levels and progress |
| `/checkxp` | Legacy alias — same as `/kb` |
| `/kb help` | Show the command help list |
| `/kb hud` | Toggle the knowledge sidebar HUD on/off |
| `/kb gui` | Open the knowledge progress chest GUI |
| `/kb list` | List all registered knowledge types and their max tiers |

### Admin Commands (OP Level 2+)

| Command | Description |
|:---|:---|
| `/kb check <player>` | View another player's knowledge levels |
| `/kb set <player> <knowledge> <tier>` | Set a player's tier for a specific knowledge (0–max) |
| `/kb grant <player> <knowledge> <minutes>` | Grant XP minutes to a player (1–10000, bypasses rate limiter) |
| `/kb give <item>` | Give yourself a custom KB item (e.g. `royal_honey`). Tab-complete for item list |
| `/kb reset <player>` | Reset ALL knowledges for a player to Tier 0 |
| `/kb reset <player> <knowledge>` | Reset a single knowledge to Tier 0 |
| `/kb reload` | Hot-reload `knowledgebound.json` without restart |
| `/kb config get <key>` | Query the current value of a configuration key |
| `/kb config set <key> <value>` | Modify a configuration key value and save it to disk immediately |

### 🛡️ LuckPerms & Permissions API Support
KnowledgeBound fully integrates with the **Fabric Permissions API** (`me.lucko.fabric.api.permissions.v0.Permissions`). If you have **LuckPerms** (or any compatible permission plugin) installed, you can fine-tune access using the following permission nodes (which fallback to standard OP levels if no plugin is installed):

| Permission Node | Default OP | Description |
|:---|:---:|:---|
| `knowledgebound.command.base` | `0` | Access to `/kb` and `/checkxp` |
| `knowledgebound.command.help` | `0` | Access to `/kb help` |
| `knowledgebound.command.hud` | `0` | Access to `/kb hud` |
| `knowledgebound.command.gui` | `0` | Access to `/kb gui` |
| `knowledgebound.command.list` | `0` | Access to `/kb list` |
| `knowledgebound.command.check` | `2` | Access to `/kb check <player>` |
| `knowledgebound.command.set` | `2` | Access to `/kb set ...` |
| `knowledgebound.command.grant` | `2` | Access to `/kb grant ...` |
| `knowledgebound.command.give` | `2` | Access to `/kb give ...` |
| `knowledgebound.command.reset` | `2` | Access to `/kb reset ...` |
| `knowledgebound.command.reload` | `2` | Access to `/kb reload` |
| `knowledgebound.command.config` | `2` | Access to `/kb config get/set` |
| `knowledgebound.command.admin` | `2` | Access to `/kb admin` (Config GUI) |
| `knowledgebound.exempt.deathloss` | `0` | Exempts the player from knowledge loss on death |

---

## 💀 Knowledge Loss on Death & Staff Exemptions

KnowledgeBound supports an optional penalty system where players lose their knowledge progress upon dying. By default, dying wipes all knowledge completely back to 0, forcing players to start fresh.

### Configuring Death Penalties
- `knowledgeLossOnDeathEnabled`: Set to `true` to enable death penalties (default `true`).
- `knowledgeLossResetEverything`: Set to `true` to wipe ALL knowledge back to Tier 0, 0 minutes on death (default `true`).
- `knowledgeLossMinutesPercentage`: Fraction of current minutes lost towards the next tier (used if `resetEverything` is `false`).
- `knowledgeLossTiers`: Number of full tiers lost upon dying (used if `resetEverything` is `false`).

### Staff & Exemption Lists
You can exempt staff members or specific players from losing knowledge on death in two ways:
1. **Config Exemption List**: Paste staff usernames directly into the `knowledgeLossExemptUsernames` array in `knowledgebound.json` (or via `/kb admin`).
2. **LuckPerms Exemption**: Grant the player/group the `knowledgebound.exempt.deathloss` permission node.

---

## 🏆 Datapack Compatibility

KnowledgeBound stores player data in their root NBT under `knowledgebound_knowledge` as an SNBT List. You can use this with standard datapack conditions.

**Example: Advancement when a player reaches Tier 5 Melee Combat:**
```json
{
  "criteria": {
    "reached_tier_5": {
      "trigger": "minecraft:tick",
      "conditions": {
        "player": [
          {
            "condition": "minecraft:entity_properties",
            "entity": "this",
            "predicate": {
              "nbt": "{knowledgebound_knowledge:[{id:\"knowledgebound:melee_combat\", tier:5}]}"
            }
          }
        ]
      }
    }
  }
}
```

### Item Tags

The mod registers custom item tags under `data/knowledgebound/tags/item/` for tool tier detection. These can be extended via datapacks to add modded items:

| Tag | Contents |
|:---|:---|
| `knowledgebound:wooden_tools` | All wooden tools |
| `knowledgebound:stone_tools` | All stone tools |
| `knowledgebound:copper_tools` | Copper tools (currently empty, for modded) |
| `knowledgebound:iron_tools` | All iron tools |
| `knowledgebound:diamond_tools` | All diamond tools |
| `knowledgebound:leather_armor` | All leather armor |
| `knowledgebound:chainmail_armor` | All chainmail armor |
| `knowledgebound:bows` | Bows |
| `knowledgebound:crossbows` | Crossbows |
| `knowledgebound:fishing_rods` | Fishing rods |
| `knowledgebound:melee_weapons` | All swords |

---

## 🛑 Current Limitations

- **Hardcoded Categories:** The 17 knowledge categories are defined in Java code. You cannot create entirely new categories (like "Sorcery") via the config — but you can add items/blocks to existing categories.
- **Enchantment Qualities:** "Excellent" and "Master" quality crafting with fixed enchantments is planned but not yet implemented.

---

## 👥 Credits

- **Maxello** — Lead developer. Core mod architecture, all game mechanics (gathering, crafting, combat, armor restrictions), job category system (Material/Class/Gathering/Combat), Carpentry, Masonry, Beekeeping, Stonecutter, proficiency limits, boat blocking, configuration system, admin commands.
- **nipatiitti** — Client-side Knowledge HUD overlay, server→client state synchronization (`KnowledgeSyncPayload`), item tag system for tool tier detection (`KnowledgeTags`), unit tests (`CraftingTierChancesTest`, `GatherFailConfigTest`), code cleanup and import reorganization.

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Drop `KnowledgeBound-x.x.x.jar` into your server's `mods/` folder
4. (Optional) Also install on the client for the Knowledge HUD overlay
5. Start the server — `knowledgebound.json` will generate in `config/`
6. Tweak the config to your liking and use `/kb reload` to apply changes
