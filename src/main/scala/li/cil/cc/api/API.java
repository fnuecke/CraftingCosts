package li.cil.cc.api;

import net.minecraft.item.ItemStack;

import java.util.Collections;

/**
 * Public API for the CraftingCosts mod.
 * <p/>
 * Here you can register custom recipe providers and query information, such as
 * determined crafting costs for specific items.
 */
public class API {
    /**
     * Register a new recipe provider.
     * <p/>
     * Providers are queried whenever the costs for an item have to be resolved
     * (only happens once per item). From all accumulated recipes, the best fit
     * is determined based on the mods configuration (e.g. prefer inputs that
     * are ores, dusts or ingots over others, use specific recipes for certain
     * items, and so on).
     * <p/>
     * CraftingCosts itself comes with a few built-in providers, such as for
     * vanilla crafting and smelting recipes.
     *
     * @param provider the provider to register.
     */
    public static void register(RecipeProvider provider) {
        if (instance != null)
            instance.register(provider);
    }

    /**
     * Invalidate all currently known crafting costs.
     * <p/>
     * Costs for items are cached for performance. If something that has an
     * influence on recipes changes, call this to invalidate the cache and
     * recompute costs from scratch.
     * <p/>
     * This is called automatically when the player enters a new game, for
     * example, say, to take into account server-specific recipes.
     */
    public static void invalidateCosts() {
        if (instance != null)
            instance.invalidateCosts();
    }

    /**
     * Get all possible recipes for the specified stack.
     * <p/>
     * This is determined using the list of registered recipe providers.
     *
     * @param stack the stack to get the recipes for.
     * @return the list of known recipes for this stack.
     */
    public static Iterable<Recipe> recipesFor(ItemStack stack) {
        if (instance != null)
            return instance.recipesFor(stack);
        return Collections.emptyList();
    }

    // --------------------------------------------------------------------- //

    public static APIImpl instance;

    public interface APIImpl {
        void register(RecipeProvider provider);

        void invalidateCosts();

        Iterable<Recipe> recipesFor(ItemStack stack);
    }

    private API() {
    }
}
