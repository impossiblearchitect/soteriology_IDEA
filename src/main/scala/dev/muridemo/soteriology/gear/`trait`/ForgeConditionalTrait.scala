package dev.muridemo.soteriology.gear.`trait`

import com.google.common.collect.Multimap
import com.google.gson.{JsonArray, JsonElement, JsonObject, JsonParseException}
import dev.muridemo.soteriology.gear.util.GearHelpers.*
import dev.muridemo.soteriology.gear.util.GeneralizedTraitActionContext
import dev.muridemo.soteriology.util.EventuallyConstant.*
import dev.muridemo.soteriology.util.Helpers.{*, given}
import dev.muridemo.soteriology.util.Nullable.*
import dev.muridemo.soteriology.util.ObfuscatedReflectivePredicate
import dev.muridemo.soteriology.util.typelevel.instances.given
import dev.muridemo.soteriology.{Config, Soteriology}
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.{GsonHelper, Mth}
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.{MobEffect, MobEffectInstance}
import net.minecraft.world.entity.ai.attributes.{Attribute, AttributeModifier}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.{Entity, LivingEntity}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraftforge.registries.ForgeRegistries
import net.silentchaos512.gear.SilentGear
import net.silentchaos512.gear.api.item.{GearType, ICoreArmor, ICoreItem}
import net.silentchaos512.gear.api.stats.ItemStat
import net.silentchaos512.gear.api.traits.{ITrait, ITraitSerializer, TraitActionContext, TraitInstance}
import net.silentchaos512.gear.gear.`trait`.SimpleTrait.Serializer
import net.silentchaos512.gear.gear.`trait`.{SimpleTrait, TraitManager, TraitSerializers}
import net.silentchaos512.gear.util.TraitHelper
import net.silentchaos512.lib.util.TimeUtils
import net.silentchaos512.utils.EnumUtils

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*
//import scala.jdk.FunctionConverters.*
import cats.Functor
import cats.implicits.toFunctorOps

import scala.collection.immutable.ArraySeq
import scala.util.boundary
import scala.util.boundary.break


class ForgeConditionalTrait (val id: ResourceLocation, val serializer: ITraitSerializer[? <: ForgeConditionalTrait])
  extends SimpleTrait(id, serializer) {
  import ForgeConditionalTrait.*
  sealed abstract class Indexer(val getIndex: (context: GeneralizedTraitActionContext) => Int)
  case object TraitLevelIndexer extends Indexer(_.traitLevel - 1)
  case object PieceCountIndexer extends Indexer(_.countSetPieces(this) - 1)
  case object TotalLevelIndexer extends Indexer(_.countTotalLevel(this) - 1)

  class ConditionalTraitData[F[T] <: IndexedSeq[T] : Functor]
  (val `trait`: ITrait, val indexer: Indexer, val levels: F[Int], val conditions: F[Condition], val target: Targeter):
    def traits(gtac: GeneralizedTraitActionContext): Option[TraitInstance] = {
      val index = indexer.getIndex(gtac)
      if (conditions.ec(index)(gtac.self))
        Some(TraitInstance.of(`trait`, levels.ec(index)).nn)
      else
        None
    }

  object ConditionalTraitData:
    def nextID = {
      var n = 0
      () => {
        val i = n
        n += 1
        i
      }
    }
    def read[F[X] <: IndexedSeq[X] : Functor, S, R, I, C, T]
    (
      parse: R => (S, I, F[Int], F[C], T),
      map_trait: S => ITrait,
      map_index: I => Indexer,
      map_condition: C => Condition,
      map_target: T => Targeter
    )(r: R): ConditionalTraitData[F] = {
      val (s, i, l, c, t) = parse(r)
      ConditionalTraitData(map_trait(s), map_index(i), l, c.fmap(map_condition), map_target(t))
    }

    def write[F[X] <: IndexedSeq[X] : Functor, S, R, I, C, T]
    (
      serialize: (S, I, F[Int], F[C], T) => R,
      map_trait: ITrait => S,
      map_index: Indexer => I,
      map_condition: Condition => C,
      map_target: Targeter => T
    )(data: ConditionalTraitData[F]): R = {
      serialize(map_trait(data.`trait`), map_index(data.indexer), data.levels, data.conditions.fmap(map_condition), map_target(data.target))
    }

    def deserializeJSON = {
      read(
        (json: JsonObject) => (
          json.get("trait").!,
          GsonHelper.getAsString(json, "type"),
          if json.get("level").!.isJsonArray
          then json.getAsImmutableArray("level").nn.map(_.getAsInt())
          else
            ArraySeq(json.get("level").!.getAsInt),
          if json.get("condition").!.isJsonArray
          then json.getAsImmutableArray("condition").nn
          else
            ArraySeq(json.get("condition").nn),
          GsonHelper.getAsString(json, "target")
        ),
        t => {
          if t.isJsonPrimitive() then TraitManager.get(t.getAsString()).!
          else {
            val subtrait = TraitSerializers.deserialize(new ResourceLocation(s"$id/subtrait/${nextID()}"), t.getAsJsonObject()).nn
            Soteriology.registerTrait(subtrait)
          }
        },
        {
          case "trait_level" => TraitLevelIndexer
          case "piece_count" => PieceCountIndexer
          case "total_level" => TotalLevelIndexer
          case e => throw new JsonParseException(s"Unknown conditional trait type '$e' for Forge conditional trait $id")
        },
        c => Condition.fromString(c.getAsString.nn).getOrElse{
          throw new JsonParseException(s"Error parsing condition '$c' for Forge conditional trait $id")
        },
        {
          case "self" => Targeter.SELF
          case "struck_entity" => Targeter.STRUCK
          case "struck_by_entity" => Targeter.STRUCKBY
          case "look_entity" => Targeter.LOOK
          case t =>
            throw new JsonParseException(s"Unknown target $t for Forge conditional trait $id")
        }
      )
    }

    def readFromNetwork = read(
      (buf: FriendlyByteBuf) => {
        val anon = buf.readBoolean()
        val `trait` =
          if anon then TraitSerializers.read(buf).!
          else TraitManager.get(buf.readResourceLocation()).!
        val indexer = buf.readVarInt()
        val levels = ArraySeq.unsafeWrapArray(buf.readVarIntArray().!)
        val conditions = buf.readList(c => Condition.readBuf(c.nn).get).!.asScala.toArray
        val target = buf.readVarInt()
        (`trait`, indexer, levels, ArraySeq.unsafeWrapArray(conditions), target)
      },
      identity,
      {
        case 0 => TraitLevelIndexer
        case 1 => PieceCountIndexer
        case 2 => TotalLevelIndexer
        case e => throw new JsonParseException(s"Unknown conditional trait type '$e' for Forge conditional trait $id")
      },
      identity,
      Targeter.fromOrdinal
    )

    def writeToNetwork[F[X] <: IndexedSeq[X] : Functor](buf: FriendlyByteBuf, data: ConditionalTraitData[F]) = write(
      (`trait`: ITrait, indexer: Int, levels: F[Int], conditions: F[Condition], target: Targeter) => {
        `trait`.getId.toString match
          case s"$_ns:$_this/subtrait/$_id" =>
            buf.writeBoolean(true)
            TraitSerializers.write(`trait`, buf)
          case _ =>
            buf.writeBoolean(false)
        buf.writeVarInt(indexer)
        buf.writeVarIntArray(levels.toArray)
        buf.writeCollection(conditions.asJava, (buf, c) => Condition.writeBuf(buf.nn, c.!))
        buf.writeVarInt(target.ordinal)
      },
      identity,
      {
        case TraitLevelIndexer => 0
        case PieceCountIndexer => 1
        case TotalLevelIndexer => 2
      },
      identity, identity)(data)


  private var ctrait: Map[GearType, ConditionalTraitData[IndexedSeq]] = Map.empty
  // private var traits: (GeneralizedTraitActionContext => Option[TraitInstance]) = (_ => None)
  // private var target: Target = Target.SELF

  // private var traits: Map[String, Map[Int, List[TraitInstance]]] = Map.empty
  // private var conditions: Map[String, Map[Int, (SELF => Boolean)]] = Map.empty

  def this(id: ResourceLocation) = this(id, ForgeConditionalTrait.SERIALIZER)

  private def forwardTrait(context: TraitActionContext): Option[(TraitActionContext, ITrait)] = {
    ctrait.getByGearType(context.gearType).flatMap(cdata => {
      cdata.traits(context.gtac).map{tInst =>
        val traitLevel = tInst.getLevel
        val newContext = context.withTraitLevel(traitLevel)
        (newContext, tInst.getTrait)
      }
    })
  }

  override def onUpdate(context: TraitActionContext, isEquipped: Boolean): Unit = {
    //Early exits
    if (!isEquipped) return
    val player = context.getPlayer
    if (player == null || player.tickCount % 10 != 0) return

    forwardTrait(context).foreach((c, t) => t.onUpdate(c, isEquipped))
  }

  override def onAttackEntity(context: TraitActionContext, target: LivingEntity, baseValue: Float): Float = {
    ctrait.getByGearType(context.gearType).flatMap(_.traits(context.gtac.withTarget(target)).map{tInst =>
      val traitLevel = tInst.getLevel
      val newContext = context.withTraitLevel(traitLevel)
      tInst.getTrait.onAttackEntity(newContext, target, baseValue)
    }).getOrElse(baseValue)
  }

  override def onDurabilityDamage(context: TraitActionContext, damageTaken: Int): Float = {
    forwardTrait(context).map((c, t) => t.onDurabilityDamage(c, damageTaken)).getOrElse(damageTaken.toFloat)
  }

  //onRecalculatePre/Post?

  override def onGetStat(context: TraitActionContext, stat: ItemStat, value: Float, damageRatio: Float): Float = {
    forwardTrait(context).map((c, t) => t.onGetStat(c, stat, value, damageRatio)).getOrElse(value)
  }

  override def onGetAttributeModifiers(context: TraitActionContext, modifiers: Multimap[Attribute, AttributeModifier], slot: String): Unit = {
    forwardTrait(context).foreach((c, t) => t.onGetAttributeModifiers(c, modifiers, slot))
  }

  override def onItemUse(context: UseOnContext, traitLevel: Int): InteractionResult = {
    val player = Option.ofNullable(context.getPlayer)
    val gtac = GeneralizedTraitActionContext(player, traitLevel, context.getItemInHand.!, None)
    ctrait.getByGearType(gtac.gearType).flatMap(_.traits(gtac).map{tInst =>
      val traitLevel = tInst.getLevel
      tInst.getTrait.onItemUse(context, traitLevel).nn
    }).getOrElse(InteractionResult.PASS)
  }

  //TODO Check if this uses the trait level of the conditional trait, or of the trait it's forwarding to
  override def onItemSwing(stack: ItemStack, wielder: LivingEntity, traitLevel: Int): Unit = {
    // Handle struck entity as if it were look entity, to handle cases like Gaia Burst or other "sword projection" attacks
    val context = GeneralizedTraitActionContext(Option(wielder), traitLevel, stack, None)
    ctrait.getByGearType(context.gearType).foreach(_.traits(context.withTargetEntityInCrosshairs(Config.lookRange)).foreach{ tInst =>
      val traitLevel = tInst.getLevel
      tInst.getTrait.onItemSwing(stack, wielder, traitLevel)
    })
  }

  override def addLootDrops(context: TraitActionContext, stack: ItemStack): ItemStack | Null = {
    forwardTrait(context).map((c, t) => t.addLootDrops(context, stack)).getOrElse(stack)
  }

}

object ForgeConditionalTrait {
  final val SERIALIZER : ITraitSerializer[ForgeConditionalTrait] = new Serializer[ForgeConditionalTrait](
    Soteriology.FORGE_CONDITIONAL_ID,
    ForgeConditionalTrait.apply(_),
    ForgeConditionalTrait.deserializeJson,
    ForgeConditionalTrait.readFromNetwork,
    ForgeConditionalTrait.writeToNetwork
    )

  enum Targeter(val getTarget: GeneralizedTraitActionContext => Option[Entity]):
    case SELF extends Targeter(_.self)
    case STRUCK extends Targeter(_.target)
    case STRUCKBY extends Targeter(_.self.mapNullable(_.getLastHurtByMob()))
    case LOOK extends Targeter(_.getEntityInCrosshairs(Config.lookRange))

  enum Condition(val check: Option[Entity] => Boolean):
    case ALWAYS extends Condition(_ => true)
    case VALID extends Condition(_.isDefined)
    case NEVER extends Condition(_ => false)
    case Reflective(methodName: String) extends Condition(_.map(ObfuscatedReflectivePredicate(methodName)).boolFlat)
    case NOT(condition: Condition) extends Condition(!condition(_))
    case AND(conditions: Condition*) extends Condition(e => conditions.forall(_(e)))
    case OR(conditions: Condition*) extends Condition(e => conditions.exists(_(e)))

    def apply(e: Option[Entity]): Boolean = check(e)
    override def toString: String = {
      this match
        case ALWAYS => "always"
        case VALID => "valid"
        case NEVER => "never"
        case Reflective(methodName) => methodName
        case NOT(condition) => s"not($condition)"
        case AND(conditions*) => s"and(${conditions.mkString(",")})"
        case OR(conditions*) => s"or(${conditions.mkString(",")})"
    }
  object Condition:
    def fromString(s: String): Option[Condition] = {
      s match
        case "always" => Some(ALWAYS)
        case "valid" => Some(VALID)
        case "never" => Some(NEVER)
        case s"not($condition)" => fromString(condition).map(NOT.apply)
        case s"and($conditions)" =>
          boundary:
            Some(AND(conditions.split(",").map{fromString(_).getOrElse(break(None))}*))
        case s"or($conditions)" =>
          boundary:
            Some(OR(conditions.split(",").map{fromString(_).getOrElse(break(None))}*))
        case _ => Some(Reflective(s))
    }
    def readBuf(buf: FriendlyByteBuf): Option[Condition] = {
      buf.readVarInt() match
        case 0 => Some(ALWAYS)
        case 1 => Some(VALID)
        case 2 => Some(NEVER)
        case 3 => Option.ofNullable(buf.readUtf()).map(Reflective.apply)
        case 4 => readBuf(buf).map(NOT.apply)
        case 5 =>
          boundary:
            Some(AND(ArraySeq.fill(buf.readVarInt())(readBuf(buf).getOrElse(break(None)))*))
        case 6 =>
          boundary:
            Some(OR(ArraySeq.fill(buf.readVarInt())(readBuf(buf).getOrElse(break(None)))*))
    }
    def writeBuf(buf: FriendlyByteBuf, condition: Condition): FriendlyByteBuf = {
      condition match
        case ALWAYS => buf.writeVarInt(0).nn
        case VALID => buf.writeVarInt(1).nn
        case NEVER => buf.writeVarInt(2).nn
        case Reflective(methodName) => buf.writeVarInt(3).nn.writeUtf(methodName).nn
        case NOT(c) =>
          buf.writeVarInt(4)
          writeBuf(buf, c)
        case AND(conditions*) =>
          buf.writeVarInt(5).nn.writeVarInt(conditions.length)
          conditions.foldLeft(buf)(writeBuf)
        case OR(conditions*) =>
          buf.writeVarInt(6).nn.writeVarInt(conditions.length)
          conditions.foldLeft(buf)(writeBuf)
    }
    def deserializeJSON(json: JsonObject): Option[Condition] = fromString(json.getAsString.nn)

  /*
    Schema: Follows the convention of e.g. WielderEffectTrait.
    {
      "type" : "soteriology:forge_conditional_trait",
      "max_level": 5,
      "name": { "translate": "trait.foo.bar"},
      "description": { "translate": "trait.foo.bar.desc"},
      "conditional_traits": {
        "tool": {
          "type": "trait_level"
          "trait": "silentgear:baz",
          "level": [1, 1, 2, 2, 3]
          "target": "self",
          "condition": ["isUnderWater", "isInWater", "isInWater", "isInWaterOrRain", "true"]
          }, ...
        }
      }
    }
    * `target` should be one of `self`, `struck`, `struckby` or `look`.
      * `self` -- the entity wielding the gear; for all traits
      * `struck` -- the entity being hit; for melee weapons and projectiles
      * `struckby` -- the entity that hit the wielder; for armor traits
      * `look` -- the entity being looked at; for bows and curios
    * `type` should be one of `trait_level`, `piece_count`, or `total_level` (meaning the sum of all copies of the trait across all gear pieces),
      and determines the index of the level and condition arrays to use
    * in addition to the reflective properties provided by `ReflectivePredicate`, `condition` recognizes five special values:
      * "and"/"or"/"not" for logical operations
      * "always"/"never" for unconditional activation/deactivation
      * "valid" for a condition that is always true if the target is not null
   */

  def deserializeJson(`trait`: ForgeConditionalTrait, json: JsonObject): Unit = {
    if(!json.has("conditional_traits"))
      throw new JsonParseException(s"Forge conditional trait ${`trait`.getId} is missing 'conditional_traits' object")

    val jsonCTraits = json.getAsJsonObject("conditional_traits").nn
    `trait`.ctrait = jsonCTraits.asMap.nn.asScala.map(
      (gearType, jsonCTrait) => (
        GearType.get(gearType) ??! new JsonParseException(s"Forge conditional trait ${`trait`.getId} mentions invalid gear type $gearType"),
        `trait`.ConditionalTraitData.deserializeJSON(jsonCTrait.getAsJsonObject.!))
    ).toMap
  }

  def readFromNetwork(`trait`: ForgeConditionalTrait, buf: FriendlyByteBuf): Unit = {
    `trait`.ctrait =
      buf.readMap[GearType, `trait`.ConditionalTraitData[IndexedSeq]](
        buf => GearType.get(buf.readUtf()), `trait`.ConditionalTraitData.readFromNetwork(_)
      ).asScala.toMap
  }

  def writeToNetwork(`trait`: ForgeConditionalTrait, buf: FriendlyByteBuf): Unit = {
    buf.writeMap(`trait`.ctrait.asJava, (buf, name) => buf.writeUtf(name.getName()), `trait`.ConditionalTraitData.writeToNetwork)
  }
}