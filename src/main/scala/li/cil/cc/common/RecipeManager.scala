package li.cil.cc.common

import li.cil.cc.Settings
import li.cil.cc.api.RecipeProvider
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object RecipeManager {
  var providers = mutable.LinkedHashSet.empty[RecipeProvider]

  def recipesFor(stack: ItemStack) =
    (Settings.recipeOverrides.get(new ItemStackWrapper(stack.copy())) match {
      case Some(recipe) => Iterable(recipe)
      case _ => providers.flatMap(_.recipesFor(stack))
    }).filter(_ != null)

  def inputsFor(stack: ItemStack) =
    RecipeProcessor.processRecipes(recipesFor(stack)).
      toSeq.sortBy(_.sortIndex).
      headOption.fold(Iterable.empty[ItemStackWrapper])(recipe => {
      // Collect equal item stacks into bins.
      val grouped = recipe.input.groupBy(input => new ItemStackWrapper(input.copy(), 1))
      // Merge the bins by summing up the stack sizes and normalize by output stack size.
      val merged = grouped.map { case (w, inputs) => new ItemStackWrapper(w.inner, inputs.map(_.stackSize).sum / recipe.output.stackSize.toDouble) }
      // Cleaned up, distinct set of inputs stacks, normalized to an output size of one.
      merged
    })
}
