<!--
Structure: pitch, the three steps, one short section per step, the three tools, the
optional chance system, then reference material. Say each thing once. Numbers go in tables.

IMAGES (uploaded, final set - work with these only):
  hero             https://media.forgecdn.net/attachments/2150/739/01-hero-lectern-png.png
  bounty scroll    https://media.forgecdn.net/attachments/2150/740/02-bounty-scroll-png.png
  ledger screen    https://media.forgecdn.net/attachments/2150/741/03-ledger-screen-png.png
  ledger copy      https://media.forgecdn.net/attachments/2150/742/04-ledger-copy-recipe-png.png
  ward recipe      https://media.forgecdn.net/attachments/2150/743/05-ward-recipe-png.png
  ward modes       https://media.forgecdn.net/attachments/2150/744/06-ward-modes-png.png
  lectern closeup  https://media.forgecdn.net/attachments/2150/745/07-lectern-closeup-png.png
  lectern recipe   https://media.forgecdn.net/attachments/2150/767/08-lectern-recipe-png.png
Sources in media/, uncropped originals in media/originals/.

RETIRED (chat-era, 2.1.0 and earlier):
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
  config screens       https://media.forgecdn.net/attachments/1852/311/villagetomesconfigcategory-png.png (and 309, 312, 313, 315)
-->

# RLCraft Villager Tomes

**Teach a librarian an enchanted book, and it sells that book to you forever.**

Pay a librarian's bounty to unlock a slot, teach it the book, and buy copies whenever you need one. Works with any enchantment, modded ones included.

![RLCraft Villager Tomes](https://media.forgecdn.net/attachments/2150/739/01-hero-lectern-png.png)

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** for bug reports, suggestions and release pings.

***

## How it works

1.  **Unlock a slot** by delivering the librarian's bounty.
2.  **Teach a book** to fill the slot.
3.  **Buy it** for 10 emeralds per level plus a book.

Everything runs through **sneak + right-click** on the librarian:

| Holding                | What it does                              |
| ---------------------- | ----------------------------------------- |
| Nothing                | Shows its bounty scroll                   |
| An item it asked for   | Delivers it toward the next slot          |
| An enchanted book      | Teaches it (sneak-click again to confirm) |
| A book and quill       | Makes a Villager Ledger                   |
| A Villager Ledger      | Writes the librarian down                 |
| A Villager Ward        | Wards the librarian                       |

***

## Slots and bounties

Five slots per librarian, one enchantment each. Later slots ask for more and rarer items.

![The bounty scroll](https://media.forgecdn.net/attachments/2150/740/02-bounty-scroll-png.png)

*   Requests are fixed per librarian and never re-roll.
*   Slots can be bought ahead of filling them.
*   Worn tools count, as do items Reskillable won't let you use yet.
*   248 bounty items across vanilla, Ice and Fire, Lycanites, Defiled Lands, Rustic, Parasites and more. Items from missing mods are skipped.

## Teaching and upgrading

A multi-enchant book teaches its top enchantment and keeps the rest. Teaching a higher level of a taught enchantment upgrades the trade. Lower or equal levels, and enchantments the librarian already sells naturally, are refused.

***

## Villager Ledger

Tracks every librarian's bounty, your carried counts against it, and where each one lives.

![The ledger](https://media.forgecdn.net/attachments/2150/741/03-ledger-screen-png.png)

*   Star villagers to pin them to the front.
*   The ribbon bookmarks a page.
*   Ward icons switch that villager's ward mode.
*   The name tag (or a double-click) renames the villager.
*   Coordinates show direction and distance on hover. With [Antique Atlas](https://www.curseforge.com/minecraft/mc-mods/antique-atlas), clicking them adds an atlas marker.

Craft a ledger with a book to copy it. Copies share one list, like Antique Atlas copies.

![Copying a ledger](https://media.forgecdn.net/attachments/2150/742/04-ledger-copy-recipe-png.png)

***

## Ledger Lectern

![Lectern recipe](https://media.forgecdn.net/attachments/2150/767/08-lectern-recipe-png.png)

![The Ledger Lectern](https://media.forgecdn.net/attachments/2150/745/07-lectern-closeup-png.png)

The ledger isn't consumed. A ledger on a lectern gets a **Deliver** button per villager that pays its bounty from your inventory, for any loaded villager. Enchanted items, renamed items and worn armor are never taken.

***

## Villager Ward

![Ward recipe](https://media.forgecdn.net/attachments/2150/743/05-ward-recipe-png.png)

Permanent protection for one villager. The icon beside its trade screen switches modes:

![Ward modes](https://media.forgecdn.net/attachments/2150/744/06-ward-modes-png.png)

| Mode                       | What can hurt it     |
| -------------------------- | -------------------- |
| **Fully immune**           | Nothing              |
| **Players only** (default) | Players              |
| **Players and zombies**    | Players and zombies, for curing |

Warded villagers are also immune to lightning. The void and `/kill` still work. Curing keeps the ward, taught books, slots and bounty.

***

## Optional: the chance system

Off by default. With `ENABLE_CHANCE = true`, a failed teach destroys the book and banked catalysts. Odds scale with unlocked slots:

| Slots unlocked | Starting chance | Max chance |
| -------------- | --------------- | ---------- |
| 1              | 30%             | 80%        |
| 2              | 40%             | 85%        |
| 3              | 50%             | 90%        |
| 4              | 60%             | 95%        |
| 5              | 70%             | **100%**   |

Catalysts bank extra chance:

| Catalyst                             | Adds   |
| ------------------------------------ | ------ |
| Iron Ingot, Gold Ingot, Silver Ingot | +5%    |
| Emerald, Glowing Powder              | +10%   |
| Diamond                              | +15%   |
| Glowing Ingot                        | +40%   |
| Glowing Gem                          | +120%  |

Each failure adds +5% per book level for that enchantment, past the cap, until it succeeds.

***

## Good to know

*   Librarians and cartographers by default (`ALLOWED_PROFESSIONS`).
*   Modded enchantments work. Treasure enchantments and curses each have a toggle.
*   `LOCK_SLOTS = false` removes bounties.
*   `NEVER_LOCK_TAUGHT_TRADES = true` stops taught trades from locking.

## Config

`config/rlcraftvillagertomes.cfg` or the in-game config screen. Applies live.

| Category      | What's in it                                                  |
| ------------- | ------------------------------------------------------------- |
| **learning**  | Who can be taught, slots per villager, enchantment lists      |
| **slots**     | Slot unlocking, request length, ledger size                   |
| **chance**    | The optional chance system                                    |
| **upgrading** | How trades level up                                           |
| **pricing**   | Emerald cost, the extra book, trade stock                     |
| **bounties**  | What librarians ask for, and how many                         |
| **catalysts** | What raises the odds, and by how much                         |
| **feedback**  | Messages, sounds, particles                                   |

## Commands

`/villagertomes` (also `/tomes` or `/vt`) acts on the villager you're looking at.

| Command                      | Who      | What it does                                   |
| ---------------------------- | -------- | ---------------------------------------------- |
| `list`                       | Everyone | What it sells, and the price of each trade     |
| `tiers`                      | Everyone | The bounty and catalyst lists                  |
| `log`                        | Everyone | Your ledger in chat, with \[Remove\] buttons   |
| `unlock [n]`                 | Ops      | Opens slots for free                           |
| `reroll`                     | Ops      | Gives it a new request                         |
| `teach` / `forget` / `clear` | Ops      | Edits its tomes                                |
| `bank <percent>`             | Ops      | Sets banked chance                             |
| `pity <ench> <percent>`      | Ops      | Sets the failure bonus for one enchantment     |

***

## Requirements & install

*   Minecraft 1.12.2, Forge 14.23.5.2847+. No dependencies.
*   Required on server and clients.
*   Remove older versions first (duplicate mod id crash).

## Pairs well with

*   **[Villager Backport](https://www.curseforge.com/minecraft/mc-mods/villager-backport):** ward button on its trade screen; cures keep both mods' data.
*   **[RLCraft Enchantment Recipes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-enchantment-recipes):** craft a max-level book once, teach it, buy it forever.

***

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams. Licensed MIT.

Source: [https://github.com/ExiledRadio/RLCraftVillagerTomes](https://github.com/ExiledRadio/RLCraftVillagerTomes)
