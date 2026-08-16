<!--
Two images only, both gameplay. Config screenshots are deliberately not in here -
a wall of settings tables breaks the read and nobody installs a mod because they saw
its config screen. They are still on the project's Images tab.

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

**Teach a librarian the enchanted books you find, and it sells them back to you forever.**

You found a Mending book. You used it once, on one tool, and that was the end of it. Every duplicate after that is anvil fodder.

Hand it to a librarian instead and it becomes a trade you can go back to — but you have to earn the room, and the book might not take.

Works out of the box. No dependencies, and clients do not need it installed.

![A villager learning a book](https://media.forgecdn.net/attachments/1852/310/villagertomeslearnedmessage-png.png)

---

## The loop

| | |
|---|---|
| **Pay for a slot** | Every librarian wants a list of items before it will hold a book. Deliver it. |
| **Stack the odds** | Feed it glowing powder, ingots or gems. Each one raises its chance. |
| **Check where you stand** | Sneak-click it empty-handed for the current odds and what it still wants. |
| **Commit the book** | Sneak-click with the book. It asks once, then rolls. |

Sneaking is the trigger throughout, for a reason specific to 1.12: vanilla refuses to open a villager's trade screen while you are sneaking. That makes it a gesture that would otherwise do nothing, and leaves the plain right-click free for trading — even with a book in hand. Nothing about ordinary villager behaviour changes unless you are deliberately sneaking at one.

---

## Slots are bought, not given

A librarian starts with nothing and rolls its own demand for each slot it opens.

```
Needed for slot 3:
  Blaze Rod 0/6
  Block of Coal 0/3
  Glowing Ingot 0/2
  Diamond 0/4
```

Requests get longer and richer the deeper you go — two items for the first slot, six by the fifth, and the bands they draw from climb with them. An opening request is material you already have stacks of. A fifth can ask for a dragon heart.

**A request is never re-rolled.** If a villager wants something you cannot get, the answer is a different villager. That is what makes the ones you have already paid into worth protecting.

Five slots per librarian by default, and every one of them costs.

---

## Teaching is a roll

A librarian on its first slot accepts a book **30%** of the time. Every slot it has opened adds **+10**, so one four slots deep sits at 60 before you have done anything.

**Catalysts** are banked into a villager beforehand and raise it further:

| | |
|---|---|
| Glowing Powder | +1% |
| Glowing Ingot | +5% |
| Glowing Gem | +30% |

Those numbers come straight from what each costs to craft — an ingot is four powder, a gem is four ingots plus four powder plus a diamond block. A single gem takes a fresh villager from its base to its ceiling.

The ceiling starts at **80%** and rises **+5 per slot unlocked**, so a developed villager can be pushed past it. Preparation never quite guarantees the result on a young one, which is the point.

Every attempt asks before it commits, and reports the odds while it asks.

---

## Failure costs you

A failed roll **destroys the book and everything you had banked**. That is what gives the gamble teeth.

What you get back is a floor. Each failure adds **+5% per level of the book that burned** — so losing a Sharpness V is worth 25, and a Sharpness I is worth 5 — tracked per villager *and* per enchantment. Failing Mending on one librarian makes Mending easier on that librarian only.

That bonus stacks **on top of the ceiling**, not under it. Enough bad luck will carry a book past what any amount of preparation could buy.

![Levelling a trade up](https://media.forgecdn.net/attachments/1852/314/villagetomesupgrademessage-png.png)

---

## Levelling a trade up

Bring a book **above** what the villager already sells and the trade becomes that level.

So pushing a trade from Unbreaking II to III means buying two Unbreaking II from it, combining them at an anvil yourself, and bringing the III back. The anvil stays in the loop — a villager will not level itself up on copies of what it already sells.

A **multi-enchantment book is attempted one line at a time.** Only the top enchantment is gambled on, and only it is peeled off the book. A book of Unbreaking III, Mending and Efficiency V is three separate decisions, and either result leaves you holding the other two.

A book *below* what it sells is refused and handed straight back. So is a book for an enchantment the librarian already sells naturally — slots are too expensive to spend one taking over a trade it could already do.

---

## The quest log

Sneak-click a librarian holding a **book and quill** and it becomes a quest log. Sneak-click with the log and that villager gets written into it: a name you choose, where it lives, and everything it still needs.

```
Gareth
x412 y68 z-1190

Slot 3 needs:
- 6 Blaze Rod
- 3 Block of Coal
- 2 Glowing Ingot
```

Naming a villager also sets its name tag, so the book and the world agree. Entries update as you deliver and cross themselves off when the slot is paid for. Ten villagers per book.

It is a **vanilla written book**, not a custom item — which is the only reason this mod still works on a completely unmodded client.

---

## Modded enchantments work

Enchantments are read from the registry at runtime, so anything any mod adds is teachable with no patch and no compatibility release. In RLCraft that means the whole SoManyEnchantments set behaves exactly like a vanilla enchantment here.

Treasure enchantments including Mending are allowed by default — being able to buy more Mending after finding one is the single biggest thing this does for a long run. Curses are allowed too; a looted curse book is still a book somebody looted. Both have switches.

---

## Trades that never lock

1.12 has a real design flaw with villagers. A villager only restocks as a *side effect of being traded with* — `EntityVillager.useRecipe` is the only place in the game that schedules a refresh. Exhaust every trade and there is nothing left to use, nothing schedules the refresh, and that villager is bricked permanently.

**`NEVER_LOCK_TAUGHT_TRADES`** is on by default and keeps this mod's trades usable forever. **`NEVER_LOCK_ANY_TRADE`** is off by default and extends that to every trade on any villager you click, which fixes the flaw outright at the cost of uncapping emerald farming.

---

## Config

Six categories in `config/rlcraftvillagertomes.cfg`, or the in-game config screen. All read live; nothing needs a restart.

- **learning** — who can be taught, the tome cap, enchantment whitelist and blacklist, treasure and curse switches, level ceilings
- **slots** — whether slots lock at all, how requests grow, the per-slot band weights, quest log size
- **chance** — base, ceiling, per-slot growth, pity, the confirmation prompt, what a failure destroys
- **bounties** — the item list villagers demand from, with per-band and per-item quantities
- **catalysts** — which items raise the odds and by how much
- **pricing** — the emerald formula, the extra input item, trade stock, experience
- **upgrading** and **feedback** — levelling rules, messages, sounds, particles

**152 bounty items** ship configured, spanning vanilla, Ice and Fire, Lycanites, Defiled Lands, Trinkets and Baubles and others. Items no loaded mod registers are skipped silently, so the list is safe on any 1.12.2 pack — it simply asks for less.

Every setting is documented in the file itself, including what it is *for*, not just what it does.

---

## Commands

`/villagertomes` — aliases `/tomes`, `/vt` — works on the villager you are looking at.

| | |
|---|---|
| `list` | What it knows and what each trade costs. Everyone. |
| `tiers` | The parsed item lists, and what the item in your hand counts as. Everyone. |
| `unlock [n]` | Open slots without paying. Op. |
| `bank <percent>` | Set banked chance outright. Op. |
| `pity <ench> <percent>` | Set what a villager owes on one book. Op. |
| `reroll` | Throw the current demand away and roll another. Op. |
| `teach` / `forget` / `clear` | Edit its tomes directly. Op. |

---

## Things to know

**Curing a zombie villager does not preserve tomes.** A zombie villager is a different entity class, so the cured one is brand new with nothing learned. Protect your good librarians.

**Librarians only, by default.** Books are a librarian's business, and the grind this removes is rerolling one for the enchantment you want — not finding one in the first place. `ALLOWED_PROFESSIONS` opens it up. Note that cartographers share the librarian profession and are included.

**All of it can be switched off.** `ENABLE_CHANCE` restores guaranteed teaching and `LOCK_SLOTS` gives every villager its full allowance immediately — including ones already in your world. Between them you get the 1.x mod back.

---

## Requirements

- Minecraft 1.12.2, Forge 14.23.5.2847 or newer
- No dependencies

Everything happens server-side. Clients do not need the mod installed, and there are no custom packets.

## Install

Download the jar and drop it in your `mods` folder. **Delete any older version first** — two copies will crash on startup with a duplicate mod id.

---

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams. Licensed MIT.

Source: https://github.com/ExiledRadio/RLCraftVillagerTomes
