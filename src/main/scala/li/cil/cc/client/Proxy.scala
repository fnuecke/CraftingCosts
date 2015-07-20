package li.cil.cc.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.cc.common
import li.cil.cc.common.CostManager
import net.minecraft.client.gui.GuiScreen
import net.minecraft.item.Item
import net.minecraft.util.StatCollector
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import org.lwjgl.input.Keyboard

import scala.language.postfixOps

class Proxy extends common.Proxy {
  @SubscribeEvent
  def onItemTooltip(e: ItemTooltipEvent): Unit = {
    val stack = e.itemStack
    val costs = CostManager.costsFor(stack)
    if (costs.nonEmpty) {
      if (KeyBindings.showMaterialCosts) {
        e.toolTip.add(StatCollector.translateToLocal("cc.tooltip.Materials"))
        costs.toSeq.sortBy(_.wrapper.inner.getDisplayName).
          foreach(cost => e.toolTip.add(cost.toString(stack.stackSize)))
      }
      else {
        e.toolTip.add(StatCollector.translateToLocalFormatted("cc.tooltip.MaterialCosts", Keyboard.getKeyName(KeyBindings.materialCosts.getKeyCode)))
      }
    }

    lazy val descriptor = Item.itemRegistry.getNameForObject(stack.getItem) +
      (if (stack.getItemDamage != 0 || stack.stackSize > 1) s"@${stack.getItemDamage}" else "") +
      (if (stack.stackSize > 1) s":${stack.stackSize}" else "")
    if (KeyBindings.showItemDescriptor) {
      e.toolTip.add(s"§8$descriptor§r")
    }
    if (KeyBindings.shouldCopyItemDescriptor) {
      GuiScreen.setClipboardString(descriptor)
    }
  }

  override def onInit(e: FMLInitializationEvent): Unit = {
    super.onInit(e)

    ClientRegistry.registerKeyBinding(KeyBindings.materialCosts)
    ClientRegistry.registerKeyBinding(KeyBindings.itemDescriptor)
    ClientRegistry.registerKeyBinding(KeyBindings.copyItemDescriptor)
  }

  override def onPostInit(e: FMLPostInitializationEvent): Unit = {
    super.onPostInit(e)

    MinecraftForge.EVENT_BUS.register(this)
  }
}
