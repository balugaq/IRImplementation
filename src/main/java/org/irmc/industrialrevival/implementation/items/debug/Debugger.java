package org.irmc.industrialrevival.implementation.items.debug;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.irmc.industrialrevival.api.data.runtime.IRBlockData;
import org.irmc.industrialrevival.api.events.vanilla.IRBlockPlaceEvent;
import org.irmc.industrialrevival.api.events.vanilla.IRItemInteractEvent;
import org.irmc.industrialrevival.api.items.IndustrialRevivalItem;
import org.irmc.industrialrevival.api.items.handlers.BlockTicker;
import org.irmc.industrialrevival.api.items.handlers.ItemInteractHandler;
import org.irmc.industrialrevival.api.objects.ChunkPosition;
import org.irmc.industrialrevival.api.timings.PerformanceSummary;
import org.irmc.industrialrevival.core.services.IIRRegistry;
import org.irmc.industrialrevival.dock.IRDock;
import org.irmc.industrialrevival.implementation.IndustrialRevival;
import org.irmc.industrialrevival.implementation.items.IndustrialRevivalItems;
import org.irmc.industrialrevival.implementation.services.IRRegistry;
import org.irmc.industrialrevival.utils.DataUtil;
import org.irmc.industrialrevival.utils.NumberUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This item is used for debugging purposes.
 * Usage:
 * - Left-click on a block to break it normally.
 * - Right-click on a non-IR block to see its block state.
 * - Right-click on an IR block to see its properties.
 * - Right-click on air to see the chunk's timings.
 * - Shift+Left-click on a non-IR block to break it forcefully.
 * - Shift+Left-click on an IR block to remove its IR block data.
 * - Shift+Left-click on air to see Industrial Revival's status.
 * - Shift+Right-click on a block to place a Debug Head.
 * - Shift+Right-click on air to see the server's status.
 */
@SuppressWarnings("deprecation")
public class Debugger extends IndustrialRevivalItem {
    private static final String DEBUG_INFO_HEAD = "&e&l[IndustrialRevival Debugger]";
    private static final ChatColor red = ChatColor.RED;
    private static final ChatColor green = ChatColor.GREEN;
    private static final ChatColor yellow = ChatColor.YELLOW;
    private static final ChatColor blue = ChatColor.BLUE;
    private static final ChatColor white = ChatColor.WHITE;
    private static final ChatColor gray = ChatColor.GRAY;
    private static final ChatColor darkGray = ChatColor.DARK_GRAY;
    private static final ChatColor black = ChatColor.BLACK;

    public Debugger() {
        super();
        addItemHandlers((ItemInteractHandler) this::interact);
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static void send(Player player, String s) {
        player.sendMessage(color(s));
    }

    private static String booleanToSymbol(boolean b) {
        return (b ? green + "✔" : red + "✘") + white;
    }

    private void interact(IRItemInteractEvent event) {
        PlayerInteractEvent e = event.getOriginalEvent();
        e.setCancelled(true);

        Player player = e.getPlayer();
        if (!player.isOp()) {
            send(player, "&cYou do not have permission to use this item.");
        }

        send(player, DEBUG_INFO_HEAD);

        boolean isShift = player.isSneaking();
        boolean isRightClick = e.getAction().isRightClick();
        boolean isLeftClick = e.getAction().isLeftClick();
        boolean clickedNormalBlock = false;
        boolean clickedIRBlock = false;
        boolean clickedAir = false;
        Block block = e.getClickedBlock();
        if (block == null) {
            clickedAir = true;
        } else {
            Location location = block.getLocation();
            IRBlockData blockData = DataUtil.getBlockData(location);
            if (blockData != null) {
                clickedIRBlock = true;
            } else {
                clickedNormalBlock = true;
            }
        }

        if (isLeftClick && !isRightClick && !isShift) {
            breakBlock(e);
        }

        if (isRightClick && !isLeftClick && !isShift) {
            if (clickedNormalBlock) {
                seeBlockState(e);
            }
            if (clickedIRBlock) {
                seeIRBlockData(e);
            }
            if (clickedAir) {
                seeChunkTimings(e);
            }
        }

        if (isLeftClick && isShift && !isRightClick) {
            if (clickedNormalBlock) {
                forceBreakBlock(e);
            }
            if (clickedIRBlock) {
                removeIRBlockData(e);
            }
            if (clickedAir) {
                seeIRStatus(e);
            }
        }

        if (isRightClick && isShift && !isLeftClick) {
            if (clickedNormalBlock) {
                placeDebugHead(e);
            }
            if (clickedAir) {
                seeServerStatus(e);
            }
        }
    }

    private void breakBlock(PlayerInteractEvent e) {
        e.setCancelled(false);
    }

    private void seeBlockState(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        send(player, "&eChecking block state: ");
        if (block == null) {
            send(player, "&eNo block was clicked.");
            return;
        }
        send(player, "&e - Location: &a" + simpleLocationToString(block.getLocation()));
        send(player, "&e - Type: &a" + block.getType());
        send(player, "&e - Biome: &a" + block.getBiome());
        send(player, "&e - Redstone Power: &a" + block.getBlockPower());
        send(player, "&e - Light level: &a" + block.getLightLevel());
        send(player, "&e - Light from sky: &a" + block.getLightFromSky());
        send(player, "&e - Humidity: &a" + block.getHumidity());
        send(player, "&e - Temperature: &a" + block.getTemperature());
        send(player, "&e - Chunk x: &a" + block.getChunk().getX());
        send(player, "&e - Chunk z: &a" + block.getChunk().getZ());
    }

    private void seeIRBlockData(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        send(player, "&eChecking IR block data: ");
        if (block == null) {
            send(player, "&cNo block was clicked.");
            return;
        }

        IRBlockData data = DataUtil.getBlockData(block.getLocation());
        if (data == null) {
            send(player, "&cThis block has no IR block data.");
            return;
        }
        Location location = data.getLocation();

        send(player, "&e - Location: " + simpleLocationToString(location));
        send(player, "&e - ID: " + data.getId());

        IndustrialRevivalItem iritem = IndustrialRevivalItem.getById(data.getId());

        boolean hasTicker;
        boolean ticking;

        BlockTicker ticker = iritem.getItemHandler(BlockTicker.class);
        if (ticker == null) {
            hasTicker = false;
        } else {
            hasTicker = true;
        }

        IRBlockData blockData = IRDock.getRunningProfilerService().getTask().getTickingBlocks().get(location);
        ticking = blockData != null;

        send(player, "&e - Ticker: " + booleanToSymbol(hasTicker));
        send(player, "&e - Ticking: " + booleanToSymbol(ticking));
        if (hasTicker) {
            NamespacedKey id = data.getId();
            PerformanceSummary summary = IRDock.getRunningProfilerService().getSummary();
            long timingsOfThisBlock = summary.getDataByLocation().getOrDefault(location, 0L);
            long totalTimingsOfThisBlock = summary.getDataByID().getOrDefault(id, 0L);
            send(player, "&e- Timings: ");
            send(player, "&e  - This Timings: &7" + NumberUtil.round(NumberUtil.ns2Ms(timingsOfThisBlock), 2) + "ms");
            if (totalTimingsOfThisBlock > 0 && !summary.getDataByID().isEmpty()) {
                long avgTimingsOfThisBlock = totalTimingsOfThisBlock / summary.getDataByID().size();
                send(player, "&e  - Average Timings: &7" + NumberUtil.round(NumberUtil.ns2Ms(avgTimingsOfThisBlock), 2) + "ms");
            }
            send(player, "&e  - Total Timings: &7" + NumberUtil.round(NumberUtil.ns2Ms(totalTimingsOfThisBlock), 2) + "ms");
        }

        Map<String, String> dataMap = data.getMapData();
        if (!dataMap.isEmpty()) {
            send(player, " - Data: [");
            for (String key : dataMap.keySet()) {
                send(player, "   - " + key + ": " + dataMap.get(key));
            }
            send(player, " ]");
        }
    }

    private void seeChunkTimings(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        send(player, "&eChecking chunk timings: ");
        Chunk chunk = player.getChunk();
        ChunkPosition position = new ChunkPosition(chunk);
        PerformanceSummary summary = IRDock.getRunningProfilerService().getSummary();

        Long chunkTimings = summary.getDataByChunk().get(position);
        if (chunkTimings == null) {
            chunkTimings = 0L;
        }
        int machineCount = summary.getDataByLocation().keySet().stream().filter(location -> location.getChunk().equals(chunk)).toList().size();
        long avgTimingsPerMachine;
        if (machineCount == 0) {
            avgTimingsPerMachine = 0L;
        } else {
            avgTimingsPerMachine = chunkTimings / machineCount;
        }

        long avgTimingsPerChunk = summary.getTotalTime();
        send(player, "&e- Timings: ");
        send(player, "&e  - Total Chunk Timings: &7" + NumberUtil.round(NumberUtil.ns2Ms(chunkTimings), 2) + "ms");
        send(player, "&e  - Average Timings Per Machine in This Chunk: &7" + NumberUtil.round(NumberUtil.ns2Ms(avgTimingsPerMachine), 2) + "ms");
        send(player, "&e  - Average Timings Per Chunk: &7" + NumberUtil.round(NumberUtil.ns2Ms(avgTimingsPerChunk), 2) + "ms");
    }

    private void forceBreakBlock(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        send(player, "&eForce-breaking block: ");
        if (block == null) {
            send(player, "&cNo block was clicked.");
            return;
        }
        send(player, "&e - Location: " + simpleLocationToString(block.getLocation()));
        send(player, "&e - Type: " + block.getType());
        IRBlockData data = DataUtil.getBlockData(block.getLocation());
        if (data != null) {
            send(player, "&cCannot force-break an IR block before removing its data.");
            return;
        }
        block.setType(Material.AIR);
        send(player, "&eBlock force-broken.");
    }

    private void removeIRBlockData(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        send(player, "&eRemoving IR block data: ");
        if (block == null) {
            send(player, "&cNo block was clicked.");
            return;
        }
        IRBlockData data = DataUtil.getBlockData(block.getLocation());
        if (data == null) {
            send(player, "&cThis block has no IR block data.");
            return;
        }

        Location location = data.getLocation();
        send(player, "&e - Location: " + simpleLocationToString(location));
        send(player, "&e - ID: " + data.getId());

        DataUtil.removeBlockData(location);
        send(player, "&aIR block data removed.");
    }

    private void seeIRStatus(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        send(player, "&eChecking Industrial Revival status: ");

        PluginMeta pluginMeta = IRDock.getPlugin().getPluginMeta();
        send(player, "&e - Enabled: &7" + booleanToSymbol(IRDock.isEnabled()));
        send(player, "&e - Name: &7" + IRDock.getPlugin().getName());
        send(player, "&e - Version: &7" + pluginMeta.getVersion());
        send(player, "&e - Authors: &7" + pluginMeta.getAuthors());
        send(player, "&e - Description: &7" + pluginMeta.getDescription());
        send(player, "&e - Website: &7" + pluginMeta.getWebsite());
        send(player, "&e - API version: &7" + pluginMeta.getAPIVersion());
        send(player, "&e - Issue tracker: &7" + IRDock.getIssueTrackerURL());
        send(player, "&e - Installed addons: &7" + IRDock.getAddons().size());

        IIRRegistry registry = IRDock.getRegistry();
        send(player, "&e - Loaded items: &7" + registry.getItems().size());
        send(player, "&e - Loaded item groups: &7" + registry.getItemGroups().size());
        send(player, "&e - Loaded recipe types: &7" + registry.getAllRecipeTypes().size());
        send(player, "&e - Loaded menu presets: &7" + registry.getMenuPresets().size());
        send(player, "&e - Loaded player profiles: &7" + IRDock.getDataManager().getPlayerProfiles().size());
        //send(player, "&e - Loaded researches: &7" + registry.getResearches().size());
        AtomicInteger recipes = new AtomicInteger();
        registry.getAllProduceMethods().forEach(_ -> recipes.addAndGet(1));
        send(player, "&e - Loaded recipes: &7" + recipes.get());
        AtomicInteger mobDrops = new AtomicInteger();
        registry.getMobDrops().forEach((_, drops) -> {
            mobDrops.addAndGet(drops.size());
        });
        send(player, "&e - Loaded mob drops: &7" + mobDrops.get());
        AtomicInteger blockDrops = new AtomicInteger();
        registry.getBlockDrops().forEach((_, drops) -> {
            blockDrops.addAndGet(drops.size());
        });
        send(player, "&e - Loaded block drops: &7" + blockDrops.get());
        send(player, "&e - Loaded listeners: &7" + IRDock.getListenerManager().getListeners().size());
    }

    private void placeDebugHead(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        send(player, "&ePlacing Debug Head: ");
        Block block = e.getClickedBlock();
        if (block == null) {
            send(player, "&cNo block was clicked.");
            return;
        }
        Location location = block.getRelative(e.getBlockFace()).getLocation();
        if (location.getBlock().getType() != Material.AIR) {
            send(player, "&cCannot place Debug Head");
            return;
        }
        send(player, "&e - Location: &7" + simpleLocationToString(location));
        IRBlockPlaceEvent event = new IRBlockPlaceEvent(new BlockPlaceEvent(location.getBlock(), location.getBlock().getState(), block, IndustrialRevivalItems.DEBUG_HEAD.clone(), player, true, EquipmentSlot.HAND), IndustrialRevivalItem.getByItem(IndustrialRevivalItems.DEBUG_HEAD));
        Bukkit.getPluginManager().callEvent(event);

        send(player, "&aDebug Head placed.");
    }

    private void seeServerStatus(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Server server = Bukkit.getServer();
        send(player, "&eChecking server status: ");
        send(player, "&e - Server software: &7" + Bukkit.getName());
        send(player, "&e - Name: &7" + server.getName());
        send(player, "&e - Server Version: &7" + server.getVersion());
        send(player, "&e - Bukkit version: &7" + server.getBukkitVersion());
        send(player, "&e - Minecraft version: &7" + server.getMinecraftVersion());
        send(player, "&e - Plugins: &7" + server.getPluginManager().getPlugins().length);
        send(player, "&e - TPS: &b" + Arrays.toString(Arrays.stream(server.getTPS()).map(number -> NumberUtil.round(number, 2)).toArray()));
        send(player, "&e - Average tick time: &7" + NumberUtil.round(server.getAverageTickTime(), 2));
        send(player, "&e - Online players: &7" + server.getOnlinePlayers().size());
        send(player, "&e - Max players: &7" + server.getMaxPlayers());
        send(player, "&e - Worlds: &7" + server.getWorlds().size());
    }

    private String simpleLocationToString(Location location) {
        return "Location{world=" + location.getWorld().getName() + ", x=" + location.getBlockX() + ", y=" + location.getBlockY() + ", z=" + location.getBlockZ() + "}";
    }
}
