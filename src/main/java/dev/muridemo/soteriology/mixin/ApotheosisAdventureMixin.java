package dev.muridemo.soteriology.mixin;


import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.shadowsoffire.apotheosis.adventure.AdventureModule;
import dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingTableBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AdventureModule.class)
public abstract class ApotheosisAdventureMixin {
    private Logger logger = LogManager.getLogger();

    @WrapOperation(
        method = "tiles",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableSet;of(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableSet;", remap = false),
        expect = 1
    )
    private ImmutableSet<ReforgingTableBlock> adjustReforgingValidBlocks(Object b1, Object b2, Operation<ImmutableSet<ReforgingTableBlock>> original) {
        logger.info("Appending soteriology:superior_reforging_table to ReforgingTableTile valid blocks");
        logger.info("simple=" + b1 + ", " + "normal=" + b2);
        return original.call(b1, b2);
    }
}
