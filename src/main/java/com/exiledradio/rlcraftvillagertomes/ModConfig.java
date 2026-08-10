package com.exiledradio.rlcraftvillagertomes;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Every knob the mod has, split into four categories so the screen stays readable.
 *
 * <p>The defaults are deliberately the plainest reading of what the mod is for: any
 * villager will learn any book, five books each, and a trade costs ten emeralds per level
 * plus a plain book. Everything that could make it stricter, pricier or rarer is off until
 * somebody turns it on.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID)
public class ModConfig {

    /** Who can be taught, what they will accept, and how much of it. */
    public static final String CATEGORY_LEARNING = "learning";
    /** What happens when a villager is handed a book it already knows. */
    public static final String CATEGORY_UPGRADING = "upgrading";
    /** What the resulting trade costs and how often it can be used. */
    public static final String CATEGORY_PRICING = "pricing";
    /** Chat, sound and particle feedback. */
    public static final String CATEGORY_FEEDBACK = "feedback";

    /** Display order in the config screen. Without this the GUI sorts alphabetically. */
    private static final String[] CATEGORIES = {
            CATEGORY_LEARNING, CATEGORY_UPGRADING, CATEGORY_PRICING, CATEGORY_FEEDBACK,
    };

    private static final List<String> ORDER_LEARNING = Arrays.asList(
            "ENABLE_LEARNING", "TEACH_TRIGGER", "MAX_TOMES_PER_VILLAGER",
            "ALLOWED_PROFESSIONS", "TEACH_BABY_VILLAGERS", "ALLOW_MULTI_ENCHANT_BOOKS",
            "ENCHANTMENT_WHITELIST", "ENCHANTMENT_BLACKLIST", "ALLOW_TREASURE_ENCHANTMENTS",
            "ALLOW_CURSES", "MAX_LEARNABLE_LEVEL", "ALLOW_OVERLEVELING");

    private static final List<String> ORDER_UPGRADING = Arrays.asList(
            "ENABLE_UPGRADING", "HIGHER_LEVEL_REPLACES", "CONSUME_BOOK_ON_REJECT");

    private static final List<String> ORDER_PRICING = Arrays.asList(
            "BASE_EMERALD_COST", "EMERALDS_PER_LEVEL", "MIN_EMERALD_COST", "MAX_EMERALD_COST",
            "COST_MULTIPLIER_COMMON", "COST_MULTIPLIER_UNCOMMON", "COST_MULTIPLIER_RARE",
            "COST_MULTIPLIER_VERY_RARE", "TREASURE_COST_MULTIPLIER",
            "EXTRA_INPUT_ITEM", "EXTRA_INPUT_COUNT", "MAX_TRADE_USES",
            "NEVER_LOCK_TAUGHT_TRADES", "NEVER_LOCK_ANY_TRADE", "TRADE_GRANTS_XP");

    private static final List<String> ORDER_FEEDBACK = Arrays.asList(
            "ANNOUNCE_LEARNED", "ANNOUNCE_UPGRADED", "ANNOUNCE_REJECTED",
            "PLAY_SOUNDS", "SPAWN_PARTICLES", "DEBUG_LOGGING");

    /** Valid values for {@link #TEACH_TRIGGER}, also shown as a dropdown in the GUI. */
    public static final String TRIGGER_RIGHT_CLICK = "right_click";
    public static final String TRIGGER_SNEAK_RIGHT_CLICK = "sneak_right_click";
    private static final String[] TRIGGER_VALUES = {TRIGGER_RIGHT_CLICK, TRIGGER_SNEAK_RIGHT_CLICK};

    /**
     * Hands Forge its own mutable copy of an order list.
     *
     * <p>{@code ConfigCategory.setPropertyOrder} keeps the list it is given and then
     * appends any property already in the category that the list does not mention, so the
     * list has to be growable - {@code Arrays.asList} is fixed-size and would throw the
     * moment a stale key existed in somebody's config. It also has to be a copy, or Forge
     * would append into the shared constants above.
     */
    private static List<String> mutableOrder(List<String> order) {
        return new ArrayList<String>(order);
    }

    /** Which category each setting belongs to. Doubles as the list of keys that are real. */
    private static final Map<String, String> CATEGORY_OF_KEY = new HashMap<String, String>();

    static {
        for (String key : ORDER_LEARNING) CATEGORY_OF_KEY.put(key, CATEGORY_LEARNING);
        for (String key : ORDER_UPGRADING) CATEGORY_OF_KEY.put(key, CATEGORY_UPGRADING);
        for (String key : ORDER_PRICING) CATEGORY_OF_KEY.put(key, CATEGORY_PRICING);
        for (String key : ORDER_FEEDBACK) CATEGORY_OF_KEY.put(key, CATEGORY_FEEDBACK);
    }

    public static Configuration config;

    // learning
    public static boolean ENABLE_LEARNING = true;
    public static String TEACH_TRIGGER = TRIGGER_SNEAK_RIGHT_CLICK;
    public static int MAX_TOMES_PER_VILLAGER = 5;
    public static String[] ALLOWED_PROFESSIONS = {"minecraft:librarian"};
    public static boolean TEACH_BABY_VILLAGERS = false;
    public static boolean ALLOW_MULTI_ENCHANT_BOOKS = true;
    public static String[] ENCHANTMENT_WHITELIST = new String[0];
    public static String[] ENCHANTMENT_BLACKLIST = new String[0];
    public static boolean ALLOW_TREASURE_ENCHANTMENTS = true;
    public static boolean ALLOW_CURSES = true;
    public static int MAX_LEARNABLE_LEVEL = 0;
    public static boolean ALLOW_OVERLEVELING = false;

    // upgrading
    public static boolean ENABLE_UPGRADING = true;
    public static boolean HIGHER_LEVEL_REPLACES = true;
    public static boolean CONSUME_BOOK_ON_REJECT = false;

    // pricing
    public static int BASE_EMERALD_COST = 0;
    public static float EMERALDS_PER_LEVEL = 10.0F;
    public static int MIN_EMERALD_COST = 1;
    public static int MAX_EMERALD_COST = 64;
    public static float COST_MULTIPLIER_COMMON = 1.0F;
    public static float COST_MULTIPLIER_UNCOMMON = 1.0F;
    public static float COST_MULTIPLIER_RARE = 1.0F;
    public static float COST_MULTIPLIER_VERY_RARE = 1.0F;
    public static float TREASURE_COST_MULTIPLIER = 1.0F;
    public static String EXTRA_INPUT_ITEM = "minecraft:book";
    public static int EXTRA_INPUT_COUNT = 1;
    public static int MAX_TRADE_USES = 16;
    public static boolean NEVER_LOCK_TAUGHT_TRADES = true;
    public static boolean NEVER_LOCK_ANY_TRADE = false;
    public static boolean TRADE_GRANTS_XP = true;

    // feedback
    public static boolean ANNOUNCE_LEARNED = true;
    public static boolean ANNOUNCE_UPGRADED = true;
    public static boolean ANNOUNCE_REJECTED = true;
    public static boolean PLAY_SOUNDS = true;
    public static boolean SPAWN_PARTICLES = true;
    public static boolean DEBUG_LOGGING = false;

    /**
     * A stack of emeralds is 64, and a merchant recipe's buy stack is one stack. Anything
     * above this simply cannot be represented as a trade, so it is the ceiling on every
     * emerald setting rather than an arbitrary limit.
     */
    private static final int MAX_STACK = 64;

    // Lower-cased and trimmed copies of the string lists, rebuilt on every load so the
    // per-book lookups the interaction handler does stay cheap.
    private static Set<String> allowedProfessions = new HashSet<String>();
    private static Set<String> enchantmentWhitelist = new HashSet<String>();
    private static Set<String> enchantmentBlacklist = new HashSet<String>();

    /** Resolved once per load from {@link #EXTRA_INPUT_ITEM}; empty means "emeralds only". */
    private static ItemStack extraInput = ItemStack.EMPTY;

    /** Player-readable form of {@link #ALLOWED_PROFESSIONS}; null when every profession qualifies. */
    private static String allowedProfessionsLabel;

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            loadConfig();
        }
    }

    public static void loadConfig() {
        migrateSecondInput();
        // Must run before the loaders: they call setCategoryPropertyOrder, which appends
        // any key the order list does not mention, and that call throws on a fixed-size
        // list the moment a stale key exists. It is also what deletes the old keys the
        // migration above has just finished reading.
        pruneUnknownKeys();

        loadLearning();
        loadUpgrading();
        loadPricing();
        loadFeedback();

        clampAndDerive();

        if (config.hasChanged()) {
            config.save();
        }

        RLCraftVillagerTomes.LOGGER.info(
                "Config loaded - learning {}, up to {} tome(s) per villager, {} emerald(s) per level",
                ENABLE_LEARNING ? "on" : "off", MAX_TOMES_PER_VILLAGER, EMERALDS_PER_LEVEL);
    }

    /**
     * Carries SECOND_INPUT_ITEM and SECOND_INPUT_COUNT onto the keys that replaced them.
     *
     * <p>The setting was renamed when the inputs were reordered: what used to be the second
     * item in the trade is now the first, with the emeralds behind it, so "second" had
     * become an outright lie. Renaming a key without this would silently reset anyone who
     * had changed it back to a plain book, since {@link #pruneUnknownKeys} deletes keys the
     * mod no longer reads.
     */
    private static void migrateSecondInput() {
        if (!config.hasCategory(CATEGORY_PRICING)) {
            return;
        }
        ConfigCategory pricing = config.getCategory(CATEGORY_PRICING);
        boolean moved = false;

        if (pricing.containsKey("SECOND_INPUT_ITEM") && !pricing.containsKey("EXTRA_INPUT_ITEM")) {
            pricing.put("EXTRA_INPUT_ITEM", pricing.get("SECOND_INPUT_ITEM"));
            moved = true;
        }
        if (pricing.containsKey("SECOND_INPUT_COUNT") && !pricing.containsKey("EXTRA_INPUT_COUNT")) {
            pricing.put("EXTRA_INPUT_COUNT", pricing.get("SECOND_INPUT_COUNT"));
            moved = true;
        }

        if (moved) {
            RLCraftVillagerTomes.LOGGER.info("Renamed SECOND_INPUT_* to EXTRA_INPUT_* - the item "
                    + "now sits in the first trade slot with the emeralds second, matching vanilla. "
                    + "Your setting was kept.");
        }
    }

    /**
     * Drops any property sitting in one of our categories that the mod no longer reads.
     *
     * <p>Forge never removes a property once it has stopped asking for it, so without this
     * a renamed setting would linger in the file forever, looking editable while doing
     * nothing at all. The order lists double as the definition of what is still real.
     */
    private static void pruneUnknownKeys() {
        int removed = 0;
        for (String category : CATEGORIES) {
            if (!config.hasCategory(category)) {
                continue;
            }
            ConfigCategory cat = config.getCategory(category);
            for (String key : new ArrayList<String>(cat.keySet())) {
                if (!category.equals(CATEGORY_OF_KEY.get(key))) {
                    cat.remove(key);
                    removed++;
                }
            }
        }
        if (removed > 0) {
            RLCraftVillagerTomes.LOGGER.info(
                    "Removed {} config setting(s) this version no longer uses.", removed);
        }
    }

    // ---------------------------------------------------------------- learning

    private static void loadLearning() {
        config.setCategoryComment(CATEGORY_LEARNING,
                "Who can be taught, what they will accept, and how much.\n"
                        + "\n"
                        + "The basic loop: hold an enchanted book, right-click a villager, and it\n"
                        + "eats the book and starts selling that enchantment for emeralds forever.\n"
                        + "The point is that a book you already found is worth something even when\n"
                        + "it is the fourth Unbreaking I of the week - it becomes a trade you can\n"
                        + "come back to instead of anvil fodder.");
        config.setCategoryPropertyOrder(CATEGORY_LEARNING, mutableOrder(ORDER_LEARNING));

        ENABLE_LEARNING = config.getBoolean(
                "ENABLE_LEARNING", CATEGORY_LEARNING, true,
                "Master on/off switch. When false the mod does nothing at all - villagers\n"
                        + "ignore books entirely and no trades are added.\n"
                        + "Tomes already taught are NOT erased; they stop being offered while this\n"
                        + "is off and come back when it is turned on again."
        );

        TEACH_TRIGGER = config.getString(
                "TEACH_TRIGGER", CATEGORY_LEARNING, TRIGGER_SNEAK_RIGHT_CLICK,
                "Which click hands a villager the book you are holding.\n"
                        + "\n"
                        + "  sneak_right_click  default. Sneak and right-click to teach; a plain\n"
                        + "                     right-click always opens the trade screen.\n"
                        + "  right_click        the reverse: a plain right-click with a book in hand\n"
                        + "                     teaches it.\n"
                        + "\n"
                        + "Sneaking is the default for a reason specific to 1.12: vanilla refuses to\n"
                        + "open a villager's trade screen while the player is sneaking at all\n"
                        + "(EntityVillager.processInteract checks !player.isSneaking()). So sneaking\n"
                        + "is a click that would otherwise do nothing, which makes it free to use for\n"
                        + "teaching, and it leaves the plain right-click free for trading.\n"
                        + "\n"
                        + "The consequence for right_click mode: while you are holding an enchanted\n"
                        + "book there is NO click that opens the trade screen, because the plain click\n"
                        + "teaches and the sneak click is refused by vanilla. Put the book away to\n"
                        + "trade. Nothing is lost, it is just an extra step.\n"
                        + "\n"
                        + "Either way, a click with anything other than an enchanted book in hand is\n"
                        + "left alone completely.",
                TRIGGER_VALUES
        );

        MAX_TOMES_PER_VILLAGER = config.getInt(
                "MAX_TOMES_PER_VILLAGER", CATEGORY_LEARNING, 5, 1, 64,
                "How many different enchantments one villager can hold at once.\n"
                        + "\n"
                        + "This is what stops a single villager becoming the entire enchanting\n"
                        + "system. At the default of 5 you end up with a shelf of specialists - the\n"
                        + "armour one, the tool one - and losing one to a zombie actually costs you\n"
                        + "something.\n"
                        + "\n"
                        + "Counts DISTINCT enchantments, not levels. Upgrading Unbreaking II to III\n"
                        + "does not use another slot, so a full villager can still be improved.\n"
                        + "Lowering this later never deletes anything: a villager over the new limit\n"
                        + "keeps and sells everything it already knows, it just cannot learn more."
        );

        ALLOWED_PROFESSIONS = config.getStringList(
                "ALLOWED_PROFESSIONS", CATEGORY_LEARNING, new String[]{"minecraft:librarian"},
                "Which villager professions will accept books - one registry name per line.\n"
                        + "\n"
                        + "Librarians only, by default. Books are a librarian's business, every other\n"
                        + "profession selling enchantments makes the whole trade tier meaningless, and\n"
                        + "a butcher offering Mending is the kind of thing that stops feeling like\n"
                        + "part of the game. The grind this mod removes is rerolling a librarian over\n"
                        + "and over for the enchantment you want - not finding a librarian at all.\n"
                        + "\n"
                        + "Note that minecraft:librarian is the PROFESSION, which covers both the\n"
                        + "Librarian and Cartographer careers. Cartographers will therefore take books\n"
                        + "too. There is no way to separate them here: a villager nobody has traded\n"
                        + "with yet has not picked a career, so filtering on career would refuse\n"
                        + "librarians you had never spoken to.\n"
                        + "\n"
                        + "Empty means EVERY profession accepts books. Vanilla names are\n"
                        + "minecraft:farmer, minecraft:librarian, minecraft:priest, minecraft:smith,\n"
                        + "minecraft:butcher and minecraft:nitwit. Modded professions use their own.\n"
                        + "Matching is case-insensitive, and a bare name with no colon is assumed to\n"
                        + "be minecraft:."
        );

        TEACH_BABY_VILLAGERS = config.getBoolean(
                "TEACH_BABY_VILLAGERS", CATEGORY_LEARNING, false,
                "If true, baby villagers can be taught books.\n"
                        + "Off by default because a baby has no trade screen to sell them from, so\n"
                        + "the book vanishes into a villager you cannot buy from until it grows up.\n"
                        + "Turning this on works fine, it is just confusing."
        );

        ALLOW_MULTI_ENCHANT_BOOKS = config.getBoolean(
                "ALLOW_MULTI_ENCHANT_BOOKS", CATEGORY_LEARNING, true,
                "If true (default), a book carrying several enchantments teaches all of them\n"
                        + "at once for the price of one book. Each one still needs its own free slot,\n"
                        + "so a two-enchantment book handed to a villager with one slot left teaches\n"
                        + "neither and is not consumed.\n"
                        + "\n"
                        + "Set to false to reject multi-enchantment books outright. Worth doing if\n"
                        + "your pack generates them often and handing one over feels like cheating."
        );

        ENCHANTMENT_WHITELIST = config.getStringList(
                "ENCHANTMENT_WHITELIST", CATEGORY_LEARNING, new String[0],
                "If non-empty, ONLY these enchantments can ever be taught - one registry name\n"
                        + "per line, case-insensitive, minecraft: assumed when the colon is missing.\n"
                        + "Empty (default) allows everything that the other settings here permit.\n"
                        + "\n"
                        + "Examples: minecraft:mending, minecraft:unbreaking, minecraft:fortune,\n"
                        + "somanyenchantments:vigor. Enchantments from any mod work - the list is\n"
                        + "read from the registry at runtime, nothing is hard-coded.\n"
                        + "\n"
                        + "The blacklist is applied after this, so a name in both is blocked."
        );

        ENCHANTMENT_BLACKLIST = config.getStringList(
                "ENCHANTMENT_BLACKLIST", CATEGORY_LEARNING, new String[0],
                "Enchantments that can never be taught - one registry name per line, same\n"
                        + "format as the whitelist. Empty (default) blocks nothing.\n"
                        + "This is the setting to reach for when one enchantment in your pack is\n"
                        + "too strong to be freely buyable - put it here and it stays a drop."
        );

        ALLOW_TREASURE_ENCHANTMENTS = config.getBoolean(
                "ALLOW_TREASURE_ENCHANTMENTS", CATEGORY_LEARNING, true,
                "If true (default), treasure enchantments can be taught. Those are the ones\n"
                        + "the enchanting table will never give you - Mending, Frost Walker, Soul\n"
                        + "Speed and the curses, plus whatever your pack marks as treasure.\n"
                        + "\n"
                        + "ON by default, and Mending is the reason. Finding one Mending book and\n"
                        + "then being able to buy more is the single biggest thing this mod does for\n"
                        + "a long RLCraft run. Turn it off if you want treasure to stay one-per-find,\n"
                        + "and consider TREASURE_COST_MULTIPLIER as a middle ground - keep them\n"
                        + "buyable, just expensive."
        );

        ALLOW_CURSES = config.getBoolean(
                "ALLOW_CURSES", CATEGORY_LEARNING, true,
                "If true (default), Curse of Binding and Curse of Vanishing - and any modded\n"
                        + "curse - can be taught and sold.\n"
                        + "\n"
                        + "On by default because this mod's job is to turn books you already found\n"
                        + "into something useful, and deciding which of them count is not its call.\n"
                        + "A curse book is still a book somebody looted, some packs use curses\n"
                        + "deliberately, and refusing them just means the book stays dead weight.\n"
                        + "\n"
                        + "Set to false if you would rather a villager never offered one - a curse\n"
                        + "sitting in a trade list next to real enchantments is easy to buy by\n"
                        + "accident.\n"
                        + "Ignored entirely when ALLOW_TREASURE_ENCHANTMENTS is false, since every\n"
                        + "vanilla curse is also a treasure enchantment."
        );

        MAX_LEARNABLE_LEVEL = config.getInt(
                "MAX_LEARNABLE_LEVEL", CATEGORY_LEARNING, 0, 0, 255,
                "A hard ceiling on the level any villager will ever offer.\n"
                        + "\n"
                        + "  0  default - no flat ceiling. Each enchantment stops at its own natural\n"
                        + "     maximum instead: Unbreaking at III, Sharpness at V, and whatever the\n"
                        + "     mod that added it declares for modded ones.\n"
                        + "  1  villagers only ever sell level I books, whatever you hand them.\n"
                        + "  3  nothing above III, so Sharpness stops at III but Unbreaking is\n"
                        + "     unaffected.\n"
                        + "\n"
                        + "Applied on top of the natural maximum, never instead of it - setting 10\n"
                        + "does not get you Unbreaking X unless ALLOW_OVERLEVELING is also on."
        );

        ALLOW_OVERLEVELING = config.getBoolean(
                "ALLOW_OVERLEVELING", CATEGORY_LEARNING, false,
                "If true, villagers can be taught and can upgrade past an enchantment's natural\n"
                        + "maximum - Unbreaking IV, Sharpness VI and so on, up to MAX_LEARNABLE_LEVEL\n"
                        + "(which must be set to something above 0, or there is no ceiling at all and\n"
                        + "upgrades run to 255).\n"
                        + "\n"
                        + "OFF by default and worth leaving off. Over-levelled enchantments do work -\n"
                        + "the game reads the number and scales the effect - but nothing in vanilla\n"
                        + "balances for them, and a pack with mods that already push past the vanilla\n"
                        + "caps can produce genuinely silly results.\n"
                        + "\n"
                        + "Books above the natural maximum that you found elsewhere are still accepted\n"
                        + "when this is off; they are just clamped down to the maximum on the way in."
        );
    }

    // --------------------------------------------------------------- upgrading

    private static void loadUpgrading() {
        config.setCategoryComment(CATEGORY_UPGRADING,
                "What happens when you hand a villager a book it already knows.\n"
                        + "\n"
                        + "The default is anvil rules, which is what players already expect: two of\n"
                        + "the same level make the next one up. Give a villager Unbreaking II, come\n"
                        + "back later with another Unbreaking II, and its trade becomes Unbreaking\n"
                        + "III. A lower level than it already sells is refused and handed back.");
        config.setCategoryPropertyOrder(CATEGORY_UPGRADING, mutableOrder(ORDER_UPGRADING));

        ENABLE_UPGRADING = config.getBoolean(
                "ENABLE_UPGRADING", CATEGORY_UPGRADING, true,
                "If true (default), matching a villager's current level bumps its trade up one.\n"
                        + "Unbreaking II + Unbreaking II = Unbreaking III.\n"
                        + "\n"
                        + "Set to false to freeze every trade at the level it was first taught. A\n"
                        + "villager taught Unbreaking I sells Unbreaking I forever, and getting III\n"
                        + "means finding a III book and a villager with a free slot."
        );

        HIGHER_LEVEL_REPLACES = config.getBoolean(
                "HIGHER_LEVEL_REPLACES", CATEGORY_UPGRADING, true,
                "If true (default), handing over a book HIGHER than the villager's current level\n"
                        + "replaces it outright - a Sharpness IV book turns a Sharpness II trade\n"
                        + "straight into Sharpness IV. This does not stack with upgrading; the trade\n"
                        + "becomes exactly the level of the book you gave it, not one above.\n"
                        + "\n"
                        + "Set to false and higher books are refused as well, so the only way up is\n"
                        + "one level at a time through matching pairs. Strict, slow, and much closer\n"
                        + "to how an anvil actually behaves."
        );

        CONSUME_BOOK_ON_REJECT = config.getBoolean(
                "CONSUME_BOOK_ON_REJECT", CATEGORY_UPGRADING, false,
                "If true, a refused book is eaten anyway.\n"
                        + "Off by default and there is no good reason to turn it on except as a\n"
                        + "difficulty setting - normally a villager that cannot use your book gives\n"
                        + "it back and tells you why."
        );
    }

    // ----------------------------------------------------------------- pricing

    private static void loadPricing() {
        config.setCategoryComment(CATEGORY_PRICING,
                "What a taught trade costs.\n"
                        + "\n"
                        + "The formula is:\n"
                        + "  emeralds = (BASE_EMERALD_COST + EMERALDS_PER_LEVEL x level)\n"
                        + "             x rarity multiplier x treasure multiplier\n"
                        + "then rounded and clamped between MIN_EMERALD_COST and MAX_EMERALD_COST.\n"
                        + "\n"
                        + "Out of the box every multiplier is 1.0 and the base is 0, so the price is\n"
                        + "ten emeralds per level: Mending I costs 10, Unbreaking III costs 30,\n"
                        + "Sharpness V costs 50.\n"
                        + "\n"
                        + "Note where that runs out. A merchant trade can only ask for one stack, so\n"
                        + "64 is a hard ceiling - at ten per level everything from level 7 upwards\n"
                        + "costs the same 64 emeralds. That does not matter for vanilla enchantments,\n"
                        + "which stop at V, but a pack with mods that push past that will see high\n"
                        + "levels flatten out. Lower EMERALDS_PER_LEVEL if you want them to stay\n"
                        + "distinguishable.");
        config.setCategoryPropertyOrder(CATEGORY_PRICING, mutableOrder(ORDER_PRICING));

        BASE_EMERALD_COST = config.getInt(
                "BASE_EMERALD_COST", CATEGORY_PRICING, 0, 0, MAX_STACK,
                "Flat emerald cost added to every taught trade before the per-level part.\n"
                        + "0 (default) means the price is purely the level. Set to 5 and Unbreaking\n"
                        + "III costs 35 instead of 30 - a floor under every trade so even level I\n"
                        + "books are not the cheapest thing on the villager."
        );

        EMERALDS_PER_LEVEL = config.getFloat(
                "EMERALDS_PER_LEVEL", CATEGORY_PRICING, 10.0F, 0.0F, 64.0F,
                "Emeralds added per level of the enchantment. This is the main price dial.\n"
                        + "\n"
                        + "10.0 (default) means Mending I costs 10, Unbreaking III costs 30 and\n"
                        + "Sharpness V costs 50 - enough that buying a book is a real trip's worth of\n"
                        + "emeralds rather than pocket change, without being out of reach.\n"
                        + "\n"
                        + "Fractions are allowed: 2.5 gives 3, 5, 8, 10, 13 across levels I to V,\n"
                        + "since the total is rounded once at the end rather than per level.\n"
                        + "0 makes level irrelevant and every trade cost BASE_EMERALD_COST.\n"
                        + "\n"
                        + "Remember the 64 ceiling - see the note at the top of this category."
        );

        MIN_EMERALD_COST = config.getInt(
                "MIN_EMERALD_COST", CATEGORY_PRICING, 1, 1, MAX_STACK,
                "The cheapest a taught trade can ever be, whatever the formula works out to.\n"
                        + "Cannot go below 1: a merchant recipe has to ask for at least one item, so\n"
                        + "a free trade is not something the game can represent."
        );

        MAX_EMERALD_COST = config.getInt(
                "MAX_EMERALD_COST", CATEGORY_PRICING, MAX_STACK, 1, MAX_STACK,
                "The most expensive a taught trade can ever be.\n"
                        + "64 is the hard ceiling - the emerald price is a single stack in the trade\n"
                        + "screen and there is nowhere to put a 65th. Lower this if you want prices\n"
                        + "to stay in a range players can carry loose."
        );

        COST_MULTIPLIER_COMMON = config.getFloat(
                "COST_MULTIPLIER_COMMON", CATEGORY_PRICING, 1.0F, 0.0F, 64.0F,
                "Price multiplier for common enchantments - Efficiency, Sharpness, Protection,\n"
                        + "Unbreaking and the like. The bread-and-butter ones you find constantly."
        );

        COST_MULTIPLIER_UNCOMMON = config.getFloat(
                "COST_MULTIPLIER_UNCOMMON", CATEGORY_PRICING, 1.0F, 0.0F, 64.0F,
                "Price multiplier for uncommon enchantments - Fire Aspect, Knockback, Punch,\n"
                        + "Respiration, Aqua Affinity and similar."
        );

        COST_MULTIPLIER_RARE = config.getFloat(
                "COST_MULTIPLIER_RARE", CATEGORY_PRICING, 1.0F, 0.0F, 64.0F,
                "Price multiplier for rare enchantments - Looting, Fortune, Depth Strider,\n"
                        + "Flame, Infinity's neighbours."
        );

        COST_MULTIPLIER_VERY_RARE = config.getFloat(
                "COST_MULTIPLIER_VERY_RARE", CATEGORY_PRICING, 1.0F, 0.0F, 64.0F,
                "Price multiplier for very rare enchantments - Silk Touch, Thorns, Infinity,\n"
                        + "Mending, Luck of the Sea.\n"
                        + "Set this to 3.0 and Mending costs 30 emeralds instead of 10; combined with\n"
                        + "BASE_EMERALD_COST it is the cleanest way to make the good books expensive\n"
                        + "without touching anything else. Watch the 64 ceiling as you raise it.\n"
                        + "\n"
                        + "Which bucket an enchantment falls into is declared by whoever added it -\n"
                        + "vanilla or a mod - not by this config."
        );

        TREASURE_COST_MULTIPLIER = config.getFloat(
                "TREASURE_COST_MULTIPLIER", CATEGORY_PRICING, 1.0F, 0.0F, 64.0F,
                "An extra multiplier applied on top of the rarity one, for treasure\n"
                        + "enchantments only - the ones an enchanting table will never produce.\n"
                        + "Mending, Frost Walker, the curses, and whatever your pack marks as\n"
                        + "treasure.\n"
                        + "1.0 (default) treats them like anything else. This is the setting to raise\n"
                        + "if you want Mending buyable but genuinely costly - try 3.0 alongside a\n"
                        + "BASE_EMERALD_COST of 5."
        );

        EXTRA_INPUT_ITEM = config.getString(
                "EXTRA_INPUT_ITEM", CATEGORY_PRICING, "minecraft:book",
                "An item the trade asks for alongside the emeralds, given as a registry name.\n"
                        + "Default minecraft:book, matching how vanilla librarians sell books - you\n"
                        + "supply the paper, they supply the magic.\n"
                        + "\n"
                        + "This item takes the FIRST input slot and the emeralds take the second,\n"
                        + "which is the order vanilla's own enchanted book trades use. A taught trade\n"
                        + "therefore looks the same as a natural one at a glance.\n"
                        + "\n"
                        + "Leave blank for emeralds only, in which case the emeralds move to the first\n"
                        + "slot - a trade's first input is the one that cannot be empty. Any item\n"
                        + "works: minecraft:paper for something cheaper, minecraft:lapis_lazuli to tie\n"
                        + "it to enchanting, a modded item if your pack has a better fit.\n"
                        + "An item name the game does not recognise is ignored with a warning in the\n"
                        + "log, and the trade falls back to emeralds only rather than failing."
        );

        EXTRA_INPUT_COUNT = config.getInt(
                "EXTRA_INPUT_COUNT", CATEGORY_PRICING, 1, 0, MAX_STACK,
                "How many of EXTRA_INPUT_ITEM the trade asks for.\n"
                        + "0 disables the extra input the same way blanking the item name does.\n"
                        + "This is a flat count - it does not scale with the enchantment level."
        );

        MAX_TRADE_USES = config.getInt(
                "MAX_TRADE_USES", CATEGORY_PRICING, 16, 1, 9999,
                "How many times a taught trade can be used before it locks and needs the\n"
                        + "villager to restock.\n"
                        + "\n"
                        + "16 (default) is roughly double a vanilla trade, because a trade you had to\n"
                        + "find a book for should not run dry after seven purchases.\n"
                        + "\n"
                        + "IGNORED while NEVER_LOCK_TAUGHT_TRADES is on, which it is by default. Turn\n"
                        + "that off if you want this number to mean anything."
        );

        NEVER_LOCK_TAUGHT_TRADES = config.getBoolean(
                "NEVER_LOCK_TAUGHT_TRADES", CATEGORY_PRICING, true,
                "If true (default), trades this mod taught can be used forever and never lock.\n"
                        + "\n"
                        + "This exists because of a genuine 1.12 design problem. A villager only\n"
                        + "restocks as a side effect of being traded with - EntityVillager.useRecipe\n"
                        + "is the only place that schedules a refresh. So once every trade on a\n"
                        + "villager is exhausted there is no trade left to use, nothing can schedule\n"
                        + "the refresh, and that villager is bricked permanently. Later versions fixed\n"
                        + "this with workstations and twice-daily restocking; 1.12 never did.\n"
                        + "\n"
                        + "Rather than reimplement restocking, this keeps the trade's use limit ahead\n"
                        + "of its use count so the lock is never reached. You found the book - having\n"
                        + "the trade expire was never the interesting part.\n"
                        + "\n"
                        + "Only affects trades this mod added. See NEVER_LOCK_ANY_TRADE for the rest."
        );

        NEVER_LOCK_ANY_TRADE = config.getBoolean(
                "NEVER_LOCK_ANY_TRADE", CATEGORY_PRICING, false,
                "If true, NO trade on any villager you interact with ever locks - including\n"
                        + "every ordinary vanilla and modded trade, not just the ones taught here.\n"
                        + "\n"
                        + "This is the blunt fix for the bricked-villager problem described above, and\n"
                        + "it is off by default because it is a real change to how the whole pack's\n"
                        + "economy works. Emerald farming stops having any ceiling at all: one\n"
                        + "villager buying wheat becomes an infinite emerald tap.\n"
                        + "\n"
                        + "Applies as you interact - a villager is unlocked when you click it, not\n"
                        + "world-wide on a timer - and it never creates a trade list on a villager\n"
                        + "that has not generated one yet."
        );

        TRADE_GRANTS_XP = config.getBoolean(
                "TRADE_GRANTS_XP", CATEGORY_PRICING, true,
                "If true (default), using a taught trade drops experience orbs for the player,\n"
                        + "the same as any normal villager trade.\n"
                        + "Set to false and taught trades pay no experience at all. This does not\n"
                        + "stop the villager restocking or levelling up - that is driven by the trade\n"
                        + "being used, not by the experience."
        );
    }

    // ---------------------------------------------------------------- feedback

    private static void loadFeedback() {
        config.setCategoryComment(CATEGORY_FEEDBACK,
                "What the mod tells you when a villager takes, upgrades or refuses a book.");
        config.setCategoryPropertyOrder(CATEGORY_FEEDBACK, mutableOrder(ORDER_FEEDBACK));

        ANNOUNCE_LEARNED = config.getBoolean(
                "ANNOUNCE_LEARNED", CATEGORY_FEEDBACK, true,
                "If true (default), say in chat what the villager learned and how many slots it\n"
                        + "has left."
        );

        ANNOUNCE_UPGRADED = config.getBoolean(
                "ANNOUNCE_UPGRADED", CATEGORY_FEEDBACK, true,
                "If true (default), say in chat when a trade levels up, and what it went from\n"
                        + "and to."
        );

        ANNOUNCE_REJECTED = config.getBoolean(
                "ANNOUNCE_REJECTED", CATEGORY_FEEDBACK, true,
                "If true (default), explain in chat why a book was handed back - out of slots,\n"
                        + "blacklisted, already at a higher level, and so on.\n"
                        + "Worth keeping on: without it a refused book looks like the mod is broken."
        );

        PLAY_SOUNDS = config.getBoolean(
                "PLAY_SOUNDS", CATEGORY_FEEDBACK, true,
                "If true (default), the villager agrees or disagrees out loud when handed a\n"
                        + "book - the same yes and no noises it makes while trading."
        );

        SPAWN_PARTICLES = config.getBoolean(
                "SPAWN_PARTICLES", CATEGORY_FEEDBACK, true,
                "If true (default), green sparkles on a book accepted, angry puffs on one\n"
                        + "refused."
        );

        DEBUG_LOGGING = config.getBoolean(
                "DEBUG_LOGGING", CATEGORY_FEEDBACK, false,
                "If true, write a line to the log every time this mod looks at a villager -\n"
                        + "what it knows, how many trades the villager had before and after, and\n"
                        + "which branch the click took.\n"
                        + "Off by default because it is noisy. Turn it on when a taught trade is not\n"
                        + "showing up and the log will say which step went wrong."
        );
    }

    // ------------------------------------------------------------------------

    /**
     * Forge's own range checking covers the config GUI, but a hand-edited .cfg can contain
     * anything at all, so every value is clamped again here. The derived lookup sets are
     * rebuilt at the same time.
     */
    private static void clampAndDerive() {
        if (MAX_TOMES_PER_VILLAGER < 1) MAX_TOMES_PER_VILLAGER = 1;
        if (MAX_LEARNABLE_LEVEL < 0) MAX_LEARNABLE_LEVEL = 0;

        if (BASE_EMERALD_COST < 0) BASE_EMERALD_COST = 0;
        if (BASE_EMERALD_COST > MAX_STACK) BASE_EMERALD_COST = MAX_STACK;
        if (EMERALDS_PER_LEVEL < 0.0F) EMERALDS_PER_LEVEL = 0.0F;
        if (MIN_EMERALD_COST < 1) MIN_EMERALD_COST = 1;
        if (MIN_EMERALD_COST > MAX_STACK) MIN_EMERALD_COST = MAX_STACK;
        if (MAX_EMERALD_COST > MAX_STACK) MAX_EMERALD_COST = MAX_STACK;
        // A minimum above the maximum is a contradiction with no sensible reading, so the
        // maximum gives way - a player who set a floor of 10 clearly wants trades to cost
        // at least 10.
        if (MAX_EMERALD_COST < MIN_EMERALD_COST) MAX_EMERALD_COST = MIN_EMERALD_COST;

        if (EXTRA_INPUT_COUNT < 0) EXTRA_INPUT_COUNT = 0;
        if (EXTRA_INPUT_COUNT > MAX_STACK) EXTRA_INPUT_COUNT = MAX_STACK;
        if (MAX_TRADE_USES < 1) MAX_TRADE_USES = 1;

        if (!TRIGGER_SNEAK_RIGHT_CLICK.equals(TEACH_TRIGGER)) {
            TEACH_TRIGGER = TRIGGER_RIGHT_CLICK;
        }

        allowedProfessions = toLookupSet(ALLOWED_PROFESSIONS);
        enchantmentWhitelist = toLookupSet(ENCHANTMENT_WHITELIST);
        enchantmentBlacklist = toLookupSet(ENCHANTMENT_BLACKLIST);
        allowedProfessionsLabel = describeProfessions(ALLOWED_PROFESSIONS);

        extraInput = resolveExtraInput();
    }

    /**
     * Turns the raw profession list into something worth putting in a chat message -
     * "librarians", or "librarians or priests".
     *
     * <p>Built from the raw array rather than the lookup set so the order the player wrote
     * is preserved; the set is a HashSet and would reorder them.
     *
     * <p>Pluralising by adding an "s" is crude and works for every vanilla profession. A
     * modded profession with an awkward name gets a slightly wrong plural in one chat
     * message, which is a fair trade for not shipping a dictionary.
     */
    private static String describeProfessions(String[] professions) {
        List<String> names = new ArrayList<String>();
        for (String profession : professions) {
            if (profession == null || profession.trim().isEmpty()) {
                continue;
            }
            String name = profession.trim();
            int colon = name.indexOf(':');
            if (colon >= 0) {
                name = name.substring(colon + 1);
            }
            name = name.toLowerCase(Locale.ROOT).replace('_', ' ');
            names.add(name.endsWith("s") ? name : name + "s");
        }
        if (names.isEmpty()) {
            return null;
        }
        if (names.size() == 1) {
            return names.get(0);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(i == names.size() - 1 ? " or " : ", ");
            }
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    /**
     * Who will accept books, phrased for a player, or null when everybody will.
     *
     * <p>Exists because the default now refuses most villagers, so the refusal message has
     * to say who to go and find instead of only saying no.
     */
    public static String getAllowedProfessionsLabel() {
        return allowedProfessionsLabel;
    }

    /**
     * Normalises a config string list into something cheap to test against: trimmed,
     * lower-cased, blanks dropped, and a bare name expanded to the minecraft namespace so
     * that "mending" and "minecraft:mending" both work.
     */
    private static Set<String> toLookupSet(String[] values) {
        Set<String> set = new HashSet<String>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                continue;
            }
            set.add(trimmed.indexOf(':') < 0 ? "minecraft:" + trimmed : trimmed);
        }
        return set;
    }

    /**
     * Turns {@link #EXTRA_INPUT_ITEM} into the stack the trade will ask for.
     *
     * <p>An unrecognised item name is a warning and an empty stack rather than a crash.
     * Config strings naming other mods' items are the single most likely thing in this
     * file to be wrong after a pack update, and losing one input item is a much better
     * outcome than a world that will not load.
     */
    private static ItemStack resolveExtraInput() {
        if (EXTRA_INPUT_COUNT <= 0 || EXTRA_INPUT_ITEM == null
                || EXTRA_INPUT_ITEM.trim().isEmpty()) {
            return ItemStack.EMPTY;
        }
        String name = EXTRA_INPUT_ITEM.trim();
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
        if (item == null) {
            RLCraftVillagerTomes.LOGGER.warn(
                    "EXTRA_INPUT_ITEM '{}' is not an item any loaded mod registers - taught trades "
                            + "will ask for emeralds only. Check the spelling, or blank the setting to "
                            + "silence this.", name);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, EXTRA_INPUT_COUNT);
    }

    /**
     * One entry per category, so the config screen opens on four labelled groups rather
     * than a single wall of options.
     */
    public static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        for (String category : CATEGORIES) {
            elements.add(new ConfigElement(config.getCategory(category)));
        }
        return elements;
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(RLCraftVillagerTomes.MODID)) {
            // Everything is read live at interaction time and trades are rebuilt whenever a
            // villager is opened, so a reload is enough - nothing here needs a restart.
            // Price changes reach existing villagers the next time somebody talks to them.
            loadConfig();
        }
    }

    // ------------------------------------------------------------- lookups

    /** True when a villager of this profession registry name is willing to be taught. */
    public static boolean isProfessionAllowed(ResourceLocation profession) {
        if (allowedProfessions.isEmpty()) {
            return true;
        }
        // A profession with no registry name is broken enough that guessing is worse than
        // refusing; the player still gets their book back.
        return profession != null
                && allowedProfessions.contains(profession.toString().toLowerCase(Locale.ROOT));
    }

    /**
     * Whether an enchantment is teachable at all, before any level or slot checks.
     *
     * <p>Order matters: whitelist first as the broad gate, then blacklist as the override,
     * then the treasure and curse switches. That way a whitelisted curse still obeys
     * ALLOW_CURSES rather than sneaking through.
     */
    public static boolean isEnchantmentAllowed(Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }
        ResourceLocation name = enchantment.getRegistryName();
        if (name == null) {
            return false;
        }
        String key = name.toString().toLowerCase(Locale.ROOT);
        if (!enchantmentWhitelist.isEmpty() && !enchantmentWhitelist.contains(key)) {
            return false;
        }
        if (enchantmentBlacklist.contains(key)) {
            return false;
        }
        if (!ALLOW_TREASURE_ENCHANTMENTS && enchantment.isTreasureEnchantment()) {
            return false;
        }
        if (!ALLOW_CURSES && enchantment.isCurse()) {
            return false;
        }
        return true;
    }

    /**
     * The highest level a villager may hold for this enchantment.
     *
     * <p>With overlevelling off this is the enchantment's own maximum, optionally lowered
     * by {@link #MAX_LEARNABLE_LEVEL}. With it on the flat ceiling becomes the only limit,
     * and 255 when there isn't one - which is the point of the warning on that setting.
     */
    public static int getMaxLevel(Enchantment enchantment) {
        int natural = enchantment.getMaxLevel();
        int ceiling = MAX_LEARNABLE_LEVEL > 0 ? MAX_LEARNABLE_LEVEL : 255;
        if (ALLOW_OVERLEVELING) {
            return Math.max(1, ceiling);
        }
        return Math.max(1, Math.min(natural, ceiling));
    }

    /** The emerald price of one taught trade. Always between 1 and 64. */
    public static int getEmeraldCost(Enchantment enchantment, int level) {
        float cost = BASE_EMERALD_COST + EMERALDS_PER_LEVEL * level;
        cost *= getRarityMultiplier(enchantment);
        if (enchantment.isTreasureEnchantment()) {
            cost *= TREASURE_COST_MULTIPLIER;
        }
        int rounded = Math.round(cost);
        if (rounded < MIN_EMERALD_COST) rounded = MIN_EMERALD_COST;
        if (rounded > MAX_EMERALD_COST) rounded = MAX_EMERALD_COST;
        return rounded;
    }

    private static float getRarityMultiplier(Enchantment enchantment) {
        switch (enchantment.getRarity()) {
            case COMMON:
                return COST_MULTIPLIER_COMMON;
            case UNCOMMON:
                return COST_MULTIPLIER_UNCOMMON;
            case RARE:
                return COST_MULTIPLIER_RARE;
            case VERY_RARE:
                return COST_MULTIPLIER_VERY_RARE;
            default:
                // Only reachable if a future Minecraft or a coremod adds a rarity bucket.
                // Charging the common rate is the least surprising thing to do about it.
                return COST_MULTIPLIER_COMMON;
        }
    }

    /**
     * The extra item a taught trade asks for, or an empty stack when there is none.
     *
     * <p>A copy every time: merchant recipes hold onto the stack they are given, and
     * handing the same instance to every recipe on every villager in the world is how you
     * end up with one of them mutating the rest.
     */
    public static ItemStack getExtraInput() {
        return extraInput.isEmpty() ? ItemStack.EMPTY : extraInput.copy();
    }
}
