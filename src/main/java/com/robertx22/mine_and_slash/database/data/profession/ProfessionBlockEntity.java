package com.robertx22.mine_and_slash.database.data.profession;

import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.data.profession.screen.CraftingStationMenu;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.mmorpg.ModErrors;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashBlockEntities;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.ICommonDataItem;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.ISalvagable;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ProfessionBlockEntity extends BlockEntity {

    static MergedContainer.Inventory INPUTS = new MergedContainer.Inventory("INPUTS", 9, Direction.UP);
    static MergedContainer.Inventory OUTPUTS = new MergedContainer.Inventory("OUTPUTS", 9, Direction.DOWN);

    public MergedContainer inventory = new MergedContainer(Arrays.asList(INPUTS, OUTPUTS), this);

    public SimpleContainer show = new SimpleContainer(1);

    public Boolean recipe_locked = false;
    public ProfessionRecipe last_recipe;
    public Crafting_State craftingState = Crafting_State.STOPPED;
    public UUID ownerUUID = null;


    public ProfessionBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(SlashBlockEntities.PROFESSION.get(), pPos, pBlockState);
    }

    public Profession getProfession() {
        var id = ((ProfessionBlock) getBlockState().getBlock()).profession;
        return ExileDB.Professions().get(id);
    }

    public Player getOwner(Level l) {
        if (ownerUUID != null) {
            return l.getPlayerByUUID(ownerUUID);
        }
        return null;
    }


    public void tryInputRecipe(ProfessionRecipe recipe, Player p) {

        if (!recipe.profession.equals(getProfession().GUID())) {
            return;
        }

        this.ownerUUID = p.getUUID();

        this.recipe_locked = true;
        this.last_recipe = recipe;


    }

    public void tryTakeMaterialsFromNearbyChests() {

        if (!ServerContainer.get().STATION_SUCK_NEARBY_CHESTS.get()) {
            return;
        }

        if (last_recipe != null) {

            if (!level.isClientSide) {

                var mats = getMats();

                boolean hasfree = false;

                var inputinv = inventory.getInventory(INPUTS);

                for (int i = 0; i < inputinv.getContainerSize(); i++) {
                    if (inputinv.getItem(i).isEmpty()) {
                        hasfree = true;
                    }
                }
                if (hasfree) {

                    for (ProfessionRecipe.CraftingMaterial mat : last_recipe.getMissingMaterials(mats)) {
                        boolean did = false;

                        ItemStack stacked = ItemStack.EMPTY;

                        for (Container inv : getNearbyInventories()) {
                            if (did) {
                                break;
                            }

                            for (int i = 0; i < inv.getContainerSize(); i++) {
                                ItemStack stack = inv.getItem(i);
                                if (mat.matches(stack)) {
                                    if (stacked.isEmpty()) {
                                        stacked = stack.copy();
                                        stack.shrink(stack.getCount());
                                    } else {
                                        int can = stacked.getMaxStackSize() - stacked.getCount();
                                        int transfer = Math.min(can, stack.getCount());
                                        stack.shrink(transfer);
                                        stacked.grow(transfer);
                                    }


                                }
                            }

                        }

                    }
                }
            }

        }
    }

    public List<Container> getNearbyInventories() {
        List<Container> list = new ArrayList<>();
        for (Direction dir : Arrays.asList(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)) {
            BlockPos pos = getBlockPos().offset(dir.getNormal());
            if (this.getLevel().getBlockEntity(pos) instanceof Container inv) {
                list.add(inv);
            }
        }

        return list;
    }

    public boolean onTryInsertItem(ItemStack stack) {

        if (this.recipe_locked) {
            craftingState = Crafting_State.ACTIVE;
        }

        return true;
    }

    public void onTickWhenPlayerWatching(Player p) {
        var recipe = this.getCurrentRecipe();

        if (recipe != null) {
            this.show.setItem(0, recipe.toResultStackForJei());
        } else {
            this.show.setItem(0, ItemStack.EMPTY);

        }
    }

    int lastInputs = 0;

    public void tick(Level level) {
        try {

            if (craftingState != Crafting_State.ACTIVE) {
                if (craftingState == Crafting_State.IDLE) {
                    var inv = this.inventory.getInventory(INPUTS);
                    int inputs = 0;
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        if (inv.getItem(i).isEmpty()) {
                            inputs++;
                        }
                    }
                    if (inputs != lastInputs) {
                        craftingState = Crafting_State.ACTIVE;
                        lastInputs = inputs;
                        this.tryTakeMaterialsFromNearbyChests();

                    }
                }
            }

            if (craftingState == Crafting_State.ACTIVE) {
                boolean ifOnlyDestroy = getMats().stream().filter(x -> !x.toString().equals(Blocks.AIR.asItem().getDefaultInstance().toString())).allMatch(x -> x.toString().equals(SlashItems.DESTROY_OUTPUT.get().getDefaultInstance().toString()));
                if (this.inventory.getInventory(INPUTS).isEmpty() || ifOnlyDestroy) {
                    if (recipe_locked)
                        craftingState = Crafting_State.IDLE;
                    else {
                        craftingState = Crafting_State.IDLE;
                        //ownerUUID = null;
                    }
                    return;
                }
                Player p = getOwner(level);
                if (p != null && p.isAlive()) {
                    if (getProfession().GUID().equals(Professions.SALVAGING)) {
                        var rec = trySalvage(p);
                        if (!rec.can && !recipe_locked)
                            show.clearContent();
                    } else {
                        if (recipe_locked) {
                            if (last_recipe.canCraft(getMats())) {
                                var can = tryRecipe(p);
                                if (!can.can && p.containerMenu instanceof CraftingStationMenu) {
                                    p.sendSystemMessage(can.answer);
                                    craftingState = Crafting_State.IDLE;
                                }
                            } else
                                craftingState = Crafting_State.IDLE;
                        } else {
                            ProfessionRecipe recipe = getCurrentRecipe();
                            if (recipe == null) {
                                p.sendSystemMessage(Chats.PROF_RECIPE_NOT_FOUND.locName().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                                craftingState = Crafting_State.IDLE;
                                //ownerUUID = null;
                                return;
                            }
                            int ownerLvl = Load.player(p).professions.getLevel(recipe.profession);
                            if (recipe.getLevelRequirement() > ownerLvl) {
                                p.sendSystemMessage(Chats.PROF_RECIPE_LEVEL_NOT_ENOUGH.locName(getProfession().locName(), recipe.getLevelRequirement(), ownerLvl).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                                craftingState = Crafting_State.IDLE;
                                //ownerUUID = null;
                                return;
                            }

                            if (recipe.canCraft(getMats())) {
                                var rec = tryRecipe(p);
                                if (!rec.can) {
                                    show.clearContent();
                                    craftingState = Crafting_State.IDLE;
                                    //ownerUUID = null;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ModErrors.print(e);
        }

        //    this.setChanged(); // todo will this cause any problems to have it perma on?
    }

    public boolean hasAtLeastOneFreeOutputSlot() {
        var inv = this.inventory.getInventory(OUTPUTS);

        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ExplainedResult tryRecipe(Player p) {
        ProfessionRecipe recipe;

        if (recipe_locked) {
            recipe = last_recipe;
        } else {
            recipe = getCurrentRecipe();
        }

        if (recipe == null) {
            return ExplainedResult.failure(Chats.PROF_RECIPE_NOT_FOUND.locName());
        }
        int ownerLvl = Load.player(p).professions.getLevel(getProfession().GUID());
        if (recipe.getLevelRequirement() > ownerLvl) {
            return ExplainedResult.failure(Chats.PROF_RECIPE_LEVEL_NOT_ENOUGH.locName(getProfession().locName(), recipe.getLevelRequirement(), ownerLvl));
        }

        float expMulti = 1;

        boolean destroyOuput = false;

        if (this.inventory.getInventory(INPUTS).countItem(SlashItems.DESTROY_OUTPUT.get()) > 0) {
            expMulti = 3;
            destroyOuput = true;
        }

        int expGive = (int) (recipe.getExpReward(p, ownerLvl, getMats()) * expMulti);
        this.addExp(expGive);
        var output = recipe.craft(p, getMats());
        if (!destroyOuput && !tryPutToOutputs(output))
            return ExplainedResult.failure(Chats.PROF_OUTPUT_SLOT_NOT_EMPTY.locName());
        recipe.spendMaterials(getMats());
        this.setChanged();
        return ExplainedResult.success();

    }

    public boolean tryPutToOutputs(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!inventory.addStack(OUTPUTS, stack)) {
                ItemEntity itementity = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY() + 0.5, getBlockPos().getZ(), stack);
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        }
        return true;
    }

    public ExplainedResult trySalvage(Player p) {

        if (!hasAtLeastOneFreeOutputSlot()) {
            return ExplainedResult.failure(Chats.PROF_OUTPUT_SLOT_NOT_EMPTY.locName());
        }

        int ownerLvl = Load.player(p).professions.getLevel(getProfession().GUID());

        if (getProfession().GUID().equals(Professions.SALVAGING)) {
            for (ItemStack stack : this.getMats()) {

                ExileStack ex = ExileStack.of(stack);

                ICommonDataItem data = ICommonDataItem.load(stack);
                ISalvagable sal = ISalvagable.load(stack);
                if (data != null && sal != null && sal.isSalvagable(ex)) {

                    float multi = data.getRarity().item_value_multi;

                    List<ItemStack> output = new ArrayList<>();
                    output.addAll(sal.getSalvageResult(ex));
                    output.addAll(getProfession().getAllDrops(p, ownerLvl, data.getLevel(), multi));

                    tryPutToOutputs(output);

                    this.addExp(data.getSalvageExpReward());

                    stack.shrink(1);

                    this.setChanged();

                    return ExplainedResult.success();

                } else {
                    if (stack != null && !stack.isEmpty()) {

                        ItemStack copy = stack.copy();

                        stack.shrink(stack.getCount());

                        ItemEntity itementity = new ItemEntity(level, this.getBlockPos().getX(), getBlockPos().getY() + 1.5, getBlockPos().getZ(), copy);
                        itementity.setDefaultPickUpDelay();
                        level.addFreshEntity(itementity);

                        p.sendSystemMessage(Words.UNSALVAGEABLE.locName().append(": ").append(copy.getDisplayName()));
                    }
                }
            }

        }
        return ExplainedResult.failure(Component.literal(""));
    }

    public void addExp(int xp) {

        Player p = getOwner(level);

        if (p != null) {
            Load.player(p).professions.addExp(p, getProfession().GUID(), xp);
        }

    }


    public List<ItemStack> getMats() {
        return inventory.getAllStacks(INPUTS);
    }

    public ProfessionRecipe getCurrentRecipe() {

        if (this.recipe_locked) {
            if (this.last_recipe != null) {
                return last_recipe;
            }
        }

        if (true) {
            return null; // todo i dont want these?
        }

        var mats = getMats();
        if (mats.stream().anyMatch(x -> !x.isEmpty())) {

            String prof = getProfession().id;

            // this could be a bit laggy
            var list = ExileDB.Recipes().getFilterWrapped(x -> {
                return x.profession.equals(prof) && x.canCraft(mats);
            }).list;

            if (list.isEmpty()) {
                return null;
            }
            // higher power versions usually just require more materials, so to make sure it always uses the highest power recipe, we do this
            return list.stream().max(Comparator.comparingInt(x -> x.tier)).get();
        }
        return null;
    }

    public ListTag createTag() {
        ListTag listtag = new ListTag();

        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemstack = inventory.getItem(i);
            if (itemstack.isEmpty())
                continue;
            CompoundTag slot = new CompoundTag();
            slot.putInt("slot", i);
            itemstack.save(slot);
            listtag.add(slot);
        }
        return listtag;
    }

    public void fromTag(ListTag pContainerNbt) {
        inventory.clearContent();
        for (int i = 0; i < pContainerNbt.size(); ++i) {
            CompoundTag tag = pContainerNbt.getCompound(i);
            int slot = tag.getInt("slot");
            tag.remove("slot");
            ItemStack itemstack = ItemStack.of(tag);
            inventory.setItem(slot, itemstack);
        }
    }


    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        try {
            fromTag(pTag.getList("inv", 10));
            this.show.fromTag(pTag.getList("show", 10));
            this.recipe_locked = pTag.getBoolean("locked");

            var state = pTag.getString("state");
            if (state.isEmpty()) {
                state = Crafting_State.IDLE.name();
            }
            this.craftingState = Crafting_State.valueOf(state);
            if (craftingState != Crafting_State.STOPPED) {
                if (pTag.contains("owner"))
                    this.ownerUUID = pTag.getUUID("owner");
                else {
                    this.ownerUUID = null;
                    craftingState = Crafting_State.STOPPED;
                }
            }
            if (recipe_locked && pTag.contains("recipe")) {
                this.last_recipe = ExileDB.Recipes().get(pTag.getString("recipe"));
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);

        try {
            pTag.put("inv", createTag());
            pTag.put("show", this.show.createTag());
            if (recipe_locked) {
                if (last_recipe != null) {
                    pTag.putBoolean("locked", recipe_locked);
                    pTag.putString("recipe", last_recipe.GUID());
                } else {
                    pTag.putBoolean("locked", false);
                }
            }
            if (craftingState != Crafting_State.STOPPED) {
                if (this.ownerUUID != null) {
                    pTag.putUUID("owner", this.ownerUUID);
                    pTag.putString("state", craftingState.name());
                } else {
                    pTag.putString("state", Crafting_State.STOPPED.name());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

