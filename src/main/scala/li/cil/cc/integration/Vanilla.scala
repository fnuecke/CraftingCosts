package li.cil.cc.integration

import java.lang

import li.cil.cc.api.API
import li.cil.cc.api.Recipe
import li.cil.cc.api.RecipeProvider
import li.cil.cc.common.ItemStackWrapper
import li.cil.cc.integration.Integration.Mod
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.ShapedRecipes
import net.minecraft.item.crafting.ShapelessRecipes
import net.minecraftforge.oredict.OreDictionary
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraftforge.oredict.ShapelessOreRecipe

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.language.postfixOps

object Vanilla extends Mod {
  def init(): Unit = {
    API.register(new RecipeProvider {
      override def recipesFor(stack: ItemStack) =
        asJavaIterable(FurnaceRecipes.smelting.getSmeltingList.collect {
          case (input: ItemStack, output: ItemStack) if input != null && output != null && OreDictionary.itemMatches(stack, output, false) && ItemStack.areItemStackTagsEqual(output, stack) =>
            new Recipe(output, asJavaIterable(Iterable(input)))
        })
    })
    API.register(new RecipeProvider {
      override def recipesFor(stack: ItemStack): lang.Iterable[Recipe] =
        asJavaIterable(allRecipes.
          filter(recipe => OreDictionary.itemMatches(stack, recipe.getRecipeOutput, false) && ItemStack.areItemStackTagsEqual(recipe.getRecipeOutput, stack)).
          collect {
          case recipe: ShapedRecipes =>
            new Recipe(recipe.getRecipeOutput, asJavaIterable(parseItems(recipe.recipeItems)), 50)
          case recipe: ShapedOreRecipe =>
            new Recipe(recipe.getRecipeOutput, asJavaIterable(parseItems(recipe.getInput)), 75)
          case recipe: ShapelessRecipes =>
            new Recipe(recipe.getRecipeOutput, asJavaIterable(parseItems(recipe.recipeItems)), 100)
          case recipe: ShapelessOreRecipe =>
            new Recipe(recipe.getRecipeOutput, asJavaIterable(parseItems(recipe.getInput)), 125)
        })

      def allRecipes = CraftingManager.getInstance.getRecipeList.collect {
        case recipe: IRecipe if recipe.getRecipeOutput != null => recipe
      }
    })
  }

  private def parseItems(values: Iterable[_]) = {
    def resolveOreDictList(list: Iterable[ItemStack]) = {
      // Sort oredict lists to always pick the same oredict entry for all
      // recipes, to avoid discrepancies, causing cost computation to fail.
      list.filter(_ != null).map(stack => new ItemStackWrapper(stack.copy(), 1)).toSeq.sorted.head.inner
    }
    values.collect {
      case stack: ItemStack =>
        // Because Minecraft.
        val copy = stack.copy()
        copy.stackSize = 1
        copy
      case list: scala.collection.Iterable[ItemStack]@unchecked if list.nonEmpty => resolveOreDictList(list)
      case list: java.lang.Iterable[ItemStack]@unchecked if list.nonEmpty => resolveOreDictList(list)
    }.filter(_ != null)
  }
}
