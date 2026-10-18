#!/usr/bin/env bash
# Builds the website, the guide and the API docs, into build/site.
# Requires asciidoctor and rouge (gem install asciidoctor rouge).
# Deployed by .github/workflows/docs.yml.
set -euo pipefail

cd "$(dirname "$0")/.."

version=$(sed -n 's/^VERSION_NAME=//p' gradle.properties)

rm -rf build/site
./gradlew :dokkaGenerate

mkdir -p build/site
# Fail on warnings, e.g. a sample tag that doesn't exist anymore.
asciidoctor \
  --failure-level WARN \
  --attribute winter-version="$version" \
  --destination-dir build/site \
  doc/index.adoc
cp -R build/dokka/html build/site/api

echo "Website built in build/site"
