#!/usr/bin/env bash
BASE="$(pwd)"
CLASSES="$BASE/target/classes"

echo "=== Executing tests from lot ==="
bash "$BASE/tests/lot/lot-scenarios.sh" "$CLASSES"

echo "=== Executing tests from shop ==="
bash "$BASE/tests/shop/rental-scenarios.sh" "$CLASSES"