package li.cil.cc

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import li.cil.cc.common.CommonProxy
import li.cil.cc.common.Costs
import li.cil.cc.common.Graph
import li.cil.cc.common.Settings
import li.cil.cc.integration.Integration
import net.minecraft.item.ItemStack
import net.minecraftforge.common.config.Configuration
import org.apache.logging.log4j.LogManager

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.language.postfixOps

@Mod(modid = CraftingCosts.Id, name = CraftingCosts.Id, version = CraftingCosts.Version, dependencies = "after:NotEnoughItems", modLanguage = "scala")
object CraftingCosts {
  final val Id = "CraftingCosts"
  final val Version = "1.1.0"

  @SidedProxy(clientSide = "li.cil.cc.client.ClientProxy", serverSide = "li.cil.cc.common.CommonProxy")
  var proxy: CommonProxy = null

  var log = LogManager.getLogger("OpenComputers")

  type ItemTransformation = (Iterable[Any], Iterable[Any])

  // Cache for cost lookups, because the linear search in the actual item
  // cost array may become slow when there are a lot of items, and we may
  // be asked for costs very frequently (e.g. once per rendered frame for
  // tooltips).
  private val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(5, TimeUnit.SECONDS).
    asInstanceOf[CacheBuilder[ItemStack, Option[Array[Costs]]]].
    build[ItemStack, Option[Array[Costs]]]()

  // Known crafting costs for item stacks. The stacks used as key of this
  // pseudo map are compared to lookups taking into account ore dictionary
  // entries, so an actual hash map would not work.
  private var itemCosts = Array.empty[(ItemStack, Array[Costs])]

  // List of recipe providers that allow adding item transformations in a
  // uniform way (e.g. simple crafting or smelting in a furnace).`
  private val recipeProviders = mutable.ArrayBuffer.empty[() => Iterable[ItemTransformation]]

  // Whether costs need to be recomputed. May become necessary due to
  // certain recipe sources updating dynamically, such as NEI.
  private var needsRecomputing = true
  private var isRecomputing = false

  // ----------------------------------------------------------------------- //

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) {
    log = e.getModLog
    Settings.load(new Configuration(e.getSuggestedConfigurationFile))
  }

  @EventHandler
  def init(e: FMLInitializationEvent) = proxy.init()

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = {
    proxy.postInit()
    recompute()
  }

  @EventHandler
  def imc(e: IMCEvent): Unit = {
    for (message <- e.getMessages) {
      Integration.process(message)
    }
  }

  // ----------------------------------------------------------------------- //

  def addRecipeProvider(provider: () => Iterable[(Iterable[Any], Iterable[Any])]): Unit = {
    recipeProviders += provider
  }

  def recompute(): Unit = {
    this.synchronized {
      if (!needsRecomputing && !isRecomputing) {
        // Reset our stuff.
        cache.invalidateAll()
        cache.cleanUp()
        itemCosts = Array.empty[(ItemStack, Array[Costs])]
        needsRecomputing = true
      }
    }
  }

  def getCosts(stack: ItemStack) =
    this.synchronized {
      if (needsRecomputing && !isRecomputing) {
        needsRecomputing = false
        isRecomputing = true

        // Get list of recipes synchronized as it may access structures from other
        // mods, which may go wrong if that happens from another thread.
        val recipes = collectRecipes()

        // Build actual costs in separate thread because that can take some time.
        Future {
          try initializeCosts(buildRecipeGraph(recipes)) catch {
            case t: Throwable => log.warn("Error computing item costs.", t)
          }
          this.synchronized(isRecomputing = false)
        }
      }
      if (stack != null && itemCosts.nonEmpty) {
        val result = cache.get(stack, new Callable[Option[Array[Costs]]] {
          override def call() = itemCosts.find {
            case (other, _) => other.getItem == stack.getItem && (stack.isItemDamaged || (other.getItemDamage == stack.getItemDamage))
          } match {
            case Some((_, costs)) => Some(costs.sortBy(_.item.getDisplayName))
            case _ => None
          }
        })
        cache.cleanUp()
        result
      }
      else None
    }

  // ----------------------------------------------------------------------- //

  /**
   * Get the concatenated list of known recipes as a lazily evaluated iterable.
   */
  private def collectRecipes() = recipeProviders.view.flatMap(_())

  /**
   * Prepare our graph of items and recipes based on all known recipes.
   */
  private def buildRecipeGraph(recipes: Iterable[(Iterable[Any], Iterable[Any])]) = {
    CraftingCosts.log.info("Starting to build recipe graph...")
    val start = System.currentTimeMillis()

    val graph = new Graph()
    for ((input, output) <- recipes) {
      graph.addRecipe(input, output)
    }

    CraftingCosts.log.info(s"Done building recipe graph. Took ${math.round((System.currentTimeMillis() - start) / 100.0) / 10.0}s for ${graph.size} items.")

    graph
  }

  /**
   * Perform cost computation based on our graph.
   */
  private def initializeCosts(graph: Graph): Unit = {
    CraftingCosts.log.info("Computing crafting costs for each node...")
    val start = System.currentTimeMillis()

    graph.ignoreItemsAsOutput(Settings.ignoreAsOutput)
    graph.ignoreItemsAsInput(Settings.ignoreAsInput)
    for ((mod, overrides) <- Settings.recipeOverrides if Loader.isModLoaded(mod)) {
      graph.overrideRecipes(overrides)
    }

    val costs = graph.computeCostMap()
    this.synchronized(itemCosts = costs)

    CraftingCosts.log.info(s"Done computing item costs. Took ${math.round((System.currentTimeMillis() - start) / 100.0) / 10.0}s for ${costs.length} results.")
  }
}
