#!/usr/bin/env bash
#-----------------------------------------------------------------------
#  jpackage_for_linux.sh
#
#  Builds the fat JAR with Gradle (shadowJar) then creates a native
#  Linux installer (.deb or .rpm) with jpackage.
#
#  Mirrors jpackage_for_macos.sh. Differences for Linux:
#    - package type is deb (default) or rpm, selectable
#    - icon is a .png used directly (no sips/iconutil/.icns needed)
#    - no --mac-entitlements
#    - JavaFX modules come from the SDKMAN "fx" JDK ($JAVA_HOME/jmods),
#      so no Homebrew/openjfx path is needed by default
#
#  jpackage must run ON Linux (it cannot cross-compile installers).
#
#  Prerequisites:
#    - a JDK with JavaFX bundled (e.g. sdk install java 25.fx-zulu)
#    - for .deb : dpkg, fakeroot        (Debian/Ubuntu)
#    - for .rpm : rpm-build             (Fedora/RHEL/openSUSE)
#
#  Usage:
#    ./jpackage_for_linux.sh          # builds a .deb (default)
#    ./jpackage_for_linux.sh rpm      # builds a .rpm
#    ./jpackage_for_linux.sh deb      # builds a .deb explicitly
#-----------------------------------------------------------------------

set -euo pipefail

# -------------------------------------------------------
# Package type: deb (default) or rpm
# -------------------------------------------------------
PKG_TYPE="${1:-deb}"
if [[ "$PKG_TYPE" != "deb" && "$PKG_TYPE" != "rpm" ]]; then
    echo "ERROR: package type must be 'deb' or 'rpm' (got '$PKG_TYPE')"
    exit 1
fi

# -------------------------------------------------------
# Resolve JAVA_HOME / JavaFX module path
# -------------------------------------------------------
if [[ -z "${JAVA_HOME:-}" ]]; then
    echo "ERROR: JAVA_HOME is not set. Install a JavaFX-bundled JDK, e.g.:"
    echo "       sdk install java 25.fx-zulu"
    exit 1
fi

# With a .fx-zulu JDK, the javafx.* modules live in \$JAVA_HOME/jmods.
# Override by exporting JAVAFX_LIB to an openjfx 'lib' dir if you use a
# plain JDK + separate JavaFX SDK.
MODULE_PATH="$JAVA_HOME/jmods"
if [[ -n "${JAVAFX_LIB:-}" ]]; then
    MODULE_PATH="$JAVA_HOME/jmods:$JAVAFX_LIB"
fi

# -------------------------------------------------------
# Build the fat JAR
# -------------------------------------------------------
echo "Building the fat JAR with Gradle ..."
gradle shadowJar

# -------------------------------------------------------
# Run jpackage
# -------------------------------------------------------
echo "Packaging with jpackage (--type $PKG_TYPE) ..."

jpackage \
  --type "$PKG_TYPE" \
  --name Klikr \
  --app-version 1.0 \
  --vendor "Klikr" \
  --description "Klikr JavaFX application" \
  --input build/libs \
  --main-jar klikr.jar \
  --main-class klikr.Klikr_application \
  --module-path "$MODULE_PATH" \
  --add-modules javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.media,javafx.web \
  --icon src/main/resources/icons/klikr.png \
  --java-options "-Xmx8g --enable-native-access=ALL-UNNAMED" \
  --resource-dir src/main/resources/scripts \
  --add-modules ALL-MODULE-PATH \
  --linux-package-name klikr \
  --linux-shortcut \
  --dest build/jpackage

echo "Done. ${PKG_TYPE} package created in build/jpackage/"
