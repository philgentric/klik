documenting a failed attempt to use GraalVM (jdk24) with javafx24 directly (not using gluon)

install graalvm:

sdk install java 24.0.2-graal

install locally the javafx sdk:

see the path in "--module-path"
../javafx-sdk-24.0.2/lib
in build.gradle:

uncomment        //id 'org.graalvm.buildtools.native' version '0.11.0'

as well as the whole section 'graalvmNative'



then:

gradle clean run

gradle clean shadowJar

// first run
java -agentlib:native-image-agent=config-output-dir=META-INF/native-image -jar build/libs/klikr.jar

// next times: ('merge')
java -agentlib:native-image-agent=config-merge-dir=META-INF/native-image -jar build/libs/klikr.jar


native-image --exact-reachability-metadata --no-fallback --enable-sbom -march=native -cp "./META-INF/native-image/reachability-metadata.json" -jar build/libs/klikr.jar

... generates an executable that fails at runtime

alternative:

gradle nativeRun

... idem fails at runtime
