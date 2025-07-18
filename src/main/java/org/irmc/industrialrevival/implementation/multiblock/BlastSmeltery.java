package org.irmc.industrialrevival.implementation.multiblock;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.intellij.lang.annotations.RegExp;
import org.irmc.industrialrevival.api.elements.Smeltery;
import org.irmc.industrialrevival.api.elements.melt.MeltedObject;
import org.irmc.industrialrevival.api.elements.tinker.TinkerType;
import org.irmc.industrialrevival.api.items.IndustrialRevivalItem;
import org.irmc.industrialrevival.api.items.attributes.ExtraTickable;
import org.irmc.industrialrevival.api.items.attributes.Meltable;
import org.irmc.industrialrevival.api.items.attributes.TinkerModel;
import org.irmc.industrialrevival.api.items.attributes.TinkerProduct;
import org.irmc.industrialrevival.api.menu.MachineMenu;
import org.irmc.industrialrevival.api.menu.MachineMenuPreset;
import org.irmc.industrialrevival.api.menu.MatrixMenuDrawer;
import org.irmc.industrialrevival.api.menu.PublicBehaviors;
import org.irmc.industrialrevival.api.menu.handlers.ClickHandler;
import org.irmc.industrialrevival.api.multiblock.MultiBlock;
import org.irmc.industrialrevival.api.multiblock.StructureBuilder;
import org.irmc.industrialrevival.api.multiblock.StructureUtil;
import org.irmc.industrialrevival.dock.IRDock;
import org.irmc.pigeonlib.items.CustomItemStack;
import org.irmc.industrialrevival.api.recipes.RecipeType;
import org.irmc.industrialrevival.core.listeners.MultiblockTicker;
import org.irmc.industrialrevival.implementation.IndustrialRevival;
import org.irmc.industrialrevival.utils.Debug;
import org.irmc.industrialrevival.utils.KeyUtil;
import org.irmc.industrialrevival.utils.MenuUtil;
import org.irmc.pigeonlib.items.ItemUtils;
import org.irmc.pigeonlib.pdc.PersistentDataAPI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class BlastSmeltery extends MultiBlock implements ExtraTickable {
    public static final TextColor MELTING_TEXT_COLOR = TextColor.color(16746003);
    public static final TextColor FUEL_TEXT_COLOR = TextColor.color(16734003);
    private static final TextComponent BLOCKS = Component.text("\u25a0\u25a0\u25a0\u25a0\u25a0");
    private static final ItemStack STORAGE_ICON = new CustomItemStack(Material.BARREL, "&fBlast Smeltery Storage", "", "&f0 / 0").getBukkit();
    private static final ItemStack FUEL_ICON = new CustomItemStack(Material.BUCKET, "&fBlast Smeltery Fuel", "", "&f0 / 0").getBukkit();
    private static final ItemStack MODEL_ICON = new CustomItemStack(Material.GOLD_BLOCK, "&fTinker Model", "", "").getBukkit();
    private static final ItemStack CRAFT_ICON = new CustomItemStack(Material.CRAFTING_TABLE, "&fTinker Crafting", "", "").getBukkit();
    private static final ItemStack PRODUCT_ICON = new CustomItemStack(Material.GOLDEN_PICKAXE, "&fTinker Product", "", "").getBukkit();
    private static final NamespacedKey MELTING_KEY = KeyUtil.customKey("melting");
    @RegExp
    private static final String MELTING_PREFIX = "Melting - ";
    // todo: save
    @Getter
    private static final Map<Location, MachineMenu> menus = new HashMap<>();
    // todo: save
    @Getter
    private static final Map<Location, Smeltery> instances = new HashMap<>();
    @Getter
    private static final Map<UUID, Location> lastInteracted = new HashMap<>();
    @Getter
    private static final MachineMenuPreset preset = new MachineMenuPreset(KeyUtil.customKey("blast_smeltery"), "Blast Smeltery");
    @Getter
    private static final MatrixMenuDrawer menuDrawer = new MatrixMenuDrawer(6 * 9)
            .addLine("iiiiIBBBL")
            .addLine("iiiiISSSL")
            .addLine("iiiiIMmML")
            .addLine("iiiiIBCBL")
            .addLine("iiiiIPpPL")
            .addLine("iiiiIBBBL")
            .addExplain("I", MenuUtil.INPUT_BORDER)
            .addExplain("B", MenuUtil.BACKGROUND)
            .addExplain("S", STORAGE_ICON)
            .addExplain("L", FUEL_ICON)
            .addExplain("M", MODEL_ICON)
            .addExplain("C", CRAFT_ICON, Behaviors.CRAFT_BEHAVIOR)
            .addExplain("P", PRODUCT_ICON)
            .addExplain("i", new ItemStack(Material.AIR), Behaviors.ADD_ITEM_BEHAVIOR);

    public static RecipeType RECIPE_TYPE = new RecipeType(
            IRDock.getPlugin(),
            KeyUtil.customKey("blast_smeltery"),
            new ItemStack(Material.BLAST_FURNACE)
    );

    static {
        preset.withMenuDrawer(menuDrawer);
    }

    public BlastSmeltery(NamespacedKey key) {
        super(key);
        StructureBuilder sb = new StructureBuilder()
                .setPieces(StructureUtil.createCube(Material.COBBLESTONE, 3))
                .setColumn(1, 0, 1, StructureUtil.material(Material.FURNACE))
                .setCenter(1, 0, 1);
        setStructure(sb.build());
    }

    //<editor-fold> desc="slot functions"
    public static int[] getInputSlots() {
        return menuDrawer.getCharPositions('i');
    }

    public static int[] getStorageSlots() {
        return menuDrawer.getCharPositions('S');
    }

    public static int[] getFuelSlots() {
        return menuDrawer.getCharPositions('L');
    }

    public static int getModelSlot() {
        return menuDrawer.getCharPositions('m')[0];
    }

    public static int getProductSlot() {
        return menuDrawer.getCharPositions('p')[0];
    }

    //<editor-fold> desc="melting functions"
    public static boolean isMeltable(@NotNull ItemStack input) {
        return IndustrialRevivalItem.getByItem(input) instanceof Meltable;
    }
    //</editor-fold>

    public static @NotNull ItemStack getMeltingStack(@NotNull ItemStack input) {
        return new CustomItemStack(input).editMeta(im -> {
            if (!PersistentDataAPI.has(im, MELTING_KEY, PersistentDataType.INTEGER)) {
                im.displayName(Component.text(MELTING_PREFIX, MELTING_TEXT_COLOR).append(im.displayName()));
                List<Component> lore = im.lore();
                if (lore == null) {
                    lore = new ArrayList<>();
                }
                lore.add(Component.text("Melting: ...", MELTING_TEXT_COLOR));
                im.lore(lore);
                PersistentDataAPI.set(im, MELTING_KEY, PersistentDataType.INTEGER, 0);
            }
        }).getBukkit();
    }

    public static boolean isMeltingStack(@NotNull ItemStack input) {
        return PersistentDataAPI.has(input.getItemMeta(), MELTING_KEY, PersistentDataType.INTEGER);
    }

    public static @NotNull ItemStack getOriginalStack(@NotNull ItemStack input) {
        if (isMeltingStack(input)) {
            return new CustomItemStack(input).editMeta(im -> {
                Component displayName = im.displayName();
                if (displayName != null) {
                    String plain = PlainTextComponentSerializer.plainText().serialize(displayName);
                    if (plain.startsWith(MELTING_PREFIX)) {
                        displayName = PlainTextComponentSerializer.plainText().deserialize(plain.substring(MELTING_PREFIX.length()));
                    }
                    im.displayName(displayName);
                }

                List<Component> lore = im.lore();
                if (lore != null) {
                    int i = 0;
                    for (Component component : lore) {
                        String plain = PlainTextComponentSerializer.plainText().serialize(component);
                        if (plain.startsWith("Melting: ")) {
                            lore.remove(i);
                            break;
                        }
                        lore.set(i, component);
                    }
                    im.lore(lore);
                }
                if (PersistentDataAPI.has(im, MELTING_KEY, PersistentDataType.INTEGER)) {
                    PersistentDataAPI.remove(im, MELTING_KEY);
                }
            }).getBukkit();
        }

        return input;
    }

    @CanIgnoreReturnValue
    public static ItemStack addMeltingLevel(ItemStack input, int max) {
        ItemMeta meta = input.getItemMeta();
        int current = PersistentDataAPI.getOrDefault(meta, MELTING_KEY, PersistentDataType.INTEGER, 0) + 1;
        PersistentDataAPI.set(meta, MELTING_KEY, PersistentDataType.INTEGER, current);
        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        if (lore.isEmpty()) {
            lore.add(Component.text("Melting: " + current + " / " + max, MELTING_TEXT_COLOR));
        } else {
            int index = 0;
            Component newOne = Component.text("Melting: " + current + " / " + max, FUEL_TEXT_COLOR);
            for (Component component : lore) {
                if (PlainTextComponentSerializer.plainText().serialize(component).startsWith("Melting: ")) {
                    lore.set(index, newOne);
                    break;
                }
                index += 1;
            }
        }
        meta.lore(lore);
        input.setItemMeta(meta);
        return input;
    }

    public static int getMeltingLevel(ItemStack input) {
        return PersistentDataAPI.getOrDefault(input.getItemMeta(), MELTING_KEY, PersistentDataType.INTEGER, 0);
    }

    public static void updateMenu(Location location) {
        Smeltery smeltery = instances.get(location);
        MachineMenu menu = menus.get(location);
        if (smeltery == null || menu == null) {
            return;
        }

        List<TextComponent> lines = new ArrayList<>();
        for (MeltedObject meltedObject : smeltery.getTank().getStored()) {
            TextComponent text = Component.text()
                    .append(BLOCKS)
                    .append(meltedObject.getType().getMeltedName())
                    .append(Component.text(" / " + meltedObject.getAmount()))
                    .color(meltedObject.getType().getColor())
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true)
                    .build();
            lines.add(text);
        }

        ItemStack clone = STORAGE_ICON.clone();
        ItemMeta meta = clone.getItemMeta();
        meta.lore(lines);
        clone.setItemMeta(meta);

        for (int slot : getStorageSlots()) {
            menu.setItem(slot, clone.clone());
        }

        int fuels = smeltery.getTank().getFuels();
        int capacity = smeltery.getTank().getMaxFuel();
        int split = capacity / getFuelSlots().length;
        int lavas = (int) (((float) (fuels - 1) / split));
        if (lavas < 0) {
            lavas = 0;
        }
        if (lavas > getFuelSlots().length) {
            lavas = getFuelSlots().length;
        }
        List<TextComponent> lore = new ArrayList<>();
        lore.add(Component.text("Fuel: " + fuels + " / " + capacity, FUEL_TEXT_COLOR));

        ItemStack clone2 = FUEL_ICON.clone();
        ItemMeta meta2 = clone2.getItemMeta();
        meta2.lore(lore);
        clone2.setItemMeta(meta2);
        for (int slot : getFuelSlots()) {
            menu.setItem(slot, clone2.clone());
        }

        int j = 1;
        for (int slot : Arrays.stream(getFuelSlots()).boxed().toList().reversed()) {
            if (j > lavas) {
                menu.getItem(slot).setType(Material.BUCKET);
            } else {
                menu.getItem(slot).setType(Material.LAVA_BUCKET);
            }
            j += 1;
        }
    }
    //</editor-fold

    @Override
    public void onInteract(@NotNull PlayerInteractEvent event) {
        Debug.log(event.getAction());
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Location location = event.getClickedBlock().getLocation();
            if (!instances.containsKey(location)) {
                instances.put(location, new Smeltery());
                MachineMenu menu = new MachineMenu(location, preset);
                menu.addMenuDrawer(menuDrawer);
                menus.put(location, menu);
            }

            Player player = event.getPlayer();
            ItemStack itemStack = event.getItem();
            if (itemStack != null && Smeltery.isFuel(itemStack)) {
                Smeltery smeltery = instances.get(location);
                int add = Smeltery.getFuelAmount(itemStack);
                if (smeltery.getTank().getFuels() + add <= smeltery.getTank().getMaxFuel()) {
                    smeltery.getTank().addFuel(add);
                    return;
                }
            }

            MachineMenu menu = menus.get(location);
            lastInteracted.put(player.getUniqueId(), location);
            MultiblockTicker.addTickable(location, this);
            updateMenu(location);
            menu.open(player);
        }
    }

    //<editor-fold> desc="ticker"
    @Override
    public When getTime() {
        return When.TICK_DONE;
    }

    public void tick(Location location) {
        Smeltery smeltery = instances.get(location);
        MachineMenu menu = menus.get(location);
        if (smeltery == null || menu == null) {
            return;
        }
        tick(location, menu, smeltery);
    }

    public void tick(Location location, MachineMenu menu, Smeltery smeltery) {
        smeltery.tick();
        if (menu.hasViewer()) {
            Debug.log("101 BlastSmeltery tick hasViewer");
            updateMenu(location);
        }
        int fuels = smeltery.getTank().getFuels();
        for (int slot : getInputSlots()) {
            ItemStack input = menu.getItem(slot);
            if (input == null || input.getType() == Material.AIR) {
                continue;
            }
            if (isMeltingStack(input)) {
                if (IndustrialRevivalItem.getByItem(input) instanceof Meltable meltable) {
                    int fuelUse = meltable.getFuelUse(input);
                    if (fuelUse <= getMeltingLevel(input)) {
                        smeltery.getTank().addMelted(new MeltedObject(meltable.getMeltedType(input), meltable.getTinkerType(input).getLevel()));
                        menu.setItem(slot, new ItemStack(Material.AIR));
                        continue;
                    } else if (meltable.getMeltingPoint(input) > fuels) {
                        continue;
                    }

                    if (fuels > 0) {
                        addMeltingLevel(input, fuelUse);
                        fuels -= 1;
                    }
                }
            }
        }

        smeltery.getTank().setFuels(fuels);
        if (smeltery.getTank().isDirty()) {
            smeltery.getTank().renderAt(location);
        }
    }
    //</editor-fold>

    //<editor-fold> desc="Behaviors"
    public static class Behaviors {
        public static final ClickHandler CRAFT_BEHAVIOR = (player, _, _, clickedMenu, _) -> {
            Location location = lastInteracted.get(player.getUniqueId());
            if (location == null) {
                return false;
            }

            Smeltery smeltery = instances.get(location);
            if (smeltery == null) {
                return false;
            }

            ItemStack modelStack = clickedMenu.getItem(getModelSlot());
            if (modelStack != null && modelStack.getType() != Material.AIR && IndustrialRevivalItem.getByItem(modelStack) instanceof TinkerModel tinkerModel) {
                TinkerType tinkerType = tinkerModel.getTinkerType();

                ItemStack existingProduct = clickedMenu.getItem(getProductSlot());
                if (existingProduct != null && existingProduct.getType() != Material.AIR) {
                    player.sendMessage(Component.text("Already contains a tinker product."));
                    return false;
                }

                MeltedObject bottom = smeltery.getTank().getBottomObject();
                if (bottom == null || tinkerType.getLevel() > bottom.getAmount()) {
                    player.sendMessage(Component.text("Not enough bottom material for this tinker type."));
                    return false;
                }

                TinkerProduct product = IRDock.getRegistry().getTinkerProduct(bottom.getType(), tinkerType);
                if (product == null) {
                    player.sendMessage(Component.text(bottom.getType().getName() + " cannot be tinkered with " + tinkerType.name() + "."));
                    return false;
                }

                ItemStack productStack = product.getProduct().clone();
                clickedMenu.setItem(getProductSlot(), productStack);
                player.sendMessage(Component.text("Tinkered " + ItemUtils.getDisplayName(productStack)));
            }

            return false;
        };

        public static final ClickHandler ADD_ITEM_BEHAVIOR = ((player, clickedItem, clickedSlot, clickedMenu, _) -> {
            ItemStack cursor = player.getItemOnCursor();
            if (cursor == null || cursor.getType() == Material.AIR) {
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    // Nothing to do
                    return false;
                } else if (isMeltingStack(clickedItem)) {
                    clickedMenu.setItem(clickedSlot, new ItemStack(Material.AIR));
                    player.setItemOnCursor(getOriginalStack(clickedItem));
                } else {
                    return true;
                }
            } else {
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    if (isMeltable(cursor)) {
                        // add melting
                        ItemStack clone = cursor.asOne();
                        clickedMenu.setItem(clickedSlot, getMeltingStack(clone));
                        cursor.setAmount(cursor.getAmount() - 1);
                    } else {
                        return false;
                    }
                } else {
                    if (cursor.getAmount() == 1) {
                        if (isMeltable(cursor)) {
                            // exchange items
                            clickedMenu.setItem(clickedSlot, getMeltingStack(cursor));
                            player.setItemOnCursor(clickedItem);
                            return false;
                        }
                    }
                }
            }
            Debug.log("57 BlastSmeltery onInteract end");
            return false;
        });

        static {
            PublicBehaviors.publish(KeyUtil.customKey("blast_smeltery_craft"), CRAFT_BEHAVIOR);
            PublicBehaviors.publish(KeyUtil.customKey("blast_smeltery_add_item"), ADD_ITEM_BEHAVIOR);
        }
    }
    //</editor-fold>
}
