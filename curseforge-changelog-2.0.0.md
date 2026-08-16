**Teaching a book is no longer guaranteed.** It is a roll you prepare for, on a slot you paid for.

This is a rework rather than an addition, and it changes how the mod opens. Everything below can be switched off — see the bottom.

---

## Slots are bought

Librarians now start with nothing. Each one rolls its own list of items and wants all of it delivered before it will hold a book.

- Requests grow with depth: **two items for the first slot, six by the fifth**, drawing from richer bands as they go.
- **A request is never re-rolled.** If a villager wants something you cannot get, find another villager. That is what makes the ones you have paid into worth protecting.
- **152 bounty items** ship configured, across vanilla, Ice and Fire, Lycanites, Defiled Lands, Trinkets and Baubles and more. Items no loaded mod registers are skipped silently, so this works on any 1.12.2 pack — it just asks for less.

## Teaching is a roll

- **30%** on a librarian's first slot, **+10 per slot** it has opened.
- **Catalysts** are banked into a villager beforehand: glowing powder **+1%**, ingot **+5%**, gem **+30%**. Those are its own crafting costs — an ingot is four powder, a gem is four ingots plus four powder plus a diamond block.
- The ceiling starts at **80%** and rises **+5 per slot**, so a developed villager can be pushed past it.
- Every attempt **asks before it commits** and reports the odds while asking. A brief debounce means a double-click cannot answer the question for you.

## Failure costs, and then pays back

A failed roll **destroys the book and everything banked**.

In return you get a floor: **+5% per level of the book that burned**, so a Sharpness V is worth 25 and a Sharpness I is worth 5. Tracked per villager *and* per enchantment, and it stacks **above** the ceiling — enough bad luck carries a book past anything preparation could buy.

## The quest log

Sneak-click a librarian with a **book and quill** and it becomes a quest log. Sneak-click with the log to write that villager into it — a name you pick, its coordinates, and everything it still needs.

Naming also sets the villager's name tag. Entries update as you deliver and cross themselves off when the slot is paid. Ten villagers per book.

It is a vanilla written book rather than a custom item, which is the only reason **the mod still works on a completely unmodded client**.

## Also

- **Multi-enchantment books are attempted one line at a time.** Only the top enchantment is gambled on and only it is peeled off — either result leaves you holding the rest.
- **A villager will not learn what it already sells.** The book is refused and handed back rather than spending a slot taking over a trade it could already do.
- **Feedback per outcome**: banking, success, failure and refusal each have their own sound and particles, and landing a top-tier enchantment gets the advancement jingle.
- **New commands**: `tiers` dumps the parsed item lists and identifies whatever is in your hand; `unlock`, `bank`, `pity` and `reroll` are op-only tools for testing and fixing.

## Turning it off

Two switches restore the old mod:

- `ENABLE_CHANCE=false` — every book is accepted outright, catalysts never asked for.
- `LOCK_SLOTS=false` — every villager gets its full allowance immediately, **including ones already in your world**.

Villagers taught by 1.x carry over. They keep their trades and are read as having unlocked what they have already filled, so an established librarian owes a bounty for its next slot rather than waking up locked out of trades it already sells.

## Install

Delete `RLCraftVillagerTomes-1.2.0.jar` from your `mods` folder before adding this one. Two versions will crash on startup with a duplicate mod id.

---

Unofficial addon, not affiliated with the RLCraft or RLCraft Dregora teams. Licensed MIT.
