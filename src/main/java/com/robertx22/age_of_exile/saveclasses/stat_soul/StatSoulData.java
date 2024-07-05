package com.robertx22.age_of_exile.saveclasses.stat_soul;

import com.robertx22.age_of_exile.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.age_of_exile.database.data.gear_slots.GearSlot;
import com.robertx22.age_of_exile.database.data.gear_types.bases.SlotFamily;
import com.robertx22.age_of_exile.database.data.rarities.GearRarity;
import com.robertx22.age_of_exile.database.data.unique_items.UniqueGear;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.gui.inv_gui.actions.auto_salvage.ToggleAutoSalvageRarity;
import com.robertx22.age_of_exile.gui.texts.ExileTooltips;
import com.robertx22.age_of_exile.gui.texts.textblocks.AdditionalBlock;
import com.robertx22.age_of_exile.gui.texts.textblocks.InformationBlock;
import com.robertx22.age_of_exile.gui.texts.textblocks.RarityBlock;
import com.robertx22.age_of_exile.loot.LootInfo;
import com.robertx22.age_of_exile.loot.blueprints.GearBlueprint;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.RarityItems;
import com.robertx22.age_of_exile.mmorpg.registers.common.items.SlashItems;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipContext;
import com.robertx22.age_of_exile.saveclasses.gearitem.gear_bases.TooltipInfo;
import com.robertx22.age_of_exile.saveclasses.item_classes.GearItemData;
import com.robertx22.age_of_exile.uncommon.MathHelper;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.datasaving.StackSaving;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.ICommonDataItem;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.ISettableLevelTier;
import com.robertx22.age_of_exile.uncommon.localization.Chats;
import com.robertx22.age_of_exile.uncommon.localization.Itemtips;
import com.robertx22.age_of_exile.uncommon.utilityclasses.ClientOnly;
import com.robertx22.age_of_exile.uncommon.utilityclasses.LevelUtils;
import com.robertx22.age_of_exile.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.library_of_exile.utils.ItemstackDataSaver;
import com.robertx22.temp.SkillItemTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class StatSoulData implements ICommonDataItem<GearRarity>, ISettableLevelTier {


    public int tier = 1;

    public String slot = "";

    public String rar = "";

    public SlotFamily fam = SlotFamily.NONE;

    public String uniq = "";

    public String force_tag = "";

    public boolean can_sal = true;


    public GearItemData gear = null;


    // todo how do i make the result accept nbt.
    // and how to make jei accept nbt

    // i COULD make each of these an item ? and have profession set the correct tier?

    public static StatSoulData ofFamily(GearRarity rar, SkillItemTier tier, SlotFamily fam) {
        StatSoulData data = new StatSoulData();
        data.tier = tier.tier;
        data.fam = fam;
        data.rar = rar.GUID();
        return data;
    }

    public boolean canBeOnAnySlot() {
        return slot.isEmpty();
    }

    public void setCanBeOnAnySlot() {
        this.slot = "";
    }

    public ItemStack toStack() {


        ItemStack stack = new ItemStack(SlashItems.STAT_SOUL.get());

        StackSaving.STAT_SOULS.saveTo(stack, this);

        if (!slot.isEmpty()) {
            stack.getOrCreateTag()
                    .putInt("CustomModelData", ExileDB.GearSlots()
                            .get(slot).model_num);
        }

        return stack;

    }

    public void insertAsUnidentifiedOn(ItemStack stack, Player p) {
        if (gear != null) {
            StackSaving.GEARS.saveTo(stack, gear);
        } else {
            StackSaving.GEARS.saveTo(stack, this.createGearData(stack, p));
            //LoadSave.Save(this, stack.getOrCreateTag(), StatSoulItem.TAG);
        }
    }

    public GearSlot getSlotFor(ItemStack stack) {
        GearSlot gearslot = ExileDB.GearSlots().random();

        if (!slot.isEmpty()) {
            gearslot = ExileDB.GearSlots().get(slot);
        }
        if (stack != null) {
            gearslot = GearSlot.getSlotOf(stack.getItem());
        }
        return gearslot;
    }

    public boolean forcesTag() {
        return !force_tag.isEmpty();
    }

    public boolean isArmor() {
        return this.fam == SlotFamily.Armor || (!this.slot.isEmpty() && ExileDB.GearSlots().get(slot).fam == SlotFamily.Armor);
    }

    public boolean canApplyTo(ItemStack stack) {
        GearSlot slot = GearSlot.getSlotOf(stack.getItem());

        if (slot == null) {
            return false;
        }
        if (canBeOnAnySlot()) {
            return slot != null;
        }
        if (fam != SlotFamily.NONE) {
            if (slot.fam == fam) {
                return true;
            }
        }
        if (!this.slot.isEmpty()) {
            return this.slot.equals(slot.GUID());
        }

        return false;
    }


    public GearItemData createGearData(@Nullable ItemStack stack, Player p) {

        int lvl = MathHelper.clamp(Load.Unit(p).getLevel(), LevelUtils.tierToLevel(tier).getMinLevel(), LevelUtils.tierToLevel(tier).getMaxLevel());

        GearBlueprint b = new GearBlueprint(LootInfo.ofLevel(lvl));
        b.level.set(lvl);
        b.rarity.set(ExileDB.GearRarities()
                .get(rar));


        GearSlot gearslot = getSlotFor(stack);
        String slotid = gearslot.GUID();

        b.gearItemSlot.set(ExileDB.GearTypes()
                .getFilterWrapped(x -> x.gear_slot.equals(slotid) && (!forcesTag() ? true : x.tags.contains(force_tag)))
                .random());

        UniqueGear uniq = ExileDB.UniqueGears().get(this.uniq);

        if (!uniq.isEmpty()) {
            b.uniquePart.set(uniq);
            b.rarity.set(uniq.getUniqueRarity());
            b.gearItemSlot.set(uniq.getBaseGear());
        }

        GearItemData gear = b.createData();

        gear.setPotential((int) (gear.getPotentialNumber() * GameBalanceConfig.get().CRAFTED_GEAR_POTENTIAL_MULTI));

        gear.data.set(GearItemData.KEYS.SALVAGING_DISABLED, !this.can_sal);
        return gear;
    }

    public boolean canInsertIntoStack(ItemStack stack) {

        if (stack.isEmpty()) {
            return false;
        }

        if (StackSaving.GEARS.has(stack)) {
            return false;
        }

        if (this.gear != null) {
            return GearSlot.isItemOfThisSlot(gear.GetBaseGearType().getGearSlot(), stack.getItem());
        }

        return canApplyTo(stack);

    }

    @Override
    public String getRarityId() {
        return rar;
    }

    @Override
    public GearRarity getRarity() {
        return ExileDB.GearRarities().get(rar);
    }


    @Override
    public void setTier(int tier) {
        this.tier = tier;
    }

    @Override
    public void BuildTooltip(TooltipContext ctx) {

    }

    @Override
    public int getLevel() {
        return LevelUtils.levelToTier(tier);
    }

    @Override
    public ItemstackDataSaver<? extends ICommonDataItem> getStackSaver() {
        return StackSaving.STAT_SOULS;
    }

    public ExileTooltips getTooltip(ItemStack stack, boolean cangen) {

        ExileTooltips exileTooltips = new ExileTooltips()
                .accept(new RarityBlock(getRarity()));

        if (this.gear != null) {
            List<Component> tooltip = new ArrayList<>();
            this.gear.BuildTooltip(new TooltipContext(stack, tooltip, Load.Unit(ClientOnly.getPlayer())));
            exileTooltips.accept(new AdditionalBlock(tooltip));
        } else {
            List<Component> tierPart = new ArrayList<>();
            tierPart.add(TooltipUtils.gearTier(this.tier));
            if (new TooltipInfo().hasAltDown){
                tierPart.add(Itemtips.SOUL_TIER_TIP.locName().withStyle(ChatFormatting.BLUE));
            }
            if (this.canBeOnAnySlot()) {

            } else {
                if (this.fam != SlotFamily.NONE) {
                    tierPart.add(Itemtips.ITEM_TYPE.locName(Component.literal(this.fam.name()).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
                } else {
                    tierPart.add(Itemtips.ITEM_TYPE.locName(ExileDB.GearSlots()
                            .get(this.slot)
                            .locName().withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
                }
            }
            exileTooltips.accept(new AdditionalBlock(tierPart));

        }
        ArrayList<Component> thirdPart = new ArrayList<>();
        thirdPart.add(Chats.INFUSES_STATS.locName().withStyle(ChatFormatting.AQUA));
        thirdPart.add(TooltipUtils.dragOntoGearToUse());
        if (cangen) {
            thirdPart.add(Chats.RIGHT_CLICK_TO_GEN_ITEM.locName().withStyle(ChatFormatting.AQUA));
        }
        exileTooltips.accept(new AdditionalBlock(thirdPart));
        exileTooltips.accept(new InformationBlock().setAlt());
        return exileTooltips;
    }

    @Override
    public void saveToStack(ItemStack stack) {
        StackSaving.STAT_SOULS.saveTo(stack, this);
    }

    @Override
    public List<ItemStack> getSalvageResult(ItemStack stack) {
        int amount = 1;
        return Arrays.asList(new ItemStack(RarityItems.RARITY_STONE.getOrDefault(getRarity().GUID(), RarityItems.RARITY_STONE.get(IRarity.COMMON_ID)).get(), amount));
    }


    @Override
    public ToggleAutoSalvageRarity.SalvageType getSalvageType() {
        return ToggleAutoSalvageRarity.SalvageType.GEAR;
    }
}
