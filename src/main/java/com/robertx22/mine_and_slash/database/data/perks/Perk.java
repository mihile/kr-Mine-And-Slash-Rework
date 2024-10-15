package com.robertx22.mine_and_slash.database.data.perks;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.OptScaleExactStat;
import com.robertx22.mine_and_slash.database.data.spell_school.SpellSchool;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.types.LearnSpellStat;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.saveclasses.spells.SpellSchoolsData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientTextureUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.TooltipStatsAligner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Perk implements JsonExileRegistry<Perk>, IAutoGson<Perk>, IAutoLocName {
    public static Perk SERIALIZER = new Perk();

    public PerkType type;
    public String id;
    public String icon = "";
    public String one_kind = null;
    public transient String locname = "";
    public boolean is_entry = false;
    public int max_lvls = 1;

    public List<OptScaleExactStat> stats = new ArrayList<>();


    transient ResourceLocation cachedIcon = null;

    public SpellSchoolsData.PointType getPointType() {
        return isSpell() ? SpellSchoolsData.PointType.SPELL : SpellSchoolsData.PointType.PASSIVE;
    }

    // todo
    public int getMaxLevel() {
        return max_lvls;
    }

    public boolean isSpell() {
        return this.stats.size() > 0 && stats.get(0).getStat() instanceof LearnSpellStat;
    }

    public Spell getSpell() {
        if (stats.get(0).getStat() instanceof LearnSpellStat a) {
            return a.spell;
        }
        return null;
    }

    public boolean isPassive() {
        return !isSpell() && this.max_lvls > 1;
    }

    public Optional<SpellSchool> getSpellSchool() {
        var opt = ExileDB.SpellSchools().getList().stream().filter(x -> x.perks.containsKey(GUID())).findFirst();
        return opt;
    }

    public ResourceLocation getIcon() {
        if (cachedIcon == null) {
            ResourceLocation id = new ResourceLocation(icon);
            if (ClientTextureUtils.textureExists(id)) {
                cachedIcon = id;
            } else {
                cachedIcon = Stat.MISSING_ICON;
            }
        }
        return cachedIcon;
    }

    @Override
    public int Weight() {
        return 1000;
    }


    public List<Component> GetTooltipString(StatRangeInfo info) {
        List<Component> list = new ArrayList<>();

        try {


            if (this.type != PerkType.STAT && type != PerkType.MAJOR && type != PerkType.SPECIAL) {
                list.add(locName().withStyle(type.format));
            }
            if (type == PerkType.MAJOR) {
                // to get rid of like 100 lines of lang file
                list.add(this.locName().withStyle(ChatFormatting.DARK_PURPLE));
                // list.add(Component.empty());

            }
            //   info.statTooltipType = StatTooltipType.NORMAL;


            if (isPassive()) {
                int lvl = Load.player(info.player).ascClass.getLevel(GUID());
                if (lvl < 1) {
                    lvl = 1;
                }
                int finalLvl = lvl;
                var scaled = stats.stream().map(x -> {
                    var d = x.toExactStat(1);
                    d.percentIncrease = (finalLvl - 1) * 100;
                    d.increaseByAddedPercent();
                    return d;
                });

                scaled.forEach(x -> list.addAll(x.GetTooltipString()));

            } else {
                List<Component> preList = new ArrayList<>();
                stats.forEach(x -> preList.addAll(x.GetTooltipString(info)));
                list.addAll(new TooltipStatsAligner(preList).buildNewTooltipsStats());
            }


            if (this.one_kind != null) {
                list.add(Chats.ONLY_ONE_OF_TYPE.locName().withStyle(ChatFormatting.GREEN));

                list.add(Component.translatable(SlashRef.MODID + ".one_of_a_kind." + one_kind).withStyle(ChatFormatting.GREEN));
            }


            if (this.type == PerkType.MAJOR) {

                list.add(Words.GAME_CHANGER.locName().withStyle(ChatFormatting.RED));
            }

            if (stats.size() > 0) {
                if (stats.get(0).getStat() instanceof LearnSpellStat spell) {
                    var data = Load.player(ClientOnly.getPlayer()).spellCastingData.getSpellData(spell.spell.GUID());
                    if (data.rank > 0) {
                        list.addAll(data.getData().getTooltip(info.player));
                    } else {
                        list.addAll(spell.spell.GetTooltipString(new StatRangeInfo(ModRange.alwaysMax())));
                    }
                }
            }

            //list.add(ExileText.newLine().get());
            if (!(stats.get(0).getStat() instanceof LearnSpellStat)) {
                list.add(Words.PressAltForStatInfo.locName().withStyle(ChatFormatting.BLUE));
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Talents;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".talent." + id;
    }

    @Override
    public String locNameForLangFile() {
        return locname;
    }

    public enum Connection {
        LINKED, BLOCKED, POSSIBLE
    }

    public enum PerkType {
        STAT("stat", 24, 16, 4, ChatFormatting.WHITE),
        SPECIAL("special", 28, 16, 6, ChatFormatting.LIGHT_PURPLE),
        MAJOR("major", 33, 16, 9, ChatFormatting.RED),
        START("start", 28, 16, 6, ChatFormatting.YELLOW),
        ASC("asc", 56, 32, 14, ChatFormatting.GOLD);


        String id;
        public int size;
        public int iconSize;
        public int off;
        public ChatFormatting format;

        public ResourceLocation yes;
        public ResourceLocation no;


        public ResourceLocation yesColor = SlashRef.guiId("skill_tree/indic/yes");
        public ResourceLocation canColor = SlashRef.guiId("skill_tree/indic/can");
        public ResourceLocation noColor = SlashRef.guiId("skill_tree/indic/no");


        PerkType(String id, int size, int iconSize, int off, ChatFormatting format) {
            this.id = id;
            this.size = size;
            this.iconSize = iconSize;
            this.format = format;
            this.off = off;

            this.yes = SlashRef.guiId("skill_tree/borders/" + id + "_on");
            this.no = SlashRef.guiId("skill_tree/borders/" + id + "_off");
        }

        public ResourceLocation getColorTexture(PerkStatus s) {

            if (s == PerkStatus.BLOCKED) {
                return noColor;
            }
            if (s == PerkStatus.POSSIBLE) {
                return canColor;
            }
            if (s == PerkStatus.CONNECTED) {
                return yesColor;
            }
            return null;
        }

        public ResourceLocation getBorderTexture(PerkStatus status) {
            if (status == PerkStatus.CONNECTED) {
                return yes;
            }
            return no;
        }


        public float getOffset() {
            if (true) {
                return ((size - iconSize) * 0.5F) + 0.5F;
            }
            return off;
        }

    }


    public PerkType getType() {
        return type;
    }


    @Override
    public Class<Perk> getClassForSerialization() {
        return Perk.class;
    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.PERK;
    }

    @Override
    public String GUID() {
        return id;
    }
}
