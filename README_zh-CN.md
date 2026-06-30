# MC-AIChat

一个 Minecraft Paper 插件，允许玩家通过 `/ai` 或 `/aichat` 命令与 AI 对话。

[English](README.md) | [中文](README_zh-CN.md)

## 功能特性

- 使用 OpenAI 兼容 API 与 AI 聊天
- 多上下文对话管理（在不同聊天会话之间切换）
- 每个玩家独立的对话历史
- 可配置的 AI 参数
- 系统提示词自定义
- 支持环境变量 API 密钥
- 双重命令别名避免冲突（`/ai`, `/aichat`）
- SQLite 数据库持久化存储
- 多语言支持（中文、英语、日语）

## 系统要求

- Java 21+
- Paper 1.21+ 服务器

## 安装步骤

1. 下载最新版本的 JAR 文件
2. 将 JAR 文件放入服务器的 `plugins` 目录
3. 启动服务器以生成默认配置
4. 编辑 `plugins/MC-AIChat/config.yml`，填入你的 API 密钥
5. 重启服务器

## 命令列表

| 命令 | 描述 | 权限 |
|------|------|------|
| `/ai chat <message>` | 与 AI 聊天 | 无 |
| `/ai clear` | 清空当前上下文的聊天历史 | 无 |
| `/ai reload` | 重新加载配置 | `mcaichat.reload` |
| `/ai context` | 显示当前上下文和所有上下文列表 | 无 |
| `/ai context create <name>` | 创建新的聊天上下文 | 无 |
| `/ai context switch <name>` | 切换到其他聊天上下文（支持模糊搜索） | 无 |
| `/ai context delete <name>` | 删除聊天上下文 | 无 |
| `/ai context list` | 列出所有聊天上下文 | 无 |
| `/ai ban <player>` | 禁止玩家使用 AI 聊天 | `mcaichat.ban` |
| `/ai unban <player>` | 解除玩家的 AI 聊天禁令 | `mcaichat.unban` |
| `/ai lang <lang_code>` | 切换语言（zh-CN, en, jp） | 无 |

### 命令别名

`/ai` 和 `/aichat` 可以互换使用，避免与其他插件冲突。

## 配置说明

```yaml
api: "https://api.openai.com/v1"
api_key: "sk-xxx"
env_api_key: false
model: "gpt-4o"
temperature: 1
top_p: 1
presence_penalty: 1
frequency_penalty: 1
max_tokens: 0
max_context: 20
default_lang: "zh-CN"
```

### 配置选项

- `api`: API 基础 URL（支持 OpenAI 兼容 API）
- `api_key`: 你的 API 密钥
- `env_api_key`: 如果为 `true`，使用 `MC-AICHAT-KEY` 环境变量替代 `api_key`
- `model`: 使用的 AI 模型
- `temperature`: 控制随机性（0-2）
- `top_p`: 核采样（0-1）
- `presence_penalty`: 惩罚新 token（0-2）
- `frequency_penalty`: 惩罚频繁出现的 token（0-2）
- `max_tokens`: 最大响应 token 数（0 = 无限制）
- `max_context`: 最大对话历史数（0 = 无限制）
- `default_lang`: 默认语言（zh-CN, en, jp）

## 系统提示词

编辑 `plugins/MC-AIChat/system.md` 来自定义 AI 的行为。

## 数据库配置

编辑 `plugins/MC-AIChat/db.yml` 配置数据库选项：

- `db_file`: SQLite 数据库文件路径
- `auto_save_interval`: 自动保存间隔（秒）
- `save_on_quit`: 服务器关闭时保存
- `max_history_per_context`: 每个上下文最大消息数

## 多语言支持

插件支持以下语言：

- `zh-CN` - 简体中文（默认）
- `en` - English（英语）
- `jp` - 日本語（日语）

玩家可以使用 `/ai lang <lang_code>` 命令切换语言。

## 构建项目

```bash
./gradlew build
```

生成的 JAR 文件位于 `build/libs/` 目录。

## 安全策略

请参阅 [SECURITY_zh-CN.md](SECURITY_zh-CN.md) 了解安全最佳实践。

## 许可证

MIT 许可证 - 详见 [LICENSE](LICENSE)。
