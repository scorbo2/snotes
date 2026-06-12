# Snotes

## Build & Run

```bash
mvn clean package
java -jar target/snotes-2.2.jar
```

Run with custom settings directory:
```bash
java -DSETTINGS_DIR=/path/to/settings -jar target/snotes-2.2.jar
```

## Tests

```bash
mvn test
```

Single test:
```bash
mvn test -Dtest=TagTest
```

## Architecture

- **Entry point:** `ca.corbett.snotes.Main`
- **Desktop UI:** Java Swing + [swing-extras](https://github.com/scorbo2/swing-extras) (v3.0.0-SNAPSHOT) + FlatLaf
- **Packaging:** Maven copies runtime deps to `target/lib/` via `maven-dependency-plugin`. The jar manifest sets `lib/` as classpath prefix and `ca.corbett.snotes.Main` as main class.
- **Settings directory:** `~/.Snotes` by default (overridable via `SETTINGS_DIR` system property or `Version.SETTINGS_DIR`). Contains `Snotes.props`, `data/`, `extensions/`, `logging.properties`.
- **Extensions:** Loaded from jar files in `${SETTINGS_DIR}/extensions` at runtime. Define via `SnotesExtension` / `SnotesExtensionManager`.
- **Update sources:** `update_sources.json` enables dynamic extension discovery/download.

## Key directories

- `src/main/java/ca/corbett/snotes/ui/` — UI components and action handlers
- `src/main/java/ca/corbett/snotes/model/` — domain model (Note, Tag, Query, Template, DateTag, YMDDate)
- `src/main/java/ca/corbett/snotes/model/filter/` — search filter implementations
- `src/main/java/ca/corbett/snotes/io/` — data persistence (DataManager, SnotesIO, LoaderThread)
- `src/main/java/ca/corbett/snotes/extensions/` — extension API
- `src/test/java/` — JUnit 5 tests mirroring main structure

## Installer profile

On Linux, if `~/bin/make-installer` exists, the `make-installer` Maven profile activates automatically and runs the installer script after packaging. Override manually:
```bash
mvn package -Pmake-installer
```

Edit `installer.props` to customize the installer output.

## Config conventions

- Java: 4-space indent, end-of-line braces, no trailing whitespace. `.editorconfig` has full IntelliJ IDEA formatter settings.
- No lint/typecheck step — plain Maven compile.
- No CI, no pre-commit hooks.
