package com.exiledradio.rlcraftvillagertomes;

import com.exiledradio.rlcraftvillagertomes.catalyst.CatalystRegistry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
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
    /** The rarity tier list that drives catalysts and slot requests. */
    public static final String CATEGORY_CATALYSTS = "catalysts";
    /** Whether teaching succeeds at all, and what moves the odds. */
    public static final String CATEGORY_CHANCE = "chance";

    /** Display order in the config screen. Without this the GUI sorts alphabetically. */
    private static final String[] CATEGORIES = {
            CATEGORY_LEARNING, CATEGORY_CHANCE, CATEGORY_UPGRADING, CATEGORY_PRICING,
            CATEGORY_CATALYSTS, CATEGORY_FEEDBACK,
    };

    private static final List<String> ORDER_LEARNING = Arrays.asList(
            "ENABLE_LEARNING", "TEACH_TRIGGER", "MAX_TOMES_PER_VILLAGER",
            "ALLOWED_PROFESSIONS", "TEACH_BABY_VILLAGERS", "ALLOW_MULTI_ENCHANT_BOOKS",
            "ENCHANTMENT_WHITELIST", "ENCHANTMENT_BLACKLIST", "ALLOW_TREASURE_ENCHANTMENTS",
            "ALLOW_CURSES", "MAX_LEARNABLE_LEVEL", "ALLOW_OVERLEVELING");

    private static final List<String> ORDER_UPGRADING = Arrays.asList(
            "UPGRADE_MODE", "UPGRADE_TAKES_NEW_SLOT", "CONSUME_BOOK_ON_REJECT");

    private static final List<String> ORDER_PRICING = Arrays.asList(
            "BASE_EMERALD_COST", "EMERALDS_PER_LEVEL", "MIN_EMERALD_COST", "MAX_EMERALD_COST",
            "COST_MULTIPLIER_COMMON", "COST_MULTIPLIER_UNCOMMON", "COST_MULTIPLIER_RARE",
            "COST_MULTIPLIER_VERY_RARE", "TREASURE_COST_MULTIPLIER",
            "EXTRA_INPUT_ITEM", "EXTRA_INPUT_COUNT", "MAX_TRADE_USES",
            "NEVER_LOCK_TAUGHT_TRADES", "NEVER_LOCK_ANY_TRADE", "TRADE_GRANTS_XP");

    private static final List<String> ORDER_CHANCE = Arrays.asList(
            "ENABLE_CHANCE", "BASE_SUCCESS_CHANCE", "CHANCE_PER_SLOT", "MAX_SUCCESS_CHANCE",
            "MIN_SUCCESS_CHANCE",
            "PITY_PER_FAILURE", "PITY_CAP", "CONSUME_BOOK_ON_FAILURE",
            "CONSUME_CATALYSTS_ON_FAILURE");

    private static final List<String> ORDER_CATALYSTS = Arrays.asList(
            "CATALYST_TIERS", "CATALYST_ITEMS");

    private static final List<String> ORDER_FEEDBACK = Arrays.asList(
            "ANNOUNCE_LEARNED", "ANNOUNCE_UPGRADED", "ANNOUNCE_REJECTED",
            "PLAY_SOUNDS", "SPAWN_PARTICLES", "DEBUG_LOGGING");

    /** Valid values for {@link #TEACH_TRIGGER}, also shown as a dropdown in the GUI. */
    public static final String TRIGGER_RIGHT_CLICK = "right_click";
    public static final String TRIGGER_SNEAK_RIGHT_CLICK = "sneak_right_click";
    private static final String[] TRIGGER_VALUES = {TRIGGER_RIGHT_CLICK, TRIGGER_SNEAK_RIGHT_CLICK};

    /** A book above the current level replaces it; a matching pair also steps it up by one. */
    public static final String UPGRADE_PAIR_OR_HIGHER = "pair_or_higher";
    /** Only a book above the current level counts. Matching pairs are refused. */
    public static final String UPGRADE_HIGHER_ONLY = "higher_only";
    /** Only a matching pair counts. A higher book is refused. */
    public static final String UPGRADE_PAIR_ONLY = "pair_only";
    /** Trades are frozen at whatever level they were first taught. */
    public static final String UPGRADE_OFF = "off";
    private static final String[] UPGRADE_MODE_VALUES = {
            UPGRADE_HIGHER_ONLY, UPGRADE_PAIR_OR_HIGHER, UPGRADE_PAIR_ONLY, UPGRADE_OFF};

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
        for (String key : ORDER_CHANCE) CATEGORY_OF_KEY.put(key, CATEGORY_CHANCE);
        for (String key : ORDER_CATALYSTS) CATEGORY_OF_KEY.put(key, CATEGORY_CATALYSTS);
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
    public static String UPGRADE_MODE = UPGRADE_HIGHER_ONLY;
    public static boolean UPGRADE_TAKES_NEW_SLOT = false;
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

    // chance
    public static boolean ENABLE_CHANCE = true;
    public static float BASE_SUCCESS_CHANCE = 30.0F;
    public static float CHANCE_PER_SLOT = 10.0F;
    public static float MAX_SUCCESS_CHANCE = 80.0F;
    public static float MIN_SUCCESS_CHANCE = 1.0F;
    public static float PITY_PER_FAILURE = 5.0F;
    public static float PITY_CAP = 70.0F;
    public static boolean CONSUME_BOOK_ON_FAILURE = true;
    public static boolean CONSUME_CATALYSTS_ON_FAILURE = true;

    // catalysts
    public static String[] CATALYST_TIERS = {
            "common=1.0,8-24",
            "uncommon=2.5,4-12",
            "rare=5.0,2-8",
            "epic=10.0,1-4",
            "legendary=15.0,1-2",
            "mythic=30.0,1-1",
    };
    public static String[] CATALYST_ITEMS = {
            "xat:glowing_powder=common",
            "xat:glowing_ingot=rare",
            "xat:glowing_gem=mythic",
    };

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
        migrateUpgradeBooleans();
        // Must run before the loaders: they call setCategoryPropertyOrder, which appends
        // any key the order list does not mention, and that call throws on a fixed-size
        // list the moment a stale key exists. It is also what deletes the old keys the
        // migration above has just finished reading.
        pruneUnknownKeys();

        loadLearning();
        loadChance();
        loadUpgrading();
        loadPricing();
        loadCatalysts();
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
     * Folds the old ENABLE_UPGRADING / HIGHER_LEVEL_REPLACES pair into UPGRADE_MODE.
     *
     * <p>The two booleans expressed exactly the four behaviours the mode now names, so this
     * is lossless - it is a rename, not a behaviour change, and somebody who had deliberately
     * turned upgrading off keeps it off rather than silently picking up the new default.
     *
     * <pre>
     *   pair   higher  ->  mode
     *   true   true        pair_or_higher
     *   true   false       pair_only
     *   false  true        higher_only
     *   false  false       off
     * </pre>
     */
    private static void migrateUpgradeBooleans() {
        if (!config.hasCategory(CATEGORY_UPGRADING)) {
            return;
        }
        ConfigCategory upgrading = config.getCategory(CATEGORY_UPGRADING);
        boolean hasOld = upgrading.containsKey("ENABLE_UPGRADING")
                || upgrading.containsKey("HIGHER_LEVEL_REPLACES");
        if (!hasOld || upgrading.containsKey("UPGRADE_MODE")) {
            return;
        }

        // A missing half of the pair takes its old default, which was true for both.
        boolean fromPair = !upgrading.containsKey("ENABLE_UPGRADING")
                || upgrading.get("ENABLE_UPGRADING").getBoolean(true);
        boolean fromHigher = !upgrading.containsKey("HIGHER_LEVEL_REPLACES")
                || upgrading.get("HIGHER_LEVEL_REPLACES").getBoolean(true);

        String mode = fromPair
                ? (fromHigher ? UPGRADE_PAIR_OR_HIGHER : UPGRADE_PAIR_ONLY)
                : (fromHigher ? UPGRADE_HIGHER_ONLY : UPGRADE_OFF);

        upgrading.put("UPGRADE_MODE", new Property("UPGRADE_MODE", mode, Property.Type.STRING));
        RLCraftVillagerTomes.LOGGER.info(
                "ENABLE_UPGRADING and HIGHER_LEVEL_REPLACES have been replaced by the single "
                        + "UPGRADE_MODE setting. Yours carried over as '{}', so upgrading behaves "
                        + "exactly as it did before.", mode);
        // The two obsolete keys are cleared by pruneUnknownKeys once this returns.
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
                        + "Counts TRADES. With UPGRADE_TAKES_NEW_SLOT off, which is the default, that\n"
                        + "is the same as counting distinct enchantments - upgrading replaces a trade\n"
                        + "rather than adding one, so a villager at its cap can still be improved.\n"
                        + "Turn that setting on and every level is its own trade and its own slot.\n"
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
                        + "A book BELOW the villager's current level is always refused and handed\n"
                        + "back, in every mode. Nothing here can make you lose a book.");
        config.setCategoryPropertyOrder(CATEGORY_UPGRADING, mutableOrder(ORDER_UPGRADING));

        UPGRADE_MODE = config.getString(
                "UPGRADE_MODE", CATEGORY_UPGRADING, UPGRADE_HIGHER_ONLY,
                "How a taught trade moves up a level.\n"
                        + "\n"
                        + "  higher_only     default. Only a book ABOVE the current level counts, and\n"
                        + "                  the trade becomes exactly that book's level. A matching\n"
                        + "                  book is refused.\n"
                        + "                  So pushing a villager from Unbreaking II to III means\n"
                        + "                  buying two Unbreaking II from it, combining them at an\n"
                        + "                  anvil yourself, and bringing the III back. Every level\n"
                        + "                  costs an anvil trip and the experience to pay for it,\n"
                        + "                  and the villager cannot bootstrap itself upward on\n"
                        + "                  copies of what it already sells.\n"
                        + "\n"
                        + "  pair_or_higher  two of the SAME level step the trade up by one, and a\n"
                        + "                  higher book still replaces it outright. The most\n"
                        + "                  convenient option: no anvil needed, no experience cost.\n"
                        + "\n"
                        + "  pair_only       two of the same level step it up by one, and a higher\n"
                        + "                  book is refused. The only way up is one level at a time\n"
                        + "                  through matching pairs.\n"
                        + "\n"
                        + "  off             trades freeze at the level they were first taught.\n"
                        + "                  Getting Unbreaking III means finding a III book and a\n"
                        + "                  villager with a free slot.\n"
                        + "\n"
                        + "Replaces the old ENABLE_UPGRADING and HIGHER_LEVEL_REPLACES pair, which\n"
                        + "expressed these same four behaviours but gave no clue which combination\n"
                        + "produced which. An existing config is carried over automatically.",
                UPGRADE_MODE_VALUES
        );

        UPGRADE_TAKES_NEW_SLOT = config.getBoolean(
                "UPGRADE_TAKES_NEW_SLOT", CATEGORY_UPGRADING, false,
                "If true, levelling a trade up keeps the old one instead of replacing it, and\n"
                        + "costs another slot.\n"
                        + "\n"
                        + "A villager that has been pushed from Unbreaking II to III ends up selling\n"
                        + "BOTH - two trades, two of its MAX_TOMES_PER_VILLAGER slots gone. It is the\n"
                        + "difference between a slot meaning 'an enchantment this villager deals in'\n"
                        + "and a slot meaning 'one trade on the board'.\n"
                        + "\n"
                        + "OFF by default, which keeps upgrading free: the old trade is replaced and\n"
                        + "a villager at its cap can still be improved.\n"
                        + "\n"
                        + "Turn it on to make levelling genuinely expensive. With five slots you can\n"
                        + "have five enchantments at level I, or one at level V and nothing else, and\n"
                        + "that trade-off is the point. Be aware it fills villagers fast, and that a\n"
                        + "villager with no free slot cannot be levelled up at all - it will say so\n"
                        + "rather than quietly replacing something."
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

    // ------------------------------------------------------------------ chance

    private static void loadChance() {
        config.setCategoryComment(CATEGORY_CHANCE,
                "Whether a book actually takes, and what moves the odds.\n"
                        + "\n"
                        + "The loop: bank catalyst items into a villager to raise its odds, check\n"
                        + "where you stand by sneak-clicking it empty-handed, then commit the book.\n"
                        + "A failed attempt destroys the book and the catalysts you banked, and\n"
                        + "raises the floor for that enchantment on that villager - so a bad run is\n"
                        + "expensive but never endless.\n"
                        + "\n"
                        + "The whole system is one switch away from the old guaranteed behaviour.\n"
                        + "See ENABLE_CHANCE.");
        config.setCategoryPropertyOrder(CATEGORY_CHANCE, mutableOrder(ORDER_CHANCE));

        ENABLE_CHANCE = config.getBoolean(
                "ENABLE_CHANCE", CATEGORY_CHANCE, true,
                "If true (default), teaching a book is a roll that can fail.\n"
                        + "\n"
                        + "Set to false and every book is accepted outright, exactly as it worked\n"
                        + "before this system existed. Catalysts are then never asked for and never\n"
                        + "consumed. This is the switch to reach for if you want the mod's original\n"
                        + "no-gambling behaviour back without reinstalling an old version."
        );

        BASE_SUCCESS_CHANCE = config.getFloat(
                "BASE_SUCCESS_CHANCE", CATEGORY_CHANCE, 50.0F, 0.0F, 100.0F,
                "The chance a book is accepted with nothing banked, as a percentage.\n"
                        + "50 (default) makes an unaided attempt a coin flip, which is what makes\n"
                        + "catalysts worth gathering at all."
        );

        MAX_SUCCESS_CHANCE = config.getFloat(
                "MAX_SUCCESS_CHANCE", CATEGORY_CHANCE, 80.0F, 1.0F, 100.0F,
                "The highest chance any amount of catalysts can reach, as a percentage.\n"
                        + "\n"
                        + "80 (default) deliberately leaves teaching a gamble no matter how much you\n"
                        + "pour into it - one attempt in five still fails at the ceiling. Set to 100\n"
                        + "if you would rather enough preparation guarantee the result.\n"
                        + "\n"
                        + "Banking beyond this is refused rather than wasted: a villager already at\n"
                        + "the ceiling hands your catalyst back."
        );

        MIN_SUCCESS_CHANCE = config.getFloat(
                "MIN_SUCCESS_CHANCE", CATEGORY_CHANCE, 1.0F, 0.0F, 100.0F,
                "The lowest chance an attempt can ever have, as a percentage.\n"
                        + "Only reachable if you set BASE_SUCCESS_CHANCE very low; it exists so a\n"
                        + "misconfigured base cannot make books impossible to teach."
        );

        PITY_PER_FAILURE = config.getFloat(
                "PITY_PER_FAILURE", CATEGORY_CHANCE, 5.0F, 0.0F, 100.0F,
                "How many percentage points the floor rises each time an attempt fails, for\n"
                        + "that enchantment on that villager.\n"
                        + "\n"
                        + "Tracked per villager AND per enchantment, so failing Mending on one\n"
                        + "librarian makes Mending easier on that librarian only - not Unbreaking,\n"
                        + "and not on the librarian next door. Committing to a villager is what pays\n"
                        + "off, which is the same thing the permanently locked slots are asking of\n"
                        + "you.\n"
                        + "\n"
                        + "Success clears the count. Set to 0 to remove the mercy rule entirely and\n"
                        + "let bad luck run forever."
        );

        PITY_CAP = config.getFloat(
                "PITY_CAP", CATEGORY_CHANCE, 70.0F, 0.0F, 100.0F,
                "The highest the failure floor alone can push an attempt, as a percentage.\n"
                        + "\n"
                        + "70 (default) sits below MAX_SUCCESS_CHANCE on purpose: persistence alone\n"
                        + "gets you close, but the last stretch to the ceiling still costs catalysts.\n"
                        + "Raise it to 80 and enough failures eventually buy the ceiling for free."
        );

        CONSUME_BOOK_ON_FAILURE = config.getBoolean(
                "CONSUME_BOOK_ON_FAILURE", CATEGORY_CHANCE, true,
                "If true (default), a failed attempt destroys the book.\n"
                        + "This is what gives the roll teeth. Set to false and a failure costs you\n"
                        + "only the catalysts, which makes the whole thing much gentler."
        );

        CONSUME_CATALYSTS_ON_FAILURE = config.getBoolean(
                "CONSUME_CATALYSTS_ON_FAILURE", CATEGORY_CHANCE, true,
                "If true (default), a failed attempt also burns everything you had banked, and\n"
                        + "the villager drops back to its base chance.\n"
                        + "Set to false and banked catalysts survive a failure, so a villager you\n"
                        + "have invested in stays primed for the next book."
        );
    }

    // --------------------------------------------------------------- catalysts

    private static void loadCatalysts() {
        config.setCategoryComment(CATEGORY_CATALYSTS,
                "The rarity tier list.\n"
                        + "\n"
                        + "Nothing here does anything on its own yet - it is the data the chance and\n"
                        + "slot-request systems are built on. Filling it in now means those arrive\n"
                        + "already tuned for your pack.\n"
                        + "\n"
                        + "One tier definition drives both halves of it. The percentage is what an\n"
                        + "item is worth when banked into a villager to improve a teaching attempt;\n"
                        + "the count range is how many of it a villager asks for when it rolls a slot\n"
                        + "request. Splitting tiers from items means 'how much is rare worth' is one\n"
                        + "edit rather than a hunt through every rare item.\n"
                        + "\n"
                        + "Run /villagertomes tiers in game to see exactly what was parsed, which\n"
                        + "items were found, and what tier the item in your hand belongs to.");
        config.setCategoryPropertyOrder(CATEGORY_CATALYSTS, mutableOrder(ORDER_CATALYSTS));

        CATALYST_TIERS = config.getStringList(
                "CATALYST_TIERS", CATEGORY_CATALYSTS, CATALYST_TIERS,
                "Rarity bands, one per line, as name=percent,min-max\n"
                        + "\n"
                        + "  name     lower-case identifier the item list refers to.\n"
                        + "  percent  percentage points one of these adds to a teaching attempt.\n"
                        + "  min-max  how many a villager asks for when it wants one of these.\n"
                        + "\n"
                        + "So common=1.0,8-24 means a common item is worth +1% and gets asked for in\n"
                        + "batches of 8 to 24 - the emerald x18 end of a request. legendary=15.0,1-2\n"
                        + "is the single-item end.\n"
                        + "\n"
                        + "Names are yours to choose; nothing is hard-coded. A malformed line is\n"
                        + "named in the log and skipped, never a crash."
        );

        CATALYST_ITEMS = config.getStringList(
                "CATALYST_ITEMS", CATEGORY_CATALYSTS, CATALYST_ITEMS,
                "Which items belong to which tier, one per line, as modid:item=tier\n"
                        + "\n"
                        + "Metadata is optional: minecraft:dye:4=rare picks out lapis specifically,\n"
                        + "while minecraft:dye=common covers every colour. An exact metadata match\n"
                        + "beats an any-metadata one, so you can tier a whole item and then single\n"
                        + "out one variant as worth more.\n"
                        + "\n"
                        + "NBT is ignored, so an enchanted or renamed copy of a material still counts.\n"
                        + "\n"
                        + "The defaults are the Trinkets and Baubles crafting ladder, whose own\n"
                        + "tooltips call them Tier 1, 2 and 3. Items no loaded mod registers are\n"
                        + "skipped with one summary line in the log, so a list written for a full pack\n"
                        + "is safe to carry to a smaller one."
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

        // A ceiling below the base would make every attempt impossible, and a pity cap below
        // the base would silently subtract from it - both are contradictions with no sensible
        // reading, so the base wins and the other value is pulled up to meet it.
        if (BASE_SUCCESS_CHANCE < 0.0F) BASE_SUCCESS_CHANCE = 0.0F;
        if (BASE_SUCCESS_CHANCE > 100.0F) BASE_SUCCESS_CHANCE = 100.0F;
        if (MAX_SUCCESS_CHANCE < BASE_SUCCESS_CHANCE) MAX_SUCCESS_CHANCE = BASE_SUCCESS_CHANCE;
        if (MAX_SUCCESS_CHANCE > 100.0F) MAX_SUCCESS_CHANCE = 100.0F;
        if (MIN_SUCCESS_CHANCE > MAX_SUCCESS_CHANCE) MIN_SUCCESS_CHANCE = MAX_SUCCESS_CHANCE;
        if (MIN_SUCCESS_CHANCE < 0.0F) MIN_SUCCESS_CHANCE = 0.0F;
        if (CHANCE_PER_SLOT < 0.0F) CHANCE_PER_SLOT = 0.0F;
        if (PITY_PER_FAILURE < 0.0F) PITY_PER_FAILURE = 0.0F;
        if (PITY_CAP < BASE_SUCCESS_CHANCE) PITY_CAP = BASE_SUCCESS_CHANCE;
        if (PITY_CAP > 100.0F) PITY_CAP = 100.0F;

        if (EXTRA_INPUT_COUNT < 0) EXTRA_INPUT_COUNT = 0;
        if (EXTRA_INPUT_COUNT > MAX_STACK) EXTRA_INPUT_COUNT = MAX_STACK;
        if (MAX_TRADE_USES < 1) MAX_TRADE_USES = 1;

        if (!TRIGGER_SNEAK_RIGHT_CLICK.equals(TEACH_TRIGGER)) {
            TEACH_TRIGGER = TRIGGER_RIGHT_CLICK;
        }

        if (!Arrays.asList(UPGRADE_MODE_VALUES).contains(UPGRADE_MODE)) {
            RLCraftVillagerTomes.LOGGER.warn(
                    "UPGRADE_MODE '{}' is not one of {} - falling back to '{}'.",
                    UPGRADE_MODE, Arrays.toString(UPGRADE_MODE_VALUES), UPGRADE_HIGHER_ONLY);
            UPGRADE_MODE = UPGRADE_HIGHER_ONLY;
        }

        allowedProfessions = toLookupSet(ALLOWED_PROFESSIONS);
        enchantmentWhitelist = toLookupSet(ENCHANTMENT_WHITELIST);
        enchantmentBlacklist = toLookupSet(ENCHANTMENT_BLACKLIST);
        allowedProfessionsLabel = describeProfessions(ALLOWED_PROFESSIONS);

        extraInput = resolveExtraInput();

        // Syntax is checked here; the item names are looked up later, once every mod has
        // finished registering. See CatalystRegistry for why those are two separate steps.
        CatalystRegistry.reload(CATALYST_TIERS, CATALYST_ITEMS);
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

    /**
     * The floor an attempt starts from, before banked catalysts.
     *
     * <p>Base, plus the pity earned by failing this enchantment on this villager, capped by
     * {@link #PITY_CAP}. Deliberately separate from the banked half so both the chat readout
     * and the roll can be built from the same two pieces and cannot disagree.
     */
    public static float getFloorChance(int slotsFilled, int failures) {
        float base = getBaseChance(slotsFilled);
        float floor = base + Math.max(0, failures) * PITY_PER_FAILURE;
        return Math.min(floor, Math.max(base, PITY_CAP));
    }

    /** What a villager is worth before pity or catalysts, given how many slots it has filled. */
    public static float getBaseChance(int slotsFilled) {
        return BASE_SUCCESS_CHANCE + Math.max(0, slotsFilled) * CHANCE_PER_SLOT;
    }

    /** The final odds of an attempt: floor plus banked catalysts, clamped to the limits. */
    public static float getTotalChance(int slotsFilled, int failures, float banked) {
        float total = getFloorChance(slotsFilled, failures) + Math.max(0.0F, banked);
        if (total > MAX_SUCCESS_CHANCE) total = MAX_SUCCESS_CHANCE;
        if (total < MIN_SUCCESS_CHANCE) total = MIN_SUCCESS_CHANCE;
        return total;
    }

    /** True when two books of the same level step a trade up by one. */
    public static boolean upgradesFromPair() {
        return UPGRADE_PAIR_OR_HIGHER.equals(UPGRADE_MODE) || UPGRADE_PAIR_ONLY.equals(UPGRADE_MODE);
    }

    /** True when a book above the current level raises the trade to that level. */
    public static boolean upgradesFromHigher() {
        return UPGRADE_PAIR_OR_HIGHER.equals(UPGRADE_MODE)
                || UPGRADE_HIGHER_ONLY.equals(UPGRADE_MODE);
    }

    /** True when trades are frozen at whatever level they were first taught. */
    public static boolean upgradingIsOff() {
        return UPGRADE_OFF.equals(UPGRADE_MODE);
    }

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
