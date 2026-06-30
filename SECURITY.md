# Security Policy

## Reporting Security Issues

If you discover a security vulnerability in MC-AIChat, please report it responsibly.

### How to Report

1. **Do not** create a public issue
2. Contact EndlessPixel Studio via GitHub Security Advisories or email
3. Provide detailed information about the vulnerability

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Impact assessment
- Suggested fix (if applicable)

## Security Best Practices

### API Key Management

- **Never** commit your API key to version control
- Use `env_api_key: true` to load the key from environment variables
- Keep your API key secure and rotate it regularly

### Configuration

- Restrict `mcaichat.reload` permission to trusted players only
- Use strong passwords for server access
- Keep the plugin updated to the latest version

### Data Privacy

- Conversation history is stored in memory only
- No data is persisted to disk
- History is cleared when the server restarts or when `/ai clear` is used

## Supported Versions

Only the latest version of MC-AIChat is supported for security updates.
