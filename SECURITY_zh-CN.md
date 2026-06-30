# 安全策略

[English](SECURITY.md) | [中文](SECURITY_zh-CN.md)

## 报告安全问题

如果您在 MC-AIChat 中发现安全漏洞，请负责任地报告。

### 如何报告

1. **不要**创建公开的 issue
2. 通过 GitHub Security Advisories 或邮件联系 EndlessPixel Studio
3. 提供关于漏洞的详细信息

### 需要包含的信息

- 漏洞描述
- 复现步骤
- 影响评估
- 建议的修复方案（如果适用）

## 安全最佳实践

### API 密钥管理

- **切勿**将 API 密钥提交到版本控制系统
- 使用 `env_api_key: true` 从环境变量加载密钥
- 保持 API 密钥安全，并定期轮换

### 配置安全

- 仅将 `mcaichat.reload` 权限授予可信玩家
- 使用强密码保护服务器访问
- 保持插件更新到最新版本

### 数据隐私

- 对话历史存储在 SQLite 数据库中
- 数据持久化到磁盘，服务器重启后保留
- 使用 `/ai clear` 可以清除当前上下文的历史记录

## 支持的版本

只有 MC-AIChat 的最新版本会获得安全更新。
