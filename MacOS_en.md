
### Install homebrew

**/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"**

### install sdkman

**brew install sdkman**

or follow instructions at:

**https://sdkman.io/install/**

### install gradle

**sdk install gradle 9.0.0**

### install java24 (with fx)

**sdk install java 24.fx-zulu**

### install git

**brew install git**

or follow instructions at:

https://git-scm.com/downloads

### get the source code

**git clone https://github.com/philgentric/klik.git**

this will create a folder "klik"

### start klik!

**cd klik**

**gradle run**

(this starts the launcher, and then you can start both klik and the music player)

to start klik directly:

**gradle klik**

to start the music player directly:

**gradle audio_player**

(note well: you can run only one player at a time)


### Install ffmpeg

(klik uses ffmpeg to make several things including animated icons for videos)

**brew install ffmpeg**

### Install graphicsmagick

(klik uses graphicsmagick to make icons for PDF files and animated icons for folders)

**brew install graphicsmagick**





Note1: as an alternative to gradle, you can use jbang:

**sdk install jbang**

**jbang src/main/java/klik/Klik_application.java**

Note2: as an alternative to gradle, you can use the mill:

read mill.MD
