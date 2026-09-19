# Security policy

## Do not commit secrets

Do not commit API tokens, cookies, passwords, signing keys, `local.properties`, release-signing properties, `.env` files, private certificates, or personal device logs. Use the example configuration files and environment variables instead.

If a secret is ever committed, revoke or rotate it first, then remove it from the full Git history. Deleting the current file alone does not invalidate a leaked secret.

## Reporting a problem

For a security issue, do not publish working credentials or exploit details in a public issue. Contact the repository maintainer privately through the GitHub account that owns this repository and include a minimal reproduction, affected version, and mitigation suggestion.

This project is a personal open-source project and does not promise a response time or a security support level.
