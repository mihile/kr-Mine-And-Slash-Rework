package com.robertx22.mine_and_slash.capability.player.data;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.capability.player.container.BackpackMenu;
import com.robertx22.mine_and_slash.capability.player.helper.BackpackInventory;
import com.robertx22.mine_and_slash.database.data.currency.IItemAsCurrency;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.RuneItem;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.RarityStoneItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

public class Backpacks {

    public Backpacks(Player player) {
        this.player = player;

        for (BackpackType type : BackpackType.values()) {
            map.put(type, new BackpackInventory(player, type, MAX_SIZE));
        }
    }


    public enum BackpackType {
        GEARS("gear", Words.Gear) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.GEARS.has(stack) || StackSaving.JEWEL.has(stack) || StackSaving.STAT_SOULS.has(stack);
            }
        },
        MAPS("map", Words.Maps) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.MAP.has(stack);
            }
        },
        CURRENCY("currency", Words.Currency) {
            @Override
            public boolean isValid(ItemStack stack) {
                return stack.getItem() instanceof IItemAsCurrency || stack.getItem() instanceof RuneItem || stack.getItem() instanceof RarityStoneItem;
            }
        },
        SKILL_GEMS("skill_gem", Words.SkillGem) {
            @Override
            public boolean isValid(ItemStack stack) {
                return StackSaving.SKILL_GEM.has(stack);
            }
        },
        PROFESSION("profession", Words.PROFESSIONS) {
            @Override
            public boolean isValid(ItemStack stack) {
                Item item = stack.getItem();
                return item instanceof IGoesToBackpack;
            }
        };

        public String id;
        public Words name;

        BackpackType(String id, Words name) {
            this.id = id;
            this.name = name;
        }

        public ResourceLocation getIcon() {
            return SlashRef.guiId("backpack/" + id);
        }

        public abstract boolean isValid(ItemStack stack);
    }

    Player player;

    public static int MAX_SIZE = 6 * 9;

    private HashMap<BackpackType, BackpackInventory> map = new HashMap<>();


    public BackpackInventory getInv(BackpackType type) {
        return map.get(type);
    }


    public void tryAutoPickup(Player p, ItemStack stack) {

        if (p.getInventory().countItem(SlashItems.MASTER_BAG.get()) < 1) {
            return;
        }

        for (BackpackType type : BackpackType.values()) {
            if (type.isValid(stack)) {
                var bag = getInv(type);

                if (bag.hasFreeSlots()) {
                    bag.addItem(stack.copy());
                    stack.shrink(stack.getCount() + 10); // just in case
                    SoundUtils.playSound(this.player, SoundEvents.ITEM_PICKUP);
                    return;
                }
            }
        }


    }
    // todo every time before you open backpack, it will replace locked slots with blocked slots that cant be clicked on and throw out/give items back

    public void openBackpack(BackpackType type, Player p, int rows) {
        if (!p.level().isClientSide) {

            if (!p.getMainHandItem().is(SlashItems.MASTER_BAG.get())) {
                return;
            }

            BackpackInventory inv = getInv(type);
            //inv.throwOutBlockedSlotItems(rows * 9);
            p.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) -> {
                return new BackpackMenu(type, i, playerInventory, inv, rows);
            }, Component.literal("")));
        }
    }
}
