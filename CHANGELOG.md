# Changelog

All notable changes to Serial Talker Logger will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-09-19

### Added (1.1.0)

- JDK 25 build target with pinned compiler and dependency-plugin versions.
- Unit tests for `MessageFormatter`, `CommandHistory`, `AutoLogManager`, and `ConfigurationManager`.
- CI matrix covering JDK 21 and JDK 25 under Xvfb.
- Structured session/event model planned for future releases.

### Changed (1.1.0)

- Updated README to reflect Java 25 requirements and actual build/run commands.
- Reconciled CHANGELOG entries with features present in the source tree.

## [1.0.1] - 2026-01-20

### Fixed (1.0.1)

- Fix release build failures by correcting JUnit Jupiter version to 5.10.2.
- Configure Surefire for headless testing (`java.awt.headless=true`).
- Skip GUI tests in headless environments to stabilize CI builds.

## [1.0.0] - 2026-01-19

### Added (1.0.0)

- Color-coded message display (TX in blue, RX in black, system messages in gray)
- GitHub Actions CI/CD with automated release workflow
- Support for multiple Java versions in CI pipeline
- Dependabot integration for automatic dependency updates

### Changed (1.0.0)

- Upgraded to Java 21 as primary target
- Updated JUnit Jupiter to 5.10.2
- Updated Maven plugins: maven-jar-plugin to 3.5.0, maven-shade-plugin to 3.6.1
- Improved README with installation instructions for all platforms

### Fixed (1.0.0)

- Improved exception handling for document operations in GUI

## [0.9.0] - 2025-12-15

### Added (0.9.0)

- Initial release with basic serial communication features
- Real-time port detection
- Data logging and export (CSV, JSON)
- Keyboard shortcuts for common operations
- Auto-baud rate negotiation
- Command history in input field

### Features (0.9.0)

- Modern Java Swing-based GUI
- jssc library for cross-platform serial communication
- SLF4J logging framework
- Configurable serial parameters (baud rate, data bits, stop bits, parity)
