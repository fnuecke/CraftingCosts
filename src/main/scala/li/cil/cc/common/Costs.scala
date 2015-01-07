package li.cil.cc.common

import net.minecraft.item.ItemStack

final class Costs(val item: ItemStack, val amount: Double) {
  def toString(times: Int) = {
    val count = times * amount
    (if (count < 1) "<" else " ") + item.getDisplayName + " x" + math.ceil(times * amount).toInt
  }

  override def toString = toString(1)
}
