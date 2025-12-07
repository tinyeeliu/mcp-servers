#!/bin/bash

# Step 1. Build the native image of mcp project

./scripts/build_native.sh

# Step 2. Make a staging directory in target
# Copy the image to the staging directory
# Copy the profile app.yaml to the staging directory

IMPLEMENT ME


# Step 3. Deploy with gcloud

# Example gcloud deploy command
# gcloud app deploy ${CI_PROJECT_DIR}/appengine-staging/app.yaml --quiet --project $PROJECT_ID --version $CI_COMMIT_TAG $flags

IMPLEMENT ME
