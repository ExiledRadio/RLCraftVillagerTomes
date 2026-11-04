<!--
Structure: pitch, then the whole mod in four steps, then one section per step in the
order a player meets them, then reference material (config, commands) at the bottom.
One idea per section. Numbers live in tables, not sentences.

IMAGES USED:
  learned message      https://media.forgecdn.net/attachments/1852/310/villagertomeslearnedmessage-png.png
  bounty list, short   https://media.forgecdn.net/attachments/1874/998/bountylist2-png.png
  bounty list, long    https://media.forgecdn.net/attachments/1874/988/bountylist-png.png
  banked catalysts     https://media.forgecdn.net/attachments/1874/978/banked-png.png
  success              https://media.forgecdn.net/attachments/1875/9/success-png.png
  failed               https://media.forgecdn.net/attachments/1875/22/failed-png.png
  failure bonus        https://media.forgecdn.net/attachments/1875/23/successbonus-png.png
  upgrade message      https://media.forgecdn.net/attachments/1852/314/villagetomesupgrademessage-png.png
  quest log convert    https://media.forgecdn.net/attachments/description/1644374/description_3b41a2e8-954d-4010-a37a-88f066a5fceb.png
  villager naming      https://media.forgecdn.net/attachments/1875/29/villagernaming-png.png
  quest book           https://media.forgecdn.net/attachments/1875/27/questbook-png.png

UPLOADED BUT UNUSED, kept here so the URLs are not lost:
  config category      https://media.forgecdn.net/attachments/1852/311/villagetomesconfigcategory-png.png
  feedback category    https://media.forgecdn.net/attachments/1852/309/villagertomesfeedbackcategory-png.png
  learning config      https://media.forgecdn.net/attachments/1852/312/villagetomeslearningconfig-png.png
  pricing config       https://media.forgecdn.net/attachments/1852/313/villagetomespricingconfig-png.png
  upgrading category   https://media.forgecdn.net/attachments/1852/315/villagetomesupgradingcategory-png.png
-->

# RLCraft Villager Tomes

**Teach a librarian an enchanted book, and it sells that book to you forever.**

You found a Mending book. You used it once, on one tool, and that was the end of it.

Hand it to a librarian instead and it becomes a trade you can come back to. You have to earn the slot first, and the book might not take.

![A villager learning a book](https://media.forgecdn.net/attachments/1852/310/villagertomeslearnedmessage-png.png)

**Server-side only.** No dependencies, and players do not need the mod installed to join.

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** for bug reports, suggestions and release pings. It's where I'm most responsive.

***

## How it works

1.  **Unlock a slot.** The librarian asks for a list of items. Bring them.
2.  **Raise the odds.** Feed it glowing powder, ingots or gems.
3.  **Teach the book.** It rolls. On a success, the librarian sells that enchantment from then on.
4.  **Buy it back, as often as you like.** Ten emeralds per enchantment level, plus a plain book.

That's the whole mod. Everything below is the detail on each step.

### One control for everything

**Sneak + right-click the librarian.** What happens depends on what you're holding:

| Holding            | What it does                                       |
| ------------------ | -------------------------------------------------- |
| Nothing            | Shows its current odds and what it still wants     |
| An item it asked for | Delivers it toward the next slot                 |
| A glowing material | Banks it to raise the odds                         |
| An enchanted book  | Offers the book. It asks you to confirm, then rolls |
| A book and quill   | Turns it into a quest log (see below)              |

A normal right-click still opens the trade screen, exactly as in vanilla.

***

## Step 1: Unlock a slot

A librarian starts with no slots. Each slot holds one enchantment, and each one has to be paid for.

Sneak-click it empty-handed and it tells you what it wants:

![A short request](https://media.forgecdn.net/attachments/1874/998/bountylist2-png.png)

Later slots ask for more, and for rarer things:

![A long request](https://media.forgecdn.net/attachments/1874/988/bountylist-png.png)

Three rules:

*   **Five slots per librarian.**
*   **A request never re-rolls.** If a librarian wants something you can't get, find a different librarian.
*   **You can buy slots ahead.** A librarian asks for its next slot even while earlier ones are empty, so you can unlock all five before risking a single book.

***

## Step 2: Raise the odds

Teaching is a dice roll. The more slots a librarian has unlocked, the better its odds:

| Slots unlocked | Starting chance | Highest you can push it |
| -------------- | --------------- | ----------------------- |
| 1              | 30%             | 80%                     |
| 2              | 40%             | 85%                     |
| 3              | 50%             | 90%                     |
| 4              | 60%             | 95%                     |
| 5              | 70%             | **100%**                |

To get from the starting chance toward the highest, bank **catalysts**:

| Catalyst       | Adds |
| -------------- | ---- |
| Glowing Powder | +4%  |
| Glowing Ingot  | +20% |
| Glowing Gem    | +120% (fills any villager to its ceiling) |

![Banked catalysts](https://media.forgecdn.net/attachments/1874/978/banked-png.png)

A librarian that's already at its highest hands the catalyst back, so nothing is wasted.

> **The safe play:** unlock all five slots first, then bank one Glowing Gem. It takes any librarian straight to its ceiling, and at five slots that ceiling is 100% - a guaranteed success. It's the only sure thing in the mod.

***

## Step 3: Teach the book

Sneak-click with the enchanted book. The librarian tells you the odds and waits. Sneak-click again to commit.

**If it works,** the librarian sells that enchantment at that level from now on.

![A successful attempt](https://media.forgecdn.net/attachments/1875/9/success-png.png)

**If it fails, you lose the book and every catalyst you banked.**

![A failed attempt](https://media.forgecdn.net/attachments/1875/22/failed-png.png)

### Bad luck is paid back

Each failure makes the next attempt easier: **+5% for every level of the book you lost.** Lose a Sharpness V and you're owed 25%. Lose a Sharpness I and you're owed 5%.

*   The bonus is for **that enchantment on that librarian** only.
*   It goes **on top of** the highest chance in the table above, so enough failures reach 100% on any librarian.
*   It resets once that enchantment succeeds.

![The failure bonus](https://media.forgecdn.net/attachments/1875/23/successbonus-png.png)

***

## Step 4: Buy it back

A taught trade costs **10 emeralds per level, plus one plain book**. Mending is 10, Unbreaking III is 30, Sharpness V is 50.

Taught trades never lock or run out of stock.

***

## Levelling a trade up

Bring a book **higher** than what the librarian sells, and the trade becomes that level. It's a normal teaching attempt, with the same odds and the same risk.

To go from Unbreaking II to III: buy two Unbreaking II from the librarian, combine them at an anvil, and bring the III back.

![Levelling a trade up](https://media.forgecdn.net/attachments/1852/314/villagetomesupgrademessage-png.png)

**What gets refused.** The book is handed straight back in both cases:

*   A book at or below the level it already sells.
*   An enchantment the librarian already sells naturally.

**Books with several enchantments** are handled one enchantment at a time. Only the top one is rolled for, and only that one is removed from the book. Win or lose, you keep the rest.

***

## The quest log

Keeping track of what five librarians each want is a chore. The quest log does it for you.

**Make one:** sneak-click a librarian while holding a **book and quill**.

![Making a quest log](https://media.forgecdn.net/attachments/description/1644374/description_3b41a2e8-954d-4010-a37a-88f066a5fceb.png)

**Add a librarian:** sneak-click it with the log. You give it a name, and the log records where it lives and what it still needs. The name also shows on the villager as a name tag.

![Naming a villager](https://media.forgecdn.net/attachments/1875/29/villagernaming-png.png)

**It keeps itself current.** Entries update as you deliver items and cross themselves off when the slot is paid for. One log tracks ten librarians.

![The quest log](https://media.forgecdn.net/attachments/1875/27/questbook-png.png)

**Remove a librarian:** sneak-click with the log while looking at nothing, or run `/villagertomes log`. Each entry gets a **\[Remove\]** button in chat.

***

## Good to know

*   **Curing a zombie villager wipes its tomes.** The cured villager is a brand new one. Protect your good librarians.
*   **Librarians only, by default.** Cartographers count too, since they share the profession. `ALLOWED_PROFESSIONS` opens it to others.
*   **Modded enchantments work.** Anything any mod adds can be taught, including all of SoManyEnchantments. Treasure enchantments and curses are allowed by default, and both have a switch.
*   **You can turn the hard parts off.** `ENABLE_CHANCE = false` makes every book succeed. `LOCK_SLOTS = false` gives every librarian all its slots for free.

***

## Config

`config/rlcraftvillagertomes.cfg`, or the in-game config screen. Changes apply immediately, with no restart.

| Category      | What's in it                                                          |
| ------------- | --------------------------------------------------------------------- |
| **learning**  | Who can be taught, slots per villager, enchantment whitelist and blacklist |
| **slots**     | Whether slots need unlocking, how long requests get, quest log size   |
| **chance**    | Every number from Steps 2 and 3, and what a failure destroys          |
| **upgrading** | How trades level up                                                   |
| **pricing**   | Emerald cost, the extra book, trade stock                             |
| **bounties**  | The items librarians can ask for, and how many                        |
| **catalysts** | Which items raise the odds, and by how much                           |
| **feedback**  | Messages, sounds, particles                                           |

**261 bounty items** ship configured, from vanilla, Ice and Fire, Lycanites, Defiled Lands, Rustic, Parasites, Trinkets and Baubles and others. Items from mods you don't have are skipped, so the list is safe on any 1.12.2 pack.

Every setting is explained in the file itself.

***

## Commands

`/villagertomes` (or `/tomes`, `/vt`) acts on the villager you're looking at.

**Everyone**

| Command | What it does                                              |
| ------- | --------------------------------------------------------- |
| `list`  | What it sells and what each trade costs                   |
| `tiers` | The bounty and catalyst lists, and what your held item counts as |
| `log`   | Lists your quest log entries with \[Remove\] buttons      |

**Ops only**

| Command                      | What it does                              |
| ---------------------------- | ----------------------------------------- |
| `unlock [n]`                 | Opens slots without paying                |
| `bank <percent>`             | Sets the banked chance                    |
| `pity <ench> <percent>`      | Sets the failure bonus for one enchantment |
| `reroll`                     | Replaces the current request with a new one |
| `teach` / `forget` / `clear` | Edits its tomes directly                  |

***

## Requirements & install

*   Minecraft 1.12.2, Forge 14.23.5.2847 or newer
*   No dependencies
*   Server-side only. Clients don't need it.

Drop the jar in your `mods` folder. **Delete any older version first**, or the game crashes on startup with a duplicate mod id.

***

## Pairs well with

**[RLCraft Enchantment Recipes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-enchantment-recipes)**, my other mod. Craft a max-level book from RLCraft materials there, then teach it to a librarian here so you never have to craft it twice.

***

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams. Licensed MIT.

Source: [https://github.com/ExiledRadio/RLCraftVillagerTomes](https://github.com/ExiledRadio/RLCraftVillagerTomes)
