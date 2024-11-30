package dev.muridemo.soteriology.mixin;

import com.google.common.collect.ImmutableList;
import com.verdantartifice.primalmagick.common.items.armor.WardingModuleItem;
import dev.muridemo.soteriology.Soteriology;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Supplier;

import scala.jdk.javaapi.CollectionConverters;

@Mixin(value = WardingModuleItem.class, remap = false)
public class PrimalMagickWardApplicableItemsMixin {
    @Shadow @Final @Mutable
    protected static List<Supplier<? extends Item>> APPLICABLE_ITEMS;

    static {
        APPLICABLE_ITEMS = ImmutableList.<Supplier<? extends Item>>builder()
                                        .addAll(APPLICABLE_ITEMS)
                                        .addAll(CollectionConverters.asJava(Soteriology.SGEAR_GENERICS()))
                                        .build();
    }
}
