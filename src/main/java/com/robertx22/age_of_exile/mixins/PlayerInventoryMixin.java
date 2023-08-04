package com.robertx22.age_of_exile.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Shadow
    @Final
    public PlayerEntity player;

    // TODO ADD NEW BACKPACK SYSTEM!!!

    /*
    @Inject(method = "add(Lnet/minecraft/item/ItemStack;)Z", at = @At(value = "HEAD"))
    private void hookonStackInserted(ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        try {
            PlayerInventory inv = (PlayerInventory) (Object) this;

            List<ItemStack> backpacks = new ArrayList<>();

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack trystack = inv.getItem(i);

                if (trystack.getItem() instanceof BackpackItem) {
                    backpacks.add(trystack);
                }
            }

            if (!backpacks.isEmpty()) {

                for (ItemStack bagstack : backpacks) {

                    BackpackInfo info = BackpackContainer.getInfo(this.player, bagstack);

                    GearItemData gear = Gear.Load(stack);
                    if (gear != null) {

                        if (info.canSalvage(stack)) {

                            List<ItemStack> results = gear.getSalvageResult(stack);

                            stack.shrink(1);

                            if (RandomUtils.roll(25)) {
                                SoundUtils.playSound(inv.player, SoundEvents.FIRE_EXTINGUISH, 1, 1);
                                return;

                            } else {
                                SoundUtils.ding(inv.player.level, inv.player.blockPosition());
                                SoundUtils.playSound(inv.player, SoundEvents.ITEM_PICKUP, 1, 1);
                                results.forEach(x -> PlayerUtils.giveItem(x, inv.player));
                            }

                        }

                    }
                }
            }

            if (!stack.isEmpty()) {
                TryAutoInsert.run(inv, stack);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

     */
}
