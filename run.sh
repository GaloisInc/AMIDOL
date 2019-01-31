#! /bin/bash

set -e

# Build ui/index.html
pushd ui
elm make src/Main.elm
popd

# Build and launch server on http://localhost:8080
pushd ir
sbt compile
sbt run
popd

