package dev.muridemo.soteriology

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.config.ModConfigEvent
import net.minecraftforge.registries.ForgeRegistries

import dev.muridemo.soteriology.util.Helpers.*
import dev.muridemo.soteriology.util.Nullable.*

import scala.jdk.CollectionConverters.*

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Soteriology.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object Config {
  private val BUILDER: ForgeConfigSpec.Builder = new ForgeConfigSpec.Builder()

  private val LOOK_RANGE: ForgeConfigSpec.DoubleValue = BUILDER
    .comment("The actual range of 'line of sight' effects, in blocks")
    //I'm not sure what conditions would cause this to return null, but if it does,
    //throwing an NPE is the right response.
    .nn.defineInRange("lookRange", 256.0, 0.0, 10000.0).nn

//  private val LOG_DIRT_BLOCK: ForgeConfigSpec.BooleanValue = BUILDER
//    .comment("Whether to log the dirt block on common setup")
//    .define("logDirtBlock", true)
//
//  private val MAGIC_NUMBER: ForgeConfigSpec.IntValue = BUILDER
//    .comment("A magic number")
//    .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE)
//
//  private val MAGIC_NUMBER_INTRODUCTION: ForgeConfigSpec.ConfigValue[String] = BUILDER
//    .comment("What you want the introduction message to be for the magic number")
//    .define("magicNumberIntroduction", "The magic number is... ")
//
//  // a list of strings that are treated as resource locations for items
//  private val ITEM_STRINGS: ForgeConfigSpec.ConfigValue[java.util.List[_ <: String]] = BUILDER
//    .comment("A list of items to log on common setup.")
//    .defineListAllowEmpty("items", java.util.List.of("minecraft:iron_ingot"), this.validateItemName)

  val SPEC: ForgeConfigSpec = BUILDER.build.nn

  var lookRange: Double = 0.0
  var logDirtBlock: Boolean = false
  var magicNumber: Int = 0
  var magicNumberIntroduction: String = "null"
  var items: Set[Item] = Set.empty

  private def validateItemName(obj: AnyRef) = {
    obj match {
      case itemName: String =>
        Option.ofNullable(ForgeRegistries.ITEMS).map(_.containsKey(new ResourceLocation(itemName))).boolFlat
      case _ => false
    }
  }

  @SubscribeEvent
  def onLoad(event: ModConfigEvent): Unit = {
    lookRange = LOOK_RANGE.get().nn
//    logDirtBlock = LOG_DIRT_BLOCK.get()
//    magicNumber = MAGIC_NUMBER.get()
//    magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get()
//
//    // convert the java list of strings into a scala set of items
//    items = ITEM_STRINGS.get().asScala
//      .map(itemName => ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
//      .toSet
  }
}
