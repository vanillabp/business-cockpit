# Commons

Spring Boot functionality the Business Cockpit uses and nothing about the cockpit itself: the JWT
handling and the user context behind it, the MongoDB helpers for change sets, change streams and
converters, and a REST client adapter with its bearer, OAuth, TLS and versioning pieces. A workflow
module or another service may depend on it for the same reasons.

Built as part of the reactor build described in the [root README](../README.md#building-it), and
published as `io.vanillabp.businesscockpit:commons` to Maven Central, snapshots to GitHub Packages.
