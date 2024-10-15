package com.robertx22.mine_and_slash.saveclasses.prof_tool;

import com.google.common.collect.ImmutableList;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.profession.Profession;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.AdditionalBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.NameBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.OperationTipBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.RarityBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.affixdatablocks.SimpleItemStatBlock;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ITooltip;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.TooltipContext;
import com.robertx22.mine_and_slash.tags.all.SlotTags;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.TooltipUtils;
import com.robertx22.temp.SkillItemTier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfessionToolData implements ITooltip {

    public String prof = "";

    public List<ToolAffix> affixes = new ArrayList<>();


    public int lvl = 1;
    public int xp = 0;


    public List<ExactStatData> GetAllStats() {
        List<ExactStatData> list = new ArrayList<>();
        affixes.stream().forEach(x -> {
            try {
                ExileDB.Affixes().get(x.id).getStats().stream().map(e -> e.ToExactStat(x.p, lvl)).forEach(t -> list.add(t));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return list;
    }

    public Profession getProfession() {
        return ExileDB.Professions().get(prof);
    }

    public static boolean isCorrectTool(Profession prof, ItemStack stack) {

        if (prof.id.equals(Professions.MINING)) {
            return stack.is(ItemTags.PICKAXES) || stack.getItem() instanceof PickaxeItem;
        }
        if (prof.id.equals(Professions.FARMING)) {
            return stack.is(ItemTags.HOES) || stack.getItem() instanceof HoeItem;
        }
        if (prof.id.equals(Professions.FISHING)) {
            return stack.getItem() instanceof FishingRodItem;
        }
        if (prof.id.equals(Professions.HUSBANDRY)) {
            return stack.is(ItemTags.SWORDS) || stack.getItem() instanceof SwordItem;
        }

        return false;
    }

    public void addExp(Player p, ItemStack stack, int added) {

        xp += added;

        int currentXPNeeded = getExpNeeded();
        if (lvl < GameBalanceConfig.get().MAX_LEVEL) {
            while (xp >= currentXPNeeded) {

                if (lvl >= GameBalanceConfig.get().MAX_LEVEL) {
                    break;
                }

                var tier = SkillItemTier.fromLevel(lvl);
                lvl++;
                xp -= currentXPNeeded;

                var newtier = SkillItemTier.fromLevel(lvl);

                p.sendSystemMessage(Chats.TOOL_LEVEL_UP.locName(stack.getHoverName(), lvl).withStyle(ChatFormatting.YELLOW));

                if (tier != newtier) {
                    addStat();
                    p.sendSystemMessage(Chats.TOOL_ADD_STAT.locName(stack.getHoverName(), getRarity().locName()).withStyle(getRarity().textFormatting()));
                }
            }
        }
    }

    public void addStat() {

        GearRarity rar = ExileDB.GearRarities().random();

        ToolAffix data = new ToolAffix();
        data.rar = rar.GUID();
        data.p = rar.stat_percents.random();
        data.id = ExileDB.Affixes().getFilterWrapped(x -> x.type == Affix.AffixSlot.tool && x.getAllTagReq().contains(getProfession().tool_tag) && x.getAllTagReq().contains(SlotTags.tool.GUID())).random().GUID();

        this.affixes.add(data);
    }

    public GearRarity getRarity() {
        return ExileDB.GearRarities().get(getTier().rar);
    }

    public SkillItemTier getTier() {
        return SkillItemTier.fromLevel(lvl);
    }

    public int getExpNeeded() {
        return LevelUtils.getExpRequiredForLevel(lvl + 1) / 10;
    }

    @Override
    public void BuildTooltip(TooltipContext ctx) {

        /*while (ctx.tooltip.get(0).getString().equals(ctx.stack.getHoverName().getString()) || ctx.tooltip.get(0).getString().isBlank()){
            ctx.tooltip.remove(0);
        }

        while (ctx.tooltip.get(ctx.tooltip.size() - 1).getString().isBlank()){
            ctx.tooltip.remove(ctx.tooltip.size() - 1);
        }*/
        if (Screen.hasControlDown()) {
            return;
        }

        ExileTooltips exileTooltips = new ExileTooltips()
                .accept(new NameBlock(Collections.singletonList(ctx.stack.getHoverName())))
                //.accept(new AdditionalBlock(ctx.tooltip))
                .accept(new RarityBlock(getRarity()))
                .accept(new AdditionalBlock(ImmutableList.of(
                        TooltipUtils.level(lvl).withStyle(ChatFormatting.GREEN),
                        Itemtips.PROF_TOOL_EXP_TIP.locName(xp, getExpNeeded()).withStyle(ChatFormatting.GREEN)
                )))
                .accept(new SimpleItemStatBlock(new StatRangeInfo(ModRange.hide()))
                        .accept(Itemtips.PROF_TOOL_STATS_TIP.locName(), this.GetAllStats()))
                .accept(new OperationTipBlock().setCtrl().setAlt());

     
        List<Component> tooltip = ctx.tooltip;
        tooltip.clear();
        tooltip.addAll(exileTooltips.release());


    }

    public static class ToolAffix {

        public String id = "";
        public String rar = "";
        public int p = 0;
    }
}
