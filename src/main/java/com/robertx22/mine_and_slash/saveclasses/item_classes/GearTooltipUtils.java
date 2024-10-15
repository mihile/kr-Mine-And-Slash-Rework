package com.robertx22.mine_and_slash.saveclasses.item_classes;

import com.google.common.collect.ImmutableList;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.energy.Energy;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.IgnoreNullList;
import com.robertx22.mine_and_slash.gui.texts.StatCategory;
import com.robertx22.mine_and_slash.gui.texts.textblocks.*;
import com.robertx22.mine_and_slash.gui.texts.textblocks.dropblocks.LeagueBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.gearblocks.DurabilityBlock;
import com.robertx22.mine_and_slash.itemstack.CommonTooltips;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.IGearPartTooltip;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.*;
import com.robertx22.mine_and_slash.saveclasses.item_classes.tooltips.MergedStats;
import com.robertx22.mine_and_slash.saveclasses.item_classes.tooltips.TooltipStatInfo;
import com.robertx22.mine_and_slash.tags.imp.SlotTag;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.TooltipUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.robertx22.mine_and_slash.gui.texts.ExileTooltips.EMPTY_LINE;

public class GearTooltipUtils {

    public static void BuildTooltip(GearItemData gear, ItemStack stack, List<Component> tooltip, EntityData data) {

        if (gear.GetBaseGearType() == null) {
            return;
        }

        ExileStack exStack = ExileStack.of(stack);

        StatRangeInfo info = new StatRangeInfo(ModRange.hide());
        tooltip.clear();

        var etip = new ExileTooltips()
                .accept(new NameBlock(gear.GetDisplayName(exStack)))
                .accept(new RarityBlock(gear.getRarity()))
                .accept(new RequirementBlock()
                        .setStatRequirement(gear.getRequirement())
                        .setLevelRequirement(gear.getLevel()))
                .accept(new StatBlock() {
                    @Nonnull
                    private final GearItemData gearItemData = gear;
                    @Nonnull
                    private final StatRangeInfo tinfo = info;

                    public BaseStatsData baseStatsData;

                    public ImplicitStatsData implicitStatsData;

                    public GearAffixesData gearAffixesData;

                    @Nullable
                    public UniqueStatsData uniqueStatsData;

                    public GearSocketsData gearSocketsData;

                    @Nullable
                    public GearInfusionData gearEnchantData;

                    private static int getPriority(String s, Map<String, Integer> priorityMap) {
                        return priorityMap.entrySet().stream()
                                .filter(entry -> s.contains(entry.getKey()))
                                .map(Map.Entry::getValue)
                                .min(Integer::compare)
                                .orElse(Integer.MAX_VALUE);
                    }

                    @Override
                    public List<? extends Component> getAvailableComponents() {
                        IgnoreNullList<Component> list = new IgnoreNullList<>();


                        this.implicitStatsData = gearItemData.imp;
                        this.baseStatsData = gearItemData.baseStats;
                        this.gearAffixesData = gearItemData.affixes;
                        this.gearSocketsData = gearItemData.sockets;
                        this.gearEnchantData = gearItemData.ench;
                        this.uniqueStatsData = gearItemData.uniqueStats;

                        if (baseStatsData != null) {
                            list.addAll(baseStatsData.GetTooltipString(tinfo, exStack));
                            list.add(EMPTY_LINE);
                        }

                        boolean showMerge = !tinfo.useInDepthStats();
                        if (ClientConfigs.getConfig().IN_DEPTH_TOOLTIPS_BY_DEFAULT.get()) {
                            showMerge = false;
                        }

                        if (showMerge) {

                            List<ExactStatData> stats = new ArrayList<>();

                            stats.addAll(implicitStatsData.GetAllStats(exStack));

                            gearAffixesData.getAllAffixesAndSockets().forEach(x -> stats.addAll(x.GetAllStats(exStack)));

                            if (uniqueStatsData != null) {
                                stats.addAll(uniqueStatsData.GetAllStats(exStack));
                            }

                            Map<Boolean, List<ExactStatData>> map = stats.stream().collect(Collectors.groupingBy(x -> x.getStat().is_long));

                            MergedStats merged = new MergedStats(new ArrayList<>(), tinfo);
                            if (map.get(false) != null) {
                                merged = new MergedStats(map.get(false), tinfo);
                            }

                            HashMap<String, Integer> DPMap = new HashMap<>();
                            DPMap.put("damage", 1);
                            DPMap.put("dmg", 1);
                            DPMap.put("penetration", 2);

                            Comparator<TooltipStatInfo> damageAndPenetration = (s1, s2) -> {
                                int p1 = getPriority(s1.stat.GUID(), DPMap);
                                int p2 = getPriority(s2.stat.GUID(), DPMap);
                                return Integer.compare(p1, p2);
                            };


                            Map<String, Integer> priorityMap = new HashMap<>();
                            Arrays.asList(Elements.values()).forEach(x -> priorityMap.put(x.guidName, x.ordinal()));


                            Comparator<TooltipStatInfo> eleOrder = (s1, s2) -> {
                                int p1 = getPriority(s1.stat.GUID(), priorityMap);
                                int p2 = getPriority(s2.stat.GUID(), priorityMap);
                                return Integer.compare(p1, p2);
                            };
                            //handle and sort the tt.
                            //this map is used to store the TooltipStatInfos in order of StatCategory.
                            LinkedHashMap<String, ArrayList<TooltipStatInfo>> orderStatMap = new LinkedHashMap<>();
                            for (StatCategory category : StatCategory.values()) {
                                orderStatMap.put(category.name(), new ArrayList<>());
                            }
                            orderStatMap.put("other", new ArrayList<>());
                            //insert and sort the TooltipStatInfo into map.

                            for (TooltipStatInfo tooltipStatInfo : merged.mergedList) {
                                StatCategory.distributeStat(tooltipStatInfo, orderStatMap);
                            }


                            //get each TooltipStatInfo List, and turn it to tt.
                            for (ArrayList<TooltipStatInfo> tooltipStatInfos : orderStatMap.values()) {
                                tooltipStatInfos.sort(Comparator.comparing(o -> o.stat.GUID()));
                                tooltipStatInfos.sort(Comparator.comparing(o -> o.stat.GUID().length()));
                                tooltipStatInfos.sort(eleOrder);
                                tooltipStatInfos.sort(damageAndPenetration);
                                for (TooltipStatInfo tooltipStatInfo : tooltipStatInfos) {
                                    list.addAll(tooltipStatInfo.GetTooltipString());
                                }
                                //list.add(EMPTY_LINE);
                            }

                            Optional.ofNullable(map.get(true))
                                    .map(x -> x.stream()
                                            .flatMap(y -> y.GetTooltipString().stream())
                                            .toList()
                                    ).ifPresent(list::addAll);

                            list.add(EMPTY_LINE);

                        } else {
                            List<Component> impComps;
                            List<Component> uniComps = new ArrayList<>();
                            List<Component> prefixComps = new ArrayList<>();
                            List<Component> corComps = new ArrayList<>();
                            List<Component> suffixComps = new ArrayList<>();
                            impComps = implicitStatsData.GetTooltipString(tinfo, exStack);
                            if (uniqueStatsData != null)
                                uniComps = uniqueStatsData.GetTooltipString(tinfo, exStack);
                            var color = ChatFormatting.BLUE;
                            if (!gearAffixesData.getPreStatsWithCtx(gearItemData, tinfo).isEmpty()) {
                                prefixComps.add(Itemtips.PREFIX_STATS.locName().withStyle(color));
                                prefixComps.addAll(gearAffixesData.getPreStatsWithCtx(gearItemData, tinfo)
                                        .stream()
                                        .flatMap(x -> x.GetTooltipString().stream())
                                        .map(x -> (Component) x)
                                        .toList());
                            }

                            if (!gearAffixesData.getCorStatsWithCtx(gearItemData, tinfo).isEmpty()) {
                                corComps.add(Itemtips.COR_STATS.locName().withStyle(ChatFormatting.RED));
                                corComps.addAll(gearAffixesData.getCorStatsWithCtx(gearItemData, tinfo)
                                        .stream()
                                        .flatMap(x -> x.GetTooltipString().stream())
                                        .map(x -> (Component) x)
                                        .toList());
                            }

                            if (!gearAffixesData.getSufStatsWithCtx(gearItemData, tinfo).isEmpty()) {
                                suffixComps.add(Itemtips.SUFFIX_STATS.locName().withStyle(color));
                                suffixComps.addAll(gearAffixesData.getSufStatsWithCtx(gearItemData, tinfo)
                                        .stream()
                                        .flatMap(x -> x.GetTooltipString().stream())
                                        .map(x -> (Component) x)
                                        .toList());
                            }


                            Stream<List<Component>> addOrder = Stream.of(impComps, uniComps, prefixComps, suffixComps, corComps);
                            addOrder.forEachOrdered(x -> {
                                x.sort(Comparator.comparing(component -> component.getString().contains("\u25C6")));
                                list.addAll(x);
                                list.add(EMPTY_LINE);

                            });

                        }
                        //handle sockets and enchantment
                        IgnoreNullList<IGearPartTooltip> list3 = IgnoreNullList.of(gearSocketsData, gearEnchantData);
                        ListIterator<IGearPartTooltip> iGearPartTooltipListIterator = list3.listIterator();
                        while (iGearPartTooltipListIterator.hasNext()) {
                            list.addAll(iGearPartTooltipListIterator.next().GetTooltipString(tinfo, exStack));
                            if (iGearPartTooltipListIterator.hasNext()) {
                                list.add(EMPTY_LINE);
                            }
                        }

                        return list;
                    }
                })
                .accept(CommonTooltips.potentialCorruptionAndQuality(exStack))
                .accept(new AdditionalBlock(() -> {
                            int cost = (int) Energy.getInstance().scale(ModType.FLAT, gear.GetBaseGearType().getGearSlot().weapon_data.energy_cost_per_swing, data.getLevel());
                            int permob = (int) Energy.getInstance().scale(ModType.FLAT, gear.GetBaseGearType().getGearSlot().weapon_data.energy_cost_per_mob_attacked, data.getLevel());
                            float damageFactor = (gear.GetBaseGearType().getGearSlot().getBasicDamageMulti() * 100) / 100F;
                            return Collections.singletonList(Words.Energy_Cost_Per_Mob.locName(cost, permob, damageFactor).withStyle(ChatFormatting.GREEN));
                        }).showWhen(() -> info.hasShiftDown && gear.GetBaseGearType().getGearSlot().weapon_data.damage_multiplier > 0)
                )
                .accept(new AdditionalBlock(() -> {
                            return ImmutableList.of(Itemtips.ITEM_TYPE.locName(gear.GetBaseGearType().locName().withStyle(ChatFormatting.BLUE)),
                                    Words.TAGS.locName().append(TooltipUtils.joinMutableComps(gear.GetBaseGearType().getTags().getTags(SlotTag.SERIALIZER).stream().map(x -> ((SlotTag) x).locName()).toList().iterator(), Gui.COMMA_SEPARATOR.locName())));
                        }).showWhen(() -> info.hasShiftDown)
                );

        if (gear.isUnique() && !gear.uniqueStats.getUnique(exStack).league.isEmpty()) {
            etip.accept(new LeagueBlock(ExileDB.LeagueMechanics().get(gear.uniqueStats.getUnique(exStack).league)));
        }

        etip.accept(new SalvageBlock(gear, exStack))
                .accept(new OperationTipBlock().setAll())
                .accept(new DurabilityBlock(stack));

        tooltip.addAll(etip.release());


    }

}