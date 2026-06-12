# 🎓 KnowledgeBound

> **A server-side Fabric mod for Minecraft 1.21.1 that replaces vanilla XP with a deep skill & knowledge progression system.**

KnowledgeBound completely overhauls how players interact with the world. Instead of a single XP bar, players advance through **13 distinct knowledge types** — each with its own tier, progression speed, and gameplay consequences. Gathering resources, crafting items, fighting mobs, fishing, and even beekeeping are all governed by your knowledge level. Unskilled players fail more often, deal less damage, and produce lower-quality items. Mastery takes real time and dedication.

---

## ✨ Features

- **13 Knowledge Types** across 5 categories: Gathering, Material crafting, Combat, Fishing, and Class professions
- **Tier-based progression** — earn XP in real-time minutes by performing relevant actions
- **Gathering failure** — low-tier players have a chance to destroy blocks without getting drops
- **Crafting quality system** — craft attempts can fail entirely, produce poor-quality items (10% durability), or succeed normally
- **Combat damage scaling** — deal reduced damage until you've trained your combat skills
- **Armor restrictions** — higher-tier armor requires higher combat knowledge
- **Stonecutter masonry** — requires Masonry knowledge, with a chance to cut yourself
- **Beekeeping** — fail chance, angry bees, and rare Royal Honey with potion effects + enchant glint
- **Proficiency limits** — prevent players from mastering everything; force specialization
- **Boats are blocked** — all vanilla boats/rafts cannot be crafted
- **Anvils are free** — anvil operations cost 0 XP levels
- **Enchanting is free** — enchanting table operations cost 0 XP levels
- **XP orbs are disabled** — orbs are discarded on contact; knowledge replaces vanilla XP entirely
- **Scoreboard HUD** — toggleable sidebar showing all knowledge tiers and progress
- **Fully configurable** — JSON config with hundreds of tunable values
- **Datapack/tag extensible** — add modded items via item tags and config lists
- **Persistent data** — knowledge saves with the player via NBT and survives death

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft **1.21.1**
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (required dependency)
3. Drop the `KnowledgeBound-x.x.x.jar` into your server's `mods/` folder
4. Start the server — a default `knowledgebound.json` config file will be generated in the `config/` directory
5. **No client-side mod is required** — the mod is fully server-side (an optional client module exists for HUD rendering)

---

## 📚 Knowledge Types

KnowledgeBound defines **13 knowledge types** organized into 5 categories.

### Gathering (4 types — 5 tiers each)

Gathering knowledges govern resource collection. Each tier requires a specific **tool tier** to earn XP, and has a configurable **fail chance** where the block breaks but drops nothing.

| Knowledge | Blocks Affected | XP Tool Progression |
|-----------|----------------|---------------------|
| **Forestry** | Logs (`_log`), wood (`_wood`), stems (`_stem`), hyphae (`_hyphae`) | Tier 0→1: Wood · Tier 1→2: Stone · Tier 2→3: Copper · Tier 3→4: Iron · Tier 4→5: Diamond |
| **Mining** | Stone, deepslate, netherrack, blackstone, tuff, all ores (`_ore`), gilded blackstone | Tier 0→1: Wood · Tier 1→2: Stone · Tier 2→3: Copper · Tier 3→4: Iron · Tier 4→5: Diamond |
| **Digging** | Dirt, coarse dirt, rooted dirt, grass block, podzol, mycelium, mud, muddy mangrove roots, sand, red sand, gravel, clay, snow, snow block, powder snow, soul sand, soul soil | Tier 0→1: Wood · Tier 1→2: Stone · Tier 2→3: Copper · Tier 3→4: Iron · Tier 4→5: Diamond |
| **Farming** | Mature crops only: wheat, carrots, potatoes, beetroots, melon stems, pumpkin stems | Tier 0→1: Fist/Wood · Tier 1→2: Wood/Stone · Tier 2→3: Stone/Copper · Tier 3→4: Copper/Iron · Tier 4→5: Iron/Diamond |

> **Note:** Farming XP is granted regardless of held tool (the tool tier gate only applies to the XP-earning check, not the action itself). Immature crops are completely ignored — no XP, no fail chance.

#### Gathering Fail Chances (defaults)

| Tier | Forestry | Mining | Digging | Farming |
|------|----------|--------|---------|---------|
| 0 | 40% | 40% | 40% | 30% |
| 1 | 25% | 25% | 25% | 20% |
| 2 | 10% | 10% | 10% | 10% |
| 3 | 5% | 5% | 5% | 5% |
| 4 | 2% | 2% | 2% | 2% |

When a gather attempt fails, the block still breaks (keeping the client in sync) but all dropped items within a 2-block radius are removed on the next tick.

---

### Material Professions (3 types — 5 tiers each)

Material professions control the quality of crafted tools, weapons, and armor. They use a **strict tier gate** — you **cannot** craft items above your current tier (100% fail, ingredients lost). Within your tier and below, the crafting quality system applies.

| Knowledge | Items Governed | XP Tool Tiers |
|-----------|---------------|---------------|
| **Toolsmithing** | Pickaxes, axes, shovels, hoes (all materials) | Wood → Stone → Copper → Iron → Diamond |
| **Weaponsmithing** | Swords (all materials), bow, crossbow, trident | Wood → Stone → Copper → Iron → Diamond |
| **Armouring** | All armor pieces (leather through netherite) | Leather → Chainmail → Copper → Iron → Diamond |

#### Crafting Item Tiers

**Toolsmithing:**

| Item Tier | Items |
|-----------|-------|
| 0 | Wooden axe, pickaxe, shovel, hoe |
| 1 | Stone axe, pickaxe, shovel, hoe |
| 2 | Iron axe, pickaxe, shovel, hoe |
| 3 | Diamond axe, pickaxe, shovel, hoe |
| 4 | Netherite axe, pickaxe, shovel, hoe |

**Weaponsmithing:**

| Item Tier | Items |
|-----------|-------|
| 0 | Wooden sword |
| 1 | Stone sword, golden sword, bow |
| 2 | Iron sword, crossbow |
| 3 | Diamond sword |
| 4 | Netherite sword |

**Armouring:**

| Item Tier | Items |
|-----------|-------|
| 0 | Leather helmet, chestplate, leggings, boots |
| 1 | Chainmail armor (all), golden armor (all) |
| 2 | Iron armor (all), turtle helmet |
| 3 | Diamond armor (all) |
| 4 | Netherite armor (all) |

#### Crafting Quality System

Crafting outcomes are determined by the **tier difference** between your knowledge tier and the item's required tier:

```
diff = playerKnowledgeTier - itemTier
```

| Diff | Description | Fail % | Poor % | Normal % |
|------|------------|--------|--------|----------|
| ≤ −3 | Impossible | 100% | 0% | 0% |
| −2 | Very risky | 85% | 12% | 3% |
| −1 | Challenging | 45% | 35% | 20% |
| 0 | At your level | 10% | 15% | 75% |
| +1 | Below your skill | 0% | 8% | 92% |
| ≥ +2 | Trivial | 0% | 0% | 100% |

> **Material jobs hard-block tier jumps** — if `diff < 0`, the craft is always 100% fail. The table above applies only to Class jobs (Carpentry, Masonry) which can attempt above-tier crafts.

- **Fail**: No item produced. Ingredients are consumed.
- **Poor quality**: Item is created with only **10%** of its max durability (configurable via `poorDurabilityFraction`). Non-damageable items cannot be poor quality and fall through to normal.
- **Normal**: Full durability item as expected.

---

### Combat (2 types — 5 tiers each)

Combat knowledges affect how much damage you deal. Your damage is **multiplied** by a scaling factor based on your combat tier.

| Knowledge | How XP Is Earned | XP Tool Tiers |
|-----------|-----------------|---------------|
| **Melee Combat** | Hitting entities with swords (items in `knowledgebound:melee_weapons` tag) | Wood → Stone → Copper → Iron → Diamond |
| **Ranged Combat** | Hitting entities with projectiles (bow/crossbow) | Tier 0→1: Bow · Tier 1→2: Bow/Crossbow · Tier 2+: Crossbow only |

#### Combat Damage Scaling (defaults)

| Combat Tier | Damage Multiplier |
|-------------|-------------------|
| 0 | 40% |
| 1 | 55% |
| 2 | 70% |
| 3 | 85% |
| 4 | 100% |
| 5 | 100% |

Damage scaling is applied via mixin before any other damage processing. It applies to both melee and ranged, with the appropriate knowledge tier used based on whether the damage source is a projectile.

---

### Fishing (1 type — 3 tiers)

| Knowledge | How XP Is Earned | XP Tool Tiers |
|-----------|-----------------|---------------|
| **Fishing** | Successfully catching an item with a fishing rod | Fishing Rod (all tiers) |

Fishing uses its own base minutes per tier (default: `[60, 120, 240]`) and is classified as a `PROFESSION` type rather than a `SKILL`.

---

### Class Professions (3 types — 3 tiers each)

Class professions govern crafting of specific item categories. Unlike Material professions, class jobs **can attempt above-tier crafts** using the standard crafting quality diff table (with appropriate fail/poor chances).

| Knowledge | Items Governed |
|-----------|---------------|
| **Carpentry** | All wood-based crafting: planks, sticks, torches (T0) · banners, bookshelves, bowls, buttons, campfires, doors, trapdoors, fences, levers, stairs, slabs, trapped chests (T1) · bamboo mosaic, beds, beehive, chiseled bookshelf, crafting table, cartography/fletching table, fence gates, signs, hanging signs, pressure plates, paintings, item frames, full wood blocks (T2) · chests, barrels, armor stands, composters, jukeboxes, lecterns, looms, scaffolding, note blocks, ladders (T3) |
| **Masonry** | Stone-based crafting and stonecutter use: stone/cobblestone stairs & slabs, stone buttons/plates, levers, furnaces, smokers, concrete (T1) · bricks, polished stone variants, blast furnaces, flower/decorated pots, glass (all), glass panes (all), tinted glass, glowstone, stonecutter (T2) · dispensers, droppers, observers, pistons, sticky pistons (T3) |
| **Beekeeping** | Honey/honeycomb harvesting from beehives (see Beekeeping Mechanics below) |

> **Bamboo items** have +1 tier compared to their wood equivalents (e.g., bamboo door = T2, bamboo fence gate = T3).

#### Class Job Base Minutes (defaults)

| Tier | Minutes Required |
|------|-----------------|
| 1 | 60 |
| 2 | 120 |
| 3 | 240 |

---

## ⚙️ Mechanics In Detail

### XP Progression

- XP is tracked in **real-time minutes**. Each relevant action (block break, successful craft, mob hit, fish catch) can grant **1 minute of XP** — but only if at least 1 real-time minute has passed since the last XP gain for that specific knowledge.
- The vanilla XP bar is repurposed: the **level number** shows your current tier, and the **bar fill** shows progress toward the next tier.
- When you earn XP for a non-crafting knowledge, an action bar message says: `"You're learning <Knowledge>!"`
- When you tier up: `"Your <Knowledge> knowledge increased to Tier <N>!"`

### Default XP Minutes Per Tier (5-tier knowledges)

| Tier | Base Minutes | With 1.0× Multiplier |
|------|-------------|----------------------|
| 1 | 60 | 60 |
| 2 | 120 | 120 |
| 3 | 240 | 240 |
| 4 | 480 | 480 |
| 5 | 960 | 960 |

The `minutesMultiplier` config scales all of these (e.g., `2.0` = twice as slow).

---

### Stonecutter Mechanics

The stonecutter is gated behind **Masonry** knowledge:

1. **Minimum tier required** — You need at least Masonry Tier 1 (configurable: `stonecutterMinTier`) to select recipes. Attempting to use it below this tier shows: `"You need Masonry Tier 1 to use the stonecutter."`

2. **Fail chance** — When taking stonecutter output, the crafting diff table is used. If the craft fails, the input block is consumed, the output is cleared, and you see: `"Your Masonry attempt failed to yield any items."`

3. **Cut damage** — Even on success, there's a chance to cut yourself. Base chance at Tier 1: **10%** (`stonecutterCutChanceTier1`), reduced by **5%** per tier above 1 (`stonecutterCutReductionPerTier`). At Tier 3, cut chance reaches 0%. Cut damage: **2.0 half-hearts** (`stonecutterCutDamage`). Message: `"You cut yourself on the stonecutter!"`

4. **XP is granted** on both success and failure (you learn from mistakes).

---

### Beekeeping Mechanics

Beekeeping applies when players interact with beehives/bee nests using glass bottles or shears:

1. **Fail chance** — Based on your beekeeping tier. On fail, bees are angered regardless of campfire placement. Both stored bees and nearby bees (within 10 blocks) become hostile for 20–40 seconds. Message: `"Your clumsy handling angered the bees!"`

   | Tier | Fail Chance |
   |------|-------------|
   | 0 | 50% |
   | 1 | 30% |
   | 2 | 10% |
   | 3+ | 0% |

2. **Royal Honey** — On successful glass bottle harvest, there's a chance to receive "Royal Honey" — a special honey bottle with:
   - Custom gold-colored name: **"Royal Honey"**
   - Enchantment glint (shimmer effect)
   - Potion effects: Regeneration II (10 seconds) + Saturation I (5 seconds)

   | Tier | Royal Honey Chance |
   |------|-------------------|
   | 0 | 0% |
   | 1 | 10% |
   | 2 | 25% |

3. **Beehive breaking restriction** — Breaking a beehive that contains stored bees requires Beekeeping Tier 3 (configurable: `silkTouchBeehiveMinTier`). Message: `"You need Beekeeping Tier 3 to move beehives with bees."`

4. **XP is granted** on both success and failure.

---

### Armor Restrictions

Armor is restricted based on your **highest combat knowledge** tier (max of Melee Combat and Ranged Combat). Every second (20 ticks), the server checks all online players' armor slots. If a player is wearing armor above their combat tier, the armor is:
1. Unequipped from the slot
2. Placed in the player's inventory (or dropped if inventory is full)
3. An action bar message is shown: `"You need <Tier Name> Combat Knowledge to wear this armor!"`

| Required Tier | Tier Name | Armor Material |
|--------------|-----------|----------------|
| 0 | Leather | Leather |
| 1 | Chainmail | Chainmail |
| 2 | Iron | Iron |
| 3 | Gold | Gold |
| 4 | Diamond | Diamond |
| 5 | Netherite | Netherite |

Modded armor with no explicit override is **unrestricted** by default. Per-item overrides can be set in the config.

---

### Anvils Are Free

The anvil XP cost is forced to **0 levels**. The "can take output?" check is overridden so players can always take the result regardless of XP level.

### Enchanting Is Free

When a player clicks an enchantment option, their XP level is temporarily set to 30 (if below 30) to pass vanilla's level check. Any negative XP subtraction is blocked by a separate mixin. After the enchant completes, the XP bar is restored to show the player's knowledge progress.

### XP Orbs Are Disabled

All vanilla experience orbs are **discarded on player collision** — they never grant XP. KnowledgeBound has its own XP tracking system, so vanilla XP is irrelevant.

### Boats Are Blocked

All vanilla boats and rafts (oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo raft, and all chest boat variants) **cannot be crafted**. Attempting to craft them shows: `"This item cannot be crafted."` and ingredients are consumed.

### Shift-Click Crafting

Shift-clicking in the crafting table result slot is **disabled** (returns `ItemStack.EMPTY`). This prevents players from bypassing the crafting quality system. Shift-clicking in the stonecutter output is handled separately and does work with the masonry mechanics applied.

---

### Proficiency Limits

Players cannot master everything. The following caps prevent over-specialization:

| Limit | Default | Description |
|-------|---------|-------------|
| `maxMasterMaterial` | 1 | Max Material professions (5-tier) at Tier 5 |
| `maxTier4Material` | 3 | Max Material professions at Tier 4 or higher |
| `maxMasterClass` | 1 | Max Class professions (3-tier) at Tier 3 |

When a player's XP reaches the level-up threshold but they've hit a proficiency cap, their XP is frozen at the maximum and they see: `"You've reached your proficiency limit for <Knowledge>. You cannot advance further."`

Set any limit to `-1` to disable it.

---

### Scoreboard HUD

Players can toggle a server-side scoreboard sidebar displaying all 13 knowledge types with their current tier, max tier, progress (current/needed minutes), and color coding:

- **Gray** — Tier 0 (untrained)
- **Yellow** — In progress
- **Green** — Maxed (`MAX`)

The HUD is toggled with `/kb hud` and is fully server-side using the vanilla scoreboard system. When disabled, the previous sidebar objective is restored.

---

### Death & Respawn

Knowledge data **persists through death**. When a player respawns, all knowledge state is copied from the old player entity to the new one. The XP bar is restored after a 1-tick delay (to avoid conflicts with vanilla's player loading), and a full client sync is sent.

---

## 🔧 Commands

All commands use the `/kb` prefix. Legacy alias `/checkxp` is also available for self-check.

| Command | Permission | Description |
|---------|-----------|-------------|
| `/kb` | Everyone (level 0) | Show your own knowledge levels and progress |
| `/checkxp` | Everyone (level 0) | Legacy alias — same as `/kb` |
| `/kb help` | Everyone (level 0) | Show the command help list |
| `/kb hud` | Everyone (level 0) | Toggle the knowledge sidebar HUD on/off |
| `/kb gui` | Everyone (level 0) | Open the knowledge progress chest GUI |
| `/kb list` | Everyone (level 0) | List all registered knowledge types and their max tiers |
| `/kb check <player>` | OP (level 2) | Check another player's knowledge levels |
| `/kb set <player> <knowledge> <tier>` | OP (level 2) | Set a player's tier for a specific knowledge (0–max). Resets progress minutes to 0. |
| `/kb grant <player> <knowledge> <minutes>` | OP (level 2) | Grant a specific number of XP minutes to a player (1–10000) |
| `/kb give <item>` | OP (level 2) | Give yourself a custom KB item (e.g. Royal Honey). Use tab-complete for item list |
| `/kb reset <player>` | OP (level 2) | Reset ALL knowledges for a player to Tier 0 |
| `/kb reset <player> <knowledge>` | OP (level 2) | Reset a single knowledge for a player to Tier 0 |
| `/kb reload` | OP (level 2) | Reload config from `knowledgebound.json` without restarting |

### Command Notes

- `/kb check` requires a `<player>` argument — it does **not** have an optional knowledge parameter. It always shows all knowledge types.
- `/kb set` accepts tier values from 0 to 10 in the argument, but validates against the knowledge's actual max tier.
- All `<knowledge>` arguments support tab-completion with all registered knowledge IDs (e.g., `forestry`, `mining`, `melee_combat`, `beekeeping`).
- The `<item>` argument in `/kb give` supports tab-completion. Currently available: `royal_honey`.
- When setting or resetting knowledge, both the admin and the target player receive feedback messages.
- `/kb gui` opens a read-only double chest GUI showing all 13 knowledges with tier progress, progress bars, and enchant glint on mastered skills.

### Self-Check Output Format

Running `/kb` or `/checkxp` displays all knowledges in a formatted list:

```
=== Your Knowledge Levels ===
  Forestry: Tier 2 (45 / 240 min)       (yellow)
  Mining: Tier 0 (0 / 60 min)           (gray)
  Toolsmithing: Tier 5 (MAX)            (green)
  ...
```

---

## 📋 Complete Config Reference

The config file is located at `config/knowledgebound.json` and is generated on first launch. All values can be hot-reloaded with `/kb reload`.

### Global XP Tuning

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `baseMinutesPerTier` | `int[5]` | `[60, 120, 240, 480, 960]` | Base real-time minutes for tiers 1–5 (before multiplier) |
| `minutesMultiplier` | `double` | `1.0` | Global multiplier for all minute values. `2.0` = twice as slow, `0.5` = twice as fast |

### Class Job Minutes

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `classJobBaseMinutes` | `int[3]` | `[60, 120, 240]` | Base minutes for class job tiers 1–3 (before multiplier) |

### Fishing Minutes

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `fishingBaseMinutes` | `int[3]` | `[60, 120, 240]` | Base minutes for fishing tiers 1–3 (before multiplier) |

### Proficiency Limits

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `maxMasterMaterial` | `int` | `1` | Max Material jobs at Tier 5. `-1` = unlimited |
| `maxTier4Material` | `int` | `3` | Max Material jobs at Tier 4+. `-1` = unlimited |
| `maxMasterClass` | `int` | `1` | Max Class jobs at Tier 3. `-1` = unlimited |

### Combat Damage Scaling

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `combatDamageScale` | `double[6]` | `[0.40, 0.55, 0.70, 0.85, 1.0, 1.0]` | Damage multiplier per combat tier (index 0 = Tier 0 through index 5 = Tier 5) |

### Gathering Fail Chances

Each gathering knowledge has a `GatherFailConfig` object with `tier0` through `tier4` fields (all `double`, range 0.0–1.0):

| Field | Forestry Default | Mining Default | Digging Default | Farming Default |
|-------|-----------------|---------------|-----------------|-----------------|
| `tier0` | 0.40 | 0.40 | 0.40 | 0.30 |
| `tier1` | 0.25 | 0.25 | 0.25 | 0.20 |
| `tier2` | 0.10 | 0.10 | 0.10 | 0.10 |
| `tier3` | 0.05 | 0.05 | 0.05 | 0.05 |
| `tier4` | 0.02 | 0.02 | 0.02 | 0.02 |

Config keys: `forestryGatherFail`, `miningGatherFail`, `diggingGatherFail`, `farmingGatherFail`

### Crafting Quality

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `poorDurabilityFraction` | `double` | `0.10` | Fraction of max durability for poor-quality items (0.10 = 10%) |
| `craftingDiffChances` | `CraftingTierChances[6]` | See table below | Chances per tier-diff index |

Each `CraftingTierChances` entry has `failChance`, `poorChance`, `normalChance` (all `double`). Values are auto-normalized if they don't sum to 1.0.

| Index | Diff | `failChance` | `poorChance` | `normalChance` |
|-------|------|-------------|-------------|----------------|
| 0 | ≤ −3 | 1.00 | 0.00 | 0.00 |
| 1 | −2 | 0.85 | 0.12 | 0.03 |
| 2 | −1 | 0.45 | 0.35 | 0.20 |
| 3 | 0 | 0.10 | 0.15 | 0.75 |
| 4 | +1 | 0.00 | 0.08 | 0.92 |
| 5 | ≥ +2 | 0.00 | 0.00 | 1.00 |

### Item Crafting Tier Overrides

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `itemCraftingTierOverrides` | `Map<String, Integer>` | `{}` | Per-item tier overrides. Key = full item ID (e.g., `"minecraft:iron_sword"`), value = required tier |

### Armor Restrictions

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `armorTiers.leatherTier` | `int` | `0` | Required combat tier for leather armor |
| `armorTiers.chainTier` | `int` | `1` | Required combat tier for chainmail armor |
| `armorTiers.ironTier` | `int` | `2` | Required combat tier for iron armor |
| `armorTiers.goldTier` | `int` | `3` | Required combat tier for gold armor |
| `armorTiers.diamondTier` | `int` | `4` | Required combat tier for diamond armor |
| `armorTiers.netheriteTier` | `int` | `5` | Required combat tier for netherite armor |
| `armorTiers.extraItemTiers` | `Map<String, Integer>` | `{}` | Per-item armor tier overrides. Key = full item ID, value = required tier |

### Block/Item Extension Lists

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `extraForestryBlocks` | `List<String>` | `[]` | Additional block IDs that count for Forestry XP |
| `extraMiningBlocks` | `List<String>` | `[]` | Additional block IDs that count for Mining XP |
| `extraDiggingBlocks` | `List<String>` | `[]` | Additional block IDs that count for Digging XP |
| `extraFarmingBlocks` | `List<String>` | `[]` | Additional block IDs that count for Farming XP |
| `extraToolItems` | `List<String>` | `[]` | Additional item IDs using the Toolsmithing crafting rule |
| `extraArmorItems` | `List<String>` | `[]` | Additional item IDs using the Armouring crafting rule |
| `extraWeaponItems` | `List<String>` | `[]` | Additional item IDs using the Weaponsmithing crafting rule |
| `extraCarpentryItems` | `List<String>` | `[]` | Additional item IDs using the Carpentry crafting rule |
| `extraMasonryItems` | `List<String>` | `[]` | Additional item IDs using the Masonry crafting rule |

### Blocked Crafting Items

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `blockBoats` | `boolean` | `true` | Whether vanilla boats/rafts are blocked from crafting |
| `blockedCraftingItems` | `List<String>` | `[]` | Additional item IDs completely blocked from crafting |

### Stonecutter Settings

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `stonecutterMinTier` | `int` | `1` | Minimum Masonry tier required to use the stonecutter |
| `stonecutterCutChanceTier1` | `double` | `0.10` | Chance of cutting yourself at Masonry Tier 1 (0.0–1.0) |
| `stonecutterCutReductionPerTier` | `double` | `0.05` | Cut chance reduction per tier above 1 |
| `stonecutterCutDamage` | `float` | `2.0` | Damage dealt when cutting yourself (in half-hearts) |

### Beekeeping Settings

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `beekeepingHarvestFail` | `GatherFailConfig` | `{0.50, 0.30, 0.10, 0.0, 0.0}` | Fail chance per beekeeping tier (only tiers 0–2 are relevant for 3-tier knowledge) |
| `betterHoneyChance` | `double[3]` | `[0.0, 0.10, 0.25]` | Chance to get Royal Honey per tier (index 0 = Tier 1, etc.) |
| `silkTouchBeehiveMinTier` | `int` | `3` | Min beekeeping tier to break beehives containing bees |
| `betterHoney.itemId` | `String` | `"minecraft:honey_bottle"` | Base item for Royal Honey |
| `betterHoney.customName` | `String` | `"Royal Honey"` | Display name for Royal Honey |
| `betterHoney.nameColor` | `String` | `"gold"` | Formatting color for the name |
| `betterHoney.effects` | `PotionEffectEntry[]` | See below | Potion effects applied to Royal Honey |

Default Royal Honey effects:

| Effect | Duration | Amplifier |
|--------|----------|-----------|
| `minecraft:regeneration` | 200 ticks (10s) | 1 (level II) |
| `minecraft:saturation` | 100 ticks (5s) | 0 (level I) |

---

## 💬 All User-Facing Messages

### Action Bar Messages

| Trigger | Message | Color |
|---------|---------|-------|
| XP gain (non-crafting) | `"You're learning <Knowledge>!"` | Green |
| Tier up | `"Your <Knowledge> knowledge increased to Tier <N>!"` | Gold |
| Gathering fail | `"Your <Knowledge> attempt failed to yield any resources."` | Red |
| Crafting fail | `"Your <Knowledge> attempt failed to yield any items."` | Red |
| Crafting poor quality | `"You crafted a **poor** quality item. Improve your <Knowledge> knowledge for better quality."` | Aqua with bold purple "poor" |
| Material tier too low | `"You don't have enough <Knowledge> knowledge to work with these materials."` | Red |
| Proficiency limit | `"You've reached your proficiency limit for <Knowledge>. You cannot advance further."` | Dark Red |
| Blocked item craft | `"This item cannot be crafted."` | Red |
| Stonecutter cut | `"You cut yourself on the stonecutter!"` | Red |
| Stonecutter tier gate | `"You need Masonry Tier <N> to use the stonecutter."` | Red |
| Beekeeping fail | `"Your clumsy handling angered the bees!"` | Red |
| Royal Honey obtained | `"You harvested some Royal Honey!"` | Gold |
| Beehive break blocked | `"You need Beekeeping Tier <N> to move beehives with bees."` | Red |
| Armor restriction | `"You need <Tier Name> Combat Knowledge to wear this armor!"` | Red |

### Chat Messages (not action bar)

| Trigger | Message | Color |
|---------|---------|-------|
| `/kb set` (target player) | `"Your <Knowledge> knowledge has been set to tier <N>!"` | Gold |
| `/kb set` (admin) | `"Set <Player>'s <Knowledge> to tier <N>."` | Green |
| `/kb reset` all (target) | `"All your knowledge has been reset!"` | Red |
| `/kb reset` all (admin) | `"Reset ALL knowledge for <Player>."` | Green |
| `/kb reset` one (target) | `"Your <Knowledge> knowledge has been reset!"` | Red |
| `/kb reset` one (admin) | `"Reset <Knowledge> for <Player>."` | Green |
| `/kb reload` | `"KnowledgeBound config reloaded!"` | Green |
| `/kb hud` on | `"Knowledge HUD enabled."` | Green |
| `/kb hud` off | `"Knowledge HUD disabled."` | Green |
| `/kb give` success | `"Given <Item Name>!"` | Green |
| `/kb grant` (admin) | `"Granted <N> minutes of <Knowledge> to <Player>."` | Green |
| `/kb grant` (target) | `"You received <N> minutes of <Knowledge> experience!"` | Gold |
| `/kb list` | Formatted list of all knowledge types with max tiers | Gold/Yellow |
| `/kb help` | Full formatted command list | Gold/Yellow/Gray |
| `/kb` / `/kb check` | Formatted knowledge list with tiers and progress | Gold header, colored lines |

---

## 🏷️ Datapack / Tag Extension

KnowledgeBound uses **custom item tags** under the `knowledgebound` namespace to classify tools and weapons. You can extend these via datapacks to add modded items.

### Available Tags

All tags are in `data/knowledgebound/tags/item/`:

| Tag | Purpose | Default Contents |
|-----|---------|-----------------|
| `wooden_tools` | Items that count as "Wood" tool tier | Wooden pickaxe, axe, shovel, hoe, sword |
| `stone_tools` | Items that count as "Stone" tool tier | Stone pickaxe, axe, shovel, hoe, sword |
| `copper_tools` | Items that count as "Copper" tool tier | (empty by default — for modded copper tools) |
| `iron_tools` | Items that count as "Iron" tool tier | Iron pickaxe, axe, shovel, hoe, sword |
| `diamond_tools` | Items that count as "Diamond" tool tier | Diamond & netherite pickaxe, axe, shovel, hoe, sword |
| `leather_armor` | Items that count as "Leather" armor tier | Leather helmet, chestplate, leggings, boots |
| `chainmail_armor` | Items that count as "Chainmail" armor tier | Chainmail helmet, chestplate, leggings, boots |
| `bows` | Items that count as bows for ranged combat | Bow |
| `crossbows` | Items that count as crossbows for ranged combat | Crossbow |
| `fishing_rods` | Items that count as fishing rods | Fishing rod |
| `melee_weapons` | Items that count as melee weapons for combat XP | All swords (wooden through netherite) |

### Adding Modded Items via Datapack

Create a datapack with tag files that **append** to KnowledgeBound's tags. For example, to make a modded copper pickaxe count as a "Copper" tool:

```
data/knowledgebound/tags/item/copper_tools.json
```

```json
{
  "replace": false,
  "values": [
    "mymod:copper_pickaxe",
    "mymod:copper_axe"
  ]
}
```

### Adding Items via Config

Alternatively, use the `extra*` config lists to add modded blocks and items without a datapack:

```json
{
  "extraForestryBlocks": ["mytreesmod:ancient_log"],
  "extraMiningBlocks": ["myoremod:mythril_ore"],
  "extraToolItems": ["mymod:copper_pickaxe"],
  "extraWeaponItems": ["mymod:crystal_sword"],
  "extraArmorItems": ["mymod:mythril_chestplate"],
  "extraCarpentryItems": ["mymod:fancy_door"],
  "extraMasonryItems": ["mymod:marble_stairs"]
}
```

Use `itemCraftingTierOverrides` to assign specific crafting tiers:

```json
{
  "itemCraftingTierOverrides": {
    "mymod:copper_pickaxe": 2,
    "mymod:mythril_chestplate": 3
  }
}
```

Use `armorTiers.extraItemTiers` to assign combat tier requirements to modded armor:

```json
{
  "armorTiers": {
    "extraItemTiers": {
      "mymod:mythril_chestplate": 3,
      "mymod:aetherium_helmet": 5
    }
  }
}
```

Use `blockedCraftingItems` to prevent specific items from being crafted:

```json
{
  "blockedCraftingItems": ["mymod:overpowered_sword"]
}
```

---

## 🏗️ Technical Architecture

KnowledgeBound uses the following technical approach:

- **Mixins** for hooking into vanilla mechanics (12 mixins total):
  - `AnvilScreenHandlerMixin` — free anvil operations
  - `BeehiveMixin` — beekeeping mechanics
  - `CraftingResultSlotMixin` — crafting quality on normal clicks
  - `CraftingScreenHandlerMixin` — disables shift-click crafting
  - `DamageScalingMixin` — combat damage scaling
  - `EnchantmentScreenHandlerMixin` — free enchanting
  - `ExperienceOrbEntityMixin` — discard XP orbs
  - `FishingRodHookedCriterionMixin` — fishing XP
  - `PlayerEntityMixin` — blocks negative XP level changes
  - `ScreenHandlerMixin` — handles pre-click cursor saving, stonecutter output, and shift-click crafting
  - `ServerPlayerEntityMixin` — NBT persistence (read/write)
  - `StonecutterScreenHandlerMixin` — stonecutter tier gate and fail mechanics

- **NBT persistence** — knowledge data is stored directly in the player's NBT data under the `knowledgebound_knowledge` key
- **Client sync** — knowledge state is sent to the client via a custom payload (`KnowledgeSyncPayload`) for optional client-side HUD rendering
- **Scoreboard HUD** — uses the vanilla scoreboard system server-side, no client mod needed
- **Chest GUI** — `KnowledgeGuiHandler` creates a vanilla double-chest with visual knowledge progress (no client mod needed)
- **Custom Items** — `CustomItemRegistry` centralizes custom item creation (Royal Honey, etc.) for both gameplay drops and `/kb give`

---

## 📄 License

MIT License — see [fabric.mod.json](src/main/resources/fabric.mod.json) for details.

**Made by Maxello** ⚒️
