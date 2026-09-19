# Security policy

## Do not commit secrets

Do not commit API tokens, cookies, passwords, signing keys, `local.properties`, release-signing properties, `.env` files, private certificates, or personal device logs. Use the example configuration files and environment variables instead.

If a secret is ever committed, revoke or rotate it first, then remove it from the full Git history. Deleting the current file alone does not invalidate a leaked secret.

## Support scope

This personal project does not provide a user support or vulnerability response service, and does not promise fixes or response times. Do not publish credentials or personal data. Use the HTTP service only on trusted networks.
