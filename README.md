# [![Language](https://img.shields.io/badge/Language-English-blue.svg)](README_EN.md) [![Language](https://img.shields.io/badge/語言-繁體中文-red.svg)](README.md) [![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![bStats Servers](https://img.shields.io/bstats/servers/30105.svg)](https://bstats.org/plugin/bukkit/MapLock/30105) [![bStats Players](https://img.shields.io/bstats/players/30105.svg)](https://bstats.org/plugin/bukkit/MapLock/30105)

---

# MapLock 🗺️ 🔒

一款為 Minecraft 伺服器設計的輕量化地圖保護插件。防止玩家未經許可複製你的地圖畫，並支援自訂經濟成本與多國語言。

- 測試伺服器版本: 1.21 ~ 1.21.11
- Java 版本 21

## ✨ 功能特色

* **地圖鎖定系統**：防止鎖定的地圖在工作台或製圖台被複製。
* **多國語言支援**：內建繁體中文 (`zh_tw`)、簡體中文 (`zh_cn`) 與英文 (`en`)，支援自動偵測。
* **經濟系統整合**：支援 **Vault** 經濟橋樑，可設定鎖定地圖所需的手續費。
* **自訂物品消耗**：可設定鎖定地圖時需消耗特定物品（支援自訂名稱與 Lore 檢查）。
* **管理員工具**：提供詳細的地圖資訊查詢、強制解鎖功能。
* **權限細分**：所有指令與特權皆有對應權限，方便管理員配置。
* **自動補全**：完整的 Tab-Completion 支援，操作更直覺。

## 🛠️ 指令與權限

| 指令 | 說明 | 預設權限 | 權限節點 |
| --- | --- | --- | --- |
| `/map help` | 顯示插件幫助選單 | 所有人 | `maplock.help` |
| `/map lock` | 鎖定主手上的地圖 | 所有人 | `maplock.lock` |
| `/map unlock` | 解鎖自己擁有的地圖 | 所有人 | `maplock.unlock` |
| `/map admin info` | 查詢地圖鎖定者與日期 | OP | `maplock.admin.info` |
| `/map admin forceunlock` | 強制解鎖任何地圖 | OP | `maplock.admin.forceunlock` |
| `/map admin reload` | 重新載入設定檔與語言檔 | OP | `maplock.admin.reload` |

### 💡 額外特權

* `maplock.admin.bypass`: 擁有此權限者可無視「禁用世界」限制，且鎖定地圖不收費。

## 📦 安裝教學

1. 將 `MapLock.jar` 放入伺服器的 `plugins` 資料夾中。
2. （選配）若要啟用經濟功能，請確保伺服器已安裝 **Vault** 指令與經濟插件（如 CMI, EssentialsX）。
3. 重啟伺服器。
4. 於 `plugins/MapLock/config.yml` 中調整你的設定。

## ⚙️ 設定檔預覽 (config.yml)

```yaml
language: "zh_tw" # 語言設定

disabled-worlds: # 禁用世界清單
  - "disabledworlds1"

economy: # 經濟設定
  enabled: false
  cost: 100.0

item-cost: # 物品消耗設定
  enabled: true
  material: "DIAMOND"
  amount: 1
  display-name: "地圖鎖定憑證"
  strict-meta-check: true # 是否檢查自訂名稱與 Lore

```

---

## ❓ 常見問答 (FAQ)

**Q: 為什麼更新插件後，指令提示會顯示 "Missing"？**
A: 這通常是因為舊的語言檔案殘留。請刪除 `plugins/MapLock/lang/` 資料夾並輸入 `/map admin reload`，讓系統重新生成最新的語言檔案。

**Q: 為什麼鎖定地圖沒有扣錢？**
A: 請檢查 `config.yml` 中的 `economy.enabled` 是否設為 `true`，且伺服器必須安裝 **Vault** 與相容的經濟插件（如 EssentialsX 或 CMI）。若經濟系統未啟動，插件會為了安全考量自動阻擋鎖定功能。

**Q: 我可以讓鎖定地圖變成免費嗎？**
A: 可以，請將 `config.yml` 中的 `cost` 設為 `0.0`，或直接將 `economy.enabled` 設為 `false`。

---

## 🤝 如何參與貢獻 (Contributing)

本專案是一個特別的實驗性專案：**程式碼完全由 AI (Gemini) 根據 Andy 的需求開發與優化**。

如果你有任何想法或發現 Bug，歡迎透過以下方式參與：
1. **回報問題**：在 GitHub 的 [Issues](../../issues) 頁面提交你遇到的 Bug。
2. **功能建議**：如果你有想加的功能（例如：地圖展示框保護、資料庫支援等），歡迎提出討論。
3. **翻譯支援**：如果你能提供除繁體、簡體中文與英文以外的語言翻譯，非常歡迎透過 Pull Request 貢獻！

---

## 🤖 關於開發者

本插件由 **Gemini** (AI 合作夥伴) 與 **Andy** 共同協作完成。
---

## ⚖️ 授權 (License)

本專案採用 **MIT 授權協議**。詳細內容請參閱 [LICENSE](LICENSE) 檔案。
