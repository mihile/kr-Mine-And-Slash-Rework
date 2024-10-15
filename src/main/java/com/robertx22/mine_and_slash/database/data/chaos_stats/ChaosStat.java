package com.robertx22.mine_and_slash.database.data.chaos_stats;

import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.FilterListWrap;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.requirements.bases.GearRequestedFor;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.database.registry.ExileRegistryTypes;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.AffixData;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;

import java.util.ArrayList;
import java.util.List;

// todo will be used for both uniques, runed items, normal items etc

public class ChaosStat implements JsonExileRegistry<ChaosStat>, IAutoGson<ChaosStat>, IAutoLocName {

    public static ChaosStat SERIALIZER = new ChaosStat();


    public String id = "";
    public String name = "";
    public int weight = 1000;
    public int affix_number = 0;
    public Affix.AffixSlot affix_type = Affix.AffixSlot.chaos_stat;
    public int bonus_sockets = 0;
    public List<String> for_item_rarities = new ArrayList<>();

    public ChaosStat() {

    }

    public ChaosStat(String id, String name, int weight, int affix_number, Affix.AffixSlot affix_type, int bonus_sockets, List<String> for_item_rarities) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.affix_number = affix_number;
        this.affix_type = affix_type;
        this.bonus_sockets = bonus_sockets;
        this.for_item_rarities = for_item_rarities;
    }

    public void applyToGear(ExileStack stack) {

        var gear = stack.get(StackKeys.GEAR).get();

        stack.get(StackKeys.CUSTOM).edit(x -> x.data.set(CustomItemData.KEYS.CORRUPT, true));

        stack.get(StackKeys.POTENTIAL).getOrCreate().potential = 0;
        stack.get(StackKeys.POTENTIAL).save();

        for (int i = 0; i < bonus_sockets; i++) {
            gear.sockets.addSocket();
        }


        int tries = 0;
        while (gear.affixes.cor.size() < affix_number || tries > 100) {

            FilterListWrap<Affix> list = ExileDB.Affixes().getFilterWrapped(x -> x.type == this.affix_type);
            list = list.of(x -> x.meetsRequirements(new GearRequestedFor(gear)));

            var affix = list.random();

            AffixData data = new AffixData(Affix.AffixSlot.chaos_stat);
            data.create(gear, affix);

            if (gear.affixes.cor.stream().noneMatch(x -> x.id.equals(affix.GUID()))) {
                gear.affixes.cor.add(data);
            }
            tries++;
        }


    }

    @Override
    public ExileRegistryType getExileRegistryType() {
        return ExileRegistryTypes.CHAOS_STAT;
    }

    @Override
    public String GUID() {
        return id;
    }

    @Override
    public int Weight() {
        return weight;
    }

    @Override
    public Class<ChaosStat> getClassForSerialization() {
        return ChaosStat.class;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.CHAOS_STAT;
    }

    @Override
    public String locNameLangFileGUID() {
        return SlashRef.MODID + ".chaos_stat." + GUID();
    }

    @Override
    public String locNameForLangFile() {
        return name;
    }
}
