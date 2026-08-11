Initial release.

Enchanted books you find become permanent villager trades. Works out of the box with no setup.

## Teaching

Hold an enchanted book, **sneak and right-click a villager**. The book is consumed and that enchantment becomes a trade you can come back to forever.

Sneaking is the trigger for a reason specific to 1.12: vanilla refuses to open a villager's trade screen while you are sneaking, so it is a click that would otherwise do nothing. That leaves the plain right-click free for trading, even with a book in hand.

Any villager, any profession, by default. Five different enchantments per villager.

## Upgrading

Anvil rules. Give a villager that already sells **Unbreaking II** another **Unbreaking II** and the trade becomes **Unbreaking III**. A higher-level book replaces the trade outright; a lower one is refused and handed back rather than wasted.

Upgrading never uses a slot, so a villager at its five-enchantment cap can still be improved.

## Price

**Ten emeralds per level, plus one book.** Mending I costs 10, Unbreaking III costs 30, Sharpness V costs 50. The book goes in the first slot and the emeralds in the second, the same order vanilla's own librarian trades use, so a taught trade reads the same as a natural one.

Every part of that is configurable, including separate multipliers for each rarity tier and an extra one for treasure enchantments if you want Mending to cost real money.

## Modded enchantments

Enchantments are read from the registry at runtime rather than being hard-coded, so anything your pack adds works with no patch. In RLCraft that means Spellproof, Adept, Freezing, Inner Berserk, Penetration and the rest of the SoManyEnchantments set are all teachable.

## Trades that never lock

1.12 has a genuine design flaw here: a villager only restocks as a side effect of being traded with, so once every trade is exhausted there is nothing left to use, nothing schedules a refresh, and that villager is bricked permanently.

`NEVER_LOCK_TAUGHT_TRADES` is **on by default** and keeps this mod's trades usable forever. `NEVER_LOCK_ANY_TRADE` is off by default and extends that to every trade on any villager you click, which fixes the flaw outright at the cost of uncapping emerald farming.

## Config

36 settings in `config/rlcraftvillagertomes.cfg` or **Mods → RLCraft Villager Tomes → Config**, read live with no restart. Covers which villagers and which enchantments qualify, the tome cap, level ceilings, the full price formula, trade stock, and chat/sound/particle feedback.

`/villagertomes list` shows what the villager you are looking at knows and what each trade costs. `teach`, `forget` and `clear` require op. Aliased to `/tomes` and `/vt`.

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847+
- No dependencies.

Server-side. Clients do not need the mod installed.

## Install

Download `RLCraftVillagerTomes-1.0.0.jar` below and drop it in your `mods` folder.

---

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams. Licensed MIT.
