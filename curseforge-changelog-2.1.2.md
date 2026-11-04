**Villagers ask for fewer tools, and worn ones count.**

---

## Smaller tool bounties

Asking for five stone pickaxes was busywork, not a challenge. Requests for tools and other single items have come down:

| Item                                    | Was | Now |
| --------------------------------------- | --- | --- |
| Stone pickaxe / axe / shovel / sword    | 2-5 | 1-2 |
| Golden pickaxe / axe / shovel / sword   | 2-3 | 1-2 |
| Iron pickaxe / axe / shovel / sword     | 1-3 | 1-2 |
| Diamond pickaxe / axe / shovel / sword  | 1-2 | 1   |
| Silver pickaxe / axe / shovel / sword   | 1-2 | 1   |
| Flint and steel, saddle                 | 1-2 | 1   |
| Emerald ring / amulet                   | 1-2 | 1   |

A request a villager has already rolled stays as it is, because requests never re-roll. The new counts apply to the next slot it asks for.

**Existing installs:** Forge doesn't overwrite values already in your config. To get the new counts, delete the `BOUNTY_ITEMS` block from `config/rlcraftvillagertomes.cfg` (or the whole file) and restart.

## Any durability is accepted

A chipped pickaxe now always counts towards a pickaxe bounty. Worn tools already worked with the default list, but a config line that pinned a metadata value (`minecraft:iron_axe:0`) would quietly demand a brand-new one. For tools and armour, durability is now ignored entirely.
