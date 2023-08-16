package com.robertx22.age_of_exile.mmorpg.registers.common.items;

import com.robertx22.age_of_exile.capability.player.BackpackItem;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.Def;
import com.robertx22.age_of_exile.mmorpg.registers.deferred_wrapper.RegObj;
import com.robertx22.age_of_exile.uncommon.interfaces.data_items.IRarity;
import com.robertx22.age_of_exile.vanilla_mc.items.misc.RarityStoneItem;

import java.util.HashMap;

public class RarityItems {

    public static HashMap<String, RegObj<RarityStoneItem>> RARITY_STONE = new HashMap<>();
    public static HashMap<String, RegObj<BackpackItem>> BACKPACKS = new HashMap<>();

    public static void init() {


        int tier = 0;
        for (String rar : IRarity.NORMAL_GEAR_RARITIES) {


            int finalTier = tier;

            RARITY_STONE.put(rar, Def.item(() -> new RarityStoneItem("Stone", finalTier), "stone/" + tier));
            BACKPACKS.put(rar, Def.item(() -> new BackpackItem(rar), "backpack/" + tier));


            tier++;


        }


    }
}
