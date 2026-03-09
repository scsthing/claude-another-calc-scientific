#!/usr/bin/env bash
# build.sh  — Compile and package the Scientific Calculator without requiring Maven plugins.
set -e

SRC_DIR="src/main/java"
OUT_DIR="target/classes"
JAR_OUT="target/scientific-calculator.jar"
MAIN_CLASS="com.calculator.ScientificCalculator"

echo "==> Cleaning target directory..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "==> Compiling Java sources..."
javac -encoding UTF-8 -d "$OUT_DIR" \
  "$SRC_DIR/com/calculator/CalculatorEngine.java"  \
  "$SRC_DIR/com/calculator/CalculatorUI.java"      \
  "$SRC_DIR/com/calculator/ScientificCalculator.java"

echo "==> Packaging JAR..."
MANIFEST_FILE="$(mktemp)"
printf "Manifest-Version: 1.0\nMain-Class: %s\n" "$MAIN_CLASS" > "$MANIFEST_FILE"
jar cfm "$JAR_OUT" "$MANIFEST_FILE" -C "$OUT_DIR" .
rm "$MANIFEST_FILE"

echo "==> Build complete: $JAR_OUT"
echo ""
echo "Run with:  java -jar $JAR_OUT"
