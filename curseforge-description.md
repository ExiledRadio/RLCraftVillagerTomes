<!--
Two images only, both gameplay. The config screenshots are deliberately not in here -
a wall of settings tables breaks the read and nobody decides to install a mod because
they saw its config screen. They are still uploaded to the project's Images tab, where
people who want them will look.

USED:
  learned message   https://media.forgecdn.net/attachments/1852/310/villagertomeslearnedmessage-png.png
  upgrade message   https://media.forgecdn.net/attachments/1852/314/villagetomesupgrademessage-png.png

UPLOADED BUT UNUSED, kept here so the URLs are not lost:
  config category      https://media.forgecdn.net/attachments/1852/311/villagetomesconfigcategory-png.png
  feedback category    https://media.forgecdn.net/attachments/1852/309/villagertomesfeedbackcategory-png.png
  learning config      https://media.forgecdn.net/attachments/1852/312/villagetomeslearningconfig-png.png
  pricing config       https://media.forgecdn.net/attachments/1852/313/villagetomespricingconfig-png.png
  upgrading category   https://media.forgecdn.net/attachments/1852/315/villagetomesupgradingcategory-png.png
-->

# RLCraft Villager Tomes

**The enchanted books you find become permanent villager trades.**

You found a Mending book. You used it once, on one tool, and that was the end of it. Every duplicate after that is anvil fodder.

Hand it to a villager instead and it becomes a trade you can go back to forever.

Works out of the box. No config editing, no dependencies.

---

## How it works

**Hold an enchanted book and sneak-right-click a villager.** The book is consumed and that enchantment becomes a permanent trade on that villager, priced in emeralds.

**Bring a higher book later and the trade levels up.** Buy two Unbreaking II off the villager, combine them at an anvil yourself, and hand the III back — its trade becomes Unbreaking III.

**Librarians only**, by default. Books are a librarian's business, and the grind this removes is rerolling one over and over for the enchantment you want — not finding a librarian in the first place. `ALLOWED_PROFESSIONS` opens it up to any profession, or all of them, if you disagree.

![A villager learning a book](https://media.forgecdn.net/attachments/1852/310/villagertomeslearnedmessage-png.png)

---

## Why sneak-right-click?

Because of a quirk specific to 1.12: vanilla refuses to open a villager's trade screen while you are sneaking at all. `EntityVillager.processInteract` checks `!player.isSneaking()` before doing anything.

That makes sneaking a gesture that would otherwise do nothing, which is exactly what you want for a new interaction — the plain right-click stays free for trading, even with a book in your hand. Nothing about ordinary villager behaviour changes unless you are deliberately holding an enchanted book at one.

`TEACH_TRIGGER` flips it to plain right-click if you prefer, with the caveat that you then have to put the book away to open the trade screen.

---

## Upgrading

| You give it | It knows | Result |
|---|---|---|
| Unbreaking III | Unbreaking II | **Unbreaking III** |
| Unbreaking II | Unbreaking II | Refused — go make a III |
| Unbreaking I | Unbreaking II | Refused, book handed back |
| Unbreaking III | Unbreaking III | Refused, already at maximum |

**The anvil stays in the loop.** A villager will not level itself up on copies of what it already sells, so every level costs you an anvil trip and the experience to pay for it. `UPGRADE_MODE` has three other settings if you want it cheaper — `pair_or_higher` lets two matching books step it up directly with no anvil at all.

**Upgrading never uses a slot.** A villager at its cap can still be improved, so a full villager is not a finished one.

**A refused book is never consumed.** It goes straight back in your hand with a message saying why.

![Upgrading a trade](https://media.forgecdn.net/attachments/1852/314/villagetomesupgrademessage-png.png)

---

## The cap

**Five different enchantments per villager**, configurable.

This is what stops one villager becoming the entire enchanting system. At five you end up with a shelf of specialists — the armour one, the tool one — and losing one to a zombie actually costs you something.

Lowering the cap later never deletes anything. A villager over the new limit keeps and sells everything it already knows, it just cannot learn more.

---

## Price

**Ten emeralds per level, plus one book.**

- Mending I — 10 emeralds
- Unbreaking III — 30 emeralds
- Sharpness V — 50 emeralds

The book takes the first input slot and the emeralds the second, matching vanilla's own librarian recipe, so a taught trade looks the same as a naturally rolled one sitting next to it.

The full formula is `(base + perLevel × level) × rarity × treasure`, and every term is a setting. Separate multipliers for common, uncommon, rare and very rare, plus an extra one for treasure enchantments — set `TREASURE_COST_MULTIPLIER` to 3 and Mending costs 30 instead of 10 while everything else stays put.

One ceiling worth knowing: a trade can only ask for a single stack, so 64 emeralds is a hard maximum. At ten per level everything from level VII up costs the same. That never comes up with vanilla enchantments, which stop at V, but packs that push past that will see high levels flatten out.

---

## Modded enchantments work

Enchantments are read from the registry at runtime rather than hard-coded, so anything any mod adds is teachable with no patch and no compatibility release.

In RLCraft that means the whole SoManyEnchantments set — Spellproof, Adept, Freezing, Inner Berserk, Penetration — behaves exactly like a vanilla enchantment here.

Treasure enchantments including Mending are allowed by default, because being able to buy more Mending after finding one is the single biggest thing this does for a long run. Curses are allowed too; a looted curse book is still a book somebody looted. Both have switches if you disagree.

---

## Trades that never lock

1.12 has a real design flaw with villagers, and it is worth explaining because this mod ships with a fix on by default.

A villager only restocks as a *side effect of being traded with* — `EntityVillager.useRecipe` is the only place in the game that schedules a refresh. So once every trade on a villager is exhausted, there is no trade left to use, nothing schedules the refresh, and that villager is bricked permanently. Later versions fixed this with workstations and twice-daily restocking. 1.12 never did.

**`NEVER_LOCK_TAUGHT_TRADES`** is on by default and keeps this mod's own trades usable forever. You found the book; the trade expiring was never the interesting part.

**`NEVER_LOCK_ANY_TRADE`** is off by default and extends the same treatment to *every* trade on any villager you interact with, vanilla and modded alike. That fixes the flaw outright, but it also means one farmer buying wheat becomes an uncapped emerald tap, so it does not switch itself on.

---

## Config

**36 settings across four categories**, in `config/rlcraftvillagertomes.cfg` or **Mods → RLCraft Villager Tomes → Config**. All read live — nothing needs a restart, and price changes reach existing villagers the next time somebody talks to them.

- **learning** — who can be taught, the tome cap, enchantment whitelist and blacklist, treasure and curse switches, level ceilings, whether books can go past an enchantment's natural maximum.
- **upgrading** — whether levelling works at all, whether higher books replace, whether a refused book is eaten anyway.
- **pricing** — the whole formula, the extra input item, trade stock, experience, and the never-lock switches.
- **feedback** — chat messages, villager sounds, particles, and a debug logging toggle.

Screenshots of every category are on the Images tab.

---

## Commands

`/villagertomes` — aliases `/tomes` and `/vt` — operates on the villager you are looking at.

- `list` — what it knows and what each trade costs. Available to everyone.
- `teach <enchantment> [level]` — add a tome directly. Op only.
- `forget <enchantment>` — remove one. Op only.
- `clear` — remove all of them. Op only.

`teach` deliberately ignores the whitelist, blacklist and level ceiling. An admin command that refused to do what it was told would be useless for fixing a villager.

---

## Things to know

**Curing a zombie villager does not preserve tomes.** A zombie villager is a different entity class entirely, so the cured villager is a brand new one that has learned nothing. Protect your good villagers.

**A villager will not learn what it already sells.** If a librarian naturally rolled an Unbreaking trade, offering it an Unbreaking book is refused and handed straight back — buy it from them instead. Slots are too expensive to let one be spent taking over a trade the villager could already do.

**Multi-enchantment books are all-or-nothing.** Every enchantment on the book needs to be allowed and to have a free slot, or the whole book is refused. Watching a Mending + Unbreaking book vanish to teach only the Unbreaking would be a genuinely bad surprise, and there is no way to hand half a book back.

---

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847 or newer
- No dependencies

Everything happens server-side; the client half is a config screen and nothing else. Clients do not need the mod installed, and there are no custom packets.

## Install

Download the jar and drop it in your `mods` folder.

---

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams. Licensed MIT.

Source: https://github.com/ExiledRadio/RLCraftVillagerTomes
