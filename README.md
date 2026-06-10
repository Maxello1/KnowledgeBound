# KnowledgeBound

**KnowledgeBound** is a server-side RPG skill progression and balancing mod for Minecraft (Fabric 1.21.1). It replaces the standard Minecraft progression with a time-and-action-based RPG knowledge system. 

Players start knowing nothing—they will fail to gather resources, craft poor-quality tools, and deal very little damage in combat. As they perform actions, they level up their "Knowledge" in various categories, unlocking full vanilla capabilities and mastering their skills.

Because KnowledgeBound is **server-side**, clients do not need to install the mod to play on the server (though visual client-side glitches like block desyncs are handled smoothly by the server).

---

## 🌟 Features

### 📚 Knowledge Categories
Players progress in 10 distinct knowledge categories:
- **Gathering:** Forestry, Mining, Digging, Farming, Fishing
- **Crafting:** Toolsmithing, Weaponsmithing, Armouring
- **Combat:** Melee Combat, Ranged Combat

Each category has **6 Tiers (Tier 0 to Tier 5)**. Players start at Tier 0 and must perform actions relevant to that category to gain XP and level up.

### ⏳ Time-Based Progression
Unlike traditional RPG mods that give XP per block broken, KnowledgeBound grants XP based on **time spent performing the activity**. 
- You gain "minutes" in a skill for actively using it.
- There is an internal cooldown to prevent players from spam-clicking to farm XP.
- Progression scales up: Tier 1 takes 1 hour (60 minutes) of active use, Tier 2 takes 2 hours, Tier 3 takes 4 hours, up to Tier 5 which requires massive dedication. (All values are configurable).

### 🪓 Gathering Mechanics
At low knowledge tiers, players will struggle to gather resources.
- **Fail Chance:** When breaking a log, stone, dirt, or mature crop, there is a chance the block will break but **drop nothing**.
- As you level up Forestry, Mining, Digging, or Farming, the fail chance decreases drastically. At higher tiers, failure is rare.
- **Note on Farming:** Only mature crops grant XP and are subject to the fail chance.

### 🛠️ Crafting Mechanics (Tier-Difference System)
Crafting gear is no longer guaranteed. Success depends on the difference between the **Player's Knowledge Tier** and the **Item's Required Tier**.
- If you are Tier 0 Toolsmithing and try to craft a Diamond Pickaxe (Item Tier 3), the difference is `-3`. You have a 100% chance to fail, losing the ingredients entirely.
- **Crafting Results:**
  - **Fail:** The item is destroyed during crafting. Ingredients are lost.
  - **Poor Quality:** The item is crafted, but it only has 10% of its maximum durability.
  - **Normal:** The item is crafted perfectly.
- As your tier exceeds the item's tier, you guarantee perfect crafts.

### ⚔️ Combat & Armor Mechanics
- **Damage Scaling:** At Tier 0, players only deal 40% of their normal melee and ranged damage. As they level up Melee and Ranged Combat, their damage scales up to 100%.
- **Armor Restrictions:** You cannot equip high-tier armor if you don't know how to move in it. Armor equipping is locked behind your **Combat Knowledge** (the highest of your Melee or Ranged tier). For example, Diamond Armor requires Combat Tier 4. 

---

## ⚙️ Configuration & Customization

KnowledgeBound is highly customizable for Server Admins. Everything is handled in the `config/knowledgebound.json` file.

### What Admins CAN Change:
- **Global XP Speed:** Adjust `baseMinutesPerTier` and `minutesMultiplier` to make the mod faster or grindier.
- **Gathering Fail Chances:** Tweak exactly how often players fail to get drops at each tier for Forestry, Mining, Digging, and Farming.
- **Crafting Chances:** Adjust the exact percentages for Fail, Poor, and Normal crafts based on the tier-difference gap.
- **Combat Damage Scaling:** Change the damage multipliers for each combat tier.
- **Armor Requirements:** Change which combat tier is required to wear Leather, Iron, Diamond, etc.
- **Modded Item Support (Overrides):**
  - Add extra blocks to count towards gathering XP (`extraForestryBlocks`, `extraMiningBlocks`, etc.).
  - Add extra items to use crafting rules (`extraToolItems`, `extraArmorItems`, etc.).
  - Override the required crafting tier of any specific item using `itemCraftingTierOverrides` (e.g., make a modded sword require Tier 5).
  - Override the required equip tier of any specific armor piece using `extraItemTiers`.

*Note: Changes to the config can be applied instantly in-game using `/kb reload`.*

### What Admins CANNOT Change:
- **Adding entirely new knowledge categories:** The 10 categories are hardcoded.
- **Adding custom UI elements to the client:** Because this is a server-side mod, it relies on vanilla chat messages and action bars. Custom menus or skill-tree GUIs are not possible without requiring a client-side mod download.

---

## 📜 Admin Commands

Admins with OP permissions (Level 2+) can manage player knowledge using the `/kb` command:

- `/kb help` - Displays a list of all commands.
- `/kb check <player> [knowledge]` - View a player's current tier and progress (minutes) for all categories or a specific one.
- `/kb set <player> <knowledge> <tier>` - Forcibly set a player's knowledge to a specific tier (0-5).
- `/kb reset <player> <knowledge>` - Completely reset a player's knowledge in a category back to 0.
- `/kb reload` - Hot-reload the `knowledgebound.json` config file without restarting the server.

*Knowledge names support tab-completion in-game.*

---

## 🏆 Datapack Compatibility (Custom Achievements)

If you are a server admin creating custom datapacks (like custom Advancements), you can detect player knowledge!

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
