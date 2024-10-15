package com.robertx22.mine_and_slash.mixin_methods;

import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.database.data.auto_item.AutoItem;
import com.robertx22.mine_and_slash.database.data.currency.IItemAsCurrency;
import com.robertx22.mine_and_slash.database.data.currency.base.ModifyResult;
import com.robertx22.mine_and_slash.database.data.currency.loc_reqs.LocReqContext;
import com.robertx22.mine_and_slash.database.data.currency.reworked.ExileCurrency;
import com.robertx22.mine_and_slash.database.data.currency.reworked.item_mod.ItemModification;
import com.robertx22.mine_and_slash.database.data.profession.items.CraftedSoulItem;
import com.robertx22.mine_and_slash.database.data.runewords.RuneWord;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.SavedGearSoul;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulData;
import com.robertx22.mine_and_slash.saveclasses.stat_soul.StatSoulItem;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import com.robertx22.mine_and_slash.vanilla_mc.items.SoulExtractorItem;
import com.robertx22.mine_and_slash.vanilla_mc.items.TagForceSoulItem;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.RarityStoneItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import java.util.ArrayList;
import java.util.List;

public class OnItemInteract {

    static class Result {

        public boolean can;

        public Result(boolean can) {
            this.can = can;
        }

        private boolean doDing = false;

        public Result ding() {
            this.doDing = true;
            return this;
        }
    }

    public abstract static class ClickFeature {
        public abstract Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot);
    }

    static List<ClickFeature> CLICKS = new ArrayList<>();


    public static void register() {

        // new datapack currencies
        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {
                var opt = ExileCurrency.get(currency);

                if (opt.isPresent()) {
                    LocReqContext ctx = new LocReqContext(player, craftedStack, currency);

                    var cur = opt.get();
                    if (!craftedStack.isEmpty()) {
                        var can = cur.canItemBeModified(ctx);

                        if (can.can) {
                            var result = cur.modifyItem(ctx);

                            if (result.resultEnum != ModifyResult.SUCCESS) {
                                player.sendSystemMessage(result.result.answer);
                                return new Result(false);
                            }

                            craftedStack.shrink(1); // seems the currency creates a copy of a new item, so we delete the old one
                            currency.shrink(1);
                            // PlayerUtils.giveItem(result, player);
                            slot.set(result.stack.getStack().copy());

                            if (result.outcome == ItemModification.OutcomeType.BAD) {
                                SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.GLASS_BREAK, 1, 1);
                                return new Result(true);
                            } else {
                                return new Result(true).ding();
                            }
                        } else {
                            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.VILLAGER_NO, 1, 1);
                            player.sendSystemMessage(can.answer);
                        }
                    }

                }
                return new Result(false);
            }
        });

        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {

                if (StackSaving.JEWEL.has(craftedStack)) {
                    var data = StackSaving.JEWEL.loadFrom(craftedStack);

                    if (data.uniq.isCraftableUnique()) {
                        ItemStack cost = data.uniq.getStackNeededForUpgrade();

                        if (cost.getItem() == currency.getItem()) {
                            if (currency.getCount() >= cost.getCount()) {
                                if (data.uniq.getCraftedTier().canUpgradeMore()) {
                                    data.uniq.upgradeUnique(data);

                                    StackSaving.JEWEL.saveTo(craftedStack, data);
                                    currency.shrink(cost.getCount());

                                    return new Result(true).ding();
                                }
                            }
                        }
                    }
                }

                return new Result(false);
            }
        });

        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {
                if (currency.getItem() instanceof TagForceSoulItem force) {
                    if (craftedStack.getCount() == 1) {
                        StatSoulData data = StackSaving.STAT_SOULS.loadFrom(craftedStack);
                        if (data != null && data.isArmor()) {
                            data.force_tag = force.tag.tag;
                            data.saveToStack(craftedStack);
                            currency.shrink(1);

                            return new Result(true).ding();

                        } else {
                            if (craftedStack.getItem() instanceof CraftedSoulItem i) {
                                var soul = i.getSoul(craftedStack);

                                if (soul != null && soul.isArmor()) {
                                    craftedStack.getOrCreateTag().putString("force_tag", force.tag.tag);
                                    currency.shrink(1);
                                    return new Result(true).ding();
                                }
                            }
                        }
                    }
                }
                return new Result(false);
            }
        });

        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {
                if (craftedStack.isDamaged() && currency.getItem() instanceof RarityStoneItem) {

                    if (!StackSaving.GEARS.has(craftedStack)) {
                        return new Result(false);
                    }
                    RarityStoneItem essence = (RarityStoneItem) currency.getItem();

                    SoundUtils.playSound(player, SoundEvents.ANVIL_USE, 1, 1);

                    int repair = essence.getTotalRepair();

                    craftedStack.setDamageValue(craftedStack.getDamageValue() - repair);

                    currency.shrink(1);
                    return new Result(true).ding();
                }
                return new Result(false);
            }
        });

        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {
                if (currency.getItem() instanceof StatSoulItem || currency.getItem() instanceof CraftedSoulItem) {
                    StatSoulData data = StackSaving.STAT_SOULS.loadFrom(currency);
                    if (currency.getItem() instanceof CraftedSoulItem cs) {
                        data = cs.getSoul(currency);
                    }
                    if (data != null) {
                        if (data.canInsertIntoStack(craftedStack)) {
                            if (craftedStack.getCount() == 1) {
                                ItemStack result = data.insertAsUnidentifiedOn(craftedStack, player);
                                craftedStack.shrink(1);
                                slot.set(result);
                                currency.shrink(1);
                                return new Result(true).ding();
                            }
                        }
                    }

                }
                return new Result(false);
            }
        });


        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {
                if (currency.getItem() instanceof IItemAsCurrency) {
                    LocReqContext ctx = new LocReqContext(player, craftedStack, currency);

                    if (!craftedStack.isEmpty()) {
                        var can = ctx.effect.canItemBeModified(ctx);
                        if (can.can) {
                            ItemStack result = ctx.effect.modifyItem(ctx).stack.getStack().copy();
                            craftedStack.shrink(1); // seems the currency creates a copy of a new item, so we delete the old one
                            currency.shrink(1);
                            // PlayerUtils.giveItem(result, player);
                            slot.set(result);
                            return new Result(true);
                        } else {
                            player.sendSystemMessage(can.answer);
                        }
                    }
                }
                return new Result(false);
            }
        });


        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {

                if (currency.getItem() == SlashItems.SOCKET_EXTRACTOR.get()) {

                    GearItemData gear = StackSaving.GEARS.loadFrom(craftedStack);

                    if (gear != null) {
                        if (gear.sockets != null && gear.sockets.getSocketed().size() > 0) {
                            try {


                                int index = gear.sockets.lastFilledSocketIndex();

                                if (index > -1) {
                                    RuneWord runeword = null;
                                    if (gear.sockets.hasRuneWord()) {
                                        runeword = gear.sockets.getRuneWord();
                                    }
                                    var s = gear.sockets.getSocketed().get(index);

                                    ItemStack gem = s.getOriginalItemStack();

                                    gear.sockets.getSocketed().remove(index);

                                    if (gear.sockets.hasRuneWord()) {

                                        if (!runeword.hasMatchingRunesToCreate(gear)) {
                                            gear.sockets.removeRuneword();
                                        }
                                    }

                                    StackSaving.GEARS.saveTo(craftedStack, gear);

                                    PlayerUtils.giveItem(gem, player);
                                    currency.shrink(1);
                                    return new Result(true).ding();

                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }
                return new Result(false);
            }
        });
        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {
                if (currency.getItem() instanceof SoulExtractorItem se) {

                    GearItemData gear = StackSaving.GEARS.loadFrom(craftedStack);

                    if (gear != null) {
                        try {

                            if (se.canExtract(gear.getRarity())) {
                                StatSoulData soul = new StatSoulData();
                                soul.slot = gear.GetBaseGearType().getGearSlot().GUID();
                                var ex = ExileStack.of(craftedStack);

                                soul.gear = new SavedGearSoul(ex.get(StackKeys.GEAR).get(), ex.get(StackKeys.POTENTIAL).getOrCreate(), ex.get(StackKeys.CUSTOM).getOrCreate());

                                ItemStack soulstack = soul.toStack();

                                SoundUtils.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP);

                                craftedStack.shrink(1);
                                currency.shrink(1);
                                PlayerUtils.giveItem(soulstack, player);
                                return new Result(true).ding();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
                return new Result(false);
            }
        });
        CLICKS.add(new ClickFeature() {
            @Override
            public Result tryApply(Player player, ItemStack craftedStack, ItemStack currency, Slot slot) {
                if (currency.is(SlashItems.SOUL_CLEANER.get())) {

                    GearItemData gear = StackSaving.GEARS.loadFrom(craftedStack);

                    if (gear != null) {
                        try {
                            craftedStack.getOrCreateTag().remove(StackSaving.GEARS.GUID());
                            currency.shrink(1);
                            return new Result(true).ding();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return new Result(false);
            }
        });


        ForgeEvents.registerForgeEvent(ItemStackedOnOtherEvent.class, x -> {
            Player player = x.getPlayer();

            if (player.level().isClientSide) {
                return;
            }
            if (x.getClickAction() != ClickAction.SECONDARY) {
                // return;
            }

            ItemStack currency = x.getStackedOnItem();
            ItemStack craftedStack = x.getCarriedItem();


            for (ClickFeature click : CLICKS) {
                var result = click.tryApply(player, craftedStack, currency, x.getSlot());

                if (result.doDing) {
                    SoundUtils.ding(player.level(), player.blockPosition());
                    SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.ANVIL_USE, 1, 1);
                }

                if (result.can) {
                    x.setCanceled(true);
                    break;
                }
            }


        });
        ForgeEvents.registerForgeEvent(EntityItemPickupEvent.class, x -> {
            ItemStack stack = x.getItem().getItem();
            if (!StackSaving.GEARS.has(stack)) {
                if (!stack.hasTag() || (stack.hasTag() && !stack.getTag().getBoolean("free_souled"))) {
                    var auto = AutoItem.getRandom(stack.getItem());
                    if (auto != null) {
                        stack.getOrCreateTag().putBoolean("free_souled", true);

                        var data = auto.create(x.getEntity());
                        data.apply(ExileStack.of(stack));
                    }
                }
            }
        });
    }


}
