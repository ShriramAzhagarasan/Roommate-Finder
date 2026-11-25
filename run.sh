#!/usr/bin/env bash
# Compiles and launches Roommate Finder without Eclipse.
# Point JAVAFX_HOME at your JavaFX SDK (the folder that contains lib/), e.g.
#   JAVAFX_HOME=~/javafx-sdk-23.0.1 ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

JAVAFX_HOME="${JAVAFX_HOME:-$HOME/Desktop/Projects/javafx-sdk-23.0.1}"
FX_LIB="$JAVAFX_HOME/lib"
if [ ! -f "$FX_LIB/javafx.controls.jar" ]; then
    echo "JavaFX SDK not found at $JAVAFX_HOME. Set JAVAFX_HOME to your javafx-sdk folder." >&2
    exit 1
fi

rm -rf out && mkdir -p out
javac --module-path "$FX_LIB" --add-modules javafx.controls,javafx.fxml -d out $(find src -name '*.java')
rsync -a --exclude '*.java' --exclude '.DS_Store' src/ out/   # FXML, CSS and images
exec java --enable-native-access=javafx.graphics --module-path "$FX_LIB" --add-modules javafx.controls,javafx.fxml \
    -cp out application.Main "$@"
