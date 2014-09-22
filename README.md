CraftingCosts
=============

This is an NEI addon for showing overall crafting costs for items. This was originally a debug feature in OpenComputers, but was popular enough to become a proper feature. So I decided to expand upon the idea and make use of NEI's recipe information, making it an NEI addon. The plugin extends the tooltip of items with information on what ingredients are required to craft them. This makes it a log easier to judge the worth of items, and also has the practical effect that you can gather the raw materials for whatever you want to craft without manually going through nested recipes in NEI.

![Compact](http://i.imgur.com/G0xfcpy.png)
![Expanded](http://i.imgur.com/pg8sZnL.png)

When there are multiple recipes available, the mod will choose the one with the least overall complexity (i.e. the fewest required crafting steps), and breaks ties by preferring crafting processes with fewer outputs. For example, iron ingots can be crafted either by decomposing a block of iron, or melting iron ore - the melting process is preferred, because it has only one output, where the iron block gives nine ingots. This can result in the costs overestimating the real costs, though. For example, it will prefer ingots melted in a furnace over multi-step-ore-multiplication.

Modpacks
--------
You are free to add this mod to your modpack. I'd be happy to hear about it, but you have my permission nonetheless (not that you'd need it, the mod is MIT licensed, but hey, thanks for asking!)

Download
--------
You can downoad the mod [over on Curse](http://www.curse.com/mc-mods/minecraft/224427-craftingcosts).
