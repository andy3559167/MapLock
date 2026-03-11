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
            setupEconomy();
        }

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("map") != null) {
            getCommand("map").setExecutor(this);
            getCommand("map").setTabCompleter(this);
        }

        getLogger().info("MapLock 1.1 has been successfully enabled on " + getServer().getName());
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
        String[] languages = {"zh_tw.yml", "zh_cn.yml", "en.yml"};
        for (String file : languages) {
            if (!new File(langFolder, file).exists()) saveResource("lang/" + file, false);
        }

        String lang = getConfig().getString("language", "zh_tw");
        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) langFile = new File(langFolder, "zh_tw.yml");
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
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
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

        if (subCommand.equals("admin")) {
            if (args.length < 2) {
                player.sendMessage(getMsg("admin_usage"));
                return true;
            }
            String adminSub = args[1].toLowerCase();

            if (adminSub.equals("reload") && player.hasPermission("maplock.admin.reload")) {
                loadConfiguration();
                if (getConfig().getBoolean("economy.enabled")) setupEconomy(); 
                player.sendMessage(getMsg("reload_success"));
                return true;
            }

            if (adminSub.equals("info") && player.hasPermission("maplock.admin.info")) {
                if (item.getType() != Material.FILLED_MAP) {
                    player.sendMessage(getMsg("must_hold_map"));
                    return true;
                }
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                    player.sendMessage(getMsg("admin_info_none"));
                    return true;
                }
                String uuidStr = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                Long time = meta.getPersistentDataContainer().get(dateKey, PersistentDataType.LONG);
                String name = "Unknown";
                if (uuidStr != null) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                    if (op.getName() != null) name = op.getName();
                }
                String date = time != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time)) : "Unknown";
                player.sendMessage(getRawMsg("admin_info_header"));
                player.sendMessage(getRawMsg("admin_info_name").replace("%name%", name));
                player.sendMessage(getRawMsg("admin_info_uuid").replace("%uuid%", uuidStr != null ? uuidStr : "None"));
                player.sendMessage(getRawMsg("admin_info_date").replace("%date%", date));
                return true;
            }

            if (adminSub.equals("forceunlock") && player.hasPermission("maplock.admin.forceunlock")) {
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
                    lore.removeIf(line -> line.equals(getRawMsg("lore_locked")) || line.contains(getRawMsg("lore_owner").replace("%player%", "").trim()));
                    meta.setLore(lore);
                }
                item.setItemMeta(meta);
                player.sendMessage(getMsg("force_unlock_success"));
                return true;
            }
            return true;
        }

        // --- 以下為 Lock/Unlock ---
        List<String> disabledWorlds = getConfig().getStringList("disabled-worlds");
        if (disabledWorlds.contains(player.getWorld().getName()) && !player.hasPermission("maplock.admin.bypass")) {
            if (subCommand.equals("lock") || subCommand.equals("unlock")) {
                player.sendMessage(getMsg("world_disabled"));
                return true;
            }
        }

        if (item.getType() != Material.FILLED_MAP || item.getItemMeta() == null) {
            player.sendMessage(getMsg("must_hold_map"));
            return true;
        }
        ItemMeta meta = item.getItemMeta();

        if (subCommand.equals("lock") && player.hasPermission("maplock.lock")) {
            if (meta.getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                player.sendMessage(getMsg("already_locked"));
                return true;
            }

            if (!player.hasPermission("maplock.admin.bypass")) {
                if (getConfig().getBoolean("economy.enabled")) {
                    if (econ == null) { player.sendMessage(getMsg("economy_error")); return true; }
                    double cost = getConfig().getDouble("economy.cost", 0.0);
                    if (cost > 0 && econ.getBalance(player) < cost) {
                        player.sendMessage(getMsg("not_enough_money").replace("%money%", String.valueOf(cost)));
                        return true;
                    }
                    if (cost > 0) econ.withdrawPlayer(player, cost);
                }

                if (getConfig().getBoolean("item-cost.enabled")) {
                    Material mat = Material.getMaterial(getConfig().getString("item-cost.material", "DIAMOND").toUpperCase());
                    int amt = getConfig().getInt("item-cost.amount", 1);
                    if (mat != null && amt > 0) {
                        if (!checkAndConsumeItem(player, mat, amt)) {
                            player.sendMessage(getMsg("not_enough_item").replace("%amount%", String.valueOf(amt)).replace("%item%", getConfig().getString("item-cost.display-name", "Item")));
                            return true;
                        }
                    }
                }
            }

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

        if (subCommand.equals("unlock") && player.hasPermission("maplock.unlock")) {
            if (!meta.getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                player.sendMessage(getMsg("not_locked"));
                return true;
            }
            String owner = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (owner != null && !owner.equals(player.getUniqueId().toString())) {
                player.sendMessage(getMsg("not_owner"));
                return true;
            }
            meta.getPersistentDataContainer().remove(lockKey);
            meta.getPersistentDataContainer().remove(ownerKey);
            meta.getPersistentDataContainer().remove(dateKey);
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                lore.removeIf(line -> line.equals(getRawMsg("lore_locked")) || line.contains(getRawMsg("lore_owner").replace("%player%", "").trim()));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
            player.sendMessage(getMsg("unlock_success"));
            return true;
        }

        return true;
    }

    private boolean checkAndConsumeItem(Player p, Material m, int a) {
        if (!p.getInventory().containsAtLeast(new ItemStack(m), a)) return false;
        p.getInventory().removeItem(new ItemStack(m, a));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("help");
            if (sender.hasPermission("maplock.lock")) list.add("lock");
            if (sender.hasPermission("maplock.unlock")) list.add("unlock");
            if (sender.hasPermission("maplock.admin.info") || sender.hasPermission("maplock.admin.reload")) list.add("admin");
            StringUtil.copyPartialMatches(args[0], list, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("maplock.admin.info")) list.add("info");
            if (sender.hasPermission("maplock.admin.forceunlock")) list.add("forceunlock");
            if (sender.hasPermission("maplock.admin.reload")) list.add("reload");
            StringUtil.copyPartialMatches(args[1], list, completions);
        }
        Collections.sort(completions);
        return completions;
    }

    @EventHandler
    public void onMapCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.getType() == Material.FILLED_MAP && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                event.getInventory().setResult(null); break;
            }
        }
    }

    @EventHandler
    public void onCartography(PrepareInventoryResultEvent event) {
        if (event.getInventory() instanceof CartographyInventory inv) {
            ItemStack map = inv.getItem(0);
            if (map != null && map.getType() == Material.FILLED_MAP && map.hasItemMeta() && map.getItemMeta().getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE)) {
                if (inv.getItem(1) != null && inv.getItem(1).getType() == Material.MAP) event.setResult(null);
            }
        }
    }
}