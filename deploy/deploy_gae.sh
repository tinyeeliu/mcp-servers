#!/bin/bash

# Usage: ./deploy/deploy_gae.sh <PROJECT_ID> [VERSION] [--debug]
# Example: ./deploy/deploy_gae.sh my-gcp-project
# Example: ./deploy/deploy_gae.sh my-gcp-project v1.2.3
# Example: ./deploy/deploy_gae.sh my-gcp-project --debug

# Parse arguments
DEBUG_MODE=false
PROJECT_ID=""
VERSION=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --debug)
            DEBUG_MODE=true
            shift
            ;;
        *)
            if [ -z "$PROJECT_ID" ]; then
                PROJECT_ID="$1"
            elif [ -z "$VERSION" ]; then
                VERSION="$1"
            else
                echo "Error: Too many arguments"
                echo "Usage: $0 <PROJECT_ID> [VERSION] [--debug]"
                echo "Examples:"
                echo "  $0 my-gcp-project                    # Uses timestamp version"
                echo "  $0 my-gcp-project v1.2.3            # Uses specified version"
                echo "  $0 my-gcp-project --debug           # Runs locally for debugging"
                exit 1
            fi
            shift
            ;;
    esac
done

if [ -z "$PROJECT_ID" ] && [ "$DEBUG_MODE" = false ]; then
    echo "Error: PROJECT_ID is required when not in debug mode"
    echo "Usage: $0 <PROJECT_ID> [VERSION] [--debug]"
    echo "Examples:"
    echo "  $0 my-gcp-project                    # Uses timestamp version"
    echo "  $0 my-gcp-project v1.2.3            # Uses specified version"
    echo "  $0 my-gcp-project --debug           # Runs locally for debugging"
    exit 1
fi

if [ "$DEBUG_MODE" = false ]; then
    VERSION="${VERSION:-$(date +'%Y%m%d-%H%M')}"
fi

# Step 1. Build the modules and mcp project
./scripts/build.sh

# Step 2. Make a staging directory in target
# Copy the image to the staging directory
# Copy the profile app.yaml to the staging directory

echo "Step 2: Creating staging directory and copying files..."

# Create staging directory in target
STAGING_DIR="projects/mcp/target/appengine-staging"
mkdir -p "$STAGING_DIR"

rm "$STAGING_DIR/*"

# Copy the native image and rename it to 'application' (as expected by entrypoint)
cp "projects/mcp/target/mcp-service-1.0.0.jar" "$STAGING_DIR/mcp-service-1.0.0.jar"

# Copy the app.yaml configuration
cp "deploy/profiles/mcp/standard.yaml" "$STAGING_DIR/app.yaml"

# Make the application executable
chmod +x "$STAGING_DIR/application"

echo "Staging directory created at: $STAGING_DIR"


# Step 3. Deploy or run locally

if [ "$DEBUG_MODE" = true ]; then
    echo "Step 3: Running application locally for debugging..."
    echo "JAR file: $STAGING_DIR/mcp-service-1.0.0.jar"

    # Run the JAR file locally
    java -DHTTP_PREFIX=/api/v2/mcp -jar "$STAGING_DIR/mcp-service-1.0.0.jar"
else
    echo "Step 3: Deploying to Google App Engine..."
    echo "Using version: $VERSION"

    # Deploy with gcloud
    gcloud app deploy "$STAGING_DIR/app.yaml" --project "$PROJECT_ID" --version "$VERSION"
fi 
