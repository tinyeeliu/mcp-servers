#!/bin/bash

# Give a module name, build and package the module into a jar.

if [ $# -eq 0 ]; then
    echo "Usage: $0 <module_name>"
    echo "Example: $0 random"
    exit 1
fi

MODULE_NAME=$1
MODULE_PATH="modules/${MODULE_NAME}"

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

echo "Building module: $MODULE_NAME"
cd "$(dirname "$0")/.." || exit 1
mvn clean package -pl "$MODULE_PATH" -am

if [ $? -eq 0 ]; then
    echo "Successfully built module: $MODULE_NAME"
    echo "JAR location: $MODULE_PATH/target/mcp-${MODULE_NAME}-1.0.0-SNAPSHOT.jar"
else
    echo "Failed to build module: $MODULE_NAME"
    exit 1
fi