package li.cil.cc

import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.{FMLPostInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.{Mod, SidedProxy}
import net.minecraftforge.common.config.Configuration

import scala.collection.mutable

@Mod(modid = CraftingCosts.ID, name = CraftingCosts.Name, version = CraftingCosts.Version,
  dependencies = "required-after:NotEnoughItems", modLanguage = "scala", useMetadata = true)
object CraftingCosts {
  final val ID = "CraftingCosts"

  final val Name = "CraftingCosts"

  final val Version = "@VERSION@"

  @SidedProxy(clientSide = "li.cil.cc.ClientProxy", serverSide = "li.cil.cc.CommonProxy")
  var proxy: CommonProxy = null

  val blackList = mutable.Set.empty[String]

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) {
    val config = new Configuration(e.getSuggestedConfigurationFile)

    blackList ++= config.get("common", "blacklist", Array.empty[String],
      "List of items for which not to display costs. These are regular expressions, e.g. `^OpenComputers:.*$`.").
      getStringList

    config.save()
  }

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit()

}
