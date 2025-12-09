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

# Build and run packaged JAR
./scripts/build_module.sh

# Check if build succeeded
if [ $? -ne 0 ]; then
    echo "Build failed. Exiting..."
    exit 1
fi

# Change to project root directory
cd "$(dirname "$0")/.." || exit 1

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

echo "Running tests for module: $MODULE_NAME"
echo ""

# Common Maven options:
# -Dsurefire.useFile=false : Print test output to console instead of files
# This allows debug logs (written to stderr) to be visible
MAVEN_OPTS="-Dsurefire.useFile=false"

# Run tests with optional test class filter
if [ -n "$TEST_CLASS" ]; then
    echo "Running specific test: $TEST_CLASS"
    mvn test -pl "$MODULE_PATH" -Dtest="$TEST_CLASS" $MAVEN_OPTS
else
    echo "Running all tests in module"
    mvn test -pl "$MODULE_PATH" $MAVEN_OPTS
fi
