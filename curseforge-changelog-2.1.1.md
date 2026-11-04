**Catalysts are four times stronger, and switching chance off now actually hides it.**

---

## Catalysts x4

The old numbers were tuned for a slow endgame grind, but villagers are a midgame tool. They now hit four times harder:

| Catalyst       | Was  | Now   |
| -------------- | ---- | ----- |
| Glowing Powder | +1%  | +4%   |
| Glowing Ingot  | +5%  | +20%  |
| Glowing Gem    | +30% | +120% |

The unused bands scale with them (uncommon 10, epic 40, legendary 60), so custom catalysts added to those keep their place on the ladder. A single Glowing Gem now takes any librarian straight to its ceiling. The ceiling itself hasn't changed, so a one-slot villager still tops out at 80%.

**Existing installs:** Forge doesn't overwrite values already in your config. To get the new numbers, delete the `CATALYST_TIERS` block from `config/rlcraftvillagertomes.cfg` (or the whole file) and restart.

## Chance off means chance off

With `ENABLE_CHANCE=false`, the teach confirmation still said "60% chance" and warned that the book could be destroyed. It now just asks "Give Unbreaking III to this villager?" and confirms. The admin `/villagertomes unlock` reply drops its chance figure too.
