package li.cil.cc.api;

import net.minecraft.item.ItemStack;

/**
 * Represents a single recipe.
 * <p/>
 * Used by {@link RecipeProvider}s to return a list of recipes.
 */
public class Recipe {
    /**
     * The output of the recipe. Stack size is taken into account.
     */
    public final ItemStack output;

    /**
     * The inputs of the recipe. Stack sizes are taken into account.
     */
    public final Iterable<ItemStack> input;

    /**
     * The sort index of the recipe. Used to select recipes, lowest wins.
     */
    public final int sortIndex;

    public Recipe(ItemStack output, Iterable<ItemStack> input, int sortIndex) {
        this.output = output;
        this.input = input;
        this.sortIndex = sortIndex;
    }

    public Recipe(ItemStack output, Iterable<ItemStack> input) {
        this.output = output;
        this.input = input;
        this.sortIndex = 0;
    }
}
