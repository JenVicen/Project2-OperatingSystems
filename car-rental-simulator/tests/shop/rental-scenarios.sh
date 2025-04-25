#!/usr/bin/env bash
CLASSES="$1"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "--> Scenario shop1 (SanJose)"

pushd "$SCRIPT_DIR" > /dev/null
LOT_DIR="$(dirname "$SCRIPT_DIR")/lot"

java -cp "$CLASSES" carrental.RentalShop \
    --location=SanJose --spaces-available=5 --lots="$LOT_DIR/Central" \
    < shop1-commands.txt \
    > shop1-output.txt

echo "   Output saved to $SCRIPT_DIR/shop1-output.txt"

popd > /dev/null
echo

echo "--> Scenario shop2 (Alajuela)"
pushd "$SCRIPT_DIR" > /dev/null

java -cp "$CLASSES" carrental.RentalShop \
    --location=Alajuela --spaces-available=5 --lots="$LOT_DIR/Central,$LOT_DIR/North" \
    < shop2-commands.txt \
    > shop2-output.txt

echo "   Output saved to $SCRIPT_DIR/shop2-output.txt"

popd > /dev/null
