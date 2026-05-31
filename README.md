# Neotrition 🍎

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-80ba42?logo=minecraft)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1-f16436)](https://neoforged.net)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

> Minecraft 营养系统模组 — 15 种营养素，13 条效果规则，你的饮食决定你的增益。

> 最初灵感源自 [Diet](https://modrinth.com/mod/diet)，可视为其（另一款）精神续作。

> **⚠️ AI 生成声明**：本项目由 [OpenCode](https://opencode.ai) 工具辅助生成，主力模型为 **DeepSeek**，GPT-5.4/5.5 辅助。如你对 AI 工具方案、代码质量或安全风险有顾虑，请谨慎使用本模组。

## 🎮 功能

- **15 种营养组** — 水果、蔬菜、谷物、蛋白、鱼类、蛋、奶、菌类、坚果、糖、蜂蜜、酒、咖啡、盐、微量元素
- **13 条效果规则** — 满足营养阈值后触发药水效果（生命恢复、速度、急迫、夜视、水下呼吸、幸运、饱和）和属性加成（生命、护甲、抗击退）
- **组合条件** — 多营养组 AND/OR 逻辑（比如水果+蔬菜+谷物+蛋白+酒全都 ≥50k 加护甲，鱼类≥25k 水下呼吸）
- **弧形进度条** — GUI 中 260° 弧形渲染，64 段细分，绕图标外侧
- **BFS 自动推断** — 世界加载时沿配方链自动分配食物到营养组
- **可配置衰减** — 每组独立衰减值/频率/幂函数压力曲线
- **F3+H Tooltip** — 物品提示显示所属营养组标签
- **数据包驱动** — 所有配置（组、物品、效果）均通过 JSON 数据包定义

## 📦 安装

- Minecraft **1.21.1**
- NeoForge **21.1+**
- Java **21+**

从 [Releases](https://github.com/uitstalie/Neotrition-public/releases) 下载 `.jar`，放入 `mods/` 文件夹。

## ⌨️ 命令

| 命令 | 说明 |
|------|------|
| `/neotrition set <组> <值>` | 设置营养值（0–100000） |
| `/neotrition get <组>` | 查看当前营养值 |
| `/neotrition list` | 列出所有营养组及数值 |
| `/neotrition autogen` | 运行 BFS 自动生成 |
| `/neotrition find-seeds` | 查找最小标记物品集合 |

## 📊 营养组

| ID | 名称 | 图标 | 衰减 |
|----|------|------|------|
| `fruits` | 水果 | Apple | 每 6s |
| `vegetables` | 蔬菜 | Carrot | 每 6s |
| `grains` | 谷物 | Bread | 每 6s |
| `proteins` | 蛋白 | Cooked Beef | 每 6s |
| `fishs` | 鱼类 | Cod | 每 6s |
| `eggs` | 蛋 | Egg | 每 6s |
| `milks` | 奶 | Milk Bucket | 每 6s |
| `mushrooms` | 菌类 | Brown Mushroom | 每 6s |
| `nuts` | 坚果 | Cocoa Beans | 每 6s |
| `sugars` | 糖 | Sugar | 每 6s |
| `honeys` | 蜂蜜 | Honey Bottle | 每 6s |
| `wines` | 酒 | Potion | 每 6s |
| `coffee` | 咖啡 | Cocoa Beans | 每 6s |
| `salt` | 盐 | Sugar | 每 6s |
| `trace_elements` | 微量元素 | Iron Ingot | 每 6s |

## ⚡ 效果规则

| # | 效果 | 条件 | 阈值 |
|---|------|------|------|
| 1 | +生命上限 5 | 蛋白 **或** 蛋 **或** 鱼 **或** 谷物 | ≥ 25k |
| 2 | +生命上限 10 | 蛋白 **且** 蛋 **且** 鱼 **且** 谷物 | ≥ 50k |
| 3 | +护甲 5 | 酒 **且** 蛋白 **且** 谷物 **且** 蔬菜 **且** 水果 | ≥ 50k |
| 4 | +护甲 10 & +抗击退 | 蛋白 **且** 谷物 **且** 蔬菜 **且** 水果 **且** 微量元素 | ≥ 50k |
| 5 | 水下呼吸 | 鱼类 | ≥ 25k |
| 6 | 夜视 | 蔬菜 | ≥ 50k |
| 7 | 幸运 | 糖 **或** 蜂蜜 **或** 酒 | ≥ 20k |
| 8 | 急迫 | 坚果 **或** 盐 **或** 咖啡 | ≥ 25k |
| 9 | 速度 | 咖啡 | ≥ 25k |
| 10 | 生命恢复 | 菌类 **或** 蛋 **或** 奶 | ≥ 20k |
| 11 | 速度 II | 谷物 **且** 水果 **且** 蛋白 **且** 蔬菜 | ≥ 50k |
| 12 | 饱和 | 蛋 **且** 谷物 **且** 奶 | ≥ 50k |
| 13 | 幸运 II | 蜂蜜 **且** 糖 **且** 酒 **且** 微量元素 | ≥ 50k |

## 🛠 数据包自定义

创建自定义数据包来添加或修改配置：

```
data/neotrition/config/config.json    — 全局设置（频率、营养值公式）
data/neotrition/groups/<组>.json      — 营养组定义
data/neotrition/items/<组>.json       — 物品-组绑定及手动值
data/neotrition/effects/default.json  — 效果规则（AND/OR 条件）
```

详见 [Wiki](https://github.com/uitstalie/Neotrition-public/wiki) 配置参考。

## 🔨 构建

```bash
./gradlew build
```

## 📄 许可

MIT License — 详见 [LICENSE](LICENSE)
