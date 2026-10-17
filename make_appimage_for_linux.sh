#!/usr/bin/env bash
#-----------------------------------------------------------------------
#  make_appimage_for_linux.sh
#
#  Two-step Linux build:
#    1. gradle shadowJar                -> build/libs/klikr.jar
#    2. jpackage --type app-image       -> a self-contained app directory
#       (bundles a JRE + JavaFX + the fat jar)
#    3. wrap that app directory in an AppDir and run appimagetool
#                                        -> a single Klikr-x86_64.AppImage
#
#  The result is ONE file the user downloads, marks executable, and runs
#  on virtually any Linux distro (Ubuntu, Fedora, Mint, openSUSE, Arch...)
#  with no install and no root -- the Linux equivalent of the macOS DMG.
#
#  Must run ON Linux. jpackage cannot cross-compile.
#
#  Prerequisites:
#    - a JavaFX-bundled JDK   (e.g. sdk install java 25.fx-zulu)
#    - appimagetool on PATH, OR let this script auto-download it
#      (needs curl + FUSE available to run the downloaded AppImage).
#
#  Usage:
#    ./make_appimage_for_linux.sh
#-----------------------------------------------------------------------

set -euo pipefail

# -------------------------------------------------------
# Config
# -------------------------------------------------------
APP_NAME="Klikr"
APP_VERSION="1.0"
MAIN_JAR="klikr.jar"
MAIN_CLASS="klikr.Klikr_application"
ICON_SRC="src/main/resources/icons/klikr.png"
ARCH="x86_64"

BUILD_DIR="build"
APPIMAGE_WORK="${BUILD_DIR}/appimage"          # scratch area for this script
JPACKAGE_OUT="${APPIMAGE_WORK}/jpackage"       # where jpackage writes the app-image
APPDIR="${APPIMAGE_WORK}/${APP_NAME}.AppDir"   # the AppDir appimagetool consumes
OUTPUT_APPIMAGE="${BUILD_DIR}/${APP_NAME}-${APP_VERSION}-${ARCH}.AppImage"

# -------------------------------------------------------
# Resolve JAVA_HOME / JavaFX module path
# -------------------------------------------------------
if [[ -z "${JAVA_HOME:-}" ]]; then
    echo "ERROR: JAVA_HOME is not set. Install a JavaFX-bundled JDK, e.g.:"
    echo "       sdk install java 25.fx-zulu"
    exit 1
fi

# With a .fx-zulu JDK the javafx.* modules are inside \$JAVA_HOME/jmods.
# Override JAVAFX_LIB if you use a plain JDK + separate JavaFX SDK.
MODULE_PATH="$JAVA_HOME/jmods"
if [[ -n "${JAVAFX_LIB:-}" ]]; then
    MODULE_PATH="$JAVA_HOME/jmods:$JAVAFX_LIB"
fi

# -------------------------------------------------------
# Locate (or fetch) appimagetool
# -------------------------------------------------------
APPIMAGETOOL=""
if command -v appimagetool >/dev/null 2>&1; then
    APPIMAGETOOL="appimagetool"
else
    echo "appimagetool not found on PATH -- downloading a local copy ..."
    APPIMAGETOOL="${APPIMAGE_WORK}/appimagetool-${ARCH}.AppImage"
    mkdir -p "${APPIMAGE_WORK}"
    curl -fL -o "${APPIMAGETOOL}" \
        "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${ARCH}.AppImage"
    chmod +x "${APPIMAGETOOL}"
fi

# -------------------------------------------------------
# 1. Build the fat JAR
# -------------------------------------------------------
echo "==> [1/4] gradle shadowJar"
gradle shadowJar

# -------------------------------------------------------
# 2. jpackage: produce a self-contained app directory
#    (--type app-image => a folder, NOT an installer)
# -------------------------------------------------------
echo "==> [2/4] jpackage --type app-image"
rm -rf "${JPACKAGE_OUT}"
mkdir -p "${JPACKAGE_OUT}"

jpackage \
  --type app-image \
  --name "${APP_NAME}" \
  --app-version "${APP_VERSION}" \
  --vendor "Klikr" \
  --description "Klikr JavaFX application" \
  --input build/libs \
  --main-jar "${MAIN_JAR}" \
  --main-class "${MAIN_CLASS}" \
  --module-path "${MODULE_PATH}" \
  --add-modules javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.media,javafx.web \
  --icon "${ICON_SRC}" \
  --java-options "-Xmx8g --enable-native-access=ALL-UNNAMED" \
  --add-modules ALL-MODULE-PATH \
  --dest "${JPACKAGE_OUT}"

# jpackage creates "${JPACKAGE_OUT}/${APP_NAME}" containing bin/, lib/
JP_APP_DIR="${JPACKAGE_OUT}/${APP_NAME}"
if [[ ! -d "${JP_APP_DIR}" ]]; then
    echo "ERROR: expected jpackage output at ${JP_APP_DIR} but it is missing."
    exit 1
fi

# -------------------------------------------------------
# 3. Assemble an AppDir around the jpackage output
#    appimagetool needs: AppRun, <name>.desktop, an icon, at AppDir root.
# -------------------------------------------------------
echo "==> [3/4] assembling AppDir"
rm -rf "${APPDIR}"
mkdir -p "${APPDIR}"

# Put the whole jpackage app tree under the AppDir.
cp -a "${JP_APP_DIR}/." "${APPDIR}/"

# The real launcher jpackage generated (lowercase or app name, under bin/).
# Pick the first executable found in bin/.
LAUNCHER_REL=""
for cand in "bin/${APP_NAME}" "bin/klikr"; do
    if [[ -x "${APPDIR}/${cand}" ]]; then
        LAUNCHER_REL="${cand}"
        break
    fi
done
if [[ -z "${LAUNCHER_REL}" ]]; then
    # fallback: first executable file in bin/
    LAUNCHER_REL="bin/$(ls "${APPDIR}/bin" | head -n1)"
fi
echo "    launcher: ${LAUNCHER_REL}"

# Icon at AppDir root (appimagetool wants <icon-name>.png matching .desktop Icon=)
cp "${ICON_SRC}" "${APPDIR}/klikr.png"

# .desktop file at AppDir root
cat > "${APPDIR}/klikr.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=${APP_NAME}
Comment=Klikr JavaFX application
Exec=AppRun
Icon=klikr
Categories=Graphics;Utility;FileTools;
Terminal=false
EOF

# AppRun entry point at AppDir root -> delegates to the jpackage launcher
cat > "${APPDIR}/AppRun" <<EOF
#!/bin/bash
HERE="\$(dirname "\$(readlink -f "\${0}")")"
exec "\${HERE}/${LAUNCHER_REL}" "\$@"
EOF
chmod +x "${APPDIR}/AppRun"

# -------------------------------------------------------
# 4. Build the single .AppImage
# -------------------------------------------------------
echo "==> [4/4] appimagetool -> ${OUTPUT_APPIMAGE}"
rm -f "${OUTPUT_APPIMAGE}"
# ARCH env var is read by appimagetool to name/stamp the arch.
ARCH="${ARCH}" "${APPIMAGETOOL}" "${APPDIR}" "${OUTPUT_APPIMAGE}"

echo ""
echo "Done. Single-file installer:"
echo "    ${OUTPUT_APPIMAGE}"
echo "Users run it with:  chmod +x $(basename "${OUTPUT_APPIMAGE}") && ./$(basename "${OUTPUT_APPIMAGE}")"
