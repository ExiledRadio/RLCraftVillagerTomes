# r/RLCraft launch post

Short on purpose. The mod explains itself in one sentence, so the post's only job is to
get that sentence in front of someone and then get out of the way.

## Title

**Recommended:**

> Found a good book and don't want to reroll villagers? Made a mod where you shift right-click one and it sells that enchantment permanently

Alternatives:

> Shift right-click a villager with an enchanted book and it'll sell that enchantment forever - buy two, anvil them together, hand it back to level the trade up

> Every enchanted book you find is a one-time use, so I made a mod that turns them into permanent villager trades

The recommended one is your opening line, which is the right instinct - it leads with the
reader's problem instead of with me. The second is better if the sub has seen a lot of
question-titles lately; it front-loads the mechanic instead.

**Don't** put Mending in the title. It's the strongest single hook but it narrows the mod
to one enchantment before anyone opens the post, and the actual selling point is that
every book qualifies.

---

## Body

Paste each block, then apply the formatting noted under it. The rich text editor does not
interpret pasted markdown - `**bold**` comes through as literal asterisks - so select the
phrase and hit `Ctrl+B` instead. Images go wherever the cursor is.

### Block 1

```
Found a good book out in the world? Don't want to reroll villagers constantly?

Shift right-click a librarian while holding an enchanted book. It eats the book and sells that enchantment permanently, for ten emeralds a level plus a book - the same shape as a normal librarian trade.
```

**Format:** bold `Shift right-click a librarian while holding an enchanted book.`

**Then insert:** `images/VillagerTomesLearnedMessage.png`

### Block 2

```
To level a trade up, bring it a book higher than what it already sells. So buy two Unbreaking II off it, combine them at an anvil yourself, and hand the III back - the trade becomes Unbreaking III. It won't level up off duplicates of what it already sells, which keeps the anvil in the loop.

Hand it something lower than it sells and it gives the book straight back instead of wasting it.

Five enchantments per villager, so you end up with a shelf of specialists rather than one god villager. Upgrading doesn't use a slot, so a full villager can still be improved.
```

**Format:** bold `bring it a book higher than what it already sells`

**Then insert:** `images/VillageTomesUpgradeMessage.png`

### Block 3 — last one

```
Two things worth knowing:

It reads enchantments straight out of the registry, so the whole SoManyEnchantments set works - Spellproof, Adept, Freezing, Inner Berserk, all of it. Nothing is hard-coded.

Trades it adds never lock. In 1.12 a villager only restocks as a side effect of being traded with, so if you exhaust every trade it's bricked permanently. That doesn't happen to these, and there's a switch to extend it to every villager trade if you want the flaw gone entirely.

The RLCraft villager grind can be tedious. I addressed some of that with my Enchantment Recipes mod - some people might prefer this method, or run both.

CurseForge: PASTE-CURSEFORGE-URL-HERE

Source (MIT): https://github.com/ExiledRadio/RLCraftVillagerTomes

Ten emeralds a level and five books per villager are numbers I picked from my own playthrough and nothing else. If either is in the wrong place, say so - those are the two I'd most like other opinions on.
```

**Format:** bold `It reads enchantments straight out of the registry` and `Trades it add
never lock.` Link the words `Enchantment Recipes mod` to its CurseForge page. The editor
turns the two bare URLs into links on its own.

---

## First comment — post immediately after the post goes live

```
A few things that didn't fit above:

This is unofficial. Not affiliated with Shivaxi or the Dregora team.

Curing a zombie villager won't preserve what it learned - a zombie villager is a different entity under the hood, so the cured one comes back blank. Protect the good ones.

Sneaking is the trigger rather than a plain right-click because 1.12 refuses to open a villager's trade screen while you're sneaking at all. It's a click that otherwise does nothing, which leaves the normal right-click free for trading even while you're holding a book. Config option to swap them if you'd rather.

Librarians only by default, though there's a setting to let any profession take books if you'd rather. 36 settings in total - price formula with per-rarity multipliers, enchantment whitelist and blacklist, level caps. Treasure enchantments and curses are both allowed.

No dependencies, and it's server-side - clients don't need it installed.
```

**Format:** bold `This is unofficial.` and `Curing a zombie villager won't preserve what it
learned`.

---

## Before posting

- [ ] **Smoke test the upgrade path on a fresh game launch** — see below, the one thing not
      yet confirmed end to end
- [ ] CurseForge page loads in a private/incognito window, both inline images render
- [ ] CurseForge URL pasted into Block 3
- [ ] Enchantment Recipes CurseForge page linked in Block 3
- [ ] Flair the post, checking what similar mod posts on the sub use
- [ ] Post when the sub is active, generally US evening
- [ ] Stay around for the first couple of hours — early replies drive visibility
- [ ] Consider r/feedthebeast on a different day, not the same evening. Reword the opening;
      a straight copy-paste across subs reads as spam

## Known-unverified before launch

The teach path and the trade appearing are both confirmed in game, and so is the upgrade
*message*. What has never been observed end to end is an upgraded trade actually showing
the higher level in the list — the one time an upgrade ran, the trade-list reflection was
still broken, and the retry after the fix hit a `NoClassDefFoundError` from the jar being
replaced mid-session.

Note that `VillageTomesUpgradeMessage.png`, used in Block 2, was captured during that
broken session. The message in it is real; the trade behind it was not written. Worth
re-taking the screenshot after the smoke test so the image matches what actually happens.

Two minutes settles both: teach a book, teach the same book again, open the villager, check
the trade reads one level higher.

---
---

# 2.0.0 update post

Everything above is the original 1.0 launch and stays as written. This is a separate post for
the rework — 2.0 changes how the mod opens, so it earns its own thread rather than an edit
buried at the bottom of an old one.

## Title

**Recommended:**

> Update: villagers no longer just take your books. You buy the slot, stack the odds, and the book can still burn

Alternatives:

> My villager enchant mod got a rework - slots cost a bounty, teaching is a roll, and a failed book is gone

> 2.0: every tome slot has to be paid for now, and handing over a Sharpness V is a gamble

Lead with the *cost*, not the feature list. The interesting thing about this update is that it
took something that used to be free and made it a decision — that is the hook, and a changelog
in a title reads as a patch note nobody asked for.

## Body

```
Update on the villager tome mod I posted a while back. It used to be simple: hand a librarian an enchanted book, it sells that enchantment forever. That turned out to be too easy, so 2.0 makes you work for it.

Slots have to be bought now. Every librarian rolls its own shopping list - two items for its first slot, six by its fifth, and the later ones start asking for dragon hearts instead of coal. Deliver the lot and it opens up. The list never re-rolls, so a villager that wants something you can't get is a villager you walk away from.

Handing the book over is a roll. A fresh librarian takes it 30% of the time, and every slot it has open adds 10. You can stack the odds beforehand by feeding it glowing powder, ingots or gems - a gem alone takes a fresh one from base to ceiling - and it tells you the odds and asks before it commits.

If it fails, the book is gone and so is everything you banked. What you get instead is a grudge: each failure raises the floor by 5% per level of the book that burned, on that villager, for that enchantment. Lose a Sharpness V and you're 25 points closer next time. That bonus stacks above the ceiling, so enough bad luck eventually gets you there anyway.

There's a quest log too - sneak-click a librarian with a book and quill and it writes down what that villager wants and where it lives, then crosses it off when you're done. It's a normal written book rather than a custom item, which is why the mod still runs on a vanilla client with nothing installed.

All of it is off-switchable if you liked it the old way. 152 items ship in the bounty list, and anything from a mod you don't have is skipped, so it works on any 1.12.2 pack and not just RLCraft.

CurseForge: PASTE-CURSEFORGE-URL-HERE

Source (MIT): https://github.com/ExiledRadio/RLCraftVillagerTomes
```

**Format:** bold `Slots have to be bought now.`, `Handing the book over is a roll.` and
`If it fails, the book is gone and so is everything you banked.` Nothing else — three bolds
across five paragraphs is enough to carry a skim.

**Images:** one of a villager's request list, one of a failed attempt with the pity line. Both
are new screens that did not exist in 1.0, so they are worth taking fresh rather than reusing
anything from the launch post.

## First comment

```
Worth saying: everything here can be turned off. ENABLE_CHANCE=false goes back to books always working, and LOCK_SLOTS=false gives every villager its slots for free - including ones already in your world. Between them you get the old mod back.

Villagers you taught on 1.x carry over. They keep their trades, and they're treated as having already unlocked the slots they filled, so an established librarian just owes a bounty for its next one rather than losing what it had.

Still server-side only. Clients don't need it installed and there are no custom packets - the quest log is a vanilla written book that the server rewrites, specifically so that stayed true.
```

**Format:** bold `everything here can be turned off.`
