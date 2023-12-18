# Klik: a file system explorer/manager with a strong focus on images

Enables to sort pictures into folders, intuitive and fast, drag-and-drop anything.

# Try klik 

At this stage no installation executable or binaries are distributed, you will have to build klik from source, but it is super easy to do!

### Beguiners' guide: how to install git and java

### On MacOS

On MacOS, I recommend leveraging homebrew and sdkman, (these are safe mainstream tools used by many developers) as follows:

Open a terminal and copy/paste the **command lines** below:

**/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"**

**brew install sdkman**

**sdk list java**

Look at the list, copy a version (last column on the right side), preferably a recent one, the first number is the java major version, for example "21.ea.35-open" is a version of java 21

**sdk install java 21.ea.35-open**

**brew install git**

### On windows or Linux

type "how to install java and git" in google!-)

### Get klik and run it!

**git clone https://github.com/philgentric/klik.git**

**cd klik**

**./gradlew run**

### Get klik updates

To get the latest version of klik, just go in the klik folder and type:

**git pull**

,this will fetch the last master source code. Then:

**./gradlew run**

# FAQ

Q: It says **git** not found?  
A: You need git to get klik source code, install it

Q: It says **java** not found?  
A: klik requires java, you need to install it

Q: It says **gradle** not found?  
A: gradle is used to compile klik, but the command **./gradlew** will fix everything your you.

Q: Where am I?  
A: klik navigates your storage following the **directory structure**. The top left button makes you go up the tree (yes, computers are crazy, they have upside-down trees).

Q: Ok, but where am I?  
A: klik starts in your home folder. On mac it is /Users/yourname. Often people drop things on the desktop, it is located there: /Users/yourname/Desktop

Q: Where are my images?  
A: To find where your images are on your disk, open a terminal and type: **find . -name "*.jpg"**. Then use klik bookmarks to save the paths.

Q: What is klik made of?  
A: klik is written 100% in a computer language named "java", and uses the javafx graphic system.

Q: Why is klik so incredibly fast?  
A: klik uses background worker threads to avoid slowing down the graphic system. 

Q: What is a "background worker thread"?  
A: It is a way to execute code on a different core of your computer. When processing is spread other multiple cores, the job gets done faster, and it does not slow down the user interface; it reacts very fast, this is what you perceive as "speed". The more cores your machine has, the faster klik is.

Q: Why is klik so slow on large images?  
A: klik uses java ImageIO library, which is pretty fast but uses a lot of RAM. Large images (say more than 100 MegaPixel) simply need more RAM and computing power to be decoded, if your computer is super recent and has a lot of RAM, it should be OK, but yes, on older machines it is going to take up to several seconds... and the garbage collection will cause hickups... on the other hand, I doubt you will find another tool that can do it faster?


Q: Why is the "sort files by aspect ratio" mode so slow?  
A: Because the aspect ratio of every image in the folder has to be computed, before they can be sorted. Computing the aspect ratio involves opening the file and reading metadata, this is not a lot of work but if you have many images (say more than 200) in a folder it will take several seconds... then klik caches these and the second time you open the folder it should be quite fast.

Q: A popup tells me the cache is getting pretty large, what should I do?  
A: To make browsing faster klik uses a cache for icons, on your disk. If you have a lot of images, this cache can grow so large your main storage will become full, which is a bad thing. Use the menu: "Files/Clean" to clear the cache. Alternatively if you have a lot of spare room, you can edit the configuration file to increase the limit so as to avoid the annoying popup when you start klik. Note that klik erases items in the disk cache that are older than 2 days.

Q: Should I edit the configuration file?  
A: Be careful when editing the configuration file, because it is super hard to make a config file reader 100% fool-proof. The good news is: if things go wrong, just erase the file, klik will create a fresh clean one, but of course you wil loose all bookmarks and preferences.

Q: Why should I edit the configuration file?  
A: To increase the warning limit of cache size, or to look for easter eggs.

Q: How can I edit the configuration file?  
A: The file name is klik_properties.txt, it is localted in the .klik hidden folder in your home folder.

# Intuitive

Klik has been designed to be very intuitive.
Play with Drag & Drop!
Moving files and folders around has never been easier.

# Transparent

Contrarily to a number of other products, Klik does not hide your images.
Klik does not use hidden folders or whatever "Libraries"!
Klik only uses 100% transparent file system operations.
Klik never modifies a file, it only creates folders at your will
and enables you to move files from folder to folder.

# Safety

Klik never deletes a file without asking you for confirmation.
In Klik, "delete" actually means moving the file into the "klik_trash" folder.
You can visit the "klik_trash" folder and recover any "deleted" file or folder.
Only clearing the "klik_trash" folder is final, and you will be asked for confirmation.
If you move a file into a folder where there is already a file with the same name, it is renamed with a postfix "\_xxx".
When you have duplicates and you merge by moving files, Klik will detect identical files with the same name and move the redundant copy into the klik_trash folder.

# Numerous formats supported

Klik supports all major image & video file formats, as well a PDF.

Klik browser window display icons for still images, animated gifs are displayed animated, PDF documents are displayed as a icon-size image of the first page, movies are displayed as a few second of animated gif (this feature requires to have ffmpeg installed).

Klik image windows supports jpeg, png, gif, animated gif, bmp, wbmp.

# Windows

Klik has 2 types of windows: "Browser" and "Image".

You can open has many windows as you want, the limit is your machine's RAM.


## Browser Windows = displays the content of a folder

Uses icons for images, PDFs and movies, and buttons for everything else. Has a slide show mode.

### Top Buttons

Klik "Browser window" has top buttons/menus that are always present (even if the folder is empty).

Up/Parent button: will open the parent directory.

Bookmark & History menu

Files menu : enables to create a new empty folder in the current folder, and much more...

View Menu: enables to open a new browsing window (for the current folder), and more ...

Preferences menu for preferences

Trash button: when you click on it, it will display the content of the "klik_trash" folder. If you drag a file over it, the file is moved to klik_trash.

### mutiple browsers

Openning multiple Browser-windows is very handy to sort images from one folder to multiple destination folders: just open one browser-window per folder, thanks to drag-and-drop.

## Image Windows = displays one image at a time

Can load images one after the other very fast to explore a folder (using the space bar or the left/right arrows).

Speed comes from a cache with pre-loading, play with it, you will see that preloading can be "forward" if you use the right arrow to scan one image after the other or "backward" if you use the left arrow.
The slideshow mode has variable speed.


## Drag & drop

In Klik, you can Drag-and-Drop (almost) everything!

In a Browser window:

Drag&Drop works for icons representing images in a folder

Drag&Drop works for buttons representing non-image files in a folder

Drag&Drop works for buttons that represent folders: you can drop a file (or folder!) on them.

In an Image window:  

Drag&Drop enables to move the image, for example dropping an image from a Image window to a Browser window

### Summary:

Drag&Drop drop areas (where you can drop something) include:

Browser window: the file will be moved into the corresponding folder

Folder buttons: the file will be moved into the corresponding folder

Trash button: the file will be moved into the trash folder

Up button: the file will be moved into the parent folder


## Customizable look-and-feel

Klik comes with a few look-and-feels, and they are customisable.

## The little features that make Klik great

You can easily rename things (folders and files).

Klik remembers all settings (in a human readable file called klik_properties.txt).

Klik tells you how many files, folders and pictures a folder contains. (and it is real fast!)

Klik displays file name and pixel sizes in the title of "Image" windows.

You can sort folders alphabetically or by file/folder size.

You can visualise how much room a folder takes on disk (folder size = everything including all sub-folder's content).

Klik history remembers the folders you visited, so you can shortcut.

Klik history can be cleared (and it effectively erases for ever the history).

Klik uses system defaults to open files: you can play music, open sheets etc.

Klik uses system defaults to edit files: you can start the system-configured default editor for anything, from Klik.

You can see the full EXIF metadata of the pictures (if any).

You can close Klik windows with a single Escape key stroke.


## The experimental features that make Klik fun

(to access experimental feature you will need to edit the properties file and change level2=false to true)

Backup: you can backup whole file trees (faster than an OS copy -r, never deletes a file, keep track of file content (independently of names)) 

Deduplication: You can find duplicated files/images (even if they have different names). There is manual mode where you will be asked one picture at a time which copy you want to move to klik-trash, and an automatic mode: very fast but it does not always move to klik-trash the copy you would have preferred to delete!-)

You can find images by keywords (it assumes keywords compose file names).

You can repair animated gifs.

You can assign tags (text strings) to images that are saved in .properties files, one per image, and klik moves this metadata file with the image!

Note: in order for movies to be displayed as animated gifs, you must have ffmpeg installed and in your path

Fusk: you can obfuscate images, for example if you store personal pictures on a network drive. Becareful that if you loose the key, the content will not be recoverable!

## TODO list


Make an installer for Mac,Linux and windows

Try faster jpeg decoding like twelveMonkey

Try supporting more formats via Java Advanced Imaging

Try better down-scaling filters (not needed on mac screens, it is built-in) for example with imgscalr

Try better EXIF for example via rkall/exiftool or apache Commons Imaging



