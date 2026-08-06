# RLCraft Villager Tomes

**Minecraft 1.12.2 · Forge · no dependencies**

Teach villagers the enchanted books you find.

Hold an enchanted book, sneak-right-click a villager, and it eats the book and starts selling
that enchantment for emeralds — permanently. Hand the same villager a matching book later and
the trade levels up by anvil rules.

Built for RLCraft, but nothing in it is RLCraft-specific. Enchantments are read from the
registry at runtime, so anything any other mod adds works without this mod knowing about it.

---

## What it does

| | |
|---|---|
| **Teach** | Sneak-right-click a villager while holding an enchanted book. The book is consumed and becomes a trade. |
| **Upgrade** | Give a villager that already sells Unbreaking II another Unbreaking II, and the trade becomes Unbreaking III. |
| **Price** | Ten emeralds per level by default — Mending I costs 10, Unbreaking III costs 30, Sharpness V costs 50 — plus a plain book, in the same slot order vanilla librarians use. |
| **Cap** | Five enchantments per villager by default, so no single villager becomes the whole enchanting system. |

The point is the enchant grind. A fourth Unbreaking I out of a dungeon chest is anvil fodder;
here it is either a new trade or one level closer to Unbreaking III.

## Rules

**Levelling is anvil rules.** Two of the same level make the next one up. A book *higher* than
the villager's current level replaces it outright (`HIGHER_LEVEL_REPLACES`, on by default). A
book *lower* is refused and handed back.

**The cap counts distinct enchantments, not levels.** Upgrading a trade never uses another
slot, so a full villager can still be improved.

**Multi-enchantment books are all-or-nothing.** Every enchantment on the book has to be
teachable and have a free slot, or the whole book is refused. Nothing is worse than watching a
Mending + Unbreaking book vanish to teach only the Unbreaking.

**Trading still works normally.** Teaching is on sneak-right-click specifically because 1.12.2
refuses to open a villager's trade screen while you are sneaking at all — `processInteract`
checks `!player.isSneaking()`. So sneaking is a gesture that would otherwise do nothing, which
leaves the plain right-click free for trading even with a book in hand. A click with anything
other than an enchanted book is left completely alone.

Setting `TEACH_TRIGGER` to `right_click` reverses it, with one consequence worth knowing: while
you hold an enchanted book there is then no click that opens the trade screen, since the plain
click teaches and the sneak click is refused by vanilla. Put the book away to trade.

## Config

Four categories in `config/rlcraftvillagertomes.cfg`, or the in-game config screen.

- **learning** — who can be taught, which enchantments are allowed (whitelist, blacklist,
  treasure, curses), the tome cap, the level ceiling, whether books can be taught past an
  enchantment's natural maximum.
- **upgrading** — whether levelling up works at all, whether higher books replace, whether a
  refused book is eaten anyway.
- **pricing** — `(base + perLevel × level) × rarity × treasure`, clamped between a minimum and
  a maximum. Every multiplier defaults to 1.0 and `EMERALDS_PER_LEVEL` to 10, so out of the box
  the price is ten emeralds per level. Note the hard 64 ceiling: a trade can only ask for one
  stack, so at ten per level everything from level 7 up costs the same 64. Also the extra input
  item, the trade's max uses, whether trades pay experience, and the two never-lock switches.

### Never-lock

1.12.2 has a real design flaw here: a villager only restocks as a side effect of being traded
with, since `EntityVillager.useRecipe` is the sole place that schedules a refresh. Exhaust
every trade on a villager and there is no trade left to use, nothing schedules the refresh, and
that villager is bricked permanently.

`NEVER_LOCK_TAUGHT_TRADES` (**on** by default) keeps this mod's own trades permanently usable by
holding their use limit ahead of their use count. `NEVER_LOCK_ANY_TRADE` (off by default) does
the same for *every* trade on any villager you click — the blunt fix for the flaw above, off
because it turns any emerald trade into an uncapped tap.
- **feedback** — chat messages, villager sounds, particles.

Every setting is documented in the file itself, including what it is *for*, not just what it
does. Changes apply on reload; existing villagers pick up new prices the next time somebody
talks to them.

## Commands

`/villagertomes` (aliases `/tomes`, `/vt`) works on the villager you are looking at.

| | |
|---|---|
| `list` | What this villager knows and what each trade costs. Available to everyone. |
| `teach <enchantment> [level]` | Add a tome directly. Permission level 2. |
| `forget <enchantment>` | Remove one. Permission level 2. |
| `clear` | Remove all of them. Permission level 2. |

`teach` deliberately ignores the whitelist, blacklist and level ceiling — an admin command that
refused to do what it was told would be useless for fixing a villager. It still respects the
tome cap.

## Notes and limitations

- **Curing a zombie villager does not preserve tomes.** A zombie villager is a different entity
  class, so the cured villager is a brand new one with nothing learned. Protect your good
  villagers.
- **Taught trades absorb matching vanilla ones.** If a librarian naturally rolled an
  Unbreaking trade and you then teach it Unbreaking, you get one trade, not two at different
  prices.
- **Server-side.** Clients do not need the mod; `acceptableRemoteVersions` is `*`.

## Building

Needs JDK 8.

```bash
./gradlew build
```

`forge_version` in `gradle.properties` is pinned at `14.23.5.2847` and must not be raised —
see the comment there. The built jar lands in `build/libs/`.
