package com.exiledradio.rlcraftvillagertomes;

import com.exiledradio.rlcraftvillagertomes.bounty.BountyRegistry;
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
    /** Items that improve a teaching attempt, and what each is worth. */
    public static final String CATEGORY_CATALYSTS = "catalysts";
    /** Items villagers may demand for a slot, and how many of each. */
    public static final String CATEGORY_BOUNTIES = "bounties";
    /** Whether teaching succeeds at all, and what moves the odds. */
    public static final String CATEGORY_CHANCE = "chance";
    /** What a villager demands before it opens another slot. */
    public static final String CATEGORY_SLOTS = "slots";

    /** Display order in the config screen. Without this the GUI sorts alphabetically. */
    private static final String[] CATEGORIES = {
            CATEGORY_LEARNING, CATEGORY_SLOTS, CATEGORY_CHANCE, CATEGORY_UPGRADING, CATEGORY_PRICING,
            CATEGORY_BOUNTIES, CATEGORY_CATALYSTS, CATEGORY_FEEDBACK,
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

    private static final List<String> ORDER_SLOTS = Arrays.asList(
            "LOCK_SLOTS", "REQUEST_ITEMS_BASE", "REQUEST_ITEMS_PER_SLOT", "REQUEST_ITEMS_MAX",
            "REQUEST_TIERS_BASE", "REQUEST_TIERS_PER_SLOT", "QUEST_LOG_CAPACITY");

    private static final List<String> ORDER_CHANCE = Arrays.asList(
            "ENABLE_CHANCE", "BASE_SUCCESS_CHANCE", "CHANCE_PER_SLOT", "MAX_SUCCESS_CHANCE",
            "MAX_CHANCE_PER_SLOT", "CONFIRM_BEFORE_TEACHING", "CONFIRM_DEBOUNCE_MS",
            "CONFIRM_TIMEOUT_SECONDS",
            "MIN_SUCCESS_CHANCE",
            "PITY_PER_BOOK_LEVEL", "ABSOLUTE_MAX_CHANCE", "CONSUME_BOOK_ON_FAILURE",
            "CONSUME_CATALYSTS_ON_FAILURE");

    private static final List<String> ORDER_CATALYSTS = Arrays.asList(
            "CATALYST_TIERS", "CATALYST_ITEMS");

    private static final List<String> ORDER_BOUNTIES = Arrays.asList(
            "BOUNTY_TIERS", "BOUNTY_ITEMS");

    private static final List<String> ORDER_FEEDBACK = Arrays.asList(
            "ANNOUNCE_LEARNED", "ANNOUNCE_UPGRADED", "ANNOUNCE_REJECTED",
            "PLAY_SOUNDS", "FANFARE_ENCHANTMENTS", "SPAWN_PARTICLES", "DEBUG_LOGGING");

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
        for (String key : ORDER_SLOTS) CATEGORY_OF_KEY.put(key, CATEGORY_SLOTS);
        for (String key : ORDER_CHANCE) CATEGORY_OF_KEY.put(key, CATEGORY_CHANCE);
        for (String key : ORDER_CATALYSTS) CATEGORY_OF_KEY.put(key, CATEGORY_CATALYSTS);
        for (String key : ORDER_BOUNTIES) CATEGORY_OF_KEY.put(key, CATEGORY_BOUNTIES);
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

    // slots
    public static boolean LOCK_SLOTS = true;
    public static int REQUEST_ITEMS_BASE = 2;
    public static int REQUEST_ITEMS_PER_SLOT = 1;
    public static int REQUEST_ITEMS_MAX = 6;
    public static int REQUEST_TIERS_BASE = 2;
    public static int REQUEST_TIERS_PER_SLOT = 1;
    public static int QUEST_LOG_CAPACITY = 10;

    // chance
    public static boolean ENABLE_CHANCE = true;
    public static float BASE_SUCCESS_CHANCE = 30.0F;
    public static float CHANCE_PER_SLOT = 10.0F;
    public static float MAX_SUCCESS_CHANCE = 80.0F;
    public static float MAX_CHANCE_PER_SLOT = 5.0F;
    public static boolean CONFIRM_BEFORE_TEACHING = true;
    public static int CONFIRM_DEBOUNCE_MS = 500;
    public static int CONFIRM_TIMEOUT_SECONDS = 15;
    public static float MIN_SUCCESS_CHANCE = 1.0F;
    public static float PITY_PER_BOOK_LEVEL = 5.0F;
    public static float ABSOLUTE_MAX_CHANCE = 100.0F;
    public static boolean CONSUME_BOOK_ON_FAILURE = true;
    public static boolean CONSUME_CATALYSTS_ON_FAILURE = true;

    // catalysts
    // Only common, rare and mythic are used out of the box - they are the three rungs of the
    // Trinkets and Baubles glowing ladder, priced from what each costs to craft: an ingot is
    // four powder, and a gem is four ingots plus four powder plus a diamond block.
    //
    // The other three are deliberately defined but empty. A band with nothing in it costs
    // nothing, and leaving the ladder complete means a pack author adding their own catalysts
    // has somewhere to put them without first working out what the gaps between 1, 5 and 30
    // ought to be.
    public static String[] CATALYST_TIERS = {
            "common=1.0",
            "uncommon=2.5",
            "rare=5.0",
            "epic=10.0",
            "legendary=15.0",
            "mythic=30.0",
    };
    public static String[] CATALYST_ITEMS = {
            "xat:glowing_powder=common",
            "xat:glowing_ingot=rare",
            "xat:glowing_gem=mythic",
    };

    // bounties
    public static String[] BOUNTY_TIERS = {
            "low=8-24",
            "mid=2-8",
            "high=1-2",
    };
    public static String[] BOUNTY_ITEMS = {
            // --- low: gathered by the stack ---
            "minecraft:glass=low",
            "minecraft:paper=low",
            "minecraft:fermented_spider_eye=low,4-10",
            "minecraft:brown_mushroom=low",
            "minecraft:red_mushroom=low",
            "minecraft:vine=low",
            "minecraft:waterlily=low",
            "minecraft:beetroot=low",
            "minecraft:pumpkin=low,4-12",
            "minecraft:melon_block=low,4-12",
            "minecraft:spider_eye=low,4-10",
            "minecraft:potato=low",
            "minecraft:carrot=low",
            "minecraft:quartz=low",
            "minecraft:flint=low",
            "minecraft:rotten_flesh=low",
            "minecraft:string=low",
            "minecraft:bone=low",
            "minecraft:gunpowder=low,4-12",
            "minecraft:reeds=low",
            "minecraft:sugar=low",
            "minecraft:cookie=low",
            "minecraft:melon=low",
            "minecraft:bread=low",
            "minecraft:apple=low",
            "minecraft:fish=low,4-10",
            "minecraft:cooked_rabbit=low,3-8",
            "minecraft:cooked_mutton=low,3-8",
            "minecraft:cooked_porkchop=low,3-8",
            "minecraft:cooked_chicken=low,3-8",
            "minecraft:cooked_beef=low,3-8",
            "foodexpansion:itemcookedsquid=low,3-8",
            "rustic:wildberries=low,4-12",
            "rustic:grapes=low,4-12",
            "lycanitesmobs:cooked_silex_meat=low,2-6",
            "lycanitesmobs:cooked_maka_meat=low,2-6",
            "lycanitesmobs:cooked_arisaur_meat=low,2-6",
            "lycanitesmobs:cooked_bobeko_meat=low,2-6",
            "lycanitesmobs:cooked_krake_meat=low,2-6",
            "lycanitesmobs:cooked_aspid_meat=low,2-6",
            "lycanitesmobs:cooked_joust_meat=low,2-6",
            "defiledlands:book_wyrm_cooked=low,2-6",
            "simpledifficulty:frost_powder=low,4-10",
            "minecraft:coal:0=low",
            "minecraft:redstone=low",
            "minecraft:glowstone_dust=low",
            "minecraft:blaze_powder=low",
            "minecraft:iron_ingot=low",
            "minecraft:gold_ingot=low",
            "iceandfire:copper_ingot=low",
            "xat:glowing_powder=low,2-4",
            "minecraft:stone_pickaxe=low,2-5",
            "minecraft:stone_axe=low,2-5",
            "minecraft:stone_shovel=low,2-5",
            "minecraft:stone_sword=low,2-5",
            "minecraft:golden_pickaxe=low,2-3",
            "minecraft:golden_axe=low,2-3",
            "minecraft:golden_shovel=low,2-3",
            "minecraft:golden_sword=low,2-3",
            "minecraft:wool=low",

            // --- mid: crafted, hunted, or one step off the beaten path ---
            "minecraft:netherbrick=mid",
            "minecraft:pumpkin_pie=mid",
            "iceandfire:pixie_dust=mid",
            "minecraft:book=mid",
            "minecraft:bookshelf=mid",
            "minecraft:iron_pickaxe=mid,1-3",
            "minecraft:iron_axe=mid,1-3",
            "minecraft:iron_shovel=mid,1-3",
            "minecraft:iron_sword=mid,1-3",
            "minecraft:flint_and_steel=mid,1-2",
            "minecraft:saddle=mid,1-2",
            "minecraft:tnt=mid,2-6",
            "minecraft:compass=mid,1-2",
            "minecraft:clock=mid,1-2",
            "minecraft:ender_pearl=mid,2-6",
            "minecraft:skull:0=mid,1-2",
            "minecraft:skull:2=mid,1-2",
            "simpledifficulty:frost_rod=mid",
            "iceandfire:sapphire_gem=mid",
            "iceandfire:troll_tusk=mid,1-4",
            "iceandfire:troll_leather_forest=mid,1-4",
            "iceandfire:troll_leather_frost=mid,1-4",
            "iceandfire:troll_leather_mountain=mid,1-4",
            "iceandfire:stymphalian_bird_feather=mid,2-6",
            "iceandfire:amphithere_feather=mid,1-4",
            "iceandfire:sea_serpent_fang=mid,1-4",
            "defiledlands:foul_slime=mid,2-6",
            "defiledlands:foul_candy=mid,2-6",
            "defiledlands:book_wyrm_scale=mid,2-6",
            "scalinghealth:heartdust=mid,2-6",
            "firstaid:bandage=mid,2-6",
            "firstaid:plaster=mid,2-6",
            "roughtweaks:bandage=mid,2-6",
            "roughtweaks:plaster=mid,2-6",
            "armorunder:heating_goo=mid,1-4",
            "armorunder:cooling_goo=mid,1-4",
            "foodexpansion:itemchocolatebar=mid,2-6",
            "sereneseasons:greenhouse_glass=mid,2-6",
            "toolbelt:belt=mid,1-1",
            "inspirations:barometer=mid,1-1",
            "qualitytools:emerald_ring=mid,1-2",
            "qualitytools:emerald_amulet=mid,1-2",
            "minecraft:emerald=mid",
            "minecraft:diamond=mid",
            "minecraft:blaze_rod=mid",
            "scalinghealth:crystalshard=mid",
            "iceandfire:dragonbone=mid",
            "xat:glowing_ingot=mid,1-3",
            "minecraft:coal_block=mid",
            "minecraft:redstone_block=mid",
            "minecraft:glowstone=mid",
            "minecraft:iron_block=mid",
            "minecraft:gold_block=mid",
            "minecraft:golden_apple:0=mid,1-3",
            "minecraft:diamond_pickaxe=mid,1-2",
            "minecraft:diamond_axe=mid,1-2",
            "minecraft:diamond_shovel=mid,1-2",
            "minecraft:diamond_sword=mid,1-2",
            "iceandfire:silver_pickaxe=mid,1-2",
            "iceandfire:silver_axe=mid,1-2",
            "iceandfire:silver_shovel=mid,1-2",
            "iceandfire:silver_sword=mid,1-2",
            "iceandfire:silver_ingot=mid",

            // --- high: trophies, and things you had to kill something for ---
            "familiarfauna:pixie_dust=high",
            "minecraft:golden_apple:1=high,1-1",
            "minecraft:ghast_tear=high,1-3",
            "firstaid:morphine=high,1-3",
            "defiledlands:book_wyrm_scale_golden=high,1-3",
            "defiledlands:umbrium_ingot=high,1-3",
            "scalinghealth:heartcontainer=high,1-1",
            "bountifulbaubles:trinketballoon=high,1-1",
            "bountifulbaubles:trinketmagiclenses=high,1-1",
            "bountifulbaubles:crowngold=high,1-1",
            "bountifulbaubles:amuletsinempty=high,1-1",
            "bountifulbaubles:amuletcross=high,1-1",
            "iceandfire:fire_dragon_flesh=high,1-3",
            "iceandfire:ice_dragon_flesh=high,1-3",
            "iceandfire:lightning_dragon_flesh=high,1-3",
            "iceandfire:fire_dragon_blood=high,1-2",
            "iceandfire:ice_dragon_blood=high,1-2",
            "iceandfire:lightning_dragon_blood=high,1-2",
            "iceandfire:fire_dragon_heart=high,1-1",
            "iceandfire:ice_dragon_heart=high,1-1",
            "iceandfire:lightning_dragon_heart=high,1-1",
            "iceandfire:hydra_fang=high,1-3",
            "iceandfire:hydra_heart=high,1-1",
            "minecraft:diamond_block=high",
            "minecraft:emerald_block=high",
            "iceandfire:dragon_skull=high",
            "xat:glowing_gem=high",
            "charm:charged_emerald=high",
            "iceandfire:silver_block=high",
    };

    // feedback
    public static boolean ANNOUNCE_LEARNED = true;
    public static boolean ANNOUNCE_UPGRADED = true;
    public static boolean ANNOUNCE_REJECTED = true;
    public static boolean PLAY_SOUNDS = true;
    public static String[] FANFARE_ENCHANTMENTS = {"somanyenchantments:supreme*"};
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

    /** Normalised {@link #FANFARE_ENCHANTMENTS}, some entries ending in a {@code *}. */
    private static Set<String> fanfarePatterns = new HashSet<String>();

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
        loadSlots();
        loadChance();
        loadUpgrading();
        loadPricing();
        loadBounties();
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

    // ------------------------------------------------------------------- slots

    private static final String SLOTS_COMMENT =
            "What a villager demands before it will hold another book.\n"
                    + "\n"
                    + "No slot is free. Each villager rolls its own list of items - drawn from the\n"
                    + "same tier list the catalysts use - and wants all of it delivered before it\n"
                    + "opens up. Requests get longer and richer with every slot.\n"
                    + "\n"
                    + "A request is NEVER re-rolled. If a villager asks for something you cannot\n"
                    + "get, the answer is a different villager, not another click. That is what\n"
                    + "makes the ones you have already paid into worth protecting.";

    private static final String LOCK_SLOTS_COMMENT =
            "If true (default), villagers start with nothing and every slot must be bought\n"
                    + "with a delivered request.\n"
                    + "\n"
                    + "Set to false and every villager simply has MAX_TOMES_PER_VILLAGER slots open\n"
                    + "from the start, which is how the mod behaved before requests existed.\n"
                    + "Villagers that already unlocked slots keep them either way.";

    private static final String REQUEST_ITEMS_PER_SLOT_COMMENT =
            "How many more items each slot asks for than the one before it.\n"
                    + "With the defaults a villager wants 2 items for its first slot and 6 for its\n"
                    + "fifth.";

    private static final String REQUEST_ITEMS_MAX_COMMENT =
            "The most items any single request will ever list, however deep you go.\n"
                    + "A request longer than this stops being a goal and starts being a chore.";

    private static final String REQUEST_TIERS_BASE_COMMENT =
            "How many tiers, counting up from the cheapest, the FIRST slot may draw from.\n"
                    + "2 keeps an opening request to the bottom two bands, so nobody is asked for a\n"
                    + "dragon skull before they have taught a single book.";

    private static final String REQUEST_TIERS_PER_SLOT_COMMENT =
            "How many more tiers each slot unlocks access to.\n"
                    + "\n"
                    + "The cheapest tier always stays in the pool, so a late request mixes the\n"
                    + "expensive with the ordinary rather than being a solid wall of legendary\n"
                    + "items. Ordering in CATALYST_TIERS is the ranking - cheapest first.";

    private static final String QUEST_LOG_CAPACITY_COMMENT =
            "How many villagers one quest log can track.\n"
                    + "\n"
                    + "A quest log is a book and quill you sneak-click a villager with: it\n"
                    + "becomes a written book listing what that villager wants and where it\n"
                    + "lives, and entries cross themselves off once the slot is paid for.\n"
                    + "\n"
                    + "One villager per page, so this is also the page count. Vanilla books\n"
                    + "hold 50 pages, which is the ceiling here.";

    private static void loadSlots() {
        config.setCategoryComment(CATEGORY_SLOTS, SLOTS_COMMENT);
        config.setCategoryPropertyOrder(CATEGORY_SLOTS, mutableOrder(ORDER_SLOTS));

        LOCK_SLOTS = config.getBoolean(
                "LOCK_SLOTS", CATEGORY_SLOTS, true, LOCK_SLOTS_COMMENT);

        REQUEST_ITEMS_BASE = config.getInt(
                "REQUEST_ITEMS_BASE", CATEGORY_SLOTS, 2, 1, 16,
                "How many different items the FIRST slot asks for.");

        REQUEST_ITEMS_PER_SLOT = config.getInt(
                "REQUEST_ITEMS_PER_SLOT", CATEGORY_SLOTS, 1, 0, 8,
                REQUEST_ITEMS_PER_SLOT_COMMENT);

        REQUEST_ITEMS_MAX = config.getInt(
                "REQUEST_ITEMS_MAX", CATEGORY_SLOTS, 6, 1, 16,
                REQUEST_ITEMS_MAX_COMMENT);

        REQUEST_TIERS_BASE = config.getInt(
                "REQUEST_TIERS_BASE", CATEGORY_SLOTS, 2, 1, 16,
                REQUEST_TIERS_BASE_COMMENT);

        REQUEST_TIERS_PER_SLOT = config.getInt(
                "REQUEST_TIERS_PER_SLOT", CATEGORY_SLOTS, 1, 0, 8,
                REQUEST_TIERS_PER_SLOT_COMMENT);

        QUEST_LOG_CAPACITY = config.getInt(
                "QUEST_LOG_CAPACITY", CATEGORY_SLOTS, 10, 1, 50,
                QUEST_LOG_CAPACITY_COMMENT);
    }

    private static final String MAX_CHANCE_PER_SLOT_COMMENT =
            "How many percentage points the CEILING rises per slot a villager has unlocked.\n"
                    + "\n"
                    + "MAX_SUCCESS_CHANCE is the ceiling for a villager on its first slot. With\n"
                    + "this at 5, a villager four slots deep can be pushed to 100 instead of 80.\n"
                    + "So committing to one villager pays twice - once in its base odds, and again\n"
                    + "in how far catalysts can take it.\n"
                    + "\n"
                    + "Set to 0 for a flat ceiling that ignores how developed a villager is.\n"
                    + "Failure pity is added on top of the ceiling either way and is not bound by\n"
                    + "it - see PITY_PER_BOOK_LEVEL.";

    private static final String CONFIRM_BEFORE_TEACHING_COMMENT =
            "If true (default), offering a book asks before it commits.\n"
                    + "\n"
                    + "The first sneak-click reports the odds and waits; a second commits the book.\n"
                    + "Losing a Sharpness V to a click you did not mean to make is the worst thing\n"
                    + "that can happen in this mod, and a confirmation costs one click to avoid it.\n"
                    + "\n"
                    + "Set to false to hand books over immediately, which is how it behaved before\n"
                    + "the prompt existed.";

    private static final String CONFIRM_DEBOUNCE_COMMENT =
            "How long after the prompt a second click is ignored, in milliseconds.\n"
                    + "\n"
                    + "500 (default) is there because the prompt appears on a click, and a\n"
                    + "double-click would otherwise answer a question the player has not read yet.\n"
                    + "Clicks inside this window do nothing at all rather than confirming.";

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

        MAX_CHANCE_PER_SLOT = config.getFloat(
                "MAX_CHANCE_PER_SLOT", CATEGORY_CHANCE, 5.0F, 0.0F, 100.0F,
                MAX_CHANCE_PER_SLOT_COMMENT);

        CONFIRM_BEFORE_TEACHING = config.getBoolean(
                "CONFIRM_BEFORE_TEACHING", CATEGORY_CHANCE, true,
                CONFIRM_BEFORE_TEACHING_COMMENT);

        CONFIRM_DEBOUNCE_MS = config.getInt(
                "CONFIRM_DEBOUNCE_MS", CATEGORY_CHANCE, 500, 0, 5000,
                CONFIRM_DEBOUNCE_COMMENT);

        CONFIRM_TIMEOUT_SECONDS = config.getInt(
                "CONFIRM_TIMEOUT_SECONDS", CATEGORY_CHANCE, 15, 1, 300,
                "How long a pending confirmation stays open, in seconds.\n"
                        + "After this it lapses and the next click asks again rather than committing\n"
                        + "a book you had forgotten about.");

        MIN_SUCCESS_CHANCE = config.getFloat(
                "MIN_SUCCESS_CHANCE", CATEGORY_CHANCE, 1.0F, 0.0F, 100.0F,
                "The lowest chance an attempt can ever have, as a percentage.\n"
                        + "Only reachable if you set BASE_SUCCESS_CHANCE very low; it exists so a\n"
                        + "misconfigured base cannot make books impossible to teach."
        );

        PITY_PER_BOOK_LEVEL = config.getFloat(
                "PITY_PER_BOOK_LEVEL", CATEGORY_CHANCE, 5.0F, 0.0F, 100.0F,
                "How many percentage points a failure is worth, PER LEVEL of the book that\n"
                        + "burned, for that enchantment on that villager.\n"
                        + "\n"
                        + "At the default of 5, losing a Sharpness V raises the floor by 25 while\n"
                        + "losing a Sharpness I raises it by 5. Scaling by level keeps the\n"
                        + "compensation proportional to what was actually lost - a level V book is\n"
                        + "five times the investment, so it buys five times the consolation.\n"
                        + "\n"
                        + "Tracked per villager AND per enchantment, so failing Mending on one\n"
                        + "librarian makes Mending easier on that librarian only - not Unbreaking,\n"
                        + "and not on the librarian next door. Committing to a villager is what pays\n"
                        + "off, which is the same thing the permanently locked slots are asking of\n"
                        + "you.\n"
                        + "\n"
                        + "Success wipes what is owed on that enchantment. Set to 0 to remove the\n"
                        + "mercy rule entirely and let bad luck run forever."
        );

        ABSOLUTE_MAX_CHANCE = config.getFloat(
                "ABSOLUTE_MAX_CHANCE", CATEGORY_CHANCE, 100.0F, 1.0F, 100.0F,
                "The hard ceiling once failure bonuses are counted, as a percentage.\n"
                        + "\n"
                        + "This is the one limit pity CAN pass MAX_SUCCESS_CHANCE to reach. The order\n"
                        + "is deliberate: preparation - your villager's slots plus banked catalysts -\n"
                        + "is capped at MAX_SUCCESS_CHANCE, and what you are owed for past failures is\n"
                        + "added on top of that cap.\n"
                        + "\n"
                        + "So a villager sitting at the 80% preparation ceiling that already owes you\n"
                        + "15% from a burnt book reads 95%, and enough losses eventually reach 100%.\n"
                        + "No amount of preparation alone ever gets there - only bad luck does, which\n"
                        + "means the guarantee is something you are compensated with rather than\n"
                        + "something you can buy.\n"
                        + "\n"
                        + "Set to 80 to match MAX_SUCCESS_CHANCE and take the escape hatch away."
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

    // ---------------------------------------------------------------- bounties

    private static final String BOUNTIES_COMMENT =
            "What villagers may demand before they open a tome slot.\n"
                    + "\n"
                    + "Separate from the catalyst list on purpose. These two overlap in places - a\n"
                    + "glowing ingot is both a fine catalyst and a fine thing to be asked for - but\n"
                    + "they answer different questions. A bounty item needs a quantity; a catalyst\n"
                    + "needs a percentage. Most of what a villager demands is ordinary material that\n"
                    + "would never belong in a percentage table.\n"
                    + "\n"
                    + "Run /villagertomes tiers in game to see what was parsed and what the item in\n"
                    + "your hand counts as.";

    private static final String BOUNTY_TIERS_COMMENT =
            "Rarity bands, one per line, as name=min-max\n"
                    + "\n"
                    + "The range is how many of an item in that band a villager asks for. So\n"
                    + "low=8-24 is the emerald x18 end of a request and high=1-2 is the single\n"
                    + "dragon skull end.\n"
                    + "\n"
                    + "ORDER MATTERS. Cheapest band first: the position in this list is the ranking\n"
                    + "that decides which bands an early slot is allowed to draw from. Names are\n"
                    + "yours to choose and nothing is hard-coded.";

    private static final String BOUNTY_ITEMS_COMMENT =
            "Which items belong to which band, one per line, as modid:item=tier\n"
                    + "\n"
                    + "A quantity can be overridden per item by adding a range:\n"
                    + "  minecraft:coal=low            asks for the band's 8-24\n"
                    + "  minecraft:diamond=low,4-12    same band, its own quantity\n"
                    + "\n"
                    + "That override exists because bands are coarse and value inside one is not.\n"
                    + "Coal and diamonds are both things you keep in stacks, but being asked for\n"
                    + "twenty of each is not the same ask at all.\n"
                    + "\n"
                    + "Metadata is optional: minecraft:coal:0 is coal but not charcoal. NBT is\n"
                    + "ignored, so an enchanted or renamed copy of a material still counts.\n"
                    + "\n"
                    + "Items no loaded mod registers are never asked for. That check matters more\n"
                    + "here than for catalysts: a request is never re-rolled, so demanding something\n"
                    + "this instance does not have would strand the villager permanently.";

    private static void loadBounties() {
        config.setCategoryComment(CATEGORY_BOUNTIES, BOUNTIES_COMMENT);
        config.setCategoryPropertyOrder(CATEGORY_BOUNTIES, mutableOrder(ORDER_BOUNTIES));

        BOUNTY_TIERS = config.getStringList(
                "BOUNTY_TIERS", CATEGORY_BOUNTIES, BOUNTY_TIERS, BOUNTY_TIERS_COMMENT);

        BOUNTY_ITEMS = config.getStringList(
                "BOUNTY_ITEMS", CATEGORY_BOUNTIES, BOUNTY_ITEMS, BOUNTY_ITEMS_COMMENT);
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

        FANFARE_ENCHANTMENTS = config.getStringList(
                "FANFARE_ENCHANTMENTS", CATEGORY_FEEDBACK, FANFARE_ENCHANTMENTS,
                "Enchantments whose successful binding is worth a fanfare - one registry name\n"
                        + "per line. Landing one of these plays the advancement jingle instead of the\n"
                        + "usual villager noise.\n"
                        + "\n"
                        + "A trailing * matches any name that starts with what comes before it, which\n"
                        + "is why the default is a single line: somanyenchantments:supreme* covers all\n"
                        + "six Supreme enchantments without naming them. Add\n"
                        + "somanyenchantments:advanced* to include the nineteen Advanced ones, or list\n"
                        + "exact names for finer control.\n"
                        + "\n"
                        + "Keep this short. The whole point is that the sound means something, and a\n"
                        + "jingle you hear every third book is just noise. Empty disables it.\n"
                        + "Ignored entirely when PLAY_SOUNDS is off.\n"
                        + "\n"
                        + "The sound is vanilla's own ui.toast.challenge_complete, so it is already on\n"
                        + "every client and this mod ships no audio of its own."
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
        if (REQUEST_ITEMS_BASE < 1) REQUEST_ITEMS_BASE = 1;
        if (REQUEST_ITEMS_PER_SLOT < 0) REQUEST_ITEMS_PER_SLOT = 0;
        if (REQUEST_ITEMS_MAX < REQUEST_ITEMS_BASE) REQUEST_ITEMS_MAX = REQUEST_ITEMS_BASE;
        if (REQUEST_TIERS_BASE < 1) REQUEST_TIERS_BASE = 1;
        if (REQUEST_TIERS_PER_SLOT < 0) REQUEST_TIERS_PER_SLOT = 0;
        if (CHANCE_PER_SLOT < 0.0F) CHANCE_PER_SLOT = 0.0F;
        if (MAX_CHANCE_PER_SLOT < 0.0F) MAX_CHANCE_PER_SLOT = 0.0F;
        if (CONFIRM_DEBOUNCE_MS < 0) CONFIRM_DEBOUNCE_MS = 0;
        if (CONFIRM_TIMEOUT_SECONDS < 1) CONFIRM_TIMEOUT_SECONDS = 1;
        if (PITY_PER_BOOK_LEVEL < 0.0F) PITY_PER_BOOK_LEVEL = 0.0F;
        if (ABSOLUTE_MAX_CHANCE < MAX_SUCCESS_CHANCE) ABSOLUTE_MAX_CHANCE = MAX_SUCCESS_CHANCE;
        if (ABSOLUTE_MAX_CHANCE > 100.0F) ABSOLUTE_MAX_CHANCE = 100.0F;

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
        fanfarePatterns = toLookupSet(FANFARE_ENCHANTMENTS);

        extraInput = resolveExtraInput();

        // Syntax is checked here; the item names are looked up later, once every mod has
        // finished registering. See CatalystRegistry for why those are two separate steps.
        CatalystRegistry.reload(CATALYST_TIERS, CATALYST_ITEMS);
        BountyRegistry.reload(BOUNTY_TIERS, BOUNTY_ITEMS);
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
     * The ceiling catalysts can reach on this villager, which rises as it opens slots.
     *
     * <p>A villager that has been paid into repeatedly is not just more likely to take a
     * book, it can be pushed further than a fresh one - so the reward for committing to one
     * villager shows up twice, once in the base and once in how high it can be taken.
     */
    public static float getMaxChance(int slotsUnlocked) {
        float ceiling = MAX_SUCCESS_CHANCE + Math.max(0, slotsUnlocked) * MAX_CHANCE_PER_SLOT;
        return Math.min(ceiling, ABSOLUTE_MAX_CHANCE);
    }

    /**
     * Base plus banked catalysts, held to this villager's ceiling.
     *
     * <p>Deliberately separate from the pity half so both the chat readout and the roll are
     * built from the same pieces and cannot disagree - and so pity can be stacked on top of
     * a ceiling that catalysts alone are not allowed to pass.
     */
    public static float getPreparedChance(int slotsFilled, float banked) {
        float prepared = getBaseChance(slotsFilled) + Math.max(0.0F, banked);
        return Math.min(prepared, getMaxChance(slotsFilled));
    }

    /** What a villager is worth before pity or catalysts, given how many slots it has filled. */
    public static float getBaseChance(int slotsFilled) {
        return BASE_SUCCESS_CHANCE + Math.max(0, slotsFilled) * CHANCE_PER_SLOT;
    }

    /** The final odds of an attempt: floor plus banked catalysts, clamped to the limits. */
    public static float getTotalChance(int slotsFilled, float pity, float banked) {
        float total = getPreparedChance(slotsFilled, banked) + Math.max(0.0F, pity);
        if (total > ABSOLUTE_MAX_CHANCE) total = ABSOLUTE_MAX_CHANCE;
        if (total < MIN_SUCCESS_CHANCE) total = MIN_SUCCESS_CHANCE;
        return total;
    }

    /**
     * Whether landing this enchantment is worth the advancement jingle.
     *
     * <p>A trailing {@code *} matches by prefix, which is what makes one line cover a whole
     * family - SoManyEnchantments names its top tier {@code supremesharpness},
     * {@code supremesmite} and so on, so {@code somanyenchantments:supreme*} catches all six
     * and keeps catching them if the mod adds a seventh.
     */
    public static boolean isFanfareEnchantment(ResourceLocation enchantment) {
        if (enchantment == null || fanfarePatterns.isEmpty()) {
            return false;
        }
        String key = enchantment.toString().toLowerCase(Locale.ROOT);
        for (String pattern : fanfarePatterns) {
            if (pattern.endsWith("*")) {
                if (key.startsWith(pattern.substring(0, pattern.length() - 1))) {
                    return true;
                }
            } else if (pattern.equals(key)) {
                return true;
            }
        }
        return false;
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
