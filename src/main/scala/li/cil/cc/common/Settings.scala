package li.cil.cc.common

import net.minecraftforge.common.config.Configuration

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

    "harvestcraft:bakewareItem", // Bakeware
    "harvestcraft:cuttingboardItem", // Cutting Board
    "harvestcraft:juicerItem", // Juicer
    "harvestcraft:mixingbowlItem", // Mixing Bowl
    "harvestcraft:potItem", // Pot
    "harvestcraft:saucepanItem", // Saucepan

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

  var preferredOreDictEntries = Array(
    "ThermalFoundation:material@64", // Copper Ingot
    "ThermalFoundation:material@67", // Lead Ingot
    "ThermalFoundation:material@66", // Silver Ingot
    "Mekanism:Ingot@4", // Steel Ingot
    "ThermalFoundation:material@65" // Tin Ingot
  )

  var recipeOverrides = Array(
    "appliedenergistics2" -> Array(
      "appliedenergistics2:item.ItemMultiMaterial@0" -> "appliedenergistics2:item.ItemMultiMaterial@2", // Certus Quartz Dust
      "appliedenergistics2:item.ItemMultiMaterial@1;minecraft:quartz;minecraft:redstone" -> "appliedenergistics2:item.ItemMultiMaterial@7" // Fluix Crystal
    ),

    "IC2" -> Array(
      "minecraft:iron_ingot" -> "IC2:itemIngot@3" // Refined Iron
    ),

    "Mekanism" -> Array(
      // Metallurgic Infuser
      "minecraft:redstone;Mekanism:Ingot@1" -> "Mekanism:ControlCircuit", // Basic Control Circuit
      "minecraft:iron_ingot;minecraft:redstone" -> "Mekanism:EnrichedAlloy", // Enriched Alloy
      "Mekanism:EnrichedAlloy;Mekanism:Dust@4" -> "Mekanism:ReinforcedAlloy", // Reinforced Alloy
      "Mekanism:ReinforcedAlloy;Mekanism:Dust@3" -> "Mekanism:AtomicAlloy", // Atomic Alloy
      "Mekanism:Dust@4;Mekanism:DirtyDust@6" -> "Mekanism:Dust@3", // Refined Obsidian Dust

      // Osmium Compressor
      "Mekanism:Ingot@1;minecraft:glowstone_dust" -> "Mekanism:Ingot@3", // Glowstone Ingot
      "Mekanism:Ingot@1;Mekanism:Dust@3" -> "Mekanism:Ingot" // Obsidian Ingot
    ),

    "ThermalExpansion" -> Array(
      "minecraft:iron_ingot;minecraft:coal@1:4" -> "IC2:itemIngot@3", // Refined Iron

      "ThermalFoundation:material@44:2;ThermalFoundation:material@512" -> "ThermalFoundation:material@76:2", // Enderium Ingot
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@15" -> "ThermalExpansion:Rockwool@0", // White Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@14" -> "ThermalExpansion:Rockwool@1", // Orange Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@13" -> "ThermalExpansion:Rockwool@2", // Magenta Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@12" -> "ThermalExpansion:Rockwool@3", // Light Blue Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@11" -> "ThermalExpansion:Rockwool@4", // Yellow Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@10" -> "ThermalExpansion:Rockwool@5", // Lime Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@9" -> "ThermalExpansion:Rockwool@6", // Pink Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@8" -> "ThermalExpansion:Rockwool@7", // Gray Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@6" -> "ThermalExpansion:Rockwool@9", // Cyan Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@5" -> "ThermalExpansion:Rockwool@10", // Purple Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@4" -> "ThermalExpansion:Rockwool@11", // Blue Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@3" -> "ThermalExpansion:Rockwool@12", // Brown Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@2" -> "ThermalExpansion:Rockwool@13", // Green Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@1" -> "ThermalExpansion:Rockwool@14", // Red Rockwool
      "ThermalExpansion:Rockwool@8:8;minecraft:dye@0" -> "ThermalExpansion:Rockwool@15", // Black Rockwool
      "ThermalFoundation:material@67;ThermalFoundation:material@4:8" -> "ThermalExpansion:Glass" // Hardened Glass
    )
  )

  // ----------------------------------------------------------------------- //

  def load(config: Configuration): Unit = {
//    ignoreAsOutput = config.getStringList("ignoreAsOutput", "", ignoreAsOutput, "Items to ignore as OUTPUTS for recipes. This is usually used for items that normally appear naturally in the world, but can also be created artificially using mods leading to cycles.")
//    ignoreAsInput = config.getStringList("ignoreAsInput", "", ignoreAsInput, "Items to ignore as INPUTS for recipes. This is usually used for items that are not consumed in the crafting operation, such as Harvestcraft or Gregtech crafting tools.")
  }
}
