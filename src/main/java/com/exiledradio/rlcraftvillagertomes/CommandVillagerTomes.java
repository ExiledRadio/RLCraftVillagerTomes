package com.exiledradio.rlcraftvillagertomes;

import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.Tome;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * {@code /villagertomes} — inspect and edit what the villager you are looking at knows.
 *
 * <p>Everything operates on the villager under the crosshair rather than taking an entity
 * selector, because the thing an admin actually wants to do is walk up to a specific
 * villager and ask what is wrong with it.
 *
 * <p>{@code list} is open to everyone: it only reports trades the player could see by
 * opening the villager anyway. The subcommands that change state need permission level 2,
 * the same level vanilla requires for {@code /gamemode}.
 */
public class CommandVillagerTomes extends CommandBase {

    private static final int ADMIN_PERMISSION_LEVEL = 2;

    /** How far the crosshair reaches when looking for a villager, in blocks. */
    private static final double REACH = 12.0D;

    @Override
    public String getName() {
        return "villagertomes";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("tomes", "vt");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/villagertomes <list|teach|forget|clear> [enchantment] [level]";
    }

    /** Zero so that unprivileged players can reach {@code list}. See {@link #checkAdmin}. */
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if ("list".equals(subCommand)) {
            executeList(sender);
        } else if ("teach".equals(subCommand)) {
            executeTeach(server, sender, args);
        } else if ("forget".equals(subCommand)) {
            executeForget(server, sender, args);
        } else if ("clear".equals(subCommand)) {
            executeClear(server, sender);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    // ------------------------------------------------------------ subcommands

    private void executeList(ICommandSender sender) throws CommandException {
        EntityVillager villager = requireTargetedVillager(sender);
        ITomeKnowledge tomes = requireTomes(villager);

        if (tomes.count() == 0) {
            reply(sender, TextFormatting.GRAY + "This villager has not been taught anything.");
            return;
        }

        reply(sender, TextFormatting.AQUA + "Tomes (" + tomes.count() + "/"
                + ModConfig.MAX_TOMES_PER_VILLAGER + "):");
        for (Tome tome : tomes.view()) {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(tome.getEnchantment());
            int level = tome.getLevel();
            if (enchantment == null) {
                // Kept in the data but unsellable until the mod that added it comes back.
                reply(sender, TextFormatting.DARK_GRAY + "  " + tome.getEnchantment() + " "
                        + level + " (not loaded)");
            } else {
                reply(sender, TextFormatting.WHITE + "  " + tome.getEnchantment() + " " + level
                        + TextFormatting.GRAY + " - "
                        + ModConfig.getEmeraldCost(enchantment, level) + " emerald(s)");
            }
        }
    }

    private void executeTeach(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        checkAdmin(server, sender);
        if (args.length < 2) {
            throw new WrongUsageException("/villagertomes teach <enchantment> [level]");
        }

        EntityVillager villager = requireTargetedVillager(sender);
        ITomeKnowledge tomes = requireTomes(villager);

        Enchantment enchantment = parseEnchantment(args[1]);
        ResourceLocation id = enchantment.getRegistryName();
        int level = args.length >= 3 ? parseInt(args[2], 1, 255) : 1;

        // Deliberately bypasses the whitelist, blacklist and level ceiling - an admin
        // command that silently refused to do what it was told would be worse than useless
        // for fixing a villager. The tome cap is still honoured, because going over it
        // produces a villager the mod cannot describe to the player.
        if (!tomes.knows(id) && tomes.count() >= ModConfig.MAX_TOMES_PER_VILLAGER) {
            throw new CommandException("This villager is already at the tome cap of %s.",
                    Integer.valueOf(ModConfig.MAX_TOMES_PER_VILLAGER));
        }

        // Replaces rather than stacking, whatever UPGRADE_TAKES_NEW_SLOT says. An admin
        // asking for "unbreaking 3" wants that villager selling Unbreaking III, not another
        // trade added next to whatever was there.
        tomes.setOnly(id, level);
        reply(sender, TextFormatting.GREEN + "Taught " + id + " " + level + ".");
    }

    private void executeForget(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        checkAdmin(server, sender);
        if (args.length < 2) {
            throw new WrongUsageException("/villagertomes forget <enchantment>");
        }

        EntityVillager villager = requireTargetedVillager(sender);
        ITomeKnowledge tomes = requireTomes(villager);

        ResourceLocation id = parseEnchantment(args[1]).getRegistryName();
        if (!tomes.knows(id)) {
            throw new CommandException("This villager does not know %s.", id);
        }
        tomes.forget(id);
        // The trade itself is swept out the next time somebody opens this villager, which
        // is the same path every other change takes - see TomeTradeSync.
        reply(sender, TextFormatting.GREEN + "Forgot " + id + ".");
    }

    private void executeClear(MinecraftServer server, ICommandSender sender)
            throws CommandException {
        checkAdmin(server, sender);
        EntityVillager villager = requireTargetedVillager(sender);
        ITomeKnowledge tomes = requireTomes(villager);

        int count = tomes.count();
        tomes.clear();
        reply(sender, TextFormatting.GREEN + "Cleared " + count + " tome(s).");
    }

    // ---------------------------------------------------------------- helpers

    /**
     * The villager the sender is looking at.
     *
     * <p>Picks whichever villager within reach the sender's line of sight enters first,
     * rather than the nearest one - in a village those are frequently different villagers,
     * and pointing at the one you mean is the only unambiguous way to say which.
     */
    private EntityVillager requireTargetedVillager(ICommandSender sender) throws CommandException {
        EntityPlayer player = sender instanceof EntityPlayer ? (EntityPlayer) sender : null;
        if (player == null) {
            throw new CommandException("This has to be run by a player - it works on the "
                    + "villager you are looking at.");
        }

        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d reach = eyes.add(player.getLook(1.0F).scale(REACH));
        AxisAlignedBB search = player.getEntityBoundingBox()
                .grow(REACH, REACH, REACH);

        EntityVillager best = null;
        double bestDistance = Double.MAX_VALUE;
        for (EntityVillager candidate
                : player.world.getEntitiesWithinAABB(EntityVillager.class, search)) {
            // A little slack on the hitbox so a villager that is walking, or standing at the
            // edge of the crosshair, is still selectable.
            RayTraceResult hit = candidate.getEntityBoundingBox().grow(0.3D)
                    .calculateIntercept(eyes, reach);
            if (hit == null) {
                continue;
            }
            double distance = eyes.squareDistanceTo(hit.hitVec);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        if (best == null) {
            throw new CommandException("Look at a villager first - none found within %s blocks.",
                    Integer.valueOf((int) REACH));
        }
        return best;
    }

    private ITomeKnowledge requireTomes(EntityVillager villager) throws CommandException {
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (tomes == null) {
            throw new CommandException("That villager has no tome data attached, which should "
                    + "not be possible - check the log for capability errors.");
        }
        return tomes;
    }

    /** Resolves a registry name, accepting a bare name as shorthand for the vanilla one. */
    private Enchantment parseEnchantment(String name) throws CommandException {
        String trimmed = name.trim().toLowerCase(Locale.ROOT);
        if (trimmed.indexOf(':') < 0) {
            trimmed = "minecraft:" + trimmed;
        }
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(
                new ResourceLocation(trimmed));
        if (enchantment == null || enchantment.getRegistryName() == null) {
            throw new CommandException("No enchantment is registered as %s.", trimmed);
        }
        return enchantment;
    }

    private void checkAdmin(MinecraftServer server, ICommandSender sender) throws CommandException {
        if (!sender.canUseCommand(ADMIN_PERMISSION_LEVEL, getName())) {
            throw new CommandException("You do not have permission to change villager tomes.");
        }
    }

    private void reply(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, net.minecraft.util.math.BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "list", "teach", "forget", "clear");
        }
        if (args.length == 2 && ("teach".equals(args[0]) || "forget".equals(args[0]))) {
            List<String> names = new ArrayList<String>();
            for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
                if (enchantment.getRegistryName() != null) {
                    names.add(enchantment.getRegistryName().toString());
                }
            }
            return getListOfStringsMatchingLastWord(args, names);
        }
        return java.util.Collections.emptyList();
    }
}
