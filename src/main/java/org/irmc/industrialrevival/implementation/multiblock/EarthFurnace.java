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
public class EarthFurnace extends MultiBlock {
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

    public EarthFurnace(NamespacedKey key) {
        super(key);
        Material mud = Material.PACKED_MUD;
        Material bricks = Material.MUD_BRICKS;
        Material slab = Material.MUD_BRICK_SLAB;
        Material fire = Material.FIRE;
        Material smoker = Material.SMOKER;
        StructureBuilder sb = new StructureBuilder()
                .setPieces(
                        StructureUtil.createStructure(new Material[][][]{
                                {
                                        {mud, mud, mud},
                                        {bricks, fire, bricks}
                                },
                                {
                                        {mud, mud, mud},
                                        {slab, smoker, slab}
                                }
                        })
                )
                .setCenter(1, 1, 1);
        setStructure(sb.build());
    }

    @Override
    public void onInteract(@NotNull PlayerInteractEvent event) {
        // todo
        IRDock.getPlugin().getLogger().info("EarthFurnace interacted by " + event.getPlayer().getName());
    }
}
