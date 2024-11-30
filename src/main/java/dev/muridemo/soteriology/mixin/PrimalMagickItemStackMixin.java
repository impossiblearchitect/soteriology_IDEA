package dev.muridemo.soteriology.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.verdantartifice.primalmagick.common.crafting.WardingModuleApplicationRecipe;
import com.verdantartifice.primalmagick.common.items.armor.WardingModuleItem;
import dev.muridemo.soteriology.gear.util.GearHelpers;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {WardingModuleItem.class, WardingModuleApplicationRecipe.class}, remap = false)
public abstract class PrimalMagickItemStackMixin {
    @WrapOperation(
            method = {"hasWardAttached", "matches", "assemble"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private static boolean onIs(ItemStack stack, TagKey<Item> tag, Operation<Boolean> original) {
        return GearHelpers.static$isSGearWardable(stack) || original.call(stack, tag);
    }


//    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
//    private void onIs(TagKey<Item> pTag, CallbackInfoReturnable<Boolean> cir) {
//        if (pTag == ItemTagsPM.WARDABLE_ARMOR)
//            if(GearHelpers.static$isSGearWardable((ItemStack) (Object) this))
//                cir.setReturnValue(true);
//    }
}
