package li.cil.cc

import li.cil.cc.api.Recipe
import li.cil.cc.common.ItemStackWrapper
import net.minecraftforge.common.config.Configuration

import scala.collection.mutable

object Settings {
  var ignoreAsOutput = Array(
    "minecraft:apple",
    "minecraft:beef",
    "minecraft:blaze_rod",
    "minecraft:bone",
    "minecraft:brown_mushroom",
    "minecraft:cactus",
    "minecraft:carrot",
    "minecraft:chicken",
    "minecraft:clay_ball",
    "minecraft:coal_ore",
    "minecraft:cobblestone",
    "minecraft:diamond_ore",
    "minecraft:dirt",
    "minecraft:double_plant@0",
    "minecraft:double_plant@1",
    "minecraft:double_plant@4",
    "minecraft:double_plant@5",
    "minecraft:dye@0",
    "minecraft:dye@3",
    "minecraft:egg",
    "minecraft:emerald_ore",
    "minecraft:ender_pearl",
    "minecraft:feather",
    "minecraft:filled_map@32767",
    "minecraft:fish@0",
    "minecraft:fish@1",
    "minecraft:fire",
    "minecraft:flint",
    "minecraft:glowstone",
    "minecraft:glowstone_dust",
    "minecraft:gold_ore",
    "minecraft:gravel",
    "minecraft:gunpowder",
    "minecraft:iron_ore",
    "minecraft:lapis_ore",
    "minecraft:leather",
    "minecraft:log2@0",
    "minecraft:log2@1",
    "minecraft:log@0",
    "minecraft:log@1",
    "minecraft:log@2",
    "minecraft:log@3",
    "minecraft:melon",
    "minecraft:milk_bucket",
    "minecraft:nether_star",
    "minecraft:netherrack",
    "minecraft:obsidian",
    "minecraft:porkchop",
    "minecraft:potato",
    "minecraft:pumpkin",
    "minecraft:quartz_ore",
    "minecraft:red_flower@0",
    "minecraft:red_flower@1",
    "minecraft:red_flower@2",
    "minecraft:red_flower@3",
    "minecraft:red_flower@4",
    "minecraft:red_flower@5",
    "minecraft:red_flower@6",
    "minecraft:red_flower@7",
    "minecraft:red_flower@8",
    "minecraft:red_mushroom",
    "minecraft:redstone_ore",
    "minecraft:reeds",
    "minecraft:rotten_flesh",
    "minecraft:sand@0",
    "minecraft:sand@1",
    "minecraft:slime_ball",
    "minecraft:snowball",
    "minecraft:spider_eye",
    "minecraft:sponge@1",
    "minecraft:string",
    "minecraft:vine",
    "minecraft:wheat",
    "minecraft:yellow_flower@0",

    "appliedenergistics2:item.ItemMultiMaterial", // Certus Quartz Crystal
    "appliedenergistics2:item.ItemMultiMaterial@13", // Inscriber Calculation Press
    "appliedenergistics2:item.ItemMultiMaterial@14", // Inscriber Engineering Press
    "appliedenergistics2:item.ItemMultiMaterial@15", // Inscriber Logic Press
    "appliedenergistics2:item.ItemMultiMaterial@19", // Inscriber Silicon Press

    "Railcraft:tile.railcraft.brick.bleachedbone@5", // Bleached Bone Cobblestone
    "Railcraft:tile.railcraft.brick.infernal@5", // Infernal Cobblestone
    "Railcraft:tile.railcraft.brick.abyssal@5", // Abyssal Cobblestone
    "Railcraft:tile.railcraft.brick.nether@5", // Nether Cobblestone
    "Railcraft:tile.railcraft.brick.sandy@5", // Sandy Cobblestone
    "Railcraft:tile.railcraft.brick.bloodstained@5", // Bloody Cobblestone
    "Railcraft:tile.railcraft.brick.quarried@5", // Quarried Cobblestone
    "Railcraft:tile.railcraft.brick.frostbound@5", // Frost Bound Cobblestone

    "ThermalExpansion:satchel@0", // Creative Satchel
    "ThermalExpansion:Cell@0", // Creative Energy Cell
    "ThermalExpansion:Strongbox@0" // Creative Strongbox
  )

  var ignoreAsInput = Array(
    "minecraft:fire", // Because Chainmail...

    "appliedenergistics2:item.ItemMultiMaterial@13", // Inscriber Calculation Press
    "appliedenergistics2:item.ItemMultiMaterial@14", // Inscriber Engineering Press
    "appliedenergistics2:item.ItemMultiMaterial@15", // Inscriber Logic Press
    "appliedenergistics2:item.ItemMultiMaterial@19", // Inscriber Silicon Press

    "gregtech:gt.metatool.01@18", // File
    "gregtech:gt.metatool.01@12", // Hammer
    "gregtech:gt.metatool.01@22", // Screwdriver
    "gregtech:gt.metatool.01@20", // Crowbar
    "gregtech:gt.metatool.01@16", // Wrench
    "gregtech:gt.metatool.01@120", // Wrench (LV)
    "gregtech:gt.metatool.01@122", // Wrench (MV)
    "gregtech:gt.metatool.01@124", // Wrench (HV)
    "gregtech:gt.metatool.01@14", // Soft Hammer
    "gregtech:gt.metatool.01@10", // Saw
    "gregtech:gt.metatool.01@110", // Chainsaw (LV)
    "gregtech:gt.metatool.01@112", // Chainsaw (MV)
    "gregtech:gt.metatool.01@114", // Chainsaw (HV)
    "gregtech:gt.metatool.01@32", // Universal Spade
    "gregtech:gt.metatool.01@26", // Wire Cutter
    "gregtech:gt.metatool.01@24", // Mortar
    "gregtech:gt.metatool.01@34", // Knife

    "harvestcraft:bakewareItem", // Bakeware
    "harvestcraft:cuttingboardItem", // Cutting Board
    "harvestcraft:juicerItem", // Juicer
    "harvestcraft:mixingbowlItem", // Mixing Bowl
    "harvestcraft:potItem", // Pot
    "harvestcraft:saucepanItem", // Saucepan

    "IC2:itemToolCutter", // Cutter
    "IC2:itemToolForgeHammer@0:1", // Forge Hammer

    "Railcraft:tile.railcraft.ore@7", // Poor Iron Ore
    "Railcraft:tile.railcraft.ore@8", // Poor Gold Ore
    "Railcraft:tile.railcraft.ore@9", // Poor Copper Ore
    "Railcraft:tile.railcraft.ore@10" // Poor Tin Ore
  )

  var ignoredCraftingHandlers = Array(
    "Fluid Transposer",
    "Scrapbox",
    "Crafting Profiling",
    "AE2 In World Crafting",
    "Fuel",
    "IC2 Recharging"
  )

  var recipeOverrides = mutable.Map.empty[ItemStackWrapper, Recipe]

  // ----------------------------------------------------------------------- //

  def load(config: Configuration): Unit = {
    ignoreAsOutput = config.getStringList("ignoreAsOutput", "", ignoreAsOutput, "Items to ignore as OUTPUTS for recipes. This is usually used for items that normally appear naturally in the world, but can also be created artificially using mods, leading to cycles.")
    ignoreAsInput = config.getStringList("ignoreAsInput", "", ignoreAsInput, "Items to ignore as INPUTS for recipes. This is usually used for items that are not consumed in the crafting operation, such as HarvestCraft or GregTech crafting tools.")
    ignoredCraftingHandlers = config.getStringList("ignoredCraftingHandlers", "", ignoredCraftingHandlers, "Names of crafting NEI handlers to ignore when looking for recipes.")

    // TODO Save/load recipeOverrides.
  }
}
