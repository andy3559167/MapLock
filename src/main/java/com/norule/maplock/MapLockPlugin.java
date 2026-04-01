package com.norule.maplock;

import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public final class MapLockPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final String INTERNAL_PREFIX = "MapLock:";
    private static final String INTERNAL_LOCKED = INTERNAL_PREFIX + "locked";
    private static final String INTERNAL_OWNER = INTERNAL_PREFIX + "owner:";
    private static final String INTERNAL_TIME = INTERNAL_PREFIX + "time:";

    private FileConfiguration langConfig;
    private Economy econ;
    private PersistentDataBridge persistentDataBridge;

    @Override
    public void onEnable() {
        persistentDataBridge = PersistentDataBridge.create(this);

        loadConfiguration();

        if (getConfig().getBoolean("economy.enabled")) {
            setupEconomy();
        }

        getServer().getPluginManager().registerEvents(this, this);
        registerCartographyCompatibilityListener();

        if (getCommand("map") != null) {
            getCommand("map").setExecutor(this);
            getCommand("map").setTabCompleter(this);
        }

        new Metrics(this, 30105);
        getLogger().info("MapLock v1.2 enabled with legacy compatibility mode.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        econ = rsp.getProvider();
        return econ != null;
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        reloadConfig();

        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String[] languages = {"zh_tw.yml", "zh_cn.yml", "en.yml"};
        for (String file : languages) {
            if (!new File(langFolder, file).exists()) {
                saveResource("lang/" + file, false);
            }
        }

        String lang = getConfig().getString("language", "zh_tw");
        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) {
            langFile = new File(langFolder, "zh_tw.yml");
        }

        langConfig = loadYamlUtf8(langFile);
    }

    private FileConfiguration loadYamlUtf8(File file) {
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            getLogger().warning("Failed to read " + file.getName() + " as UTF-8, falling back to default loader.");
            return YamlConfiguration.loadConfiguration(file);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // Ignore close failure.
                }
            }
        }
    }

    private String getMsg(String path) {
        String prefix = langConfig.getString("prefix", "&7[&bMapLock&7] ");
        String msg = langConfig.getString(path, "Missing message: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    private String getRawMsg(String path) {
        String msg = langConfig.getString(path, "Missing");
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            player.sendMessage(getRawMsg("help_header"));
            player.sendMessage(getRawMsg("help_help"));
            if (player.hasPermission("maplock.lock")) {
                player.sendMessage(getRawMsg("help_lock"));
            }
            if (player.hasPermission("maplock.unlock")) {
                player.sendMessage(getRawMsg("help_unlock"));
            }
            if (player.hasPermission("maplock.admin.info")) {
                player.sendMessage(getRawMsg("help_admin_info"));
            }
            if (player.hasPermission("maplock.admin.forceunlock")) {
                player.sendMessage(getRawMsg("help_admin_forceunlock"));
            }
            if (player.hasPermission("maplock.admin.reload")) {
                player.sendMessage(getRawMsg("help_admin_reload"));
            }
            if (player.hasPermission("maplock.admin.bypass")) {
                player.sendMessage(getRawMsg("help_admin_bypass"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        ItemStack item = getHeldItem(player);

        if ("admin".equals(subCommand)) {
            return handleAdminCommand(player, args, item);
        }

        List<String> disabledWorlds = getConfig().getStringList("disabled-worlds");
        if (disabledWorlds.contains(player.getWorld().getName()) && !player.hasPermission("maplock.admin.bypass")) {
            if ("lock".equals(subCommand) || "unlock".equals(subCommand)) {
                player.sendMessage(getMsg("world_disabled"));
                return true;
            }
        }

        if (!isSupportedMapItem(item) || item.getItemMeta() == null) {
            player.sendMessage(getMsg("must_hold_map"));
            return true;
        }

        if ("lock".equals(subCommand) && player.hasPermission("maplock.lock")) {
            return handleLockCommand(player, item);
        }

        if ("unlock".equals(subCommand) && player.hasPermission("maplock.unlock")) {
            return handleUnlockCommand(player, item);
        }

        return true;
    }

    private boolean handleAdminCommand(Player player, String[] args, ItemStack item) {
        if (args.length < 2) {
            player.sendMessage(getMsg("admin_usage"));
            return true;
        }

        String adminSub = args[1].toLowerCase();

        if ("reload".equals(adminSub) && player.hasPermission("maplock.admin.reload")) {
            loadConfiguration();
            if (getConfig().getBoolean("economy.enabled")) {
                setupEconomy();
            }
            player.sendMessage(getMsg("reload_success"));
            return true;
        }

        if ("info".equals(adminSub) && player.hasPermission("maplock.admin.info")) {
            if (!isSupportedMapItem(item)) {
                player.sendMessage(getMsg("must_hold_map"));
                return true;
            }

            LockData lockData = getLockData(item);
            if (lockData == null) {
                player.sendMessage(getMsg("admin_info_none"));
                return true;
            }

            String uuidStr = lockData.ownerUuid;
            Long time = lockData.lockTime;
            String name = "Unknown";

            if (uuidStr != null) {
                try {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                    if (offlinePlayer.getName() != null) {
                        name = offlinePlayer.getName();
                    }
                } catch (IllegalArgumentException ignored) {
                    name = uuidStr;
                }
            }

            String date = time != null
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time.longValue()))
                : "Unknown";

            player.sendMessage(getRawMsg("admin_info_header"));
            player.sendMessage(getRawMsg("admin_info_name").replace("%name%", name));
            player.sendMessage(getRawMsg("admin_info_uuid").replace("%uuid%", uuidStr != null ? uuidStr : "None"));
            player.sendMessage(getRawMsg("admin_info_date").replace("%date%", date));
            return true;
        }

        if ("forceunlock".equals(adminSub) && player.hasPermission("maplock.admin.forceunlock")) {
            if (!isSupportedMapItem(item)) {
                player.sendMessage(getMsg("must_hold_map"));
                return true;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null || !hasLockData(item)) {
                player.sendMessage(getMsg("not_locked"));
                return true;
            }

            clearLockData(meta);
            item.setItemMeta(meta);
            player.sendMessage(getMsg("force_unlock_success"));
            return true;
        }

        return true;
    }

    private boolean handleLockCommand(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(getMsg("must_hold_map"));
            return true;
        }

        if (hasLockData(item)) {
            player.sendMessage(getMsg("already_locked"));
            return true;
        }

        if (!player.hasPermission("maplock.admin.bypass")) {
            if (getConfig().getBoolean("economy.enabled")) {
                if (econ == null) {
                    player.sendMessage(getMsg("economy_error"));
                    return true;
                }

                double cost = getConfig().getDouble("economy.cost", 0.0D);
                if (cost > 0.0D && econ.getBalance(player) < cost) {
                    player.sendMessage(getMsg("not_enough_money").replace("%money%", String.valueOf(cost)));
                    return true;
                }

                if (cost > 0.0D) {
                    econ.withdrawPlayer(player, cost);
                }
            }

            if (getConfig().getBoolean("item-cost.enabled")) {
                Material material = Material.getMaterial(getConfig().getString("item-cost.material", "DIAMOND").toUpperCase());
                int amount = getConfig().getInt("item-cost.amount", 1);
                if (material != null && amount > 0 && !checkAndConsumeItem(player, material, amount)) {
                    player.sendMessage(getMsg("not_enough_item")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%item%", getConfig().getString("item-cost.display-name", "Item")));
                    return true;
                }
            }
        }

        writeLockData(meta, player.getUniqueId().toString(), Long.valueOf(System.currentTimeMillis()), player.getName());
        item.setItemMeta(meta);
        player.sendMessage(getMsg("lock_success"));
        return true;
    }

    private boolean handleUnlockCommand(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(getMsg("must_hold_map"));
            return true;
        }

        LockData lockData = getLockData(item);
        if (lockData == null) {
            player.sendMessage(getMsg("not_locked"));
            return true;
        }

        if (lockData.ownerUuid != null && !lockData.ownerUuid.equals(player.getUniqueId().toString())) {
            player.sendMessage(getMsg("not_owner"));
            return true;
        }

        clearLockData(meta);
        item.setItemMeta(meta);
        player.sendMessage(getMsg("unlock_success"));
        return true;
    }

    private boolean checkAndConsumeItem(Player player, Material material, int amount) {
        ItemStack required = new ItemStack(material, amount);
        if (!player.getInventory().containsAtLeast(new ItemStack(material), amount)) {
            return false;
        }
        player.getInventory().removeItem(required);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();
        List<String> options = new ArrayList<String>();

        if (args.length == 1) {
            options.add("help");
            if (sender.hasPermission("maplock.lock")) {
                options.add("lock");
            }
            if (sender.hasPermission("maplock.unlock")) {
                options.add("unlock");
            }
            if (sender.hasPermission("maplock.admin.info") || sender.hasPermission("maplock.admin.reload")) {
                options.add("admin");
            }
            StringUtil.copyPartialMatches(args[0], options, completions);
        } else if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("maplock.admin.info")) {
                options.add("info");
            }
            if (sender.hasPermission("maplock.admin.forceunlock")) {
                options.add("forceunlock");
            }
            if (sender.hasPermission("maplock.admin.reload")) {
                options.add("reload");
            }
            StringUtil.copyPartialMatches(args[1], options, completions);
        }

        Collections.sort(completions);
        return completions;
    }

    @EventHandler
    public void onMapCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (hasLockData(item)) {
                event.getInventory().setResult(null);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCartographyClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!isCartographyInventory(topInventory)) {
            return;
        }

        if (event.getRawSlot() != 2) {
            return;
        }

        if (!hasLockData(topInventory.getItem(0))) {
            return;
        }

        event.setCancelled(true);
        if (topInventory.getSize() > 2) {
            topInventory.setItem(2, null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCartographyDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!isCartographyInventory(topInventory)) {
            return;
        }

        if (!event.getRawSlots().contains(Integer.valueOf(2))) {
            return;
        }

        if (!hasLockData(topInventory.getItem(0))) {
            return;
        }

        event.setCancelled(true);
        if (topInventory.getSize() > 2) {
            topInventory.setItem(2, null);
        }
    }

    private void registerCartographyCompatibilityListener() {
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends Event> eventClass =
                (Class<? extends Event>) Class.forName("org.bukkit.event.inventory.PrepareInventoryResultEvent");
            final Method getInventoryMethod = eventClass.getMethod("getInventory");
            Method discoveredSetResultMethod = null;
            try {
                discoveredSetResultMethod = eventClass.getMethod("setResult", ItemStack.class);
            } catch (NoSuchMethodException ignored) {
                // Some server implementations may not expose setResult on this event.
            }
            final Method setResultMethod = discoveredSetResultMethod;

            getServer().getPluginManager().registerEvent(
                eventClass,
                this,
                EventPriority.NORMAL,
                new EventExecutor() {
                    @Override
                    public void execute(Listener listener, Event event) {
                        try {
                            Object inventoryObject = getInventoryMethod.invoke(event);
                            if (!(inventoryObject instanceof Inventory)) {
                                return;
                            }

                            Inventory inventory = (Inventory) inventoryObject;
                            if (!"CARTOGRAPHY".equals(inventory.getType().name())) {
                                return;
                            }

                            ItemStack source = inventory.getItem(0);
                            if (hasLockData(source)) {
                                if (inventory.getSize() > 2) {
                                    inventory.setItem(2, null);
                                }
                                if (setResultMethod != null) {
                                    setResultMethod.invoke(event, new Object[]{null});
                                }
                            }
                        } catch (Throwable throwable) {
                            getLogger().fine("Cartography compatibility hook skipped: " + throwable.getMessage());
                        }
                    }
                },
                this,
                true
            );
        } catch (ClassNotFoundException ignored) {
            // Cartography tables do not exist on legacy servers.
        } catch (Throwable throwable) {
            getLogger().warning("Failed to register cartography compatibility listener: " + throwable.getMessage());
        }
    }

    private ItemStack getHeldItem(Player player) {
        try {
            Method modernMethod = player.getInventory().getClass().getMethod("getItemInMainHand");
            Object result = modernMethod.invoke(player.getInventory());
            if (result instanceof ItemStack) {
                return (ItemStack) result;
            }
        } catch (Throwable ignored) {
            // Fall back to legacy API below.
        }

        return player.getItemInHand();
    }

    private boolean isCartographyInventory(Inventory inventory) {
        return inventory != null && "CARTOGRAPHY".equals(inventory.getType().name());
    }

    private boolean isSupportedMapItem(ItemStack item) {
        if (item == null || item.getType() == null) {
            return false;
        }

        String materialName = item.getType().name();
        return "MAP".equals(materialName) || "FILLED_MAP".equals(materialName) || "EMPTY_MAP".equals(materialName);
    }

    private boolean hasLockData(ItemStack item) {
        return getLockData(item) != null;
    }

    private LockData getLockData(ItemStack item) {
        if (!isSupportedMapItem(item) || item.getItemMeta() == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        LockData loreData = readLoreLockData(meta);
        if (loreData != null) {
            return loreData;
        }

        return persistentDataBridge.read(meta);
    }

    private LockData readLoreLockData(ItemMeta meta) {
        if (meta == null || !meta.hasLore()) {
            return null;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }

        boolean locked = false;
        String ownerUuid = null;
        Long lockTime = null;

        for (int i = 0; i < lore.size(); i++) {
            String stripped = ChatColor.stripColor(lore.get(i));
            if (stripped == null) {
                continue;
            }

            if (INTERNAL_LOCKED.equals(stripped)) {
                locked = true;
            } else if (stripped.startsWith(INTERNAL_OWNER)) {
                ownerUuid = stripped.substring(INTERNAL_OWNER.length());
            } else if (stripped.startsWith(INTERNAL_TIME)) {
                try {
                    lockTime = Long.valueOf(Long.parseLong(stripped.substring(INTERNAL_TIME.length())));
                } catch (NumberFormatException ignored) {
                    lockTime = null;
                }
            }
        }

        if (!locked) {
            return null;
        }

        return new LockData(ownerUuid, lockTime);
    }

    private void writeLockData(ItemMeta meta, String ownerUuid, Long lockTime, String ownerName) {
        List<String> lore = meta.hasLore() && meta.getLore() != null
            ? new ArrayList<String>(meta.getLore())
            : new ArrayList<String>();

        removeManagedLore(lore);
        if (!persistentDataBridge.isAvailable()) {
            lore.add(hiddenLine(INTERNAL_LOCKED));
            lore.add(hiddenLine(INTERNAL_OWNER + ownerUuid));
            lore.add(hiddenLine(INTERNAL_TIME + String.valueOf(lockTime.longValue())));
        }
        lore.add(getRawMsg("lore_locked"));
        lore.add(getRawMsg("lore_owner").replace("%player%", ownerName));
        meta.setLore(lore);

        persistentDataBridge.write(meta, new LockData(ownerUuid, lockTime));
    }

    private void clearLockData(ItemMeta meta) {
        List<String> lore = meta.hasLore() && meta.getLore() != null
            ? new ArrayList<String>(meta.getLore())
            : new ArrayList<String>();

        removeManagedLore(lore);
        meta.setLore(lore.isEmpty() ? null : lore);
        persistentDataBridge.clear(meta);
    }

    private void removeManagedLore(List<String> lore) {
        String lockedLine = normalizeLoreLine(getRawMsg("lore_locked"));
        String ownerPrefix = normalizeLoreLine(getRawMsg("lore_owner").replace("%player%", ""));

        for (int i = lore.size() - 1; i >= 0; i--) {
            String normalized = normalizeLoreLine(lore.get(i));
            if (normalized == null) {
                continue;
            }

            if (INTERNAL_LOCKED.equals(normalized)
                || normalized.startsWith(INTERNAL_OWNER)
                || normalized.startsWith(INTERNAL_TIME)
                || normalized.equals(lockedLine)
                || (ownerPrefix.length() > 0 && normalized.startsWith(ownerPrefix))) {
                lore.remove(i);
            }
        }
    }

    private String normalizeLoreLine(String line) {
        if (line == null) {
            return "";
        }

        String stripped = ChatColor.stripColor(line);
        return stripped == null ? line.trim() : stripped.trim();
    }

    private String hiddenLine(String value) {
        return ChatColor.BLACK + value;
    }

    private static final class LockData {
        private final String ownerUuid;
        private final Long lockTime;

        private LockData(String ownerUuid, Long lockTime) {
            this.ownerUuid = ownerUuid;
            this.lockTime = lockTime;
        }
    }

    private static final class PersistentDataBridge {
        private final Method getPersistentDataContainerMethod;
        private final Method hasMethod;
        private final Method getMethod;
        private final Method setMethod;
        private final Method removeMethod;
        private final Object lockKey;
        private final Object ownerKey;
        private final Object dateKey;
        private final Object byteType;
        private final Object stringType;
        private final Object longType;

        private PersistentDataBridge(
            Method getPersistentDataContainerMethod,
            Method hasMethod,
            Method getMethod,
            Method setMethod,
            Method removeMethod,
            Object lockKey,
            Object ownerKey,
            Object dateKey,
            Object byteType,
            Object stringType,
            Object longType
        ) {
            this.getPersistentDataContainerMethod = getPersistentDataContainerMethod;
            this.hasMethod = hasMethod;
            this.getMethod = getMethod;
            this.setMethod = setMethod;
            this.removeMethod = removeMethod;
            this.lockKey = lockKey;
            this.ownerKey = ownerKey;
            this.dateKey = dateKey;
            this.byteType = byteType;
            this.stringType = stringType;
            this.longType = longType;
        }

        private static PersistentDataBridge create(JavaPlugin plugin) {
            try {
                Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");
                Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
                Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
                Class<?> persistentDataContainerClass = Class.forName("org.bukkit.persistence.PersistentDataContainer");

                Constructor<?> keyConstructor = namespacedKeyClass.getConstructor(pluginClass, String.class);
                Method getPersistentDataContainerMethod = ItemMeta.class.getMethod("getPersistentDataContainer");
                Method hasMethod = persistentDataContainerClass.getMethod("has", namespacedKeyClass, persistentDataTypeClass);
                Method getMethod = persistentDataContainerClass.getMethod("get", namespacedKeyClass, persistentDataTypeClass);
                Method setMethod = persistentDataContainerClass.getMethod("set", namespacedKeyClass, persistentDataTypeClass, Object.class);
                Method removeMethod = persistentDataContainerClass.getMethod("remove", namespacedKeyClass);

                Field byteField = persistentDataTypeClass.getField("BYTE");
                Field stringField = persistentDataTypeClass.getField("STRING");
                Field longField = persistentDataTypeClass.getField("LONG");

                return new PersistentDataBridge(
                    getPersistentDataContainerMethod,
                    hasMethod,
                    getMethod,
                    setMethod,
                    removeMethod,
                    keyConstructor.newInstance(plugin, "locked_map"),
                    keyConstructor.newInstance(plugin, "map_owner_uuid"),
                    keyConstructor.newInstance(plugin, "map_lock_date"),
                    byteField.get(null),
                    stringField.get(null),
                    longField.get(null)
                );
            } catch (Throwable ignored) {
                return new PersistentDataBridge(null, null, null, null, null, null, null, null, null, null, null);
            }
        }

        private LockData read(ItemMeta meta) {
            if (!isAvailable()) {
                return null;
            }

            try {
                Object container = getPersistentDataContainerMethod.invoke(meta);
                Object locked = hasMethod.invoke(container, lockKey, byteType);
                if (!(locked instanceof Boolean) || !((Boolean) locked).booleanValue()) {
                    return null;
                }

                String ownerUuid = (String) getMethod.invoke(container, ownerKey, stringType);
                Long lockTime = (Long) getMethod.invoke(container, dateKey, longType);
                return new LockData(ownerUuid, lockTime);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private void write(ItemMeta meta, LockData lockData) {
            if (!isAvailable()) {
                return;
            }

            try {
                Object container = getPersistentDataContainerMethod.invoke(meta);
                setMethod.invoke(container, lockKey, byteType, Byte.valueOf((byte) 1));
                if (lockData.ownerUuid != null) {
                    setMethod.invoke(container, ownerKey, stringType, lockData.ownerUuid);
                }
                if (lockData.lockTime != null) {
                    setMethod.invoke(container, dateKey, longType, lockData.lockTime);
                }
            } catch (Throwable ignored) {
                // Ignore and rely on lore fallback.
            }
        }

        private void clear(ItemMeta meta) {
            if (!isAvailable()) {
                return;
            }

            try {
                Object container = getPersistentDataContainerMethod.invoke(meta);
                removeMethod.invoke(container, lockKey);
                removeMethod.invoke(container, ownerKey);
                removeMethod.invoke(container, dateKey);
            } catch (Throwable ignored) {
                // Ignore and rely on lore fallback.
            }
        }

        private boolean isAvailable() {
            return getPersistentDataContainerMethod != null
                && hasMethod != null
                && getMethod != null
                && setMethod != null
                && removeMethod != null
                && lockKey != null
                && ownerKey != null
                && dateKey != null
                && byteType != null
                && stringType != null
                && longType != null;
        }
    }
}
