package li.cil.cc.common

import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import li.cil.cc.Settings
import li.cil.cc.api.API
import li.cil.cc.api.RecipeProvider
import li.cil.cc.integration.Integration
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.config.Configuration

import scala.collection.convert.WrapAsJava._

class Proxy extends API.APIImpl {
  def onPreInit(e: FMLPreInitializationEvent): Unit = {
    Settings.load(new Configuration(e.getSuggestedConfigurationFile))
    API.instance = this
  }

  def onInit(e: FMLInitializationEvent): Unit = Integration.init()

  def onPostInit(e: FMLPostInitializationEvent): Unit = MinecraftForge.EVENT_BUS.register(RecipeProcessor)

  override def register(provider: RecipeProvider): Unit = RecipeManager.providers += provider

  override def invalidateCosts(): Unit = CostManager.clear()

  override def recipesFor(stack: ItemStack) = asJavaIterable(RecipeManager.recipesFor(stack))
}
