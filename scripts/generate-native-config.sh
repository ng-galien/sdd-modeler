#!/usr/bin/env bash
set -euo pipefail

# Quick helper to build the app jar and run a tiny set of commands under the
# native-image-agent to collect reflection/resource metadata for GraalVM.
# Usage: ./scripts/generate-native-config.sh [--jar path-to-jar]

JAR_PATH=""
if [ "$#" -gt 0 ]; then
  JAR_PATH="$1"
else
  # Try to find the built jar in build/libs
  JAR_PATH=$(ls state-modeler-app/build/libs/*.jar 2>/dev/null | grep -v '\(-sources\|-javadoc\)\.jar' | head -n 1 || true)
fi

if [ -z "${JAR_PATH}" ]; then
  echo "Jar not found in state-modeler-app/build/libs; running build to create it..."
  ./gradlew :state-modeler-app:clean :state-modeler-app:jar
  JAR_PATH=$(ls state-modeler-app/build/libs/*.jar 2>/dev/null | grep -v '\(-sources\|-javadoc\)\.jar' | head -n 1 || true)
fi

if [ -z "${JAR_PATH}" ]; then
  echo "Error: could not find JAR for state-modeler-app in build/libs/" >&2
  exit 1
fi

./gradlew :state-modeler-app:clean :state-modeler-app:jar
mkdir -p tmp/native-config

# Run representative commands to exercise reflection paths
echo "Using JAR: ${JAR_PATH}"
java -agentlib:native-image-agent=config-output-dir=tmp/native-config -jar "${JAR_PATH}" --help || true
java -agentlib:native-image-agent=config-output-dir=tmp/native-config -jar "${JAR_PATH}" validate scripts/examples/orders-sdd-mini-model.yaml || true

# Print files collected
echo "Collected native-image files in tmp/native-config:" 1>&2
ls -la tmp/native-config || true

echo "Copying to state-modeler-app/src/main/resources/META-INF/native-image/" 1>&2
mkdir -p state-modeler-app/src/main/resources/META-INF/native-image
cp -r tmp/native-config/* state-modeler-app/src/main/resources/META-INF/native-image/ || true

echo "Done. Inspect and commit the generated configs before building native image." 1>&2
