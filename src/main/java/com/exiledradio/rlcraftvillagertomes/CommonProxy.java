package com.exiledradio.rlcraftvillagertomes;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Side-split hooks. The server's version does nothing; {@code client.ClientProxy} overrides
 * them. Every parameter here is a common type, so this class verifies on a dedicated server
 * without dragging in anything client-only.
 */
public class CommonProxy {

    public void preInit() {
    }

    /** Opens or refreshes the ledger screen from a view the server sent. */
    public void showLedger(NBTTagCompound view) {
    }

    /** One short line: into the open ledger or scroll, else onto the action bar. */
    public void showNotice(byte tone, String json) {
    }

    /** Unrolls a villager's bounty scroll from what the server sent. */
    public void showNote(NBTTagCompound note) {
    }

    /** Records the ward of the villager whose trade screen is opening, for the ward button. */
    public void showWard(int entityId, int mode) {
    }
}
