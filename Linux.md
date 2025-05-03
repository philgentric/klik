

Step 1: install sdkman

**https://sdkman.io/install/**

Step 2: install gradle

**sdk install gradle 8.14**

Step 3: install java (with fx)

**sdk install java 24.fx-zulu**

Step 4: install git

**https://git-scm.com/book/en/v2/Getting-Started-Installing-Git**


Step 5: get the source code

**git clone https://github.com/philgentric/klik.git**

this will create a folder "klik"

Step 6: start klik!

**cd klik**

**gradle clean run**

(this starts the launcher)

to start klik directly:

**gradle klik**

to start the music player directly:

**gradle audio_player**

(note well: normally you can run only one player at a time)

Note: as an alternative to gradle, you can use jbang:

**sdk install jbang**

**jbang src/main/java/klik/Klik_application.java**
