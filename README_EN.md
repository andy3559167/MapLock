# [![Language](https://img.shields.io/badge/Language-English-blue.svg)](README_EN.md) [![Language](https://img.shields.io/badge/語言-繁體中文-red.svg)](README.md) [![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![bStats Servers](https://img.shields.io/bstats/servers/30105.svg)](https://bstats.org/plugin/bukkit/MapLock/30105) [![bStats Players](https://img.shields.io/bstats/players/30105.svg)](https://bstats.org/plugin/bukkit/MapLock/30105)
---

# MapLock 🗺️ 🔒

A lightweight map protection plugin designed for Minecraft servers. It prevents unauthorized cloning of your map art and supports custom economic costs and multi-language settings.

* **Tested Versions:** 1.21 ~ 1.21.11
* **Java Version:** 21

## ✨ Features

* **Map Locking System**: Prevents locked maps from being cloned in Crafting Tables or Cartography Tables.
* **Multilingual Support**: Built-in support for Traditional Chinese (`zh_tw`), Simplified Chinese (`zh_cn`), and English (`en`).
* **Economy Integration**: Supports **Vault** economy bridge; set custom processing fees for locking maps.
* **Custom Item Consumption**: Require specific items (supports custom Name and Lore checks) to be consumed for map locking.
* **Admin Tools**: Provides detailed map info queries and administrative force-unlock features.
* **Granular Permissions**: Every command and privilege has a corresponding permission node for easy management.
* **Tab Completion**: Full auto-complete support for all commands for a smoother user experience.

## 🛠️ Commands & Permissions

| Command | Description | Default | Permission Node |
| --- | --- | --- | --- |
| `/map help` | Show the help menu | Everyone | `maplock.help` |
| `/map lock` | Lock the map in your main hand | Everyone | `maplock.lock` |
| `/map unlock` | Unlock a map that you own | Everyone | `maplock.unlock` |
| `/map admin info` | Check map owner and lock date | OP | `maplock.admin.info` |
| `/map admin forceunlock` | Forcefully unlock any map | OP | `maplock.admin.forceunlock` |
| `/map admin reload` | Reload config and language files | OP | `maplock.admin.reload` |

### 💡 Extra Privileges

* `maplock.admin.bypass`: Allows the user to bypass "Disabled World" restrictions and lock maps for free (no economy/item cost).

## 📦 Installation

1. Place `MapLock.jar` into your server's `plugins` folder.
2. (Optional) If using economy features, ensure **Vault** and a compatible economy plugin (e.g., CMI, EssentialsX) are installed.
3. Restart the server.
4. Adjust your settings in `plugins/MapLock/config.yml`.

## ⚙️ Configuration Preview (config.yml)

```yaml
language: "en" # Language setting (en, zh_tw, zh_cn)

disabled-worlds: # List of worlds where locking is disabled
  - "disabledworlds1"
  - "disabledworlds2"

economy: # Economy settings
  enabled: false
  cost: 100.0

item-cost: # Item consumption settings
  enabled: true
  material: "DIAMOND"
  amount: 1
  display-name: "Map Lock Voucher"
  strict-meta-check: true # Whether to check for custom Display Name and Lore

```

---

## ❓ FAQ

**Q: Why do command hints show "Missing" after an update?**
A: This is usually due to legacy language files. Please delete the `plugins/MapLock/lang/` folder and run `/map admin reload` to regenerate the latest files.

**Q: Why is map locking free even though I set a cost?**
A: Ensure `economy.enabled` is set to `true` and **Vault** is installed with a working economy provider. If the economy system is unavailable, the plugin may block locking actions for safety.

**Q: Can I make map locking free?**
A: Yes, set `cost` to `0.0` in `config.yml` or simply set `economy.enabled` to `false`.

---

## 🤝 Contributing

This project is a unique experimental endeavor: **The code was developed and optimized entirely by AI (Gemini) based on Andy's specific requirements and logic.**

If you have ideas or find bugs, feel free to contribute:

1. **Report Issues**: Submit bugs via the GitHub [Issues](https://www.google.com/search?q=../../issues) page.
2. **Feature Requests**: Suggest new ideas (e.g., Item Frame protection, Database support).
3. **Localization**: Translations for languages other than Chinese and English are highly welcome via Pull Requests!

---

## 🤖 About the Developer

This plugin was co-created by **Gemini** (AI Partner) and **Andy**.

---

## ⚖️ License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
