#!/usr/bin/env bash
# tests/lot/lot-scenarios.sh

CLASSES="$1"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "--> Test1: create Central lot"
pushd "$SCRIPT_DIR" > /dev/null

java -cp "$CLASSES" carrental.LotManager \
    --lot-name=Central --add-sedan=2 --add-suv=1
echo "   Central.txt now in $SCRIPT_DIR:"
cat Central.txt

popd > /dev/null
echo

echo "--> Test2: remove XYZ-999"
pushd "$SCRIPT_DIR" > /dev/null

java -cp "$CLASSES" carrental.LotManager \
    --lot-name=Central --remove-vehicle=XYZ-999
echo "   Central.txt now in $SCRIPT_DIR:"
cat Central.txt

popd > /dev/null
echo

echo "--> Test3: create North lot"
pushd "$SCRIPT_DIR" > /dev/null

java -cp "$CLASSES" carrental.LotManager --lot-name=North --add-sedan=5 --add-suv=1 -add-van=2
echo "   North.txt now in $SCRIPT_DIR:"
cat North.txt

popd > /dev/null
