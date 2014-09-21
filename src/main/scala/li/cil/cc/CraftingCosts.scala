package li.cil.cc

import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.{FMLPostInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.{Mod, SidedProxy}
import net.minecraftforge.common.config.Configuration

@Mod(modid = CraftingCosts.ID, name = CraftingCosts.Name, version = CraftingCosts.Version,
  dependencies = "required-after:NotEnoughItems", modLanguage = "scala", useMetadata = true)
object CraftingCosts {
  final val ID = "CraftingCosts"

  final val Name = "CraftingCosts"

  final val Version = "@VERSION@"

  @SidedProxy(clientSide = "li.cil.cc.ClientProxy", serverSide = "li.cil.cc.CommonProxy")
  var proxy: CommonProxy = null

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) = proxy.preInit(new Configuration(e.getSuggestedConfigurationFile))

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit()
}
