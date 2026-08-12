package com.exiledradio.rlcraftvillagertomes;

import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.catalyst.CatalystRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = RLCraftVillagerTomes.MODID,
        name = RLCraftVillagerTomes.NAME,
        version = RLCraftVillagerTomes.VERSION,
        guiFactory = "com.exiledradio.rlcraftvillagertomes.ModGuiFactory",
        // No dependencies at all. Enchantments are read from the registry at runtime, so
        // anything any other mod adds works without this being compiled against it.
        // Every decision the mod makes happens on the server - the client half is a config
        // screen and nothing else, and no custom packets are sent - so a version mismatch
        // is not a reason to turn a player away.
        acceptableRemoteVersions = "*"
)
public class RLCraftVillagerTomes {

    public static final String MODID = "rlcraftvillagertomes";
    public static final String NAME = "RLCraft Villager Tomes";
    // Replaced at build time by ForgeGradle from mod_version in gradle.properties.
    // Shows literally as "@VERSION@" in IDE dev runs; that is expected.
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
        // Must happen in preInit: villagers can start being loaded as soon as a world does,
        // and a capability that is not registered by then cannot be attached to them.
        CapabilityTomeKnowledge.register();
    }

    /**
     * Turns configured catalyst names into real items.
     *
     * <p>Deferred to load-complete rather than done at config load because the config is
     * read during pre-init, before other mods have finished registering their items. Asking
     * the registry that early reports another mod's item as missing when it is simply not
     * there yet.
     */
    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        CatalystRegistry.resolveItems();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandVillagerTomes());
    }
}
