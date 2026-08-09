# r/RLCraft launch post

Short on purpose. The mod explains itself in one sentence, so the post's only job is to
get that sentence in front of someone and then get out of the way.

## Title

**Recommended:**

> Found a good book and don't want to reroll villagers? Made a mod where you shift right-click one and it sells that enchantment permanently

Alternatives:

> Shift right-click a villager with an enchanted book and it'll sell that enchantment forever - upgrades anvil-style if you give it a second one

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

Shift right-click a villager while holding an enchanted book. It eats the book and sells that enchantment permanently, for ten emeralds a level plus a book - the same shape as a normal librarian trade.
```

**Format:** bold `Shift right-click a villager while holding an enchanted book.`

**Then insert:** `images/VillagerTomesLearnedMessage.png`

### Block 2

```
Give it a second copy of the same level and the trade upgrades, anvil style - two Unbreaking II gets you Unbreaking III. Hand it something lower than it already sells and it gives the book straight back instead of wasting it.

Five enchantments per villager, so you end up with a shelf of specialists rather than one god villager. Upgrading doesn't use a slot, so a full villager can still be improved.
```

**Format:** bold `anvil style`

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

36 settings if you want to tune it - price formula with per-rarity multipliers, enchantment whitelist and blacklist, level caps, restrict it to librarians only. Treasure enchantments and curses are both allowed by default.

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
