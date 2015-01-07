package li.cil.cc.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.cc.CraftingCosts
import li.cil.cc.common.CommonProxy
import net.minecraft.client.gui.GuiScreen
import net.minecraft.item.Item
import net.minecraft.util.StatCollector
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import org.lwjgl.input.Keyboard

import scala.language.postfixOps

class ClientProxy extends CommonProxy {
  @SubscribeEvent
  def onItemTooltip(e: ItemTooltipEvent): Unit = {
    val stack = e.itemStack
    CraftingCosts.getCosts(stack) match {
      case Some(costs) =>
        if (KeyBindings.showMaterialCosts) {
          e.toolTip.add(StatCollector.translateToLocal("cc.tooltip.Materials"))
          costs.foreach(cost => e.toolTip.add(cost.toString(stack.stackSize)))
        }
        else {
          e.toolTip.add(StatCollector.translateToLocalFormatted("cc.tooltip.MaterialCosts", Keyboard.getKeyName(KeyBindings.materialCosts.getKeyCode)))
        }
      case _ =>
    }

    val descriptor = Item.itemRegistry.getNameForObject(stack.getItem) +
      (if (stack.getItemDamage != 0 || stack.stackSize > 1) s"@${stack.getItemDamage}" else "") +
      (if (stack.stackSize > 1) s":${stack.stackSize}" else "")
    if (KeyBindings.showItemDescriptor) {
      e.toolTip.add(s"§8$descriptor§r")
    }
    if (KeyBindings.shouldCopyItemDescriptor) {
      GuiScreen.setClipboardString(descriptor)
    }
  }

  override def init(): Unit = {
    super.init()

    ClientRegistry.registerKeyBinding(KeyBindings.materialCosts)
    ClientRegistry.registerKeyBinding(KeyBindings.itemDescriptor)
    ClientRegistry.registerKeyBinding(KeyBindings.copyItemDescriptor)
  }

  override def postInit(): Unit = {
    super.postInit()

    MinecraftForge.EVENT_BUS.register(this)
  }
}
