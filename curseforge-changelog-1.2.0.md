One new setting, off by default. Nothing changes unless you turn it on, and existing villagers are untouched.

## `UPGRADE_TAKES_NEW_SLOT`

Levelling a trade up normally replaces it: a villager that went from Unbreaking II to III sells only the III, and the upgrade cost no slots.

Turn this on and the old trade **stays**. That villager now sells Unbreaking II *and* Unbreaking III, and two of its five slots are gone.

It changes what a slot means — from "an enchantment this villager deals in" to "one trade on the board". With five slots you can have five enchantments at level I, or one at level V and nothing else. That trade-off is the point of the setting.

Worth knowing before you switch it on:

- It fills villagers fast. Getting one enchantment to V costs five slots on its own.
- A villager with no free slot **cannot be levelled up at all**. It says so rather than quietly dropping a trade to make room.
- `MAX_TOMES_PER_VILLAGER` counts trades, not distinct enchantments. With this setting off those are the same number, which is why the default is unchanged.

## Compatibility

**Villagers taught by 1.0.0 and 1.1.0 load unchanged.** The saved format already stored a list of enchantment and level pairs, so it did not need to change to hold the same enchantment twice — nothing is migrated and nothing is lost.

`/villagertomes teach` still replaces rather than stacking, whatever this setting says. An admin typing `teach unbreaking 3` wants that villager selling Unbreaking III, not a second trade next to whatever was there.

## Install

Delete `RLCraftVillagerTomes-1.1.0.jar` from your `mods` folder before adding this one. Two versions of the same mod will crash on startup with a duplicate mod id.

---

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams. Licensed MIT.
