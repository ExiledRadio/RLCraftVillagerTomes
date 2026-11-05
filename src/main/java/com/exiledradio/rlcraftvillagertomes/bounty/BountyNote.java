package com.exiledradio.rlcraftvillagertomes.bounty;

import com.exiledradio.rlcraftvillagertomes.ModConfig;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.quest.QuestBinding;
import com.exiledradio.rlcraftvillagertomes.ward.WardMode;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What a villager's bounty scroll shows: the sneak-click-with-an-empty-hand report.
 *
 * <p>Built on the server, where the villager's capability lives, and sent to the client as
 * NBT for {@code client.GuiBountyNote}. The client adds the one thing only it knows cheaply -
 * how many of each item the player is carrying - when it draws.
 */
public final class BountyNote {

    /** One "pity" line: an enchantment this villager owes extra chance on. */
    public static final class Bonus {
        public final String name;
        public final float bonus;
        public final float total;

        Bonus(String name, float bonus, float total) {
            this.name = name;
            this.bonus = bonus;
            this.total = total;
        }
    }

    /** The villager's name, as JSON (a career name stays translatable). */
    public String name;
    /** The slot the current request unlocks. */
    public int slot;
    /** Slots unlocked but not yet filled with a trade. */
    public int open;
    /** True once the villager has nothing left to unlock. */
    public boolean full;
    public final List<BountyItem> wanted = new ArrayList<BountyItem>();
    /** Whether the optional chance system is on; everything below it is only sent if so. */
    public boolean chance;
    public float total;
    public float max;
    public final List<Bonus> bonuses = new ArrayList<Bonus>();
    /** Ward label, or empty when the villager is not warded. */
    public String ward = "";
    /** A line shown across the top - what just happened. Empty for none. */
    public String banner = "";
    /** A {@code Feedback} tone for the banner. */
    public byte bannerTone;

    // ------------------------------------------------------------------ server

    public static BountyNote of(EntityVillager villager, ITomeKnowledge tomes) {
        BountyNote note = new BountyNote();
        note.name = QuestBinding.storedName(villager);
        int unlocked = SlotRequests.unlockedSlots(tomes);
        note.slot = unlocked + 1;
        note.open = SlotRequests.openSlots(tomes);
        note.full = !SlotRequests.canUnlockMore(tomes);
        if (!note.full) {
            SlotRequests.ensureRequest(villager, tomes);
            note.wanted.addAll(tomes.getRequest());
        }
        note.chance = ModConfig.ENABLE_CHANCE;
        if (note.chance) {
            float banked = tomes.getBankedChance();
            note.total = ModConfig.getTotalChance(unlocked, 0.0F, banked);
            note.max = ModConfig.getMaxChance(unlocked);
            for (Map.Entry<ResourceLocation, Float> owed : tomes.pityView().entrySet()) {
                float bonus = owed.getValue().floatValue();
                if (bonus <= 0.0F) {
                    continue;
                }
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(owed.getKey());
                String label = enchantment == null ? String.valueOf(owed.getKey())
                        : plainName(enchantment);
                note.bonuses.add(new Bonus(label, bonus,
                        ModConfig.getTotalChance(unlocked, bonus, banked)));
            }
        }
        WardMode ward = WardMode.of(tomes.getWard());
        if (ward != WardMode.NONE) {
            note.ward = ward.getLabel();
        }
        return note;
    }

    @SuppressWarnings("deprecation")
    private static String plainName(Enchantment enchantment) {
        String key = enchantment.getName();
        String translated = net.minecraft.util.text.translation.I18n.translateToLocal(key);
        return translated.equals(key) ? String.valueOf(enchantment.getRegistryName()) : translated;
    }

    // --------------------------------------------------------------------- nbt

    public NBTTagCompound toTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("name", name);
        tag.setInteger("slot", slot);
        tag.setInteger("open", open);
        tag.setBoolean("full", full);
        NBTTagList want = new NBTTagList();
        for (BountyItem line : wanted) {
            NBTTagCompound l = new NBTTagCompound();
            l.setString("item", line.getItemName().toString());
            l.setInteger("m", line.getMeta());
            l.setInteger("need", line.getRequired());
            l.setInteger("got", line.getDelivered());
            want.appendTag(l);
        }
        tag.setTag("want", want);
        tag.setBoolean("chance", chance);
        if (chance) {
            tag.setFloat("total", total);
            tag.setFloat("max", max);
            NBTTagList list = new NBTTagList();
            for (Bonus b : bonuses) {
                NBTTagCompound l = new NBTTagCompound();
                l.setString("name", b.name);
                l.setFloat("bonus", b.bonus);
                l.setFloat("total", b.total);
                list.appendTag(l);
            }
            tag.setTag("bonuses", list);
        }
        tag.setString("ward", ward);
        tag.setString("banner", banner);
        tag.setByte("tone", bannerTone);
        return tag;
    }

    public static BountyNote fromTag(NBTTagCompound tag) {
        BountyNote note = new BountyNote();
        note.name = tag.getString("name");
        note.slot = tag.getInteger("slot");
        note.open = tag.getInteger("open");
        note.full = tag.getBoolean("full");
        NBTTagList want = tag.getTagList("want", 10);
        for (int i = 0; i < want.tagCount(); i++) {
            NBTTagCompound l = want.getCompoundTagAt(i);
            String item = l.getString("item");
            if (!item.isEmpty()) {
                note.wanted.add(new BountyItem(new ResourceLocation(item), l.getInteger("m"),
                        l.getInteger("need"), l.getInteger("got")));
            }
        }
        note.chance = tag.getBoolean("chance");
        note.total = tag.getFloat("total");
        note.max = tag.getFloat("max");
        NBTTagList list = tag.getTagList("bonuses", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound l = list.getCompoundTagAt(i);
            note.bonuses.add(new Bonus(l.getString("name"), l.getFloat("bonus"),
                    l.getFloat("total")));
        }
        note.ward = tag.getString("ward");
        note.banner = tag.getString("banner");
        note.bannerTone = tag.getByte("tone");
        return note;
    }

    public static String percent(float value) {
        return (value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value))
                + "%";
    }
}
