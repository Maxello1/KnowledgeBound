# KnowledgeBound

**KnowledgeBound** is a server-side RPG skill progression and balancing mod for Minecraft (Fabric 1.21.1). It replaces standard vanilla progression with a time-and-action-based RPG knowledge system. 

Players start knowing nothing—they will fail to gather resources, craft poor-quality tools, and deal very little damage in combat. As they perform actions, they level up their "Knowledge" in various categories, unlocking full vanilla capabilities and mastering their skills.

Because KnowledgeBound is **server-side**, clients do not need to install the mod to play on the server (though visual client-side glitches like block desyncs on failure are handled smoothly by the server).

---

## 🌟 Core Concepts & Progression

Unlike traditional RPG mods that give XP per block broken or mob killed, KnowledgeBound grants XP based on **active time spent performing the activity**. 
- You gain "minutes" in a skill for actively using it.
- There is an internal cooldown to prevent players from spamming actions to farm XP artificially.
- You must perform the right action with the right tool (e.g., breaking stone with a pickaxe of an appropriate tier).

### The 10 Knowledge Categories
Progression is split into 10 distinct knowledge categories. Each category has **6 Tiers (Tier 0 to Tier 5)**.

- **Gathering:** Forestry, Mining, Digging, Farming, Fishing
- **Crafting:** Toolsmithing, Weaponsmithing, Armouring
- **Combat:** Melee Combat, Ranged Combat

### Default Time Requirements
By default, the time required to reach the next tier scales heavily:
- **Tier 0 to Tier 1:** 60 minutes (1 hour) of active use
- **Tier 1 to Tier 2:** 120 minutes (2 hours)
- **Tier 2 to Tier 3:** 240 minutes (4 hours)
- **Tier 3 to Tier 4:** 480 minutes (8 hours)
- **Tier 4 to Tier 5 (Mastered):** 960 minutes (16 hours)

*(Admins can change these base values globally, or apply a universal multiplier in the config).*

---

## 🪓 Gathering Mechanics

At low knowledge tiers, players will struggle to gather basic resources.

### Gathering Failure
When breaking a block associated with a gathering skill (e.g., Logs for Forestry, Stone/Ores for Mining, Dirt/Sand for Digging, Mature Crops for Farming), there is a chance the block will break but **drop nothing**.

**Default Fail Chances:**
| Tier | Forestry, Mining, Digging | Farming |
| :--- | :--- | :--- |
| **Tier 0** | 40% fail | 30% fail |
| **Tier 1** | 25% fail | 20% fail |
| **Tier 2** | 10% fail | 10% fail |
| **Tier 3** | 5% fail | 5% fail |
| **Tier 4 & 5** | 2% fail | 2% fail |

*(Note: For Farming, only fully mature crops yield XP and are subjected to the fail chance. Breaking seeds or un-grown crops does nothing).*

---

## 🛠️ Crafting Mechanics (Tier-Difference System)

Crafting gear is no longer guaranteed. Success depends strictly on the difference between the **Player's Knowledge Tier** and the **Item's Required Tier**.

`Difference = (Player Tier) - (Item Tier)`

### Crafting Results:
- **Fail:** The item is destroyed during crafting. Ingredients are lost forever.
- **Poor Quality:** The item is crafted, but it only has **10% of its maximum durability**.
- **Normal:** The item is crafted perfectly.

**Default Tier-Difference Crafting Chances:**
| Tier Difference | Fail Chance | Poor Chance | Normal Chance | Example |
| :--- | :--- | :--- | :--- | :--- |
| **≤ -3** (Impossible) | **100%** | 0% | 0% | Tier 0 crafting Netherite (Tier 4) |
| **-2** (Very Risky) | **85%** | 12% | 3% | Tier 1 crafting Diamond (Tier 3) |
| **-1** (Challenging) | **45%** | 35% | 20% | Tier 2 crafting Diamond (Tier 3) |
| **0** (At your level) | **10%** | 15% | **75%** | Tier 3 crafting Diamond (Tier 3) |
| **+1** (Below skill) | **0%** | 8% | **92%** | Tier 4 crafting Diamond (Tier 3) |
| **≥ +2** (Trivial) | **0%** | 0% | **100%** | Tier 5 crafting Diamond (Tier 3) |

### Default Vanilla Item Tiers
- **Tier 0:** Wooden Tools, Wooden Sword, Leather Armor
- **Tier 1:** Stone Tools, Stone/Gold Swords, Bow, Chainmail/Gold Armor
- **Tier 2:** Iron Tools, Iron Sword, Crossbow, Iron Armor, Turtle Helmet
- **Tier 3:** Diamond Tools, Diamond Sword, Diamond Armor
- **Tier 4:** Netherite Tools, Netherite Sword, Netherite Armor

*(Note: Modded items or custom overrides can be assigned to any tier via the config).*

---

## ⚔️ Combat & Armor Mechanics

### Combat Damage Scaling
You do not deal full damage until you master combat. This applies to both **Melee Combat** and **Ranged Combat**.

**Default Damage Multipliers:**
| Combat Tier | Damage Output |
| :--- | :--- |
| **Tier 0** | 40% (0.4x) |
| **Tier 1** | 55% (0.55x) |
| **Tier 2** | 70% (0.70x) |
| **Tier 3** | 85% (0.85x) |
| **Tier 4 & 5** | 100% (1.0x) |

### Armor Equip Restrictions
You cannot equip high-tier armor if you don't know how to move in combat. Armor equipping is locked behind your **Combat Knowledge** (the *highest* of your Melee or Ranged tier). 

**Default Armor Requirements:**
- **Tier 0:** Leather
- **Tier 1:** Chainmail
- **Tier 2:** Iron
- **Tier 3:** Gold
- **Tier 4:** Diamond
- **Tier 5:** Netherite

If a player attempts to equip armor they don't have the tier for, it will automatically drop off them.

---

## 🎣 Fishing Mechanics
Fishing has a simplified progression with only 3 Tiers.
- **Tier 1:** 60 minutes
- **Tier 2:** 120 minutes
- **Tier 3:** 240 minutes

*(Higher tiers could theoretically unlock better loot tables via datapacks, though currently it operates as a standard tracker).*

---

## ⚙️ Configuration File (`knowledgebound.json`)

The mod generates a `knowledgebound.json` file in your server's `config/` directory. It is highly detailed and commented.

### Global Tuning
- `baseMinutesPerTier`: Array of minutes `[60, 120, 240, 480, 960]`. Change these to alter the base grind.
- `minutesMultiplier`: A global scaler. Set to `0.5` for a 2x faster server, or `2.0` to double the grind.

### Customizing Modded Items & Overrides
You can seamlessly integrate modded blocks and items into KnowledgeBound using the config arrays:
- **Gathering Extensions:** Add block IDs (e.g., `"biomesoplenty:fir_log"`) to `extraForestryBlocks`, `extraMiningBlocks`, etc., to allow them to grant XP and be subject to fail chances.
- **Crafting Extensions:** Add item IDs to `extraToolItems`, `extraWeaponItems`, or `extraArmorItems`.
- **Item Tier Overrides:** Use `itemCraftingTierOverrides` to specify exactly what crafting tier an item belongs to. (e.g., `"mythicmetals:adamantium_sword": 5`).
- **Armor Equip Overrides:** Use `extraItemTiers` under the `armorTiers` section to define what combat tier is required to wear a specific modded helmet/chestplate.

*Tip: You can hot-reload config changes in-game at any time using `/kb reload`.*

---

## 📜 Admin Commands

Admins with OP permissions (Level 2+) can manage player knowledge using the `/kb` command. Knowledge category names fully support auto-completion.

- `/kb help` - Displays a list of all commands.
- `/kb check <player> [knowledge]` - View a player's current tier and progress (minutes) for all categories or a specific one.
- `/kb set <player> <knowledge> <tier>` - Forcibly set a player's knowledge to a specific tier (0-5).
- `/kb reset <player> <knowledge>` - Completely reset a player's knowledge in a category back to 0.
- `/kb reload` - Hot-reload the `knowledgebound.json` config file without restarting the server.

---

## 🏆 Datapack Compatibility (Custom Achievements)

If you are a server admin creating custom datapacks (like custom Advancements), you can easily detect and trigger events based on a player's knowledge tier!

KnowledgeBound stores player data directly in their root NBT under the `knowledgebound_knowledge` key as an SNBT List. You can check this using standard Minecraft datapack `minecraft:tick` triggers and the `entity_properties` condition.

**Example: Trigger an advancement when a player hits Tier 5 Melee Combat:**
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

---

## 🛑 Current Limitations
Because KnowledgeBound operates entirely on the server-side to ensure vanilla clients can connect without downloads:
- **No Custom UI:** We cannot add custom skill-tree menus, graphical overlays, or new keybinds. All feedback is provided via standard vanilla mechanics (Chat messages, Action bars, sounds).
- **Hardcoded Categories:** The 10 existing knowledge categories are hardcoded into the Java logic. You cannot create an entirely new category (like "Sorcery") purely via the config file.
