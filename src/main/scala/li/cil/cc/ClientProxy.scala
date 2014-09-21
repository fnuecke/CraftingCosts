package li.cil.cc

import java.util

import codechicken.nei.guihook.{GuiContainerManager, IContainerTooltipHandler}
import codechicken.nei.recipe.GuiCraftingRecipe
import codechicken.nei.{NEIClientUtils, NEIServerUtils, PositionedStack}
import cpw.mods.fml.common.registry.GameData
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import net.minecraftforge.common.config.Configuration

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class ClientProxy extends CommonProxy with IContainerTooltipHandler {
  // List of items not to show material costs for (e.g. if mod does it itself).
  val blackList = mutable.Set(
    "^minecraft:grass$",
    "^minecraft:dirt$",
    "^minecraft:cobblestone$",
    "^minecraft:sapling$",
    "^minecraft:bedrock$",
    "^minecraft:sand$",
    "^minecraft:gravel$",
    "^minecraft:.*_ore$",
    "^minecraft:log$",
    "^minecraft:log2$",
    "^minecraft:leaves$",
    "^minecraft:leaves2$",
    "^minecraft:sponge$",
    "^minecraft:web$",
    "^minecraft:tallgrass$",
    "^minecraft:deadbush$",
    "^minecraft:.*_flower$",
    "^minecraft:.*_mushroom$",
    "^minecraft:mossy_cobblestone$",
    "^minecraft:obsidian$",
    "^minecraft:fire$",
    "^minecraft:mob_spawner$",
    "^minecraft:ice$",
    "^minecraft:snow$",
    "^minecraft:cactus$",
    "^minecraft:clay$",
    "^minecraft:pumpkin$",
    "^minecraft:netherrack$",
    "^minecraft:soul_sand$",
    "^minecraft:glowstone$",
    "^minecraft:vine$",
    "^minecraft:mycelium$",
    "^minecraft:waterlily$",
    "^minecraft:end_stone$",
    "^minecraft:dragon_egg$",
    "^minecraft:cocoa$",

    "^minecraft:apple$",
    "^minecraft:coal$",
    "^minecraft:string$",
    "^minecraft:feather$",
    "^minecraft:gunpowder$",
    "^minecraft:wheat$",
    "^minecraft:flint$",
    "^minecraft:porkchop$",
    "^minecraft:redstone$",
    "^minecraft:leather$",
    "^minecraft:clay_ball$",
    "^minecraft:reeds$",
    "^minecraft:egg$",
    "^minecraft:fish$",
    "^minecraft:dye$",
    "^minecraft:bone$",
    "^minecraft:melon$",
    "^minecraft:melon_seeds$",
    "^minecraft:beef$",
    "^minecraft:chicken$",
    "^minecraft:rotten_flesh$",
    "^minecraft:ender_pearl$",
    "^minecraft:blaze_rod$",
    "^minecraft:ghast_tear$",
    "^minecraft:nether_wart$",
    "^minecraft:spider_eye$",
    "^minecraft:blaze_powder$",
    "^minecraft:carrot$",
    "^minecraft:potato$",
    "^minecraft:map$",
    "^minecraft:skull$",
    "^minecraft:nether_star$",
    "^minecraft:record_.*$"
  )

  // Overall costs, list of (stack, amount). Used to cache cost results, to
  // avoid having to recompute them all the time, which can be incredibly slow
  // for complicated recipes / many possible recipes.
  private val costs = mutable.Map.empty[ItemStack, Future[(Iterable[(ItemStack, Double)], Int)]]

  // Read settings.
  override def preInit(config: Configuration) {
      blackList ++= config.get("common", "blacklist", blackList.toArray,
        "List of items for which not to display costs. These are regular expressions, e.g. `^OpenComputers:.*$`.").
        getStringList

      config.save()
  }

  // Register as tooltip handler with NEI.
  override def postInit() = GuiContainerManager.addTooltipHandler(this)

  // Utility method for synchronized and fuzzy comparing cache access.
  private def cachedCosts(stack: ItemStack) = costs.synchronized(costs.find({
    case (key, value) => NEIServerUtils.areStacksSameTypeCrafting(key, stack)
  })).map(_._2)

  // Utility method for adding stuff to the cache, simplifying costs.
  private def tryCacheCosts(stack: ItemStack, callback: () => (Iterable[(ItemStack, Double)], Int)) = {
    costs.synchronized(cachedCosts(stack) match {
      case Some(value) => value
      case _ =>
        // Not in cache yet, get actual values as a future (computation may
        // take a while) and simplify results.
        val future = Future {
          costs.synchronized { /* Wait for us to be inserted into the cache. */ }
          val (stackCosts, complexity) = try callback() catch {
            case t: Throwable =>
              t.printStackTrace()
              throw t
          }
          // Get the list of unique stacks in the ingredients.
          val uniqueStacks = stackCosts.map(_._1).foldLeft(Seq.empty[ItemStack])((acc, stack) =>
            if (!acc.exists(s => NEIServerUtils.areStacksSameTypeCrafting(s, stack))) acc :+ stack
            else acc)
          // Sum up costs of the unique stacks in the ingredients and return that list.
          (uniqueStacks.map(stack => (stack, stackCosts.foldLeft(0.0)((acc, cost) =>
            if (NEIServerUtils.areStacksSameTypeCrafting(cost._1, stack)) acc + cost._2
            else acc))).sortBy(_._1.getUnlocalizedName).toIterable, complexity)
        }
        costs += stack -> future
        future
    })
  }

  // Check if a stack is blacklisted, based on its items registry name.
  private def isBlackListed(stack: ItemStack) = {
    GameData.getItemRegistry.getNameForObject(stack.getItem) match {
      case name: String => blackList.exists(name.matches)
      case _ => false
    }
  }

  // Performs actual computation of item costs, uses cache where possible.
  private def computeCosts(stack: ItemStack, visited: Seq[ItemStack] = Seq.empty): (Iterable[(ItemStack, Double)], Int) = {
    def positionedStackSize(ps: PositionedStack) = {
      Option(ps.item).fold(ps.items.find(NEIServerUtils.areStacksSameTypeCrafting(_, stack)).fold(Int.MaxValue)(_.stackSize))(_.stackSize)
    }

    if (visited.exists(vs => NEIServerUtils.areStacksSameTypeCrafting(vs, stack))) (Iterable((stack, 1.0)), Int.MaxValue)
    else cachedCosts(stack) match {
      case Some(value) if value.isCompleted => Await.result(value, Duration.Inf)
      case _ =>
        if (isBlackListed(stack)) {
          val ingredients = Iterable((stack, 1.0))
          val depth = 1
          tryCacheCosts(stack, () => (ingredients, depth))
          (ingredients, depth)
        }
        else {
          val allRecipes = GuiCraftingRecipe.craftinghandlers.
            // Get instances that can create this stack -> recipeHandler
            map(craftingHandler => craftingHandler.synchronized(craftingHandler.getRecipeHandler("item", stack))).
            // Get actual, valid recipe handlers -> recipeHandler
            filter(recipeHandler => recipeHandler != null && recipeHandler.synchronized(recipeHandler.numRecipes()) > 0).
            // Get ingredients from underlying recipes -> (PositionedStack[], output)[]
            flatMap(recipeHandler => (0 until recipeHandler.numRecipes()).map(index => recipeHandler.synchronized((recipeHandler.getIngredientStacks(index).filter(positionedStackSize(_) > 0), recipeHandler.getResultStack(index))))).
            // Filter bad outputs... some TE machines return recipes that are actually "fluid" ones, not "item" ones, leading to `null` results.
            filter(recipe => recipe._2 != null && (recipe._2.item != null || recipe._2.items != null)).
            // Make sure we have ingredients -> (PositionedStack[], output)[]
            filter(_._1.size > 0).
            // Select ingredient with minimal recipe for each ingredient -> (ItemStack[], output)[]
            map(recipe => (recipe._1.map(ingredient => ingredient.items.sortBy(computeCosts(_, visited :+ stack)._2).head), recipe._2)).
            // Get the maximum cost of any ingredient, for cycle detection and sorting -> ((ItemStack[], output)[], complexity)
            map(recipe => (recipe, recipe._1.map(computeCosts(_, visited :+ stack)._2).max))

          val sortedRecipes = allRecipes.
            // Ignore recipes that contain cycles.
            filter(_._2 != Int.MaxValue).
            // Select the most simple recipe, based on the depth of the most complex ingredient -> ((ItemStack[], output), complexity)
            sortBy(_._2)

          val recipe = sortedRecipes.
            // Get all best matches.
            takeWhile(_._2 == sortedRecipes.head._2).
            // Strip the complexity.
            map(_._1).
            // Sort by number of outputs (we want the one that leads to fewer division).
            sortBy(recipe => positionedStackSize(recipe._2)).
            // Get best recipe.
            headOption

          recipe match {
            case Some((stacks, output)) if positionedStackSize(output) != Int.MaxValue =>
              // Got a valid result, adjust individual costs based on output count.
              val inputs = stacks.map(computeCosts(_, visited :+ stack))
              val ingredients = inputs.flatMap(_._1).map(cost => (cost._1, cost._2 / positionedStackSize(output).toDouble))
              val depth = inputs.map(_._2).max + 1
              tryCacheCosts(stack, () => (ingredients, depth))
              (ingredients, depth)
            case _ =>
              // If we have nothing yet, but had some recipes, we hit a cycle.
              if (allRecipes.nonEmpty)
                (Iterable((stack, 1.0)), Int.MaxValue)
              else
                (Iterable((stack, 1.0)), 1)
          }
        }
    }
  }

  // Implementation of IContainerTooltipHandler.

  override def handleTooltip(gui: GuiContainer, x: Int, y: Int, tooltip: util.List[String]) = tooltip

  override def handleItemDisplayName(gui: GuiContainer, stack: ItemStack, tooltip: util.List[String]) = tooltip

  override def handleItemTooltip(gui: GuiContainer, stack: ItemStack, x: Int, y: Int, tooltip: util.List[String]) = {
    if (stack != null && !isBlackListed(stack)) {
      // We do a bit of caching because recipe trees can be painfully complex,
      // so recomputing everything all the time is not an option. Here we check
      // the global cache, and if the requested stack is not present in it we
      // trigger a new executor thread to compute the costs (to avoid lagging
      // or even freezing the client).
      (cachedCosts(stack) match {
        case Some(value) => value
        case _ => tryCacheCosts(stack, () => computeCosts(stack))
      }).value match {
        case Some(Success((stackCosts, _))) =>
          // Yay, we have some cost information for the stack, see if it's not
          // the identity, in which case there are no recipes.
          if (stackCosts.size > 0 && (stackCosts.size != 1 || stackCosts.head._2 != 1.0 || !NEIServerUtils.areStacksSameTypeCrafting(stackCosts.head._1, stack))) {
            if (NEIClientUtils.altKey()) {
              tooltip.add(StatCollector.translateToLocal("cc.tooltip.Materials"))
              for ((ingredient, amount) <- stackCosts) {
                tooltip.add(" §7" + (if (amount < 1) "<" else " ") + math.ceil(amount).toInt + "x" + ingredient.getDisplayName)
              }
            }
            else {
              tooltip.add(StatCollector.translateToLocal("cc.tooltip.MaterialCosts"))
            }
          }
        case Some(Failure(error)) =>
          // Well, looks like something broke trying to get the costs.
          if (NEIClientUtils.altKey()) {
            tooltip.add(error.toString)
          }
        case _ =>
          // Still waiting for the costs to be computed...
          if (NEIClientUtils.altKey()) {
            tooltip.add(StatCollector.translateToLocal("cc.tooltip.Computing"))
          }
      }
    }
    tooltip
  }
}
