#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${1:-$ROOT_DIR/release/starlion-app}"

echo "[1/4] Building fat JAR (tests skipped for packaging)..."
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" clean fatJar -x test

echo "[2/4] Preparing release directory..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/bin" "$OUT_DIR/lib"

echo "[3/4] Copying runtime artifacts..."
JAR_PATH="$(ls "$ROOT_DIR"/build/libs/*-all.jar | head -n 1)"
cp "$JAR_PATH" "$OUT_DIR/lib/StarLion-all.jar"

if [ -d "$ROOT_DIR/SampleRDFFiles" ]; then
  cp -R "$ROOT_DIR/SampleRDFFiles" "$OUT_DIR/SampleRDFFiles"
fi

cat > "$OUT_DIR/bin/run.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
java -jar "$APP_DIR/lib/StarLion-all.jar" "$@"
EOF
chmod +x "$OUT_DIR/bin/run.sh"

cat > "$OUT_DIR/bin/run.bat" <<'EOF'
@echo off
setlocal
set APP_DIR=%~dp0..
java -jar "%APP_DIR%\lib\StarLion-all.jar" %*
EOF

cat > "$OUT_DIR/README.txt" <<'EOF'
StarLion release bundle
=======================

Run:
  ./bin/run.sh
  bin\run.bat

Optional arguments:
  ./bin/run.sh -Dstarlion.backend.mode=JENA
  bin\run.bat -Dstarlion.backend.mode=JENA

Included:
  - lib/StarLion-all.jar
  - SampleRDFFiles/ (if present in source tree)
EOF

echo "[4/4] Done."
echo "Release bundle created at: $OUT_DIR"
