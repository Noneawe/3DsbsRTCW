#!/bin/sh
# Entry point for the whole build: ./gradlew assembleRelease  (delegates to the Android project in app/)
ROOT=$(cd "$(dirname "$0")" && pwd)

# the bundled Gradle 8.1.1 does not run on JDK 21+: prefer a JDK 17
if [ -z "$JAVA_HOME" ]; then
    for d in /usr/lib/jvm/java-17-openjdk-* /usr/lib/jvm/temurin-17-*; do
        [ -d "$d" ] && JAVA_HOME=$d
    done
    export JAVA_HOME
fi
if [ -z "$ANDROID_HOME" ] && [ -d "$HOME/Android/Sdk" ]; then
    export ANDROID_HOME=$HOME/Android/Sdk
fi

exec "$ROOT/app/gradlew" -p "$ROOT/app" "$@"
