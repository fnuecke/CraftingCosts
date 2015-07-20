package li.cil.cc.client

import li.cil.cc.CraftingCosts
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard

object KeyBindings {
  def showMaterialCosts = Keyboard.isCreated && Keyboard.isKeyDown(materialCosts.getKeyCode)

  def showItemDescriptor = Keyboard.isCreated && Keyboard.isKeyDown(itemDescriptor.getKeyCode)

  def shouldCopyItemDescriptor = Keyboard.isCreated && Keyboard.isKeyDown(copyItemDescriptor.getKeyCode)

  val materialCosts = new KeyBinding("key.materialCosts", Keyboard.KEY_LMENU, CraftingCosts.Id)

  val itemDescriptor = new KeyBinding("key.itemDescriptor", Keyboard.KEY_F1, CraftingCosts.Id)

  val copyItemDescriptor = new KeyBinding("key.copyItemDescriptor", Keyboard.KEY_F3, CraftingCosts.Id)
}
