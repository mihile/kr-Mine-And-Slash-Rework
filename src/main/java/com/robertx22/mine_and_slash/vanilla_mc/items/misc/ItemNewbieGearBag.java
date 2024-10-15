package com.robertx22.mine_and_slash.vanilla_mc.items.misc;

import com.robertx22.mine_and_slash.aoe_data.database.gear_slots.GearSlots;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.talent_tree.TalentTree;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.CustomItemData;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.loot.LootInfo;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ItemNewbieGearBag extends Item {

    public ItemNewbieGearBag() {
        super(new Properties());
    }


    static HashMap<String, NewbieContent> MAP = new HashMap<>();
    static NewbieContent defaultContent = new NewbieContent(Arrays.asList(GearSlots.STAFF, GearSlots.SWORD, GearSlots.BOW));

    static {
    }

    static void giveNewbieItemsFor(Player player, Perk perk) {

        NewbieContent c = defaultContent;

        if (MAP.containsKey(perk.GUID())) {
            c = MAP.get(perk.GUID());
        }

        c.give(player);

    }

    static class NewbieContent {

        public List<String> gearslots;

        public List<ItemStack> stacks = new ArrayList<>();

        public NewbieContent(List<String> gearslots) {
            this.gearslots = gearslots;

        }

        public NewbieContent addStack(ItemStack stack) {
            this.stacks.add(stack);
            return this;
        }


        public void give(Player player) {


            gearslots.forEach(x -> {
                BaseGearType gear = ExileDB.GearTypes()
                        .getFilterWrapped(e -> e.gear_slot.equals(x))
                        .random();

                GearBlueprint b = new GearBlueprint(LootInfo.ofLevel(1));
                b.level.set(1);
                b.rarity.set(ExileDB.GearRarities()
                        .get(IRarity.COMMON_ID));
                b.gearItemSlot.set(gear);


                var ex = b.createData();

                var data = ex.get(StackKeys.GEAR);

                ex.getOrCreate(StackKeys.CUSTOM).data.set(CustomItemData.KEYS.SALVAGING_DISABLED, true);

                ItemStack stack = data.GetBaseGearType().getRandomItem(data.getRarity()).getDefaultInstance();

                data.saveToStack(stack);

                
                var exfi = ExileStack.of(stack);

                ex.apply(exfi);

                stack = exfi.getStack();

                EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(Enchantments.UNBREAKING, 3));

                PlayerUtils.giveItem(stack, player);

            });

            stacks.forEach(x -> PlayerUtils.giveItem(x, player));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        if (!worldIn.isClientSide) {
            try {

                List<Perk> starts = Load.player(playerIn).talents
                        .getAllAllocatedPerks(TalentTree.SchoolType.TALENTS)
                        .values()
                        .stream()
                        .filter(x -> x.is_entry)
                        .collect(Collectors.toList());

                if (true || !starts.isEmpty()) { // todo

                    defaultContent.give(playerIn);
                    // ItemNewbieGearBag.giveNewbieItemsFor(playerIn, starts.get(0));


                    playerIn.getItemInHand(handIn)
                            .shrink(1);

                } else {
                    playerIn.displayClientMessage(Component.literal("Choose your path to open this. (Press [H] and then open Talent Tree scren"), false);
                }

                return new InteractionResultHolder<ItemStack>(InteractionResult.PASS, playerIn.getItemInHand(handIn));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new InteractionResultHolder<ItemStack>(InteractionResult.PASS, playerIn.getItemInHand(handIn));
    }


}
