#!/bin/bash

# Give a module name, run the unit tests cases

if [ $# -eq 0 ]; then
    echo "Usage: $0 <module_name> [test_class]"
    echo "Example: $0 random"
    echo "Example: $0 random RandomNumberServerTest"
    exit 1
fi

MODULE_NAME=$1
TEST_CLASS=$2
MODULE_PATH="modules/${MODULE_NAME}"

# Change to project root directory
cd "$(dirname "$0")/.." || exit 1

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

echo "Running tests for module: $MODULE_NAME"
echo ""

# Run tests with optional test class filter
if [ -n "$TEST_CLASS" ]; then
    echo "Running specific test: $TEST_CLASS"
    mvn test -pl "$MODULE_PATH" -Dtest="$TEST_CLASS"
else
    echo "Running all tests in module"
    mvn test -pl "$MODULE_PATH"
fi
