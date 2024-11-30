package dev.muridemo.soteriology.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.verdantartifice.primalmagick.common.events.PlayerEvents;
import com.verdantartifice.primalmagick.common.items.armor.WardingModuleItem;
import com.verdantartifice.primalmagick.common.sources.Source;
import dev.muridemo.soteriology.gear.util.GearHelpers;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Debug(export = true)
@Mixin(value = PlayerEvents.class, remap = false)
public abstract class PrimalMagickPlayerEventMixin {
    @ModifyExpressionValue(method = "lambda$handleWardRegeneration$6", at = @At(value = "CONSTANT", args = "intValue="+ WardingModuleItem.REGEN_COST))
    private static int adjustWardRegenCost(int original, ServerPlayer player) {
        return (int) Math.round(original * GearHelpers.static$passiveCostModifiers(player, Source.EARTH));
    }
}
