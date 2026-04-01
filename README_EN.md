# [![Language](https://img.shields.io/badge/Language-English-blue.svg)](README_EN.md) [![Language](https://img.shields.io/badge/Language-Traditional%20Chinese-red.svg)](README.md) [![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![bStats Servers](https://img.shields.io/bstats/servers/30105.svg)](https://bstats.org/plugin/bukkit/MapLock/30105) [![bStats Players](https://img.shields.io/bstats/players/30105.svg)](https://bstats.org/plugin/bukkit/MapLock/30105)

---

# MapLock | Cross-Version Map Protection

MapLock is a Minecraft plugin focused on protecting filled maps across multiple server versions. It supports `Spigot 1.8.8` through modern `Spigot / Paper / Folia 1.21.x`, blocks cloning through both crafting tables and cartography tables, and includes Vault economy support, optional item costs, and multilingual messages.

- Supported versions: Spigot 1.8.8, Spigot / Paper / Folia 1.21.x
- Java requirement: depends on your server version

## Features

- Lock filled maps to prevent unauthorized duplication
- Block cloning in both crafting tables and cartography tables
- Optional Vault economy cost
- Optional custom item cost
- Built-in Traditional Chinese, Simplified Chinese, and English language files
- Admin tools for info lookup and force unlock

## Commands

- `/map help`
- `/map lock`
- `/map unlock`
- `/map admin info`
- `/map admin reload`
- `/map admin forceunlock`

## Installation

1. Place `MapLock.jar` into your server's `plugins` folder.
2. Start the server once to generate configuration files.
3. Edit `config.yml` and the files under `lang/` if needed.
4. Install Vault and a compatible economy plugin if you want economy support.

## License

This project is released under the [MIT License](LICENSE).
