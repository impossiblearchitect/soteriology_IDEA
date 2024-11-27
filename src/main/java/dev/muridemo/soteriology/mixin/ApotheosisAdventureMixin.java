package dev.muridemo.soteriology.mixin;


import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muridemo.soteriology.Soteriology;
import dev.shadowsoffire.apotheosis.adventure.AdventureModule;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AdventureModule.class, remap = false)
public abstract class ApotheosisAdventureMixin {
//    @Unique
//    private Logger soteriology$logger = LogManager.getLogger();

    @WrapOperation(
        method = "tiles",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableSet;of(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableSet;")
    )
    private ImmutableSet<ReforgingTableBlock> adjustReforgingValidBlocks(Object b1, Object b2, Operation<ImmutableSet<ReforgingTableBlock>> original) {
        Soteriology.LOGGER().info("Appending soteriology:superior_reforging_table to ReforgingTableTile valid blocks");
        Soteriology.LOGGER().info("simple=" + b1 + ", " + "normal=" + b2);
        return ImmutableSet.<ReforgingTableBlock>builder()
                           .addAll(original.call(b1, b2))
                           .add(Soteriology.SUPERIOR_REFORGING_TABLE().get())
                           .build();
    }
}
