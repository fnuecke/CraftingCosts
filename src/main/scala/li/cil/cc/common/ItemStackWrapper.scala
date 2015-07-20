package li.cil.cc.common

import java.util.Objects

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

import scala.language.implicitConversions

class ItemStackWrapper(val inner: ItemStack, var size: Double) extends Ordered[ItemStackWrapper] {
  def this(stack: ItemStack) = this(stack, stack.stackSize)

  inner.stackSize = size.toInt

  def id = if (inner.getItem != null) Item.getIdFromItem(inner.getItem) else 0

  def damage = if (inner.getItem != null) inner.getItemDamage else 0

  override def compare(that: ItemStackWrapper) = {
    if (this.id != that.id) this.id - that.id
    else if (this.damage != that.damage) this.damage - that.damage
    else if (this.inner.hasTagCompound != that.inner.hasTagCompound) if (this.inner.hasTagCompound) 1 else -1
    else if (!this.inner.hasTagCompound && !that.inner.hasTagCompound) 0
    else this.inner.getTagCompound.hashCode - that.inner.getTagCompound.hashCode
  }

  override def hashCode() = Objects.hash(Int.box(id), Int.box(damage), Int.box(if (inner.hasTagCompound) inner.getTagCompound.hashCode() else 0))

  override def equals(obj: scala.Any) = obj match {
    case that: ItemStackWrapper => compare(that) == 0
    case _ => false
  }

  def matches(stack: ItemStack, strict: Boolean = false) = OreDictionary.itemMatches(stack, inner, strict)

  override def clone() = new ItemStackWrapper(inner.copy(), size)

  def normalized = new ItemStackWrapper(inner.copy(), 1)

  override def toString = if (inner.getItem != null) inner.toString + " (" + size + "x" + inner.getDisplayName + ")" else size + "x[invalid]"
}
