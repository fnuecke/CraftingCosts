package li.cil.cc.common

import li.cil.cc.CraftingCosts
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.language.postfixOps
import scala.util.Sorting

/**
 * This is the heart of the logic. This graph stores two kinds of nodes: items
 * and transformations. The latter are generally recipes of different types,
 * such as shaped, shapeless, furnace, mod machines, ...
 * <p/>
 * These nodes are connected based on a transformation's inputs and outputs.
 * Each transformation node may have multiple incoming and outgoing edges
 * (inputs and outputs) and keeping them together in the node is required to
 * keep the logical <em>and</em> requirement when resolving recipes.
 * <p/>
 * A simple subgraph may look like this:
 * <pre>
 * [Item: Wood] -> [Transform] -> [Item: Plank]-+
 * |        |
 * v        |
 * [Item: Sticks] <- [Transform]     |
 * |                           |
 * v                           |
 * [Transform] <-------------------+
 * |
 * v
 * [Item: Sword]
 * </pre>
 * In this graph, `Wood` is a <em>pure</em> ingredient. Pure ingredients are
 * items that are not the result of any known recipe. To compute costs, we
 * would ideally resolve transformations starting from pure ingredients,
 * storing all paths generated this way. Paths are trees with the current item
 * as its root and the pure ingredients used to craft it as its leaves. When
 * hitting a cycle, the current path is dismissed. For a perfect solution we
 * would <em>always</em> have to follow potentially new paths, even if the
 * recipe had been completed before, in case this constellation:
 * <pre>
 * Path I: ... -> [A] -> [T0] -> [B] -> [T1] -> [A, C] -> ...
 * Path II: ... -> [B] -> [T1] -> [A, C] -> ...
 * </pre>
 * That is B is craftable using A; A and C are crafted using B via T1. In that
 * case, if path I reaches T1 before path II, we will not be able to save the
 * path for A, because it contains a cycle. So we must use path II to compute
 * the costs for A, which goes through the same transformation that has already
 * been covered.
 * <p/>
 * <em>However</em> this isn't quite feasible from a technical standpoint,
 * because the number of paths explodes to an unreasonable size this way, so we
 * take the shortcut and only use the first one we find, as this is
 * conveniently also the shortest one (seeing as we start traversing the graph
 * from the leaves). If some items are incorrectly deemed uncraftable due to
 * cycles this way, their recipe will have to be provided manually.
 * <p/>
 * So in practice, we have a one-step process, generating the first-best paths
 * we can find, then cleaning those up a little.
 */
private[cc] final class Graph {
  // Not using a set because we have to manually compare item stacks anyway.
  private val nodes = mutable.ArrayBuffer.empty[Item]

  // Item entries
  private val resolvedPreferredOreDictEntries = Settings.preferredOreDictEntries.map(resolvePattern).collect {
    case Some((item, _)) => item
  }.toSet

  // ----------------------------------------------------------------------- //

  /**
   * The number of items in this graph.
   */
  def size = nodes.size

  /**
   * Registers a new recipe defined by its inputs and outputs.
   * <p/>
   * The values in the given lists can either be simple item stacks or lists of
   * item stacks (as received from the ore dictionary, e.g.) in which case only
   * the first entry will be used.
   *
   * @param inputs the items used to craft the outputs.
   * @param outputs the items crafted using the inputs.
   */
  def addRecipe(inputs: Iterable[_], outputs: Iterable[_]): Unit = {
    val inputNodes = parseItems(inputs)._1
    val (outputNodes, outputSizes) = parseItems(outputs)
    if (inputNodes.nonEmpty && outputNodes.nonEmpty && !inputNodes.head.isInputOfRecipe(inputNodes, outputNodes, outputSizes)) {
      new Transformation(inputNodes.to[mutable.ArrayBuffer], outputNodes.to[mutable.ArrayBuffer], outputSizes.to[mutable.ArrayBuffer])
    }
  }

  /**
   * Register a list of pure ingredients, based on a name pattern.
   * <p/>
   * This will <em>not</em> invalidate recipes these items are part of, they
   * will only be ignored in the recipe. This is intended to be used for items
   * that are usually found in the world, but may also be crafted (usually by
   * mod machines) to avoid cycles.
   *
   * @param list the list of items to mark as pure ingredients.
   */
  def ignoreItemsAsOutput(list: Iterable[String]): Unit = {
    list.foreach(entry => resolvePattern(entry).foreach(_._1.ignoreAsOutput()))
  }

  /**
   * Register a list of ignored items, based on a name pattern.
   * <p/>
   * This will <em>not</em> invalidate recipes these items are part of, they
   * will only be ignored in the recipe. This is intended to be used for items
   * that are returned to the player's inventory during crafting operations,
   * such as Gregtech tools.
   *
   * @param list the list of items to ignore in recipes.
   */
  def ignoreItemsAsInput(list: Iterable[String]): Unit = {
    list.foreach(entry => resolvePattern(entry).foreach(_._1.ignoreAsInput()))
  }

  /**
   * Register a custom, predefined recipe regardless of automatic findings.
   * <p/>
   * For all outputs in these recipes, the existing transformations are
   * adjusted to no longer track these items as their ouputs. A new
   * transformation is registered, with the specified recipe.
   *
   * @param list the list of recipes to apply.
   */
  def overrideRecipes(list: Iterable[(String, String)]): Unit = {
    list.foreach(entry => {
      val (inputPattern, outputPattern) = entry
      val inputItems = inputPattern.split(';').map(resolvePattern).collect { case Some(input) => input}
      val outputItems = outputPattern.split(';').map(resolvePattern).collect { case Some(output) => output}

      // Missing inputs or outputs, skip this.
      if (inputItems.isEmpty || outputItems.isEmpty) return

      // Get rid of all pointers from existing recipes to these outputs.
      for ((item, _) <- outputItems) item.ignoreAsOutput()

      // Then create new a new transformation for this recipe.
      val inputs = inputItems.flatMap(entry => {
        val (item, count) = entry
        Array.fill(count)(item)
      }).to[mutable.ArrayBuffer]
      val outputs = outputItems.map(_._1).to[mutable.ArrayBuffer]
      val outputSizes = outputItems.map(_._2).to[mutable.ArrayBuffer]
      new Transformation(inputs, outputs, outputSizes)
    })
  }

  /**
   * Computes the item costs for all items in the graph.
   * <p/>
   * If there are multiple possible ways to craft an item, the least complex
   * way is chosen, i.e. the one with the least involved crafting steps. If
   * there are ties, the recipe that was registered earlier wins.
   */
  def computeCostMap() = {
    Sorting.stableSort(nodes, (x: Item, y: Item) => compareStacks(x.stack, y.stack))

    resolveWildcards()
    stripContainerRecipes()
    removeIdentityTransforms()
    removeIsolated()
    compilePaths()
    computeCosts()
  }

  // ----------------------------------------------------------------------- //

  /**
   * Compare two item stacks, for sorting (to get a stable order for things).
   */
  private def compareStacks(x: ItemStack, y: ItemStack) = {
    val xId = Item.getIdFromItem(x.getItem)
    val yId = Item.getIdFromItem(y.getItem)
     xId < yId || (xId == yId && x.getItemDamage < y.getItemDamage)
  }

  /**
   * Parse item nodes from a given item stack or list of item stacks (oredict entries).
   */
  private def parseItems(values: Iterable[_]) = {
    // Figure out actual inputs when ore dictionary entries are in play.
    // In those cases we just use the first ore dictionary entry, because we
    // take that into account when checking for item stack equivalence anyway.
    def resolveOreDictList(list: Iterable[ItemStack]) = {
      // Sort oredict lists to always pick the same oredict entry for all
      // recipes, to avoid discrepancies, causing cost computation to fail.
      val nodes = list.filter(_.stackSize > 0).toArray.sortWith(compareStacks).map(stack => (findOrCreateNode(stack), stack.stackSize))
      nodes.find {
        case (item, count) => count > 0 && resolvedPreferredOreDictEntries.contains(item)
      }.orElse(nodes.headOption)
    }
    values.map {
      case stack: ItemStack => Some((findOrCreateNode(stack), stack.stackSize))
      case list: scala.collection.Iterable[ItemStack]@unchecked if list.size > 0 => resolveOreDictList(list)
      case list: java.lang.Iterable[ItemStack]@unchecked if list.size > 0 => resolveOreDictList(list)
      case _ => None
    }.collect { case Some(entry) => entry }.toArray.unzip
  }

  /**
   * Replace wildcard values with a real node that preferably has a cycle-free recipe.
   */
  private def resolveWildcards(): Unit = {
    val wildcardNodes = nodes.filter(_.stack.getItemDamage == OreDictionary.WILDCARD_VALUE)

    CraftingCosts.log.debug(s"Resolving ${wildcardNodes.length} wildcards...")

    wildcardNodes.foreach(wildcard => {
      val matches = nodes.filter(node => node != wildcard && areStacksEquivalent(node.stack, wildcard.stack))
      matches.find(!_.isPureIngredient).orElse(matches.headOption) match {
        case Some(node) => wildcard.resolveWildcard(node)
        case _ =>
      }
    })
  }

  /**
   * Remove all transformations that are used to fill container items. This is
   * done to avoid recipes using, for example, water buckets to be computed as
   * requiring three iron ingots.
   */
  private def stripContainerRecipes(): Unit = {
    for (node <- nodes if node.stack.getItem.hasContainerItem(node.stack)) {
      val container = node.stack.getItem.getContainerItem(node.stack)
      if (container != null) {
        node.removeContainerRecipes(container)
      }
    }
  }

  /**
   * Kill off transforms that result in output equivalent to their input. This
   * is necessary to avoid a ton of cycle exceptions for recrafting recipes,
   * such as heavily used in Harvestcraft for example.
   */
  private def removeIdentityTransforms(): Unit = {
    for (node <- nodes) {
      node.removeIdentityTransforms()
    }
  }

  /**
   * Remove any items that are completely isolated (no recipes and no ingredient)
   * as well as dead-end recipes (empty input or output sets).
   */
  private def removeIsolated(): Unit = {
    nodes.foreach(_.removeDeadEnds())
    nodes --= nodes.filter(_.isIsolated)
  }

  /**
   * Compile all shortest paths, starting at the pure ingredient nodes.
   */
  private def compilePaths(): Unit = {
    CraftingCosts.log.debug(s"Computing crafting paths...")

    var frontier: Iterable[Path] = nodes.collect {
      case item if item.isPureIngredient => new Path(item)
    }
    while (frontier.nonEmpty) {
      CraftingCosts.log.debug(s"Advancing all crafting paths, we have ${frontier.size} seeds for this step.")
      frontier = frontier.flatMap(segment => segment.item.advance(segment))
    }
  }

  /**
   * Compute the actual costs for each item.
   */
  private def computeCosts() = {
    CraftingCosts.log.debug(s"Computing item costs based on shortest paths...")

    nodes.par.map(item => item.getPath match {
      case Some(path) if path.depth > 1 => Some(item.stack -> path.computeCosts().toArray)
      case _ =>
        if (!item.isPureIngredient) {
          val stack = item.stack
          CraftingCosts.log.warn(s"No cycle-free recipe for ${stack.getDisplayName} (${Item.itemRegistry.getNameForObject(stack.getItem)}@${stack.getItemDamage}). Consider specifying explicit costs in the config or add it to the ignore list.")
        }
        None
    }).seq.collect {
      case Some((stack, costs)) => stack -> costs
    }.toArray
  }

  private def resolvePattern(pattern: String): Option[(Item, Int)] = {
    val Array(name, metaString) = pattern.split('@').take(2).padTo(2, "0")
    val Array(meta, count) = metaString.split(':').take(2).padTo(2, "1").map(_.toInt)
    Option(net.minecraft.item.Item.itemRegistry.getObject(name)) match {
      case Some(item: net.minecraft.item.Item) => return Some(findOrCreateNode(new ItemStack(item, 1, meta)) -> count)
      case _ =>
    }
    Option(net.minecraft.block.Block.getBlockFromName(name)) match {
      case Some(block) =>
        val stack = new ItemStack(block, 1, meta)
        if (stack.getItem != null) return Some(findOrCreateNode(stack) -> count)
        else CraftingCosts.log.warn(s"Trying to parse a broken block ${block.getLocalizedName} ($pattern) (stack has no item).")
      case _ =>
    }

    CraftingCosts.log.warn(s"Failed resolving pattern '$pattern', no such item or block.")
    None
  }

  private def findOrCreateNode(stack: ItemStack) = nodes.find(node => stack.isItemEqual(node.stack)) match {
    case Some(node) => node
    case _ => new Item(stack.copy())
  }

  private def areStacksEquivalent(stackA: ItemStack, stackB: ItemStack) =
    OreDictionary.getOreIDs(stackA).intersect(OreDictionary.getOreIDs(stackB)).length > 0 ||
      (stackA.getItem == stackB.getItem &&
        (stackA.getItemDamage == OreDictionary.WILDCARD_VALUE || stackB.getItemDamage == OreDictionary.WILDCARD_VALUE || stackA.getItemDamage == stackB.getItemDamage))

  // ----------------------------------------------------------------------- //

  private final class Item(val stack: ItemStack) {
    stack.stackSize = 1
    nodes += this

    // Linked hash set for stable order, makes sure we use the same ore dict
    // ingredients for all parts of a recipe to avoid fragmentation.
    private val outputOf = mutable.LinkedHashSet.empty[Transformation]
    private val inputOf = mutable.LinkedHashSet.empty[Transformation]

    // List of paths (crafting steps) can lead to this item. Filled in when
    // pushing forward the pure ingredient border during cost compilation.
    private var path: Option[Path] = None

    def isPureIngredient = outputOf.isEmpty

    def isIsolated = outputOf.isEmpty && inputOf.isEmpty

    def getPath = path

    def trySetPath(path: Path): Unit = this.synchronized {
      this.path match {
        case Some(p) if p.depth <= path.depth => // Already have a shorter / equivalent path.
        case _ => this.path = Some(path)
      }
    }

    def isInputOfRecipe(inputs: Array[Item], outputs: Array[Item], outputSizes: Array[Int]) =
      inputOf.exists(recipe =>
        recipe.inputs.length == inputs.length &&
          recipe.outputs.length == outputs.length &&
          (recipe.inputs, inputs).zipped.forall(_==_) &&
          (recipe.outputs, outputs).zipped.forall(_==_) &&
          (recipe.outputSizes, outputSizes).zipped.forall(_==_))

    def markAsResultOf(node: Transformation): Unit = outputOf += node

    def markAsIngredientIn(edge: Transformation): Unit = inputOf += edge

    def ignoreAsOutput(recipe: Option[Transformation] = None): Unit = {
      val list = recipe match {
        case Some(filter) => outputOf.filter(_ == filter)
        case _ => outputOf
      }
      list.foreach(recipe => while (recipe.outputs.contains(this)) {
        val index = recipe.outputs.indexOf(this)
        recipe.outputs.remove(index)
        recipe.outputSizes.remove(index)
      })
      outputOf --= list
    }

    def ignoreAsInput(recipe: Option[Transformation] = None): Unit = {
      val list = recipe match {
        case Some(filter) => inputOf.filter(_ == filter)
        case _ => inputOf
      }
      list.foreach(recipe => while (recipe.inputs.contains(this)) {
        recipe.inputs -= this
      })
      inputOf --= list
    }

    def resolveWildcard(replacement: Item): Unit = {
      outputOf.foreach(_.patch(this, replacement))
      inputOf.foreach(_.patch(this, replacement))
      replacement.outputOf ++= outputOf
      replacement.inputOf ++= inputOf
      outputOf.clear()
      inputOf.clear()
    }

    def removeContainerRecipes(container: ItemStack): Unit = {
      outputOf.clone().foreach(recipe => {
        if (recipe.inputs.exists(item => areStacksEquivalent(container, item.stack))) {
          while (recipe.outputs.contains(this)) {
            recipe.outputs -= this
          }
          outputOf -= recipe
        }
      })
    }

    def removeIdentityTransforms(): Unit = {
      inputOf.filter(recipe => {
        val in = recipe.inputs.toSet
        val out = recipe.outputs.toSet
        out.diff(in).size == 0
      }).foreach(recipe => {
        recipe.inputs.foreach(_.inputOf -= recipe)
        recipe.outputs.foreach(_.outputOf -= recipe)
      })
    }

    def removeDeadEnds(): Unit = {
      inputOf --= inputOf.filter(_.outputs.isEmpty)
      outputOf --= outputOf.filter(_.inputs.isEmpty)
    }

    def advance(path: Path) = {
      assert(path.item == this)
      val newSegments = mutable.ArrayBuffer.empty[Path]
      for (transformation <- inputOf) {
        newSegments ++= transformation.satisfy(path).flatMap(parents => transformation.outputs.distinct.collect {
          case output if output.getPath.isEmpty && !path.contains(output) => new Path(output, transformation.outputSizeFor(output), parents)
        })
      }
      // Avoid duplicate paths (possible for duplicate recipes, such as
      // smelting stuff in the normal furnace or mod furnaces).
      newSegments.groupBy(_.item).map(_._2.head)
    }

    override def toString = stack.getDisplayName
  }

  // ----------------------------------------------------------------------- //

  private final class Transformation(val inputs: mutable.ArrayBuffer[Item], val outputs: mutable.ArrayBuffer[Item], val outputSizes: mutable.ArrayBuffer[Int]) {
    inputs.foreach(_.markAsIngredientIn(this))
    outputs.foreach(_.markAsResultOf(this))

    // Used to track input resolution when pushing forward the pure
    // ingredient border during cost compilation. Lazy to ensure it
    // is initialized to the correct length (after cleaning up our
    // inputs in further preprocessing steps).
    private lazy val solvedInputs = Array.fill(inputs.length)(mutable.ArrayBuffer.empty[Path])

    def outputSizeFor(item: Item) = outputSizes(outputs.indexOf(item))

    // Mark the specified item as "solved". When returning true, outputs of
    // this recipe are used to continue the current path.
    def satisfy(segment: Path): Iterable[Seq[Graph.this.type#Path]] = {
      val inputIndexes = inputs.zipWithIndex collect {
        case (input, index) if input == segment.item => index
      }

      val candidates = solvedInputs.synchronized {
        // This is the line removing full coverage.
        if (solvedInputs.count(_.nonEmpty) >= inputs.length) return Iterable.empty

        // Store new incoming path.
        inputIndexes.foreach(index => solvedInputs(index) += segment)

        // If any entry is empty we're missing some dependency, so we stop
        // here for now...
        if (solvedInputs.count(_.nonEmpty) < inputs.length) return Iterable.empty

        // Generate downstream paths based on the current segment, i.e. us it
        // as the only fix-point when generating the permutations using other
        // incoming paths.
        solvedInputs.map(segments => segments.filterNot(_.contains(segment.item))).map(_.toSeq)
      }
      inputIndexes.foreach(index => candidates(index) = Seq(segment))
      candidates.foldLeft(Iterable.fill(inputs.length)(Seq.empty[Path]))((acc, seg) => cross(acc, seg))
    }

    // Used for wildcard replacement, patches references to an item to point
    // to another one (wildcard -> replacement).
    def patch(oldItem: Item, newItem: Item): Unit = {
      for (i <- 0 until inputs.length) {
        if (inputs(i) == oldItem) inputs(i) = newItem
      }
      for (i <- 0 until outputs.length) {
        if (outputs(i) == oldItem) outputs(i) = newItem
      }
    }

    private def cross[T](xs: Iterable[Seq[T]], ys: Iterable[T]) =
      for (x <- xs; y <- ys) yield x :+ y

    override def toString = "[" + inputs.mkString(", ") + "] -> [" + outputs.mkString(", ") + "]"
  }

  // ----------------------------------------------------------------------- //

  private final class Path(val item: Item, count: Int = 1, val next: Iterable[Path] = Iterable.empty) {
    item.trySetPath(this)

    def computeCosts(amount: Double = 1.0): Iterable[Costs] =
      if (next.isEmpty) {
        assert(item.isPureIngredient)
        Iterable(new Costs(item.stack, amount))
      }
      else {
        // Compute parent costs. These will *not* be unique by item type,
        // so we'll merge them as a post-processing step.
        next.flatMap(_.computeCosts(amount / count)).groupBy(_.item).map {
          case (stack, costs) => new Costs(stack, costs.map(_.amount).sum)
        }
      }

    /**
     * Tests whether the specified item already appears somewhere in this path.
     */
    def contains(item: Item): Boolean = item == this.item || next.exists(_.contains(item))

    /**
     * Computes the depth of this path.
     */
    def depth: Int = 1 + (next.map(_.depth) ++ Seq(0)).max
  }

}
