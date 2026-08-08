# r/RLCraft launch post — Rich Text Editor version

## Title

**Recommended:**

> I got tired of my fourth duplicate Unbreaking I being anvil fodder, so I made a mod where you hand your enchanted books to a villager and it sells them back to you forever

Alternatives, same hook:

> Made a mod where villagers learn the enchanted books you give them and sell them as permanent trades - two of the same level upgrade it, anvil style

> Every enchanted book you find is a one-time use. I made a mod that turns them into villager trades instead

**On naming Mending in the title:** tempting, because Mending is the emotional core of the
pitch and everyone knows the feeling. But it narrows the mod to one enchantment in the
reader's head before they open the post, and the thing that actually sells it is that
*every* book qualifies, SoManyEnchantments included. Keep Mending for Block 2 where it
lands harder with context.

**Avoid** "no more enchant grind" style titles - they read as a complaint about the pack
rather than a thing you built, and this sub has affection for the grind.

---

## Using the Rich Text Editor

**The editor does not interpret pasted markdown.** Paste `**bold**` and you get literal
asterisks on the page. So every block below is plain text with nothing to strip out, and
the formatting is applied afterwards.

- **Images go where the cursor is.** Put the cursor where the image belongs and use the
  image button.
- **Typing markdown still works, pasting it does not.** Typing `- ` at the start of an
  empty line turns it into a bullet list. Only pasted markup comes through dead.

**Formatting, once the text is in:**

- Bold: select the phrase, `Ctrl+B`
- Bullet list: click the list button, or type `- ` on an empty line first
- Links: select the words, `Ctrl+K`, paste the URL

---

## Block 1

```
Somewhere around hour forty of every RLCraft run I end up with a chest full of enchanted books I'm never going to use. Three Unbreaking I. Two Efficiency II. A Feather Falling I that I found before I had boots. They're not worth the anvil levels to combine and they're not worth throwing away, so they just sit there.

So I made a mod where you give them to a villager instead.

Hold an enchanted book, sneak-right-click any villager, and it eats the book and starts selling that enchantment. Permanently. Ten emeralds a level, plus a book, exactly like a librarian's normal trade.
```

**Format:** bold `So I made a mod where you give them to a villager instead.` — nothing else.

**Then insert:** `images/VillagerTomesLearnedMessage.png`

---

## Block 2

```
The part that made it click for me was the upgrading. It's anvil rules, so a villager already selling Unbreaking II, handed another Unbreaking II, starts selling Unbreaking III. Suddenly the duplicates aren't junk - they're progress toward a trade you'll be buying from for the rest of the run.

Give it a book lower than what it already sells and it hands it straight back to you rather than eating it. Give it a higher one and it just takes the higher level.

And it works with Mending. That's the one I actually built this for. You find one Mending book in a lich tower, you put it on one tool, and that's your relationship with Mending for the next fifty hours. Now the villager sells it, and you can decide whether the next thirty emeralds are worth putting it on your pickaxe.

The enchantments are read straight out of the registry rather than hard-coded, so the whole SoManyEnchantments set works too - Spellproof, Adept, Freezing, Inner Berserk, all of them behave exactly like vanilla ones here.
```

**Format:** bold `The part that made it click for me was the upgrading.` and `And it works with
Mending.`

**Then insert:** `images/VillageTomesUpgradeMessage.png`

---

## Block 3

```
Five enchantments per villager by default, so you end up with a shelf of specialists rather than one god villager. Upgrading an existing trade doesn't use a slot, so a full villager can still be improved.

While I was in there I also fixed something that's bugged me about 1.12 villagers forever. A villager only restocks as a side effect of being traded with - that's the only thing in the whole game that schedules a refresh. So if you exhaust every trade on a villager, there's nothing left to click, nothing schedules the restock, and that villager is bricked permanently. 1.14 fixed this with workstations. 1.12 never did.

Trades this mod adds never lock, by default. There's also a switch that extends that to every trade on every villager you click, vanilla ones included, if you want the flaw gone entirely - it's off by default because it does turn one wheat farmer into an infinite emerald tap.
```

**Format:** bold `While I was in there I also fixed something that's bugged me about 1.12
villagers forever.`

**Then insert:** `images/VillageTomesPricingConfig.png`

---

## Block 4 — last one

```
36 settings across four categories if you want to tune it - the price formula has separate multipliers per rarity tier plus one for treasure enchantments, so you can leave Mending buyable but make it genuinely expensive. You can restrict it to librarians only, whitelist or blacklist specific enchantments, or cap what level villagers will ever sell.

No dependencies, and it's server-side - clients don't need it installed.

CurseForge: PASTE-CURSEFORGE-URL-HERE

Source (MIT): https://github.com/ExiledRadio/RLCraftVillagerTomes

Ten emeralds per level is a number I picked from my own playthrough and nothing else. If that's too cheap, or the five-book cap is in the wrong place, tell me - those are the two I'd most like other opinions on before I call the defaults settled.
```

**Format:** bold `CurseForge:` and `Source (MIT):`. The editor turns both URLs into links on
its own once you paste them.

---

## First comment — post immediately after the post goes live

```
Couple of things worth saying up front:

This is unofficial. Not affiliated with Shivaxi or the Dregora team. Just a player-made addon.

Curing a zombie villager won't preserve what it learned - a zombie villager is a different entity under the hood, so the cured one comes back with nothing. Protect the good ones.

Sneak-right-click is the trigger rather than plain right-click for a reason: 1.12 refuses to open a villager's trade screen while you're sneaking at all, so it's a click that otherwise does nothing. That leaves the normal right-click free for trading even while you're holding a book. There's a config option to swap them if you'd rather.

If a villager already had a natural trade for an enchantment you then teach it, you get one trade instead of two at different prices - the taught one absorbs it.
```

**Format:** bold `This is unofficial.` and `Curing a zombie villager won't preserve what it
learned`.

---

## Before posting

- [ ] **Smoke test the upgrade path on a fresh game launch** — see the note below, this is
      the one thing not yet confirmed end to end
- [ ] CurseForge page loads in a private/incognito window
- [ ] All four images uploaded to CurseForge and the PASTE-URL markers in
      `curseforge-description.md` replaced with forgecdn URLs
- [ ] CurseForge URL pasted into Block 4 above
- [ ] Flair the post, checking what similar mod posts on the sub use
- [ ] Post when the sub is active, generally US evening
- [ ] Stay around for the first couple of hours - early replies drive visibility
- [ ] Consider r/feedthebeast as a second post on a different day, not the same evening.
      Reword the opening if you do; a straight copy-paste across subs reads as spam

## Known-unverified before launch

The teach path and the trade appearing are both confirmed working in game. The upgrade
*message* is confirmed too. What has never been observed end to end is an upgraded trade
actually showing the higher level in the trade list — the one time an upgrade ran, the
trade-list reflection was still broken, and the retry after the fix hit a
`NoClassDefFoundError` caused by the jar being replaced mid-session.

Two minutes on a fresh launch settles it: teach a book, teach the same book again, open the
villager and check the trade reads one level higher.
