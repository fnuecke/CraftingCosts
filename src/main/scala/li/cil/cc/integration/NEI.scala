package li.cil.cc.integration

import java.lang

import codechicken.nei.PositionedStack
import codechicken.nei.api.IConfigureNEI
import codechicken.nei.recipe.GuiCraftingRecipe
import li.cil.cc.CraftingCosts
import li.cil.cc.Settings
import li.cil.cc.api.API
import li.cil.cc.api.Recipe
import li.cil.cc.api.RecipeProvider
import li.cil.cc.integration.Integration.Mod
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.language.postfixOps

object NEI extends Mod {
  override def init(): Unit = {
    API.register(new RecipeProvider {
      override def recipesFor(stack: ItemStack): lang.Iterable[Recipe] = {
        val handlers = GuiCraftingRecipe.craftinghandlers.map(_.getRecipeHandler("item", stack)).
          filter(_ != null).
          filterNot(handler => Settings.ignoredCraftingHandlers.contains(handler.getRecipeName))
        val recipes = handlers.flatMap(handler => (0 until handler.numRecipes()).collect {
          case recipe if isEmpty(handler.getOtherStacks(recipe)) =>
            val input = handler.getIngredientStacks(recipe).map(getFirstItem)
            val output = getFirstItem(handler.getResultStack(recipe))
            new Recipe(output, input) // In/Output can be/contain null, that's fine.
        })
        asJavaIterable(recipes)
      }
    })
  }

  // No way to uniformly handle secondary inputs/outputs specified this way.
  // Mods tend to use this in very different, and occasionally very silly
  // ways. For example, Thermal Expansion *sometimes* uses this for
  // secondary inputs, but not always!
  private def isEmpty(ps: Iterable[PositionedStack]) = ps == null || ps.isEmpty

  // This is what makes using generic handlers from NEI feasible, performance-wise:
  // we just use the first item from a permutation list, based on the items
  // unlocalized name for stable selection. This is usually not optimal, but can
  // work to generate a reasonable baseline.
  private def getFirstItem(ps: PositionedStack) = {
    if (ps == null) null
    else if (ps.items != null && ps.items.exists(_ != null)) ps.items.filter(_ != null).sortBy(_.getUnlocalizedName).head
    else ps.item
  }
}

// This class will be automatically found by NEI's black magic.
class NEICraftingCostsConfig extends IConfigureNEI {
  override def getName = CraftingCosts.Id

  override def getVersion = CraftingCosts.Version

  // Make sure to recompute costs when something changes, usually a map change.
  override def loadConfig(): Unit = API.invalidateCosts()
}
