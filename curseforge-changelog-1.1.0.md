Two balance changes, both of which alter defaults. Existing configs keep whatever they were set to — nothing here changes behaviour on a world that has already been tuned.

## Librarians only

`ALLOWED_PROFESSIONS` now defaults to `minecraft:librarian` instead of accepting every profession.

Books are a librarian's business. Every profession selling enchantments made the whole trade tier meaningless, and a butcher offering Mending stopped feeling like part of the game. The grind this mod removes is rerolling a librarian for the enchantment you want — not finding a librarian in the first place.

Set `ALLOWED_PROFESSIONS` to an empty list for the old behaviour, or list whichever professions you want.

Note that `minecraft:librarian` is the **profession**, which covers both the Librarian and Cartographer careers, so cartographers will take books too. They cannot be separated here: a villager nobody has traded with has not picked a career yet, so filtering on career would refuse librarians you had never spoken to.

## Upgrading now wants a higher book

`ENABLE_UPGRADING` and `HIGHER_LEVEL_REPLACES` are replaced by a single `UPGRADE_MODE` setting, and the default behaviour has changed.

**Before:** two Unbreaking II handed to a villager made Unbreaking III directly.

**Now:** you need an actual Unbreaking III. Buy two Unbreaking II off the villager, combine them at an anvil yourself, and bring the III back.

The anvil stays in the loop. Every level costs a trip and the experience to pay for it, and a villager can no longer bootstrap itself upward on copies of what it already sells.

`UPGRADE_MODE` takes four values:

| | |
|---|---|
| `higher_only` | **New default.** Only a book above the current level counts. |
| `pair_or_higher` | The 1.0.0 behaviour — two matching books step it up, a higher book also replaces. |
| `pair_only` | Matching pairs are the only route; higher books are refused. |
| `off` | Trades freeze at the level they were first taught. |

The two old booleans expressed these same four behaviours, but gave no clue which combination produced which. **Your existing setting is carried over automatically** — the mod maps the old pair onto the matching mode on first load and says so in the log, so a config you had deliberately tuned keeps working and does not silently pick up the new default.

## New: `UPGRADE_TAKES_NEW_SLOT`

**Off by default**, so nothing changes unless you turn it on.

With it on, levelling a trade up **keeps the old one and costs another slot**. A villager pushed from Unbreaking II to III sells both, and two of its five slots are gone.

It changes what a slot means — from "an enchantment this villager deals in" to "one trade on the board". With five slots you can have five enchantments at level I, or one at level V and nothing else, and that trade-off is the point. It fills villagers fast, and a villager with no free slot cannot be levelled up at all; it says so rather than quietly replacing something.

## Also

- Refusal messages now name which professions *will* accept books rather than only saying no, since librarians-only makes that the message most players meet first.
- Refusing a matching book now tells you to go and make the higher one, instead of incorrectly claiming upgrading is disabled.
- `MAX_TOMES_PER_VILLAGER` counts trades rather than distinct enchantments. With `UPGRADE_TAKES_NEW_SLOT` off those are the same number, so this only matters if you turn it on.
- Villagers taught by 1.0.0 load unchanged — the saved format already stored a list of enchantment/level pairs, so it did not need to change.

## Install

Delete `RLCraftVillagerTomes-1.0.0.jar` from your `mods` folder before adding this one. Two versions of the same mod in there will crash on startup with a duplicate mod id.

---

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams. Licensed MIT.
