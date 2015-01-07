package li.cil.cc.integration

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.cc.CraftingCosts
import li.cil.cc.integration.Integration.Mod
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.crafting.ShapedRecipes
import net.minecraft.item.crafting.ShapelessRecipes
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraftforge.oredict.ShapelessOreRecipe

import scala.collection.convert.WrapAsScala._
import scala.language.postfixOps

object Vanilla extends Mod {
  def init(): Unit = {
    FMLInterModComms.sendMessage(CraftingCosts.Id, "addRecipeProvider", "li.cil.cc.integration.Vanilla.vanillaRecipes")
    FMLInterModComms.sendMessage(CraftingCosts.Id, "addRecipeProvider", "li.cil.cc.integration.Vanilla.smeltingRecipes")
  }

  def vanillaRecipes = {
    () => CraftingManager.getInstance.getRecipeList.map[CraftingCosts.ItemTransformation, Iterable[CraftingCosts.ItemTransformation]] {
      case recipe: ShapedRecipes =>
        (recipe.recipeItems, Iterable(recipe.getRecipeOutput))
      case recipe: ShapelessRecipes =>
        (recipe.recipeItems.toIterable, Iterable(recipe.getRecipeOutput))
      case recipe: ShapedOreRecipe if recipe.getInput.length > 0 =>
        (recipe.getInput, Iterable(recipe.getRecipeOutput))
      case recipe: ShapelessOreRecipe if recipe.getInput.size > 0 =>
        (recipe.getInput, Iterable(recipe.getRecipeOutput))
      case _ => null // Unsupported recipe type.
    }.filter(null !=)
  }

  def smeltingRecipes = {
    () => FurnaceRecipes.smelting.getSmeltingList.map[CraftingCosts.ItemTransformation, Iterable[CraftingCosts.ItemTransformation]] {
      case (input: ItemStack, output: ItemStack) if input != null && output != null =>
        (Iterable(input), Iterable(output))
      case _ => null
    }.filter(null !=)
  }
}
