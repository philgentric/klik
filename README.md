
#Compile & run klik as a java application, with gradle


gradle clean     (optional)
gradle build
gradle run


#Compile & run klik as a java application, with maven

mvn clean javafx:run


to compile with grall:

on macos: brew install 
export GRAALVM_HOME=<your path>/graalvm-ce-java11-20.2.0/Contents/Home/
export JAVA_HOME=$GRAALVM_HOME

mvn client:compile
mvn client:link
mvn client:run