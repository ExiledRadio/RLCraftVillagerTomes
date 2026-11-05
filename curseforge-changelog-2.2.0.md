**The quest log becomes the Villager Ledger, a Ledger Lectern lets you pay villagers without walking over, a Villager Ward protects your best villagers, teaching no longer gambles your books by default, and the bounty list grows to 248 items.**

> **Install this on clients too.** 2.2.0 adds real items and a block, so the mod is no longer server-only.

---

## Villager Ledger

Sneak-click a villager with a **book and quill** to make one, and with the ledger to write that villager down. Right-click to read it.

*   One card per villager, showing its name, which slot it's selling, and where it lives.
*   One row per item it wants: icon, name, how many it has had, and **how many you're carrying**.
*   **Rows you can finish right now are highlighted green.** Paid-off rows fade out and say "done".
*   Opening the ledger refreshes every villager that's loaded, so you see what it wants now, not what it wanted when you last visited.
*   No size limit (up to 1,000 villagers), a page at a time. The red **x** on a card removes that villager.
*   **Star a villager** to keep it at the front. Favorites stack up in the order you star them.
*   **Click the red ribbon** to bookmark a page. The ledger opens there every time. The bookmark follows the villager, so it still works after the list changes or on a different screen size.
*   **Warded villagers show their ward icon.** Click it to switch modes while the villager is loaded.
*   A blue **i** on the cover explains the lectern's quick deposit and the other features.
*   Leather cover and parchment page, in the same style as the bounty scroll.
*   **Hover a villager's coordinates** to see how far away it is and which way to walk. With Antique Atlas installed, **click them to mark the villager on your atlas**, labeled with its name.
*   **Copy it:** craft a ledger with a book to get two. Copies share one list, like copied Antique Atlases share a map, so a villager logged or paid off in one shows up in every copy. Keep one on the lectern and one in your pocket, or hand copies to friends.

Writing a villager down no longer asks you to name it in chat. It's listed by its name tag if it has one, and by its career otherwise. To name one, click the name tag next to its name in the ledger (or double-click the name), type, and press Enter. That names the villager itself, the same as a name tag would, so the name shows over its head too. Old written-book quest logs turn into ledgers, entries and all, the first time you use one.

## Bounty scroll

Sneak-clicking a villager with an empty hand no longer prints its demands in chat. A scroll unrolls on screen instead, with a paper-rustle sound: the villager's name, the slot it's selling, each item it wants with how many it has had and how many you're carrying, green rows for anything you can finish now, its ward, and its chance figures when chance is on. Click anywhere or press Escape to roll it back up.

**Nothing goes to chat any more.** Handing a villager items unrolls its scroll with "Delivered 12" or "Slot unlocked!" across the top and the updated list underneath. Everything else - teaching, the confirm prompt, logging a villager, warding, refusals - shows as one line on the action bar above your hotbar, or inside the ledger or scroll if one is open. Only `/villagertomes` command replies still use chat.

## Ledger Lectern

Three spruce slabs, a ledger, and a spruce slab, in the vanilla lectern shape. **The ledger is not used up**: it stays in the crafting grid with every entry intact. Put your ledger on the lectern, and right-clicking it opens the ledger with a **Deliver** button on every villager. One click hands that villager everything it still needs from your inventory.

*   Only **loaded** villagers can take a delivery: your village, not one across the world.
*   Worn armor, **enchanted items and renamed items are never taken.** A villager asking for a diamond sword won't get your enchanted one. Hand those over in person.
*   Sneak-click with an empty hand to take the ledger back. This works with Carry On installed too.
*   The book flips open and thuds down with 1.14's book sounds.

## Villager Ward

Emeralds in the corners, ender pearls on the edges, a Totem of Undying in the middle. Sneak-click a villager to use it up and ward that villager for good.

A warded villager shows a ward icon beside its trade window, on both the vanilla trade screen and VillagerBackport's. Click it to switch what can hurt the villager:

*   **Fully immune.** Nothing can hurt it.
*   **Players only** (the default). Safe from every mob, fall, fire and drowning.
*   **Players and zombies.** For curing. A zombie can turn it, and when you cure it the ward comes back with it.

In every mode a warded villager can't be turned into a witch by lightning, and the void and `/kill` still work.

**Curing keeps everything.** A cured villager now comes back with its ward, its taught books, its unlocked slots and its bounty. Before, curing wiped all of it.

## Chance is off by default

The slot bounty is payment enough for a book. Teaching now just works: hand the librarian the book, confirm, and it learns it.

The chance system is still there for anyone who wants the gamble. Set `ENABLE_CHANCE = true`. With it on, catalysts now cover more than the glowing materials:

| Catalyst                              | Adds  |
| ------------------------------------- | ----- |
| Iron Ingot, Gold Ingot, Silver Ingot  | +5%   |
| Emerald, Glowing Powder               | +10%  |
| Diamond                               | +15%  |
| Glowing Ingot                         | +40%  |
| Glowing Gem                           | +120% (fills any librarian to its highest) |

The highest a librarian can reach hasn't changed: 80% on one slot, 100% on five.

## Taught trades lock like other trades

Taught trades now lock after `MAX_TRADE_USES` (16) and come back when the villager restocks. Set `NEVER_LOCK_TAUGHT_TRADES = true` to keep them unlimited.

## Bounties

*   **96 new bounty items (248 in all)** from vanilla, Ice and Fire, Lycanites, Defiled Lands, Rustic, Parasites, Waystones, Bountiful Baubles, Artifacts and more. Each one was checked against the RLCraft mod jars, and every crafted one has a working recipe there.
*   **Dragon scales and sea serpent scales accept any color.** Every color shares the name "Dragon Scales", so asking for one color couldn't be read in chat. A `*` in a bounty item name now matches a whole family of items.
*   **Fewer tools asked for.** Stone tools 2-5 → 1-2, gold 2-3 → 1-2, iron 1-3 → 1-2, and a single diamond or silver tool, saddle, flint and steel, or emerald ring or amulet.
*   **Worn tools count**, whatever their durability.
*   **Skill-locked items can be handed over.** Reskillable no longer blocks giving a villager the diamond shovel it asked for just because you lack Gathering 16. You still can't *use* it.

## Fixes

*   With `ENABLE_CHANCE=false`, nothing mentions chance any more. The teach prompt used to quote a percentage and warn the book could be destroyed.
*   Sneak-clicking a villager with a book and quill no longer pops open the vanilla writing screen as well.

## Upgrading

Forge keeps the values already in your config, so an existing install won't get the new defaults by itself. To pick them up, delete `config/rlcraftvillagertomes.cfg` and restart, or edit these:

*   `ENABLE_CHANCE` to `false`, to stop gambling books
*   `CATALYST_TIERS` and `CATALYST_ITEMS`, for the new catalysts
*   `BOUNTY_ITEMS`, for the new items, grouped scales and smaller tool counts
*   `QUEST_LOG_CAPACITY` to 0 (no limit)
*   `NEVER_LOCK_TAUGHT_TRADES` to `false`, so taught trades lock like other trades
