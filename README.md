# StarLion

StarLion is a Java application for visualizing RDF/RDFS ontologies as graphs.

It now supports an incremental backend migration from SWKM to Apache Jena, with runtime modes that let you run legacy behavior, comparison behavior, or Jena-first behavior.

## 1) First-Time Setup on a New PC

### Prerequisites

- JDK 11 or newer installed up to jdk 17 (`java -version`)
- Internet access for first dependency download
- Git (optional, only if cloning)

### Get the project

- Clone the repository or copy the project folder.
- Open a terminal in the project root (where `gradlew` and `build.gradle` are).
- Ensure Gradle wrapper files exist, especially:
  - `gradlew` / `gradlew.bat`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradle/wrapper/gradle-wrapper.jar`

If `gradle/wrapper/gradle-wrapper.jar` is missing, regenerate wrapper files on a machine with Gradle installed:

```bash
gradle wrapper --gradle-version 8.13
```

### Verify environment

```bash
# Linux/macOS
./gradlew --version
```

```bat
:: Windows
gradlew.bat --version
```

## 2) Build the Project

```bash
# Linux/macOS
./gradlew clean build
```

```bat
:: Windows
gradlew.bat clean build
```

If dependencies fail to resolve:

```bash
./gradlew build --refresh-dependencies
```

## 3) Run the App (Default Mode)

Default backend mode is `JENA` unless overridden.

```bash
# Linux/macOS
./gradlew run
```

```bat
:: Windows
gradlew.bat run
```

## 4) Run with Specific Backend Mode

StarLion supports 3 modes via `starlion.backend.mode`.

- `SWKM` -> legacy backend behavior
- `DUAL` -> runs both and compares SWKM vs Jena outputs (parity-oriented mode) , if swkm breaks on file parsing this breaks too
- `JENA` -> Jena-first parsing, with SWKM fallback for unsupported inputs (this doesnt break if swkm doesnt work because its for fallback only)

### Linux/macOS

```bash
./gradlew -Dstarlion.backend.mode=SWKM run
./gradlew -Dstarlion.backend.mode=DUAL run
./gradlew -Dstarlion.backend.mode=JENA run
```

### Windows

```bat
gradlew.bat -Dstarlion.backend.mode=SWKM run
gradlew.bat -Dstarlion.backend.mode=DUAL run
gradlew.bat -Dstarlion.backend.mode=JENA run
```

## 5) Current Jena Migration Behavior

- In `JENA` mode, StarLion attempts Jena parsing first.
- If Jena parses successfully, SWKM read is skipped.
- If Jena cannot parse a file, SWKM fallback is used.
- `.ttl` and `.owl` are included in the supported project flow.

## 6) How Migration Was Implemented (Adapter + Canonical Layer)

To avoid a risky full rewrite, StarLion uses a canonical mapping contract:

- Canonical DTOs in `src/mapping/canonical`
  - `CanonicalRdfSnapshot`
  - `CanonicalGraphSnapshot`
  - `CanonicalNode`
  - `CanonicalEdge`
- Adapters in `src/mapping/adapter`
  - `JenaCanonicalAdapter`
  - `SwkmCanonicalAdapter`
  - `CanonicalNameUtils`

This lets services/controllers consume one stable format, regardless of whether data came from SWKM or Jena.

## 7) Build a Runnable Package

### Option A: Fat JAR

```bash
# Linux/macOS
./gradlew fatJar
```

```bat
:: Windows
gradlew.bat fatJar
```

Run it:

```bash
java -jar build/libs/StarLion-1.0.0-all.jar
```

Run fat JAR with a specific backend mode:

```bash
# Linux/macOS
java -Dstarlion.backend.mode=SWKM -jar build/libs/StarLion-1.0.0-all.jar
java -Dstarlion.backend.mode=DUAL -jar build/libs/StarLion-1.0.0-all.jar
java -Dstarlion.backend.mode=JENA -jar build/libs/StarLion-1.0.0-all.jar
```

```bat
:: Windows
java -Dstarlion.backend.mode=SWKM -jar build\libs\StarLion-1.0.0-all.jar
java -Dstarlion.backend.mode=DUAL -jar build\libs\StarLion-1.0.0-all.jar
java -Dstarlion.backend.mode=JENA -jar build\libs\StarLion-1.0.0-all.jar
```

### Option B: Release Bundle (recommended for sharing)

```bash
./scripts/package-release.sh
```

Output:

- `release/starlion-app/bin/run.sh` (Linux/macOS)
- `release/starlion-app/bin/run-jena.sh` (Linux/macOS, JENA mode)
- `release/starlion-app/bin/run.bat` (Windows)
- `release/starlion-app/bin/run-jena.bat` (Windows, JENA mode)
- `release/starlion-app/lib/StarLion-all.jar`

## 8) Key Features

- RDF/RDFS graph visualization
- Force-directed layout
- Top-K exploration modes
- Namespace-aware visualization
- Star-graph exploration

## 9) Publications

- Stamatis Zampetakis, Yannis Tzitzikas, Asterios Leonidis, Dimitris Kotzinos, "Star-like auto-configurable layouts of variable radius for visualizing and exploring RDF/S ontologies", Journal of Visual Languages & Computing, 2012.
- Stamatis Zampetakis, Yannis Tzitzikas, Asterios Leonidis, Dimitris Kotzinos, "StarLion: Auto-Configurable Layouts for Exploring Ontologies", ESWC 2010 (Demo Track).
- Yannis Tzitzikas, Dimitris Kotzinos, Yannis Theoharis, "On Ranking RDF Schema Elements (and its Application in Visualization)", JUCS, 2007.

## 10) More Information

Project page: https://projects.ics.forth.gr/isl/starlion
