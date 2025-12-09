#!/bin/bash


./scripts/build_native.sh random

# Check if build succeeded
if [ $? -ne 0 ]; then
    echo "Build failed. Exiting..."
    exit 1
fi

./scripts/run_native.sh random http