package com.norule.maplock;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareInventoryResultEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class MapLockPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private NamespacedKey lockKey;
    private NamespacedKey ownerKey;
    private NamespacedKey dateKey;

    private FileConfiguration langConfig;
    private Economy econ = null;

    @Override
    public void onEnable() {
        lockKey = new NamespacedKey(this, "locked_map");
        ownerKey = new NamespacedKey(this, "map_owner_uuid");
        dateKey = new NamespacedKey(this, "map_lock_date");

        loadConfiguration();

        if (getConfig().getBoolean("economy.enabled")) {
            if (!setupEconomy()) {
                getLogger().warning("找不到可用的 Vault 經濟系統 (可能是未安裝經濟插件或功能未啟用)！");
            } else {
                getLogger().info("已成功連結 Vault 經濟系統！");
            }
        }

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("map") != null) {
            getCommand("map").setExecutor(this);
            getCommand("map").setTabCompleter(this);
        }

        getLogger().info("MapLock 插件已成功啟用 (支援自訂物品驗證)！");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        reloadConfig();

        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();
        if (!new File(langFolder, "zh_tw.yml").exists()) saveResource("lang/zh_tw.yml", false);
        if (!new File(langFolder, "zh_cn.yml").exists()) saveResource("lang/zh_cn.yml", false);
        if (!new File(langFolder, "en.yml").exists()) saveResource("lang/en.yml", false);

        String lang = getConfig().getString("language", "zh_tw");
        File langFile = new File(langFolder, lang + ".yml");

        if (!langFile.exists()) {
            langFile = new File(langFolder, "zh_tw.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用此指令。");
            return true;
        }

        // ==========================
        // 幫助選單
        // ==========================
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (!player.hasPermission("maplock.help")) {
                player.sendMessage(getMsg("no_permission"));
                return true;
            }
            player.sendMessage(getRawMsg("help_header"));
            player.sendMessage(getRawMsg("help_help"));
            if (player.hasPermission("maplock.lock")) player.sendMessage(getRawMsg("help_lock"));
            if (player.hasPermission("maplock.unlock")) player.sendMessage(getRawMsg("help_unlock"));
            if (player.hasPermission("maplock.admin.info")) player.sendMessage(getRawMsg("help_admin_info"));
            if (player.hasPermission("maplock.admin.forceunlock")) player.sendMessage(getRawMsg("help_admin_forceunlock"));
            if (player.hasPermission("maplock.admin.reload")) player.sendMessage(getRawMsg("help_admin_reload"));
            if (player.hasPermission("maplock.admin.bypass")) player.sendMessage(getRawMsg("help_admin_bypass"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        ItemStack item = player.getInventory().getItemInMainHand();

        List<String> disabledWorlds = getConfig().getStringList("disabled-worlds");
        if (disabledWorlds.contains(player.getWorld().getName()) && !player.hasPermission("maplock.admin.bypass")) {
            if (subCommand.equals("lock") || subCommand.equals("unlock")) {
                player.sendMessage(getMsg("world_disabled"));
                return true;
            }
        }

        // ==========================
        // Admin 管理員指令
        // ==========================
        if (subCommand.equals("admin")) {
            if (!player.hasPermission("maplock.admin.info") && 
                !player.hasPermission("maplock.admin.reload") && 
                !player.hasPermission("maplock.admin.forceunlock")) {
                player.sendMessage(getMsg("no_permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(getMsg("admin_usage"));
                return true;
            }
            String adminSub = args[1].toLowerCase();

            if (adminSub.equals("reload")) {
                if (!player.hasPermission("maplock.admin.reload")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                loadConfiguration();
                if (getConfig().getBoolean("economy.enabled")) setupEconomy(); 
                player.sendMessage(getMsg("reload_success"));
                return true;
            }
            
            if (adminSub.equals("info")) {
                if (!player.hasPermission("maplock.admin.info")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                if (item.getType() != Material.FILLED_MAP || item.getItemMeta() == null) {
                    player.sendMessage(getMsg("must_hold_map"));
                    return true;
                }
                
                ItemMeta meta = item.getItemMeta();
                if (!meta.getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                    player.sendMessage(getMsg("admin_info_none"));
                    return true;
                }

                String ownerUUIDStr = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                Long dateLong = meta.getPersistentDataContainer().get(dateKey, PersistentDataType.LONG);
                
                String ownerName = "未知";
                if (ownerUUIDStr != null) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUIDStr));
                    if (op.getName() != null) ownerName = op.getName();
                }

                String dateStr = "未知";
                if (dateLong != null) {
                    dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(dateLong));
                }

                player.sendMessage(getRawMsg("admin_info_header"));
                player.sendMessage(getRawMsg("admin_info_name").replace("%name%", ownerName));
                player.sendMessage(getRawMsg("admin_info_uuid").replace("%uuid%", ownerUUIDStr != null ? ownerUUIDStr : "無"));
                player.sendMessage(getRawMsg("admin_info_date").replace("%date%", dateStr));
                return true;
            }

            if (adminSub.equals("forceunlock")) {
                if (!player.hasPermission("maplock.admin.forceunlock")) {
                    player.sendMessage(getMsg("no_permission"));
                    return true;
                }
                if (item.getType() != Material.FILLED_MAP || item.getItemMeta() == null) {
                    player.sendMessage(getMsg("must_hold_map"));
                    return true;
                }
                
                ItemMeta meta = item.getItemMeta();
                if (!meta.getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                    player.sendMessage(getMsg("not_locked"));
                    return true;
                }

                meta.getPersistentDataContainer().remove(lockKey);
                meta.getPersistentDataContainer().remove(ownerKey);
                meta.getPersistentDataContainer().remove(dateKey);

                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    String lockedStr = getRawMsg("lore_locked");
                    lore.removeIf(line -> line.equals(lockedStr) || line.contains(getRawMsg("lore_owner").replace("%player%", "").trim()));
                    meta.setLore(lore);
                }

                item.setItemMeta(meta);
                player.sendMessage(getMsg("force_unlock_success"));
                return true;
            }

            player.sendMessage(getMsg("admin_usage"));
            return true;
        }

        if (item.getType() != Material.FILLED_MAP || item.getItemMeta() == null) {
            player.sendMessage(getMsg("must_hold_map"));
            return true;
        }
        ItemMeta meta = item.getItemMeta();

        // ==========================
        // 鎖定指令
        // ==========================
        if (subCommand.equals("lock")) {
            if (!player.hasPermission("maplock.lock")) {
                player.sendMessage(getMsg("no_permission"));
                return true;
            }
            if (meta.getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                player.sendMessage(getMsg("already_locked"));
                return true;
            }

            // --- 經濟與物品檢查 (如果沒有 bypass 權限) ---
            if (!player.hasPermission("maplock.admin.bypass")) {
                
                // 1. 金錢檢查
                if (getConfig().getBoolean("economy.enabled")) {
                    if (econ == null) {
                        // 經濟系統未啟動，發送警告並直接【中斷鎖定】
                        player.sendMessage(getMsg("economy_error"));
                        return true; 
                    }
                    double cost = getConfig().getDouble("economy.cost", 0.0);
                    if (cost > 0) {
                        if (econ.getBalance(player) < cost) {
                            // 餘額不足，發送警告並直接【中斷鎖定】
                            player.sendMessage(getMsg("not_enough_money").replace("%money%", String.valueOf(cost)));
                            return true;
                        }
                        // 扣款成功
                        econ.withdrawPlayer(player, cost);
                    }
                }

                // 2. 自訂物品檢查
                if (getConfig().getBoolean("item-cost.enabled")) {
                    Material costMat = Material.getMaterial(getConfig().getString("item-cost.material", "DIAMOND").toUpperCase());
                    int costAmount = getConfig().getInt("item-cost.amount", 1);
                    String displayName = getConfig().getString("item-cost.display-name", costMat != null ? costMat.name() : "物品");
                    
                    boolean strictCheck = getConfig().getBoolean("item-cost.strict-meta-check", false);
                    String reqName = getConfig().getString("item-cost.required-name", "");
                    List<String> reqLore = getConfig().getStringList("item-cost.required-lore");
                    
                    if (costMat != null && costAmount > 0) {
                        // 呼叫底下的專屬方法來驗證並扣除物品
                        if (!checkAndConsumeItem(player, costMat, costAmount, strictCheck, reqName, reqLore)) {
                            // 物品不足或不符合，發送警告並直接【中斷鎖定】
                            player.sendMessage(getMsg("not_enough_item")
                                    .replace("%amount%", String.valueOf(costAmount))
                                    .replace("%item%", displayName));
                            return true;
                        }
                    }
                }
            }

            // --- 扣款與扣物品都成功後，才會執行到這裡鎖定地圖 ---
            meta.getPersistentDataContainer().set(lockKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            meta.getPersistentDataContainer().set(dateKey, PersistentDataType.LONG, System.currentTimeMillis());

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(getRawMsg("lore_locked"));
            lore.add(getRawMsg("lore_owner").replace("%player%", player.getName()));
            meta.setLore(lore);

            item.setItemMeta(meta);
            player.sendMessage(getMsg("lock_success"));
            return true;
        }

        // ==========================
        // 一般解鎖指令
        // ==========================
        if (subCommand.equals("unlock")) {
            if (!player.hasPermission("maplock.unlock")) {
                player.sendMessage(getMsg("no_permission"));
                return true;
            }
            if (!meta.getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                player.sendMessage(getMsg("not_locked"));
                return true;
            }

            String ownerUUID = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            
            if (ownerUUID != null && !ownerUUID.equals(player.getUniqueId().toString())) {
                player.sendMessage(getMsg("not_owner"));
                return true;
            }

            meta.getPersistentDataContainer().remove(lockKey);
            meta.getPersistentDataContainer().remove(ownerKey);
            meta.getPersistentDataContainer().remove(dateKey);

            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                String lockedStr = getRawMsg("lore_locked");
                lore.removeIf(line -> line.equals(lockedStr) || line.contains(getRawMsg("lore_owner").replace("%player%", "").trim()));
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
            player.sendMessage(getMsg("unlock_success"));
            return true;
        }

        player.sendMessage(getMsg("wrong_usage"));
        return true;
    }

    /**
     * 掃描背包並驗證/扣除自訂物品
     */
    private boolean checkAndConsumeItem(Player player, Material material, int amount, boolean strict, String reqName, List<String> reqLore) {
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        
        // 第一階段：計算符合條件的物品數量
        for (ItemStack item : contents) {
            if (item != null && item.getType() == material) {
                if (strict) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) continue;
                    
                    // 驗證名稱
                    if (reqName != null && !reqName.isEmpty()) {
                        if (!meta.hasDisplayName() || !meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', reqName))) {
                            continue;
                        }
                    }
                    // 驗證 Lore
                    if (reqLore != null && !reqLore.isEmpty()) {
                        if (!meta.hasLore()) continue;
                        List<String> itemLore = meta.getLore();
                        if (itemLore.size() < reqLore.size()) continue;
                        boolean loreMatch = true;
                        for (int i = 0; i < reqLore.size(); i++) {
                            if (!itemLore.get(i).equals(ChatColor.translateAlternateColorCodes('&', reqLore.get(i)))) {
                                loreMatch = false;
                                break;
                            }
                        }
                        if (!loreMatch) continue;
                    }
                }
                count += item.getAmount();
            }
        }

        // 如果數量不足，直接回傳失敗 (中斷扣除)
        if (count < amount) return false;

        // 第二階段：執行扣除
        int remaining = amount;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                boolean match = true;
                if (strict) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) match = false;
                    else {
                        if (reqName != null && !reqName.isEmpty() && (!meta.hasDisplayName() || !meta.getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', reqName)))) match = false;
                        if (reqLore != null && !reqLore.isEmpty()) {
                            if (!meta.hasLore()) match = false;
                            else {
                                List<String> itemLore = meta.getLore();
                                if (itemLore.size() < reqLore.size()) match = false;
                                else {
                                    for (int j = 0; j < reqLore.size(); j++) {
                                        if (!itemLore.get(j).equals(ChatColor.translateAlternateColorCodes('&', reqLore.get(j)))) {
                                            match = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (match) {
                    if (item.getAmount() <= remaining) {
                        remaining -= item.getAmount();
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - remaining);
                        remaining = 0;
                    }
                    if (remaining <= 0) break;
                }
            }
        }
        return true; // 成功扣除
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("maplock.help")) commands.add("help");
            if (sender.hasPermission("maplock.lock")) commands.add("lock");
            if (sender.hasPermission("maplock.unlock")) commands.add("unlock");
            if (sender.hasPermission("maplock.admin.info") || 
                sender.hasPermission("maplock.admin.reload") ||
                sender.hasPermission("maplock.admin.forceunlock")) {
                commands.add("admin");
            }
            StringUtil.copyPartialMatches(args[0], commands, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("maplock.admin.info")) commands.add("info");
            if (sender.hasPermission("maplock.admin.forceunlock")) commands.add("forceunlock");
            if (sender.hasPermission("maplock.admin.reload")) commands.add("reload");
            StringUtil.copyPartialMatches(args[1], commands, completions);
        }

        Collections.sort(completions);
        return completions;
    }

    @EventHandler
    public void onMapCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        boolean hasLockedMap = false;
        boolean hasEmptyMap = false;

        for (ItemStack item : inventory.getMatrix()) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (item.getType() == Material.FILLED_MAP) {
                if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                    hasLockedMap = true;
                }
            } else if (item.getType() == Material.MAP) {
                hasEmptyMap = true;
            }
        }
        if (hasLockedMap && hasEmptyMap) inventory.setResult(null);
    }

    @EventHandler
    public void onCartography(PrepareInventoryResultEvent event) {
        if (event.getInventory() instanceof CartographyInventory inventory) {
            ItemStack mapItem = inventory.getItem(0);
            ItemStack addonItem = inventory.getItem(1);

            if (mapItem != null && mapItem.getType() == Material.FILLED_MAP) {
                if (mapItem.hasItemMeta() && mapItem.getItemMeta().getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                    if (addonItem != null && addonItem.getType() == Material.MAP) {
                        event.setResult(null);
                    }
                }
            }
        }
    }
}