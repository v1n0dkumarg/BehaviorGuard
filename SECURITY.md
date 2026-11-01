# Security Policy

## Scope

BehaviorGuard is a defensive local portfolio system using fictional identities, synthetic behavior, and deterministic risk calculations. Do not send real identity data, credentials, or security telemetry to it.

## Reporting

Do not publish secrets or sensitive data in a public issue. Use GitHub private vulnerability reporting when available. Include the affected version, local reproduction steps, and impact.

## Deployment

The checked-in users and passwords are local examples. The app binds to loopback in Compose and uses HTTP Basic for local testing. Before any non-local deployment, replace demo users with OIDC-backed identity, require TLS, use managed secret storage, and review audit and network controls. Risk scoring must remain separate from any AI explanation layer.
