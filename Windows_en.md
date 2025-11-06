# Installation for Windows

## With Chocolatey

you need a JDK bundled with javaFX, the tested option is:
Azul 'zulu' JDK25 - FX

go to the Azul web site: https://www.azul.com

in the 'dowload' page select:
- Operating System : Windows
- Java Package : JDK FX
- Download the most recent (e.g. Azul Zulu: 25.30.17)

## Alternate method: with WSL2

Install WSL2:

**wsl --install**


### Install sdkman

**https://sdkman.io/install/#windows-installation**


### Install gradle

**sdk install gradle 9.0.0**

### Install java24

**sdk install java 24.fx-zulu**

### Install git

**https://git-scm.com/downloads/win**

### Get the source code

**git clone https://github.com/philgentric/klik.git**

### Start Klik

**cd klik**

**gradle run**

### To get the latest version

**cd klik**

**git pull**


**IMPORTANT:**
If after upgrading the source code you have a an error when starting, come read this doc again as sometimes upgrading the source code also requires the upgrade of the compilation tools 'gradle' and 'java', this doc will also be updated and you will be told the new command lines.

Try:

**gradle clean run**

If klik doesn't start anymore, it may be that you left a child (or a cat ?) playing with your computer, and he has made a mess in the code source of klik ?

Instead of erasing everything and restarting everything, do:

**git stash**

**git pull**

# Frequently Asked Questions

Q: I do "gradle run", but klik doesn't start ?

A: Check that you are in the 'klik' folder of the code source of klik. (the error message is : Directory 'xxxxx' does not contain a Gradle build.)

Q: I have no more space on my disk.

A: Go to the Preferences menu and erase the caches. Visit the trash to check that what it contains can go to heaven before doing "empty the trash".

Q: When klik runs, other applications are missing RAM.

A: Go to the Preferences menu and (1) erase the caches (2) reduce the maximum memory size of the JVM. 

Q: I want to uninstall klik.

A: (1) Delete the code source folder (2) Delete the '.klik' folder at the root of your user disk space, it contains caches and the trash, almost always this gives you more space than the code.

