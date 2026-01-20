# Installation for Windows

# option #1 via git bash (easiest)

'git bash' is a MINGW64 shell (a terminal/console, similar to PowerShell, but more Unix-like)

It is installed when you install git for windows:

https://gitforwindows.org/

then open 'git bash' and proceed to step 'install sdkman'

### Install sdkman

**https://sdkman.io/install/#windows-installation**


### Install gradle

**sdk install gradle 9.2.0**

### Install java

**sdk install java 25.fx-zulu**

### Get the source code

**git clone https://github.com/philgentric/klikr.git**

### Start Klikr (launcher)

**cd klikr**

**gradle run**


**IMPORTANT:**
If you have an error when starting after a source code update, 
come read this doc again as sometimes updating 
the source code also requires to upgrade tools like 'gradle' and 'java', 
this doc will also be updated along with the source
and you will be told the new command lines.

In all other cases, try:

**gradle clean run**

If Klikr doesn't start anymore, it may be that you left a child (or a cat ?) playing with your computer, and he has made a mess in the code source of klikr ?

Instead of erasing everything and restarting everything, do:

**git stash**

**git pull**

