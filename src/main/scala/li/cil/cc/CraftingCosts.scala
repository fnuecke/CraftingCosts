package li.cil.cc

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.LogManager

@Mod(modid = CraftingCosts.Id, name = CraftingCosts.Id, version = CraftingCosts.Version, dependencies = "after:NotEnoughItems", modLanguage = "scala")
object CraftingCosts {
  final val Id = "CraftingCosts"
  final val Version = "@VERSION@"

  @SidedProxy(clientSide = "li.cil.cc.client.Proxy", serverSide = "li.cil.cc.common.Proxy")
  var proxy: common.Proxy = null

  var log = LogManager.getLogger(Id)

  @EventHandler
  def onPreInit(e: FMLPreInitializationEvent) {
    log = e.getModLog
    proxy.onPreInit(e)
  }

  @EventHandler
  def onInit(e: FMLInitializationEvent) = {
    proxy.onInit(e)
  }

  @EventHandler
  def onPostInit(e: FMLPostInitializationEvent) = {
    proxy.onPostInit(e)
  }
}
