gradle shadowJar

mkdir myicon.iconset
sips -z 16 16   src/main/resources/icons/klikr.png --out myicon.iconset/icon_16x16.png
sips -z 32 32   src/main/resources/icons/klikr.png --out myicon.iconset/icon_32x32.png
sips -z 128 128 src/main/resources/icons/klikr.png --out myicon.iconset/icon_128x128.png
sips -z 256 256 src/main/resources/icons/klikr.png --out myicon.iconset/icon_256x256.png
sips -z 512 512 src/main/resources/icons/klikr.png --out myicon.iconset/icon_512x512.png
iconutil -c icns myicon.iconset -o myicon.icns

jpackage \
  --type dmg \
  --mac-entitlements mac-entitlements.plist \
  --name Klikr \
  --app-version 1.0 \
  --vendor "Klikr" \
  --description "Klikr JavaFX application" \
  --input build/libs \
  --main-jar klikr.jar \
  --main-class klikr.Klikr_application \
  --module-path "$JAVA_HOME/jmods:/opt/homebrew/Cellar/openjfx/25/lib" \
  --add-modules javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.media,javafx.web \
  --icon myicon.icns \
  --java-options "-Xmx8g --enable-native-access=ALL-UNNAMED" \
  --resource-dir src/main/resources/scripts \
  --add-modules ALL-MODULE-PATH
