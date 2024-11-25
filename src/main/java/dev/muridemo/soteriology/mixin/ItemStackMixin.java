package dev.muridemo.soteriology.mixin;

import com.verdantartifice.primalmagick.common.tags.ItemTagsPM;
import dev.muridemo.soteriology.Soteriology;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void onIs(TagKey<Item> pTag, CallbackInfoReturnable<Boolean> cir) {
        if (tag == ItemTagsPM.WARDABLE_ARMOR)
            Soteriology.LOGGER().info("Mixin placeholder!");
    }
}
