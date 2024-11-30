package dev.muridemo.soteriology.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.verdantartifice.primalmagick.common.capabilities.ManaStorage;
import com.verdantartifice.primalmagick.common.events.CapabilityEvents;
import com.verdantartifice.primalmagick.common.sources.Source;
import dev.muridemo.soteriology.Soteriology;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.silentchaos512.gear.util.GearHelper;
import net.silentchaos512.gear.util.TraitHelper;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Debug(export = true)
@Mixin(value = CapabilityEvents.class, remap = false)
public abstract class PrimalMagickCapabilityEventMixin {
    @WrapOperation(
            method = "attachItemStackCapability",
            at = @At(value = "NEW", target = "(III[Lcom/verdantartifice/primalmagick/common/sources/Source;)Lcom/verdantartifice/primalmagick/common/capabilities/ManaStorage$Provider;", remap = false),
            require = 1
    )
    private static ManaStorage.Provider adjustManaCapacity(
            int capacity, int maxReceive, int maxExtract, Source[] allowedSources,
            Operation<ManaStorage.Provider> original,
            AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (GearHelper.isGear(stack))
            capacity *= TraitHelper.getTraitLevel(stack, Soteriology.WARDABLE_ID());
        return original.call(capacity, maxReceive, maxExtract, allowedSources);
    }
}
