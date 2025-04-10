
On MacOS, it is recommended to install homebrew first:

**/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"**

then:

Step 1: install sdkman

**brew install sdkman**

or follow instructions at:

**https://sdkman.io/install/**

Step 2: install gradle

**sdk install gradle 8.13**

Step 3: install java (with fx)

**sdk install java 23.0.2.fx-zulu**

Step 4: install git

**brew install git**

or follow instructions at:

https://git-scm.com/downloads

Step 5: get the source code

**git clone https://github.com/philgentric/klik.git**

this will create a folder "klik"

Step 6: start klik!

**cd klik**

**gradle clean run**


Note: as an alternative to gradle, you can use jbang:

**sdk install jbang**

**jbang src/main/java/klik/Klik_application.java**
