#!/bin/bash

# Usage: ./deploy/deploy_gae.sh <PROJECT_ID> [VERSION]
# Example: ./deploy/deploy_gae.sh my-gcp-project
# Example: ./deploy/deploy_gae.sh my-gcp-project v1.2.3

if [ $# -lt 1 ]; then
    echo "Error: PROJECT_ID is required"
    echo "Usage: $0 <PROJECT_ID> [VERSION]"
    echo "Examples:"
    echo "  $0 my-gcp-project                    # Uses timestamp version"
    echo "  $0 my-gcp-project v1.2.3            # Uses specified version"
    exit 1
fi

PROJECT_ID="$1"
VERSION="${2:-$(date +'%Y%m%d-%H%M')}"

# Step 1. Build the native image of mcp project

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


# Step 3. Deploy with gcloud

echo "Step 3: Deploying to Google App Engine..."
echo "Using version: $VERSION"

# Deploy with gcloud
gcloud app deploy "$STAGING_DIR/app.yaml" --project "$PROJECT_ID" --version "$VERSION" 
