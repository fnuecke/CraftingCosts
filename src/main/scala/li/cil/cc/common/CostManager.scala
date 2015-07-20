package li.cil.cc.common

import li.cil.cc.CraftingCosts
import net.minecraft.item.ItemStack

import scala.collection.mutable

object CostManager {
  private val cache = mutable.Map.empty[ItemStackWrapper, Iterable[ItemStackCost]]

  def clear(): Unit = cache.clear()

  def costsFor(stack: ItemStack) = cache.synchronized {
    val wrapper = new ItemStackWrapper(stack.copy())
    val costs = cache.getOrElseUpdate(wrapper, computeCosts(wrapper))
    if (costs.headOption.exists(_.wrapper == wrapper)) Iterable.empty
    else costs
  }

  private def computeCosts(wrapper: ItemStackWrapper) = {
    try {
      val queue = mutable.Queue(wrapper)
      val costs = mutable.Map.empty[ItemStackWrapper, Iterable[ItemStackWrapper]]

      CraftingCosts.log.info("Computing costs for " + wrapper.toString)

      // Get immediate inputs for all involved items that are not yet known.
      while (queue.nonEmpty) {
        val current = queue.dequeue()
        if (!cache.contains(current)) {
          val inputs = RecipeManager.inputsFor(current.inner)
          CraftingCosts.log.info("Costs for " + current.toString + ": " + inputs.mkString(", "))
          if (inputs.isEmpty) {
            cache += current -> Iterable(new ItemStackCost(current))
          }
          else {
            costs += current -> inputs
            queue ++= inputs.filterNot(costs.contains).map(_.normalized)
          }
        }
      }

      // Resolve new costs, keep trying until we make no progress.
      var progress = false
      do {
        CraftingCosts.log.info("Unresolved ingredients: " + costs.keys.mkString(", "))

        // Find all inputs involved that are fully resolved already (all of their inputs are resolved).
        val resolved = costs.filter { case (_, inputs) => inputs.forall(cache.contains) }
        for ((current, inputs) <- resolved) {
          // Get all (fully resolved!) inputs of our inputs, scaled by respective input stack size.
          val resolvedInputs = inputs.flatMap(input => cache(input).map(_.times(input.size)))
          // Make the list of inputs distinct and normalize it.
          val distinctInputs = resolvedInputs.groupBy(c => c.wrapper).
            map { case (w, c) => new ItemStackCost(w, c.map(_.amount).sum, c.exists(_.error)) }
          // Write the fully resolved and cleaned up costs to the cache.
          cache += current -> distinctInputs
        }

        // Remove stuff we just resolved from the queue.
        costs --= resolved.keys

        // Check if we made any progress, i.e. if we could resolve more inputs.
        progress = resolved.nonEmpty
      } while (progress && costs.nonEmpty)

      CraftingCosts.log.info("Cycles: " + costs.keys.mkString(", "))

      // Any remaining costs have cyclic dependencies.
      for ((current, inputs) <- costs) {
        // Get all (fully resolved!) inputs of our inputs, scaled by respective input stack size.
        val resolvedInputs = inputs.flatMap(input => cache.get(input) match {
          case Some(cost) => cost.map(_.times(input.size))
          case _ => Iterable(new ItemStackCost(input, true)) // Cycle! Use input as-is.
        })
        // Make the list of inputs distinct and normalize it.
        val distinctInputs = resolvedInputs.groupBy(c => c.wrapper).
          map { case (w, c) => new ItemStackCost(w, c.map(_.amount).sum, c.exists(_.error)) }
        // Write the fully resolved and cleaned up costs to the cache.
        cache += current -> distinctInputs
      }
    }
    catch {
      case t: Throwable =>
        CraftingCosts.log.warn(s"Failed computing costs for $wrapper.", t)
        cache += wrapper -> Iterable(new ItemStackCost(wrapper, true))
    }

    cache(wrapper)
  }
}
