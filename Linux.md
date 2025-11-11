
### install sdkman

**https://sdkman.io/install/**

###  install gradle

**sdk install gradle 9.2.0**

###  install java (with fx)

**sdk install java 25.fx-zulu**

###  install git

**https://git-scm.com/book/en/v2/Getting-Started-Installing-Git**


### get the source code

**git clone https://github.com/philgentric/klik.git**

this will create a folder "klik"

###  start klik!

**cd klik**

**gradle run**

(this starts the launcher)

to start klik directly:

**gradle klik**

to start the music player directly:

**gradle audio_player**

(note well: normally you can run only one player at a time)

Note: as an alternative to gradle, you can use jbang:

**sdk install jbang**

**jbang src/main/java/klik/Klik_application.java**
