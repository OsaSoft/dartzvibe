# Contributing to DartzVibe

Thanks for your interest in contributing to DartzVibe! Here's how you can help.

## Reporting Bugs

Open a [GitHub Issue](https://github.com/OsaSoft/dartzvibe/issues) with:
- What you expected to happen
- What actually happened
- Steps to reproduce
- Device/OS info if relevant

## Suggesting Features

Open a [GitHub Issue](https://github.com/OsaSoft/dartzvibe/issues) and describe the feature you'd like to see and why it would be useful.

## Submitting Code Changes

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Run formatting: `./gradlew spotlessApply`
5. Run tests: `./gradlew :composeApp:allTests`
6. Open a Pull Request against `main`

### Code Style

- Run `./gradlew spotlessApply` before committing — the CI will reject unformatted code
- Use Kotlin idioms: `val` over `var`, collection functions over loops, exhaustive `when`
- Follow existing patterns in the codebase

### Testing

- Add tests for new game logic using Kotest `FreeSpec`
- Test names should start with "Should"
- Structure tests with GIVEN, WHEN, THEN comments

## License

By contributing, you agree that your contributions will be licensed under the [EUPL 1.2](LICENSE).
