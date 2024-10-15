package com.robertx22.mine_and_slash.aoe_data.database.affixes;

import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.requirements.Requirements;
import com.robertx22.mine_and_slash.database.data.requirements.TagRequirement;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.types.JewelEffect;
import com.robertx22.mine_and_slash.tags.TagType;
import com.robertx22.mine_and_slash.tags.imp.SlotTag;
import com.robertx22.mine_and_slash.uncommon.enumclasses.ModType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AffixBuilder {

    String guid;
    List<StatMod> stats = new ArrayList<>();
    String langName = "";
    boolean allowDupli = false;
    int weight = 1000;
    public Affix.AffixSlot type;

    public String auraReq = "";
    public String oneofakind = "";

    TagRequirement tagRequirement = new TagRequirement(TagType.GearSlot, new ArrayList<>(), new ArrayList<>());

    private AffixBuilder(String id) {
        this.guid = id;
    }

    public static AffixBuilder Normal(String id) {
        return new AffixBuilder(id);
    }

    public static AffixBuilder Paragon(String id, String name) {
        return AffixBuilder.Normal("paragon_" + id)
                .Named("Of Paragon's " + name)
                .Weight(50)
                .stats(JewelEffect.getInstance().mod(1, 5))
                .Suffix();
    }

    public AffixBuilder Named(String name) {
        langName = name;
        return this;
    }

    public AffixBuilder AuraReq(String aura) {
        auraReq = aura;
        return this;
    }

    public AffixBuilder OneOfAKind(String one) {
        oneofakind = one;
        return this;
    }


    public AffixBuilder includesTags(SlotTag... tags) {
        this.tagRequirement.included.addAll(Arrays.stream(tags)
                .map(x -> x.GUID())
                .collect(Collectors.toList()));
        return this;
    }

    public AffixBuilder mustIncludesAllTags(SlotTag... tags) {
        this.tagRequirement.included.addAll(Arrays.stream(tags)
                .map(x -> x.GUID())
                .collect(Collectors.toList()));
        this.tagRequirement.req_type = TagRequirement.ReqType.HAS_ALL;
        return this;
    }

    public AffixBuilder excludesTags(SlotTag... tags) {
        this.tagRequirement.excluded.addAll(Arrays.stream(tags)
                .map(x -> x.GUID())
                .collect(Collectors.toList()));
        return this;
    }

    public AffixBuilder Weight(int weight) {
        this.weight = weight;
        return this;
    }


    public AffixBuilder coreStat(Stat stat) {
        return this.stats(new StatMod(2, 15, stat, ModType.FLAT));
    }

    public AffixBuilder bigCoreStat(Stat stat) {
        return this.stats(new StatMod(3, 25, stat, ModType.FLAT));
    }

    public AffixBuilder stats(StatMod... stats) {
        this.stats.addAll(Arrays.asList(stats));
        return this;
    }

    public AffixBuilder stat(StatMod stat) {
        this.stats.add(stat);
        return this;
    }

    public AffixBuilder AllowDuplicatesOnSameItem() {
        allowDupli = true;
        return this;
    }

    public AffixBuilder WatchersEye() {
        type = Affix.AffixSlot.watcher_eye;
        return this;
    }

    public AffixBuilder JewelCorruption() {
        type = Affix.AffixSlot.jewel_corruption;
        return this;
    }

    public AffixBuilder GearCorrupt() {
        type = Affix.AffixSlot.chaos_stat;
        return this;
    }

    public AffixBuilder Prefix() {
        type = Affix.AffixSlot.prefix;
        return this;
    }

    public AffixBuilder Tool() {
        type = Affix.AffixSlot.tool;
        return this;
    }

    public AffixBuilder Suffix() {
        type = Affix.AffixSlot.suffix;
        return this;
    }

    public AffixBuilder Jewel() {
        type = Affix.AffixSlot.jewel;
        return this;
    }

    public AffixBuilder craftedUniqueJewel() {
        type = Affix.AffixSlot.crafted_jewel_unique;
        return this;
    }

    public AffixBuilder Enchant() {
        type = Affix.AffixSlot.enchant;
        return this;
    }

    public AffixBuilder Implicit() {
        type = Affix.AffixSlot.implicit;
        return this;
    }

    public void Build() {

        Affix affix = new Affix();
        affix.guid = guid;

        affix.one_of_a_kind = oneofakind;

        affix.requirements = new Requirements(this.tagRequirement);

        affix.stats = stats;

        affix.only_one_per_item = !allowDupli;
        affix.type = type;
        affix.weight = weight;
        affix.loc_name = langName;

        affix.eye_aura_req = auraReq;

        affix.addToSerializables();

    }

}
