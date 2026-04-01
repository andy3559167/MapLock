# [![Language](https://img.shields.io/badge/Language-English-blue.svg)](README_EN.md) [![Language](https://img.shields.io/badge/Language-繁體中文-red.svg)](README.md) [![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![bStats Servers](https://img.shields.io/bstats/servers/30105.svg)](https://bstats.org/plugin/bukkit/MapLock/30105) [![bStats Players](https://img.shields.io/bstats/players/30105.svg)](https://bstats.org/plugin/bukkit/MapLock/30105)

---

# MapLock | 跨版本地圖保護插件

MapLock 是一款專門保護已繪製地圖的 Minecraft 插件，支援 `Spigot 1.8.8` 到現代 `Spigot / Paper / Folia 1.21.x`。它可以鎖定地圖、阻止工作台與製圖台複製，並提供 Vault 經濟、自訂物品消耗與多語系支援，適合地圖畫、收藏品與展示地圖管理。

- 支援版本: Spigot 1.8.8、Spigot / Paper / Folia 1.21.x
- Java 需求: 依伺服器版本而定

## 特色

- 鎖定已繪製地圖，防止未授權複製
- 同時限制工作台與製圖台複製行為
- 支援 Vault 經濟扣款
- 支援自訂物品消耗條件
- 內建繁體中文、簡體中文與英文語系
- 提供管理員查詢與強制解鎖指令

## 指令

- `/map help`
- `/map lock`
- `/map unlock`
- `/map admin info`
- `/map admin reload`
- `/map admin forceunlock`

## 安裝

1. 將 `MapLock.jar` 放入伺服器的 `plugins` 資料夾。
2. 啟動伺服器以生成設定檔。
3. 依需求調整 `config.yml` 與 `lang/` 語系檔。
4. 若要啟用經濟功能，請另外安裝 Vault 與相容經濟插件。

## 授權

本專案採用 [MIT License](LICENSE)。
