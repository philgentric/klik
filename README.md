
#Compile & run klik as a java application, using gradle

0. clone this git tree
1. cd klik
2. install JDK version 11
3. install gradle (tested with gradle 6.7.1, not tested for Gradle 7)
4. gradle clean (optional)
5. gradle build
6. gradle run

#Compile & run klik as a java application, using maven

0. clone this git tree
1. cd klik
2. install JDK version 11
3. install maven
4. mvn clean javafx:run

#Compile klik as a NATIVE application using graalvm using maven

0. clone this git tree
1. cd klik
2. install JDK version 11
3. install maven
4. download graalvm (tested with 21.1.0)
5. configure your path for grallvm
   a. export GRAALVM_HOME=<your path to graalvm>/graalvm-ce-java11-21.1.0/Contents/Home/ 
   b. export JAVA_HOME=$GRAALVM_HOME
6. mvn client:compile
7. mvn client:link
8. mvn client:run

There one remaining bug in this version: changing the language does not work