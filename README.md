# MC-AIChat

A Minecraft Paper plugin that allows players to chat with AI through the `/ai` or `/aichat` command.

[English](README.md) | [中文](README_zh-CN.md)

## Features

- Chat with AI using OpenAI-compatible APIs
- Multi-context conversation management (switch between different chat sessions)
- Per-player conversation history
- Configurable AI parameters
- System prompt customization
- Support for environment variable API key
- Dual command aliases to avoid conflicts (`/ai`, `/aichat`)

## Requirements

- Java 21+
- Paper 1.21+ server

## Installation

1. Download the latest release JAR
2. Place the JAR in your server's `plugins` directory
3. Start the server to generate default configuration
4. Edit `plugins/MC-AIChat/config.yml` with your API key
5. Restart the server

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ai <message>` | Chat with AI | None |
| `/ai clear` | Clear current context's chat history | None |
| `/ai reload` | Reload configuration | `mcaichat.reload` |
| `/ai context` | Show current context and list all contexts | None |
| `/ai context create <name>` | Create a new chat context | None |
| `/ai context switch <name>` | Switch to another chat context | None |
| `/ai context delete <name>` | Delete a chat context | None |
| `/ai context list` | List all chat contexts | None |

### Command Aliases

Both `/ai` and `/aichat` can be used interchangeably to avoid conflicts with other plugins.

## Configuration

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
```

### Configuration Options

- `api`: The API base URL (supports OpenAI-compatible APIs)
- `api_key`: Your API key
- `env_api_key`: If `true`, uses `MC-AICHAT-KEY` environment variable instead of `api_key`
- `model`: The AI model to use
- `temperature`: Controls randomness (0-2)
- `top_p`: Nucleus sampling (0-1)
- `presence_penalty`: Penalizes new tokens (0-2)
- `frequency_penalty`: Penalizes frequent tokens (0-2)
- `max_tokens`: Maximum response tokens (0 = unlimited)
- `max_context`: Maximum conversation history (0 = unlimited)

## System Prompt

Edit `plugins/MC-AIChat/system.md` to customize the AI's behavior.

## Building

```bash
./gradlew build
```

The JAR will be in `build/libs/`.

## License

MIT License - see [LICENSE](LICENSE) for details.
