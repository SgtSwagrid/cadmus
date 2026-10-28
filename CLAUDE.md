# CLAUDE.md

This file provides guidance to [Claude Code](https://claude.com/product/claude-code) when working with code in this repository.
It is not intended for human eyes.

### Maintenance

You (robot or human) have standing permission to update this file without asking.
Add important patterns, gotchas, or context that would help future sessions.
Keep it concise and actionable.

## Project overview

This is a Scala library that reads Scaladoc comments at compile time.

It is one module in `com.alecdorrington.cadmus`, cross-built for the JVM and Scala.js, whose whole interface is
`Scaladoc` (`src/main/scala/com/alecdorrington/cadmus/Scaladoc.scala`): the model of a parsed comment, the macros `of`, `ofMember`, `rawOf` and
`rawOfMember`, and the pure functions `parse` and `plain`. It is general: nothing in it serves one user alone (Iris
aligns `params` with a `Mirror`'s labels itself).

- The macros give only the comment as written (`Symbol.docstring`), and `parse` runs at the call's run time, so
  the macros stay small and need no `ToExpr`. A singleton type (a `TermRef`, as a case object's) is read from its
  term's comment, else its module class's.
- Parsed text is faithful to the markup: only delimiters, each line's margin (`\s*\* ?`, the first line trimmed)
  and the indentation a part's lines share are removed. Every text is non-blank but a generic `Tag`'s; a
  `@param`/`@tparam` saying nothing, or repeating a name, and a second `@return` go to `tags`, so nothing is lost.
  A tag opens only on a line outside a `{{{ }}}` or ```` ``` ```` example.
- `plain` drops code examples, writes `[[target label]]` links as the label (else target), and joins each
  paragraph's lines, keeping other markup. `Scaladoc.plain` (the method) drops parts left empty, but tags.
- What Scala keeps (checked, and in the README): comments of what is compiled from source in the same run, with no
  option; of a type compiled before (a library, or a file an incremental build did not recompile) only with
  `-Xread-docs`, from TASTy; never of a constructor parameter (`caseFields` and `paramSymss` both have none).
- Test the macros from outside the package (`MacroSuite` is in `com.alecdorrington.example`), with the documented
  types in the same file, as a user's code would expand them. Tests run on both platforms.
- Tests needing `-Xdrop-docs`, which holds for a whole compilation, are in `src/test-without-docs`, compiled apart
  from the others by the matrix `cadmus-without-docs`.

See [README.md](README.md) for usage.

### Where this code lives

Cadmus is developed here: make every change in this repository, by pull request. The shared configuration
(workflows, Scalafmt, IDE settings, `project/plugins-common.sbt`, `project/plugins-library.sbt`, `LICENSE.md`,
`CONTRIBUTING.md`, `docs/STYLE_GUIDE.md`) is synced into it from
[Scala Library Config](https://github.com/SgtSwagrid/scala-library-config) by
[GitHub Graph](https://github.com/SgtSwagrid/github-graph), which overwrites local edits, so change those files there.
`build.sbt`, `release.sbt`, `project/plugins-scalajs.sbt`, `README.md`, this file and the sources belong to the library.

### Build

- The library is one `projectMatrix`, `cadmus` on the JVM and `cadmusJS` in JavaScript, published as `cadmus_3` and
  `cadmus_sjs1_3`, which `cadmus-root` aggregates (publishing nothing itself, and documenting the JVM side only).
- The matrix pins `sourceDirectory` to this build's base, as sbt otherwise resolves a matrix's sources against the
  working directory, which is not this build's when another includes it.
- `cadmus-root` shares the matrix's directory, so clears its source and resource directories alike: IntelliJ would
  otherwise count it among the owners of the shared sources and highlight them by its classpath.
- `cadmus-without-docs`, a second matrix in that directory, likewise clears all but its test sources, which it sets to
  `src/test-without-docs/scala` alone. `cadmus-root` aggregates it with the library.
- Sources sit in their packages' directories (`src/main/scala/com/alecdorrington/cadmus/`), with no `packagePrefix`:
  IntelliJ's sbt import never gives a prefix to the module holding the sources the JVM and JS share, so every `package`
  line there was flagged on each fresh import.
- On Windows, run sbt as `sbt --server --batch <tasks>`. `testFull` runs every suite (`test` skips unchanged ones).

### Releases

1. Land the changes on `main` by pull request. The README's installation line names no version (it reads
   `{version}`, pointing to the latest release), so a release leaves it as it is. Cadmus is still a prototype,
   versioned by early semver (`versionScheme`): while it is `0.x`, the minor version goes up for any change that can
   break a user's build or behaviour, and the patch version for anything else.
2. Publish a GitHub release of `main` tagged `v<version>`, with notes written for a user (the installation line, then
   what is Changed, Added and Fixed). `publish.yml` then publishes `cadmus_3` and `cadmus_sjs1_3` to Maven Central
   through `sbt-ci-release`, which takes the version from the tag, and `docs.yml` publishes the Scaladoc at
   [alecdorrington.com/cadmus](https://alecdorrington.com/cadmus/).
3. Once Maven Central serves the new version, bump it in each project using Cadmus (such as Iris), by pull request
   there.

## Instructions

### Compilation and Diagnostics

- When the user asks for help with a compilation or type error, start by running `sbt compile` to see the error for yourself.
  If there are many errors, making it unclear which one the user is referring to, ask them to clarify, and then focus only on that issue.
- IntelliJ MCP integration is active. When a request seems to implicitly refer to something the user is looking at, always check
  `mcp__ide__getDiagnostics` first to see which file(s) are open and get associated diagnostics (errors, warnings, and info hints with line numbers).

#### Testing

- After making code changes, always run `sbt compile` to verify that issues are fixed and no new ones are introduced.
- Repeatedly retry upon failure until the build succeeds. If you are unsure how to fix an issue, ask for help or refer to existing code for examples.
- Before trying to fix an error, make sure you first understand it fully.
- You should never report that a feature is complete without testing it first.

### Code Style

- You must read the [Code Style Guidelines](docs/STYLE_GUIDE.md).
- Document every public type and member: a summary, then `@param` for each explicit parameter,
  `@tparam` for each type parameter, and `@return` for any result but `Unit`, each one short
  sentence. Summaries read: types "A ...", values "The ...", Booleans "Whether ...", methods a
  third-person verb ("Reads ..."), never "Returns ...". Private members get a comment only for a
  non-obvious contract or gotcha, usually in one sentence.

### Pull Requests

When asked to publish the code changes, your task is to open one or more pull requests (PRs) to merge the changes into `main` on GitHub:

- Use `git` to check what has changed as compared to the `main` branch on `origin`.
- If the changes are thematically linked, they can be published as a single PR.
- Otherwise, you'll need to divide the changes into multiple PRs using your own judgement.
- Each PR should have a singular focus, shouldn't break anything, and should be able to be merged independently.
- Ensure that all code is staged, committed and pushed. Ensure no new files are left uncommitted, and no debug code is left in the codebase.
- When creating a PR, ensure that the title and description are clear, informative, and comprehensive.
- All feature/bugfix/etc branch names should be formatted as "feature_<short description>" or "fix_<short description>" or similar.
- All PR titles should be formatted as "[<scope>] <Short summary>", e.g. "[renderer] Fixed colour inversion bug."
- Use the `gh` CLI to do the above.
