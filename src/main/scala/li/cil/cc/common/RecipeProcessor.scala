package li.cil.cc.common

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent
import li.cil.cc.CraftingCosts
import li.cil.cc.Settings
import li.cil.cc.api.Recipe
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object RecipeProcessor {
  def processRecipes(recipes: Iterable[Recipe]) = {
    resolveIgnoredItems()
    recipes.collect {
      case recipe if !shouldIgnoreAsOutput(recipe.output) => new Recipe(recipe.output, asJavaIterable(recipe.input.filterNot(shouldIgnoreAsInput)), recipe.sortIndex)
    }.filter(isValid)
  }

  private def isValid(recipe: Recipe) = recipe != null && recipe.output != null && recipe.input != null && recipe.input.exists(_ != null)

  // ----------------------------------------------------------------------- //

  private var ignoredOutputs = Array.empty[ItemStack]

  private var ignoredInputs = Array.empty[ItemStack]

  private def shouldIgnoreAsOutput(output: ItemStack) = output == null || ignoredOutputs.exists(stack => OreDictionary.itemMatches(stack, output, false))

  private def shouldIgnoreAsInput(input: ItemStack) = input == null || ignoredInputs.exists(stack => OreDictionary.itemMatches(input, stack, false))

  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onJoinGame(e: ClientConnectedToServerEvent): Unit = needsResolving = true

  // ----------------------------------------------------------------------- //

  private var needsResolving = true

  private def resolveIgnoredItems(): Unit = if (needsResolving) {
    needsResolving = false
    ignoredOutputs = Settings.ignoreAsOutput.map(resolvePattern).collect {
      case Some(stack) => stack
    }
    ignoredInputs = Settings.ignoreAsInput.map(resolvePattern).collect {
      case Some(stack) => stack
    }
  }

  private def resolvePattern(pattern: String): Option[ItemStack] = {
    val Array(name, metaString) = pattern.trim.split('@').map(_.trim).take(2).padTo(2, "0")
    val Array(meta, count) = metaString.split(':').map(_.trim).take(2).padTo(2, "1").map(_.toInt)
    Option(net.minecraft.item.Item.itemRegistry.getObject(name)) match {
      case Some(item: net.minecraft.item.Item) => return Some(new ItemStack(item, count, meta))
      case _ =>
    }
    Option(net.minecraft.block.Block.blockRegistry.getObject(name)) match {
      case Some(block: net.minecraft.block.Block) =>
        val stack = new ItemStack(block, count, meta)
        if (stack.getItem != null) return Some(stack)
        else CraftingCosts.log.warn(s"Trying to parse a broken block ${block.getLocalizedName} ($pattern) (stack has no item).")
      case _ =>
    }

    // Only complain about unresolvable descriptors if the involved mod is present.
    if (!name.contains(":") || Loader.isModLoaded(name.split(":").head))
      CraftingCosts.log.warn(s"Failed resolving pattern '$pattern', no such item or block.")

    None
  }
}
