package li.cil.cc.integration

import codechicken.nei.PositionedStack
import codechicken.nei.api.IConfigureNEI
import codechicken.nei.recipe.GuiCraftingRecipe
import codechicken.nei.recipe.ICraftingHandler
import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.cc.CraftingCosts
import li.cil.cc.common.Settings
import li.cil.cc.integration.Integration.Mod
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.language.postfixOps

object NEI extends Mod {
  type HandlerCallback = ICraftingHandler => Iterable[(Iterable[Any], Iterable[Any])]

  lazy val handlerCallbacks = mutable.Set.empty[HandlerCallback]

  private lazy val allItems = {
    val items = mutable.ListBuffer.empty[ItemStack]
    Item.itemRegistry.foreach {
      case item: Item =>
        for (tab <- item.getCreativeTabs) item.getSubItems(item, tab, items)
    }
    items
  }

  override def init(): Unit = {
    FMLInterModComms.sendMessage(CraftingCosts.Id, "addRecipeProvider", "li.cil.cc.integration.NEI.neiRecipes")

    handlerCallbacks += defaultCallback
  }

  private def getItems(ps: PositionedStack) = {
    if (ps.items != null && ps.items.length > 0) ps.items.toIterable
    else ps.item
  }

  def defaultCallback(handler: ICraftingHandler) = {
    if (Settings.ignoredCraftingHandlers.contains(handler.getRecipeName)) Iterable.empty
    else {
      (0 until handler.numRecipes).map(recipe => try {
        val inputs = handler.getIngredientStacks(recipe).map(getItems)
        val output = getItems(handler.getResultStack(recipe))
        val other = handler.getOtherStacks(recipe)
        if (other == null || other.size < 1) {
          (inputs, Iterable(output))
        }
        else {
          // No way to uniformly handle secondary inputs/outputs specified this way.
          // Mods tend to use this in very different, and occasionally very silly
          // ways. For example, Thermal Expansion *sometimes* uses this for
          // secondary inputs, but not always!
          null
        }
      }
      catch {
        case t: Throwable =>
          // Usually NPEs due to recipes without outputs/inputs.
          null
      }).filter(null !=)
    }
  }

  def neiRecipes = {
    () => {
      val handlers = GuiCraftingRecipe.craftinghandlers.toBuffer
      // Make actual iterable lazy evaluated to have that part run in a thread.
      new Iterable[(Iterable[Any], Iterable[Any])] {
        private lazy val result = handlers.flatMap[CraftingCosts.ItemTransformation, Iterable[CraftingCosts.ItemTransformation]](craftingHandler => {
          allItems.flatMap(item => handlerCallbacks.flatMap(_(craftingHandler.getRecipeHandler("item", item))))
        })
        override def iterator = result.iterator
      }
    }
  }
}

class NEICraftingCostsConfig extends IConfigureNEI {
  override def getName = CraftingCosts.Id

  override def getVersion = CraftingCosts.Version

  override def loadConfig(): Unit = {
    CraftingCosts.recompute()
  }
}
