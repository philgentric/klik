./gradlew shadowJar

mkdir myicon.iconset
sips -z 16 16   src/main/resources/icons/klik.png --out myicon.iconset/icon_16x16.png
sips -z 32 32   src/main/resources/icons/klik.png --out myicon.iconset/icon_32x32.png
sips -z 128 128 src/main/resources/icons/klik.png --out myicon.iconset/icon_128x128.png
sips -z 256 256 src/main/resources/icons/klik.png --out myicon.iconset/icon_256x256.png
sips -z 512 512 src/main/resources/icons/klik.png --out myicon.iconset/icon_512x512.png
iconutil -c icns myicon.iconset -o myicon.icns

jpackage \
  --type dmg \
  --name Klik \
  --app-version 1.0 \
  --vendor "Klik" \
  --description "Klik JavaFX application" \
  --input build/libs \
  --main-jar klik.jar \
  --main-class klik.Klik_application \
  --module-path "$JAVA_HOME/jmods:/opt/homebrew/Cellar/openjfx/25/lib" \
  --add-modules javafx.base,javafx.graphics,javafx.controls,javafx.fxml,javafx.media,javafx.web \
  --icon myicon.icns \
  --java-options "-Xmx2g --enable-native-access=ALL-UNNAMED"