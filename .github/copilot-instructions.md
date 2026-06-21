# Snotes – Copilot Instructions

## Build & Run

```bash
mvn clean package
java -jar target/snotes-2.2.jar
```

Run with a custom settings directory:
```bash
java -DSETTINGS_DIR=/path/to/settings -jar target/snotes-2.2.jar
```

## Tests

```bash
mvn test                        # full suite
mvn test -Dtest=TagTest         # single test class
mvn test -Dtest=TagTest#myMethod  # single test method
```

Tests are JUnit 5 (jupiter) + Mockito. Test classes live in `src/test/java/` and mirror the main package structure.

## Architecture

Snotes is a Java Swing desktop note-taking app. Notes are plain-text files stored under a configurable data directory, organized by date or in a `static/` folder for undated notes.

### Package layout

| Package | Purpose |
|---|---|
| `ca.corbett.snotes` | Entry point (`Main`), app config (`AppConfig`), version info (`Version`) |
| `ca.corbett.snotes.model` | Domain model: `Note`, `Tag`, `TagList`, `DateTag`, `YMDDate`, `Query`, `Template` |
| `ca.corbett.snotes.model.filter` | `Filter` hierarchy used for search/query |
| `ca.corbett.snotes.io` | Persistence: `DataManager` (public API), `SnotesIO` (package-private), `LoaderThread` |
| `ca.corbett.snotes.ui` | Swing UI: `MainWindow`, `WriterFrame`, `ReaderFrame`, action panel |
| `ca.corbett.snotes.ui.actions` | `EnhancedAction` subclasses wired into the action panel |
| `ca.corbett.snotes.extensions` | Extension API: `SnotesExtension`, `SnotesExtensionManager` |

### Key design points

- **`AppConfig`** extends `AppProperties<SnotesExtension>` (from `swing-extras`). It is a singleton that owns all persistent preferences and exposes the PropertiesDialog. All settings are stored in `${SETTINGS_DIR}/Snotes.props`.
- **`DataManager`** is the only public IO entry point. `SnotesIO` is package-private and must not be called directly from outside `ca.corbett.snotes.io`. `DataManager` also holds `METADATA_DIR`, `STATIC_DIR`, and `SCRATCH_DIR` constants for special sub-directories.
- **`Filter`** subclasses use Jackson `@JsonSubTypes` for polymorphic serialization. When adding a new filter, register it in the `@JsonSubTypes` annotation on `Filter`.
- **`SnotesExtension`** extends `AppExtension` (swing-extras). Extensions are loaded at runtime from jar files placed in `${SETTINGS_DIR}/extensions/`. Extensions can contribute `ActionGroup`s, extra actions in built-in groups (`READ`, `WRITE`, `OPTIONS`), and `LogConsoleStyle`s.
- **UI actions** extend `EnhancedAction` and are grouped with `ActionGroup`. Built-in group names are constants on `ActionGroup` (`READ`, `WRITE`, `OPTIONS`).
- **File naming**: dated notes live at `{dataDir}/{yyyy}/{MM}/{dd}/{tag1}_{tag2}.txt`; undated notes at `{dataDir}/static/{tag1}_{tag2}.txt`; scratch notes in `.scratch/`.

### External dependencies

- **`swing-extras` v3.0.0** – provides `AppProperties`, `AppExtension`, `EnhancedAction`, `ActionPanel`, `LookAndFeelManager`, form fields, progress dialogs, and more. Documentation: https://www.corbett.ca/swing-extras-book/
- **`FlatLaf`** – Look and Feel
- **Jackson** – JSON serialization for `Query`/`Filter` persistence

## Key Conventions

- **Java 25**, 4-space indent, end-of-line braces, no trailing whitespace. See `.editorconfig` for full IntelliJ formatter settings.
- **`Tag` is immutable and always lower-case.** The constructor normalizes input (spaces/slashes/backslashes/`#` → `_`). `Tag.toString()` returns `"#value"`; `Tag.getTag()` returns the raw value without the hash.
- **`DateTag` extends `Tag`** and matches the `yyyy-MM-dd` format. A `Note` may have at most one `DateTag`.
- **Logging**: use `java.util.logging` (`Logger.getLogger(ClassName.class.getName())`). No SLF4J or Log4j.
- **No lint/typecheck step** — plain `mvn compile`.
- **`Version.SETTINGS_DIR`** is computed at class-load time from the `SETTINGS_DIR` system property (default `~/.Snotes`). Other paths (`EXTENSIONS_DIR`, `INSTALL_DIR`, `UPDATE_SOURCES_FILE`) follow the same pattern in `Version`.

## Installer (Linux only)

If `~/bin/make-installer` exists, the `make-installer` Maven profile activates automatically on `mvn package`. To force it:
```bash
mvn package -Pmake-installer
```
Edit `installer.props` to customize the output.
