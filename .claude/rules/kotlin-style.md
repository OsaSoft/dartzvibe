---
paths:
  - "**/*.kt"
  - "**/*.kts"
  - ".editorconfig"
---

# Code style

ktlint runs through Spotless. The rule overrides live in **two places that have to stay in sync**: `editorConfigOverride` in the root `build.gradle.kts`, and `.editorconfig`, which also sets the 120 max line length. The following rules are conventions for new code; lint doesn't enforce all of them, and old code doesn't always follow them.

- Imports only: no fully qualified names (use an alias import for clashes) and no wildcard imports.
- `val` over `var`; collection functions (`map`, `forEach`, `flatMap`) over `for` loops.
- An exhaustive `when` over enums and sealed types, with no `else`, so the compiler flags any new case you miss.
- Expression bodies for single-expression functions. The `=` never ends a line: `fun foo(): Int = when { … }`.
- Named arguments on multi-line calls.
- Lowercase-named composable functions, such as the `rememberXScreenModel` factories, carry `@Suppress("ktlint:standard:function-naming")`; follow the existing code.
