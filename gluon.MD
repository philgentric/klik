attempt to go native with gluon

install gluon SDK:

sdk install java 22.1.0.1.r17-gln

in bluid.gradle:
uncomment       // id 'com.gluonhq.gluonfx-gradle-plugin' version '1.0.27'

replace all javafx '24' with '17'

then:

gradle clean build

gradle nativeCompile

gradle nativeLink

gradle nativeRunAgent

gradle nativeRun
