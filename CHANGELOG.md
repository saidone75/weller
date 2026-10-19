# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Layered `weller.edn` configuration files with deep merging and environment variable placeholders.
- Automatic CRAL configuration and Alfresco ticket validation and renewal through `weller.config/ticket`.
- Node path predicates supporting exact strings and regular expressions.
- `remove-taps` to stop a pipe and remove its processing taps.
- Clojure CLI aliases and tools.build tasks for packaging, local installation, and Clojars deployment.
- Interactive `deploy.sh` script for Clojars publishing.

### Changed

- Move `make-tap` from `weller.predicates` to `weller.pipe`.
- Require pipes to be stopped before adding filtered taps.
- Run JMS reception and message callbacks on blocking workers separate from the core.async
  `go` dispatch pool, preserving sequential processing within each handler.
- Replace Leiningen with Clojure CLI and tools.build, including CI and cljdoc preview scripts.
- Update Clojure to 1.12.6, ActiveMQ to 6.3.2, data.json to 2.5.2, CRAL to 0.3.5,
  core.async to 1.9.865, and Telemere to 1.4.0.
- Update the Docker development stack to ACS and Share 25.3.0, Search Services 2.0.17,
  and PostgreSQL 18, and increase Solr memory.
- Standardize GPL license headers with SPDX identifiers and copyright years.
- Expand configuration, development, build, and publishing documentation.

### Fixed

- Wake and stop waiting message handlers through a dedicated stop channel, allowing
  restarts without leaving previous consumers active.
- Log and discard malformed JSON, non-object JSON values, and unsupported JMS message
  types without stopping the listener.
- Keep handlers processing subsequent messages after callback exceptions, without
  automatically retrying failed callbacks.
- Disconnect, close, and drain removed tap channels to release pending `mult`
  deliveries and prevent message distribution from stalling.

### Removed

- Unused Component dependency and immuconf dependency.

## [0.1.0] - 2024-05-20

### Added

- Initial Clojars release of `org.saidone/weller`, providing Alfresco out-of-process extensions in Clojure.
- ActiveMQ event listener and core.async message pipes with filtered processing taps.
- Composable predicates for Alfresco events and node resources.
- Component lifecycle operations for starting and stopping message processing.
- EDN configuration for Alfresco and ActiveMQ connections.
- Integration tests and a Docker-based Alfresco development environment.

This release was published to [Clojars](https://clojars.org/org.saidone/weller/versions/0.1.0)
on 2024-05-20. Both the published POM's SCM tag and the JAR's `pom.properties`
identify commit [`7cc85880b396c334526afe769a2b516d2d82759b`](https://github.com/saidone75/weller/commit/7cc85880b396c334526afe769a2b516d2d82759b).
The GitHub tag and release were added retrospectively for that exact commit.

[Unreleased]: https://github.com/saidone75/weller/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/saidone75/weller/releases/tag/v0.1.0
