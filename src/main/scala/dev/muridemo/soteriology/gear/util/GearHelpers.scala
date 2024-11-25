package dev.muridemo.soteriology.gear.util


import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.armortrim.ArmorTrim
import net.minecraft.world.item.armortrim.TrimPatterns
import net.silentchaos512.gear.gear.`trait`.SimpleTrait
import net.silentchaos512.gear.util.TraitHelper
import net.silentchaos512.gear.api.traits.TraitActionContext
import net.silentchaos512.gear.api.item.ICoreItem
import net.silentchaos512.gear.api.item.ICoreArmor
import net.silentchaos512.gear.api.traits.ITrait
import net.silentchaos512.gear.api.item.GearType
import net.silentchaos512.gear.util.GearHelper

import com.verdantartifice.primalmagick.common.armortrim.TrimPatternsPM
import com.verdantartifice.primalmagick.common.items.misc.RuneItem
import com.verdantartifice.primalmagick.common.runes.SourceRune
import com.verdantartifice.primalmagick.common.sources.Source
import com.verdantartifice.primalmagick.common.attunements.AttunementManager
import com.verdantartifice.primalmagick.common.attunements.AttunementThreshold
import com.verdantartifice.primalmagick.common.effects.EffectsPM
import com.verdantartifice.primalmagick.common.items.armor.IManaDiscountGear
import com.verdantartifice.primalmagick.common.tags.ItemTagsPM

import dev.muridemo.soteriology.util.Nullable.*
import dev.muridemo.soteriology.util.Nullable.nullable.{?, ??}
import dev.muridemo.soteriology.gear.util.GeneralizedTraitActionContext
import dev.muridemo.soteriology.Soteriology

import scala.quoted.*
import java.util.Optional
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*
import net.minecraft.resources.ResourceLocation

object GearHelpers:
  extension (context: TraitActionContext) {
    def gearType = context.getGear.nn.getItem.asInstanceOf[ICoreItem].getGearType.nn

    def withPlayer(player: Player) = TraitActionContext(player, context.getTraitLevel, context.getGear)
    def withTraitLevel(level: Int) = TraitActionContext(context.getPlayer, level, context.getGear)
    def withGear(gear: ItemStack) = TraitActionContext(context.getPlayer, context.getTraitLevel, gear)

    def gtac = GeneralizedTraitActionContext(context)
  }

  extension [T](gearMap: Map[GearType, T]) {
    def getByGearType(gearType: GearType) = {
      gearMap.find((key, _) => gearType.matches(key)).map((_, value) => value)
    }
  }

  object Trim:
    def unapply(trim: Optional[ArmorTrim]) = trim.toScala

  object RunicTrim:
    def unapply(trimOpt: Optional[ArmorTrim]) = {
      trimOpt.toScala match
        case Some(trim) if trim.pattern().nn.is(TrimPatternsPM.RUNIC) =>
          trim.material().nn.value().nn.ingredient().nn.value() match
            case runeItem : RuneItem => Option(runeItem.getRune)
        case _ => None
    }

  object SourceTrim:
    def unapply(trim: Optional[ArmorTrim]) = {
      trim match
        case RunicTrim(rune) => rune match
          case sourceRune: SourceRune => Option(sourceRune.getSource)
        case _ => None
    }

  extension (player: Player)
    /**
     * Calculates the (Primal Magick) mana cost modifiers for a player, disregarding main hand items.
     *
     */
    def passiveCostModifiers(source: Source) = {
      val gearDiscount = player.getAllSlots.nn.asScala.map(stack => stack.getItem match
        case gear: IManaDiscountGear => gear.getManaDiscount(stack, player, source)
        case _ => 0
      ).sum * 0.01
      val attunementDiscount =
        if AttunementManager.meetsThreshold(player, source, AttunementThreshold.MINOR) then 0.05 else 0
      val effectDiscount =
        Option.ofNullable(player.getEffect(EffectsPM.MANAFRUIT.nn.get()))
              .map {0.02 * _.getAmplifier + 0.01}.getOrElse(0.0)
      val effectPenalty =
        Option.ofNullable(player.getEffect(EffectsPM.MANA_IMPEDANCE.nn.get()))
              .map {0.05 * _.getAmplifier + 0.05}.getOrElse(0.0)
      1.0 - (gearDiscount + attunementDiscount + effectDiscount - effectPenalty)
    }

  //  def passiveCostModifiers_static(player: Player, source: Source) = player.passiveCostModifiers(source)
  extension (stack: ItemStack)
    def isSilentGear = GearHelper.isGear(stack)
    def hasTrait(`trait`: ResourceLocation) = TraitHelper.hasTrait(stack, `trait`)
    def getTraitLevel(`trait`: ResourceLocation) = TraitHelper.getTraitLevel(stack, `trait`)
    def isSGearWardable = stack.isSilentGear && stack.hasTrait(Soteriology.WARDABLE_ID)


  object statics:
    def passiveCostModifiers(player: Player, source: Source) = player.passiveCostModifiers(source)
    def isSGearWardable(stack: ItemStack) = stack.isSilentGear && stack.hasTrait(Soteriology.WARDABLE_ID)
   
