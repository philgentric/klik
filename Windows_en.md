# Installation for Windows

# option #1 via git bash (easiest)

'git bash' is a MINGW64 shell (a terminal/console, similar to PowerShell, but more Unix-like)

It is installed when you install git for windows:

https://gitforwindows.org/

then open 'git bash' and proceed to step 'install sdkman'

Caveat: you will have to reopen 'gitbash' whenever you want to start Klik (launcher)

#  option #2 via WSL 


Install WSL2: 

(exact steps depend on the exact Windows version ... can be complicated)

Go to Settings and search for 'Optional features' then 'More Optional features'

Check:
- Hyper-V : Hyper-V Platform
- virtual machine platform
- windows subsystenm for Linux


open a "windows power shell" in administrator mode
(search for 'power', then right-click the icon 'Windows PowerShell')

**wsl --install**


### Install git

**https://git-scm.com/downloads/win**



### Install sdkman

**https://sdkman.io/install/#windows-installation**


### Install gradle

**sdk install gradle 9.2.0**

### Install java

**sdk install java 25.fx-zulu**

### Get the source code

**git clone https://github.com/philgentric/klik.git**

### Start Klik (launcher)

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

If klikr doesn't start anymore, it may be that you left a child (or a cat ?) playing with your computer, and he has made a mess in the code source of klikr ?

Instead of erasing everything and restarting everything, do:

**git stash**

**git pull**

# Frequently Asked Questions

Q: I do "gradle run", but klikr doesn't start ?

A: Check that you are in the 'klikr' folder of the code source of klikr. (the error message is : Directory 'xxxxx' does not contain a Gradle build.)

Q: I have no more space on my disk.

A: Go to the Preferences menu and erase the caches. Visit the trash to check that what it contains can go to heaven before doing "empty the trash".

Q: When klikr runs, other applications are missing RAM.

A: Go to the Preferences menu and (1) erase the caches (2) reduce the maximum memory size of the JVM. 

Q: I want to uninstall klikr.

A: (1) Delete the code source folder (2) Delete the '.klikr' folder at the root of your user disk space, it contains caches and the trash, almost always this gives you more space than the code.

