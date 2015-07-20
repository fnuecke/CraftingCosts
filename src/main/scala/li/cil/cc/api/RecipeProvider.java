package li.cil.cc.api;

import net.minecraft.item.ItemStack;

/**
 * Used to extend the mod with additional processing methods.
 * <p/>
 * For example, the default providers cover vanilla recipe types (shaped and
 * shapeless) as well as smelting recipes. Additional providers could cover
 * machines from different mods, such as ore processing stuff.
 */
public interface RecipeProvider {
    /**
     * Called when the costs for an item have to be computed.
     * <p/>
     * Gets passed the item the costs are currently being computed for, and
     * expects a list of recipes that yield the specified item. If the provider
     * does not handle the item, simply return an empty list or <tt>null</tt>.
     * <p/>
     * If some proccess only yields an item with a certain chance, consider
     * multiplying the input amounts accordingly.
     * <p/>
     * For recipes with multiple outputs, one recipe per output has to be
     * provided.
     *
     * @param stack the stack to find recipes for.
     * @return the recipes known to result in the stack.
     */
    Iterable<Recipe> recipesFor(ItemStack stack);
}
