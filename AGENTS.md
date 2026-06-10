# AI Agent Guidelines for voidwalk Project

This document provides essential guidelines for AI coding agents working on the `voidwalk` project. Adhering to these guidelines will ensure efficient and effective contributions.

## Project Overview

`voidwalk` is a Java project built with Gradle. The project structure follows standard conventions for Gradle-based applications.

## Key Directories and Files

- `src/main/java/`: Contains the main Java source code.
- `src/main/resources/`: Contains project resources (e.g., assets, configuration files).
- `build.gradle`: The main Gradle build script, defining project dependencies, tasks, and configurations.
- `gradlew`, `gradlew.bat`: Gradle wrapper scripts for executing Gradle commands without a local Gradle installation.
- `run/`: Contains various runtime files, logs, and configuration for development and testing.

## Build and Run Commands

The project uses Gradle for building and managing dependencies.

- **Build the project**:
  ```bash
  ./gradlew build
  ```
- **Run the project (development environment)**:
  ```bash
  ./gradlew run
  ```
- **Generate data (if applicable)**:
  ```bash
  ./gradlew runDatagen
  ```

## Code Conventions

- Follow standard Java coding conventions.
- Ensure proper use of logging for debugging and operational insights.
- Adhere to existing patterns and architectural decisions within the codebase.

## Dependency Management

Dependencies are managed in `build.gradle`. When adding new dependencies, ensure they are correctly declared with appropriate versions.

## Testing

- Locate existing test files in `src/test/java/` (if present).
- When implementing new features, consider adding corresponding unit or integration tests.

## Debugging

- Utilize the `run/logs/latest.log` file for runtime logs and error messages.
- Familiarize yourself with the project's logging mechanisms to effectively diagnose issues.

