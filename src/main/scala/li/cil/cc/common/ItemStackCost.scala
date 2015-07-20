package li.cil.cc.common

import net.minecraft.util.EnumChatFormatting

class ItemStackCost(val wrapper: ItemStackWrapper, val amount: Double, val error: Boolean) {
  def this(wrapper: ItemStackWrapper, error: Boolean = false) {
    this(wrapper, wrapper.size, error)
  }

  def times(size: Double) = new ItemStackCost(wrapper, amount * size, error)

  def toString(times: Int) = {
    val count = times * amount
    (if (error) EnumChatFormatting.YELLOW.toString else EnumChatFormatting.GRAY.toString) +
      (if (count < 1) "<" else " ") +
      (if (wrapper.inner.getItem != null) wrapper.inner.getDisplayName else "[Invalid]") +
      " x" + math.ceil(times * amount).toInt
  }

  override def toString = toString(1)
}
