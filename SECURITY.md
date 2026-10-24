# Security and private data

Please do not publish passwords, signing keys, API credentials, or personal data in public issues. Since this launcher stores diagnostic reports locally, inspect a report before sharing it and remove any private information first. This repository's GitHub Actions workflow currently builds a debug-signed APK for testing; production signing credentials must be kept in GitHub Actions Secrets, never in source control.
