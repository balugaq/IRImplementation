package org.irmc.industrialrevival.implementation.multiblock;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.irmc.industrialrevival.api.multiblock.MultiBlock;
import org.irmc.industrialrevival.api.multiblock.StructureBuilder;
import org.irmc.industrialrevival.api.multiblock.StructureUtil;
import org.irmc.industrialrevival.dock.IRDock;
import org.irmc.pigeonlib.items.CustomItemStack;
import org.irmc.industrialrevival.api.recipes.RecipeType;
import org.irmc.industrialrevival.implementation.IndustrialRevival;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CokeOven extends MultiBlock {
    private
    final ItemStack RECIPE_TYPE_ICON = new CustomItemStack(Material.BLAST_FURNACE, "Combustion Furnace", "A Combustion Furnace", "This block is a MultiBlock structure that can be used to create Combustion Recipes.", "For testing purposes only so far.").getBukkit();
    private
    final Map<ItemStack[], ItemStack> RECIPES = new HashMap<>();
    private
    final RecipeType RECIPE_TYPE = new RecipeType(getAddon(), getKey(), RECIPE_TYPE_ICON,
            RECIPES::put,
            (input, output) -> {
                RECIPES.remove(input);
            });

    public CokeOven(NamespacedKey key) {
        super(key);
        StructureBuilder sb = new StructureBuilder()
                .setPieces(StructureUtil.createCube(Material.BRICKS, 3))
                .setColumn(1, 0, 1, StructureUtil.material(Material.FURNACE))
                .setCenter(1, 0, 1);
        setStructure(sb.build());
    }

    @Override
    public void onInteract(@NotNull PlayerInteractEvent event) {
        // todo
        IRDock.getPlugin().getLogger().info("CokeOven interacted by " + event.getPlayer().getName());
    }
}
