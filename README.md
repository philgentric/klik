
# Klik: a file system explorer/manager with a strong focus on images and readability


[En Français](LISEZ_MOI.md)
(klik is available in French, English, Chinese, Spanish, German, Breton, Italian, Japanese, Korean, Portuguese)

![Alt text](klik.png?raw=true "Klik screen shot")


Sort files by displaying pictures and documents as icons, into folders, intuitive and fast, drag-and-drop anything, hyperlegible font available in large size.

# Try klik now!

To get klik running, **you need to know how to open a terminal on your system**.
(the recipe looks complicated, but it is reliable and fast)

[Installation for MacOS](MacOS_en.md)

[Installation for Windows](Windows_en.md)

[Installation for Linux](Linux.md)

### Get klik updates

To get the latest version of klik, just click the button in the Launcher!

... Or go in the klik folder and type:

**git pull**

this will fetch the last master source code.

If you prefer an application instead of a command line tool, install github desktop:

https://desktop.github.com


# Intuitive

Klik has been designed to be very intuitive.
Play with Drag & Drop!
Moving files and folders around has never been easier.

# Transparent

Contrarily to a number of other products, Klik does not hide your images.
Klik does not use hidden folders or whatever "Libraries"!
Klik only uses 100% transparent file system operations.
Klik never modifies a file, it only creates folders when you ask
and enables you to move files from folder to folder.

# Safety

Klik features a crash-resistant undo capability: actions are stored in a file allowing to undo any action, even after a crash, even for multiple files or folder.

Klik never deletes a file without asking you for confirmation. In Klik, "delete" actually means moving the file into the (klik) "trash" folder (down in .klik)

There are 3 ways to recover a deleted or accidentally moved (moved,but you did not know where) file or folder: (1) use the undo menu item (2) Use klik search! (3) visit the "trash" folder using klik (press the top right button) (4) visit the trash folder using your favorite file manager... 
Only clearing the "trash" folder is final, and you will be asked for confirmation.
If you move a file into a folder where there is already a file with the same name, it is renamed with a postfix.
When you have duplicates and you merge by moving files, Klik will detect identical files with the same name and move the redundant copy into the trash folder.


# Numerous formats supported

Klik supports all major image & video file formats, as well a PDF (for icons). (also broad format support for the music player)

Klik browser window displays icons for still images, animated gifs are displayed animated, PDF documents are displayed as a icon-size image of the first page, movies are displayed as a few second of animated gif (this feature requires to have ffmpeg installed, the length of the animated gif can be changed in 'preferences').

Klik image windows support jpeg, png, gif, animated gif, bmp, wbmp.

Klik includes an audio player with playlists that can load music from whole folders and import youtube URls in drag-and-drop

Klik relies on your default system applications for:
- video play
- image edition
- in general opening any file that is not an image or a sound, or a not-supported format

# Windows

The Launcher can start the music player (only one music player can exist at a time) or a "Browser" window.

Klik/browser has 2 types of windows: "Browser" and "Image".

You can open as many windows (Browser or Image) as you want, the limit is your machine's RAM.


## Browser Windows = displays the content of a folder

Uses icons for images, PDFs and movies, and buttons for everything else. Has a slide show mode.

Icons size can be changed in the 'preferences' menu or using keybaord accelerators: 'meta +' or '-', just like zooming in web browsers

### Top Buttons

Klik "Browser window" has top buttons/menus that are always present (even if the folder is empty).

Up/Parent button: will open the parent directory.

Bookmark & History menu: Bookmarks, history and Undo

Files menu : enables to create a new empty folder in the current folder, and much more...

View Menu: enables to open a new browsing window (for the current folder), and more ...

Preferences menu for preferences

Trash button: will display the content of the (klik specific) trash folder. If you drag a file over it, the file is moved to the trash.

### Open multiple browsers

Opening multiple Browser-windows is very handy to sort images from one folder to multiple destination folders: just open one browser-window per folder, then move images or files using drag-and-drop.

The 'view' menu has items to create 2 side-by-side browser windows, try it!

## Image Windows = displays one image at a time

Can load images one after the other very fast to explore a folder (using the space bar or the left/right arrows).

Speed comes from a cache with pre-loading, play with it, you will see that preloading can be "forward" if you use the right arrow to scan one image after the other or "backward" if you use the left arrow.

The slideshow mode (type "s") has variable speed, use "w" to make is slower, "x" to make it faster.


## Drag & drop

In Klik, you can Drag-and-Drop (almost) everything!

In a Browser window:

Drag&Drop works for icons representing images in a folder

Drag&Drop works for buttons that represent folders: you can drop a file (or folder!) on them; or drag a folder into another folder.

Drag&Drop works for buttons representing non-image files in a folder

In an Image window:  

Drag&Drop enables to move the image, for example, dropping an image from a Image window to a Browser window

Type "y" to move an image into the same folder as the previous move.

### Summary:

Drag&Drop drop areas (where you can drop something) include:

Browser window: the file will be moved into the corresponding folder

Folder buttons: the file will be moved into the corresponding folder

Trash button: the file will be moved into the trash folder

Up button: the file will be moved into the parent folder


### License

Klik license is MIT 

## Customizable look-and-feel

Klik comes with a few look-and-feels, and they are customisable using CSS (Cascaded Style Sheets) like web pages.
(The CSS files and the icons are in src/main/resources, feel free to create your own L&F... and to submit it on Discord or as a pull-request)

## The little features that make Klik great

You can easily rename things (folders and files). In Image mode type "r".

Klik remembers all settings (in a human-readable file called klik.properties).

Klik tells you how many files, folders and pictures a folder contains, as well as the size on disk.

Klik displays file names and pixel sizes in the title of "Image" windows.

You can sort folders in many different ways:
- by name (alphabetically)
- by date
- by file size
- by image width
- by image height
- by image aspect ratio (most compact)
- random
- mix of aspect ratio & random
- image similarity (these experimental options require a computer with a 'good' GPU)

Klik history remembers the folders you visited in its 'history', that you can clear at any time (and it effectively erases forever the history).

Klik uses system defaults to open files: what happens is consistent with your OS behavior.

Klik uses system defaults to edit files: you can start the system-configured default editor for anything, from Klik, or an application of your choice

Klik can open/edit a file using a user-defined application, the first time you use the menu item, you will be asked to select the target applications and then it will be remembered for the next time.

You can see the full metadata of the pictures; including EXIF, etc

Search: You can find files/images by keywords (it looks for your keywords in file names).

You can close Klik windows with Escape (or not: see preferences).



# FAQ

Q: What is the smallest/weakest hardware/software configuration klik has been proven to run onto?  
A: It is a HP mini-PC, Celeron N2840 2.16GHz with 2GB of RAM, with a nominal memory bandwidth of 16GB/s, Windows 8.1 (2013), however Microsoft has disabled upgrades so that in 2025 this venerable hardware cannot be tested (if you have tips, please tell!)

Q: When I type "git clone...", it says: **git** not found?  
A: You need to install git, it is a safe open source code management tool used by all developers.

Q: It says **java** not found?  
A: klik requires java, you need to install it, look for the minimum version in the installation instructions: Klik requires a super recent version at least 23.

Q: It says **gradle** not found?  
A: klik requires gradle (for expert users: klik also supports jbang and the mill)

Q: Why don't you use gradlew?
A: Failures have been reported

Q: Where am I?  
A: klik navigates your storage following the **directory structure**. The top left button makes you go up the tree (yes, computer guys are crazy, they have upside-down trees).

Q: Ok, but where am I?  
A: klik starts in your home folder. On mac it is /Users/yourname. Often people drop things on the desktop, it is located there: /Users/yourname/Desktop

Q: Where are my images?  
A: To find where your images are on your disk, use klik: the "Files" menu has a "Search" item. Then use klik bookmarks to save the paths.

Q: Can klik make a slide show?  
A: Yes, both the image display window and the Browser window implement slide shows.

Q: What is klik made of?  
A: klik is written 100% in a computer language named "java", and uses the javafx graphic system.

Q: Why is klik so incredibly fast?  
A: klik uses background worker threads to avoid slowing down the User Interface.

Q: What is a "background worker thread"?  
A: It is a way to execute code on the different cores of your computer. When processing is spread other multiple cores, the job gets done faster, and it does not slow down the user interface; it reacts very fast, this is what you perceive as "speed". The more cores your machine has, the faster klik is.

Q: Why is klik so slow on large images?  
A: klik uses java ImageIO library, which is pretty fast but on very large images (say more than 100 MegaPixel) it simply requires more RAM and computing power to be decoded, if your computer is super recent and has a lot of RAM, it should be OK, but yes, on older machines it is going to take up to several seconds... and the garbage collection will cause hickups... on the other hand, I doubt you will find another FREE$ tool that can do it faster?

Q: Why is the "sort files by aspect ratio" mode slow the first time I open a folder?  
A: Because the aspect ratio of every image in the folder has to be computed, before they can be sorted. Computing the aspect ratio involves opening the file and reading metadata, this is not a lot of work but if you have many images (say more than 200) in a folder, it will take several seconds on an old machine... good news is: klik caches these values and the second time you open the folder it should be quite fast.

Q: A popup tells me the cache is getting pretty large, what should I do?  
A: To make browsing faster klik uses several caches in RAM and on your disk. If you have a lot of images, the caches on disk can grow so large that your main storage could become full, which is a bad thing. For this reason, klik monitors its disk caches and will warn you when it gets larger than a configurable limit. Use the dedicated menu to clear the caches. If you have a lot of spare room, you can change the configuration to increase the limit, to get rid of the annoying popup when you start klik. Note that klik automatically erases items that are older than 2 days from the disk caches.

Q: Should I edit the configuration file?  
A: Be careful when editing the configuration file, because it is super hard to make a config file reader 100% fool-proof. The good news is: if things go wrong, just erase the file, klik will create a fresh clean 'default' one, but of course you will lose preferences.

Q: Why should I edit the configuration file?  
A: To look for easter eggs (there used to be quite a few).

Q: How can I edit the configuration file?  
A: The file name is klik.properties, it is located in the .klik folder in your home folder.

Q: When I start the audio player, all (or some of) my songs are gone?
A: Typically, this happens because you moved your music to a different folder. Drop that folder on the audio player drop area, the songs will be reloaded and your playlists will be updated.

Q: How can I store youtube audio tracks?
A: Use 'meta v' (paste) in the audio player, it will download the audio track from youtube and add it to the current playlist. You can also drag and drop a youtube URL into the audio player.

## Dependencies

klik uses 2 optional but super useful opensource ressources:

ffmpeg: in order for movies to be displayed as animated gifs, you must have ffmpeg installed and in your path

graphicsMagick: in order to generate PDF contact sheets, you must have graphicsMagick installed and in your path. graphicsMagick is also used to extract metadata from images (gm identify)



## The experimental features that make Klik fun

(to access experimental feature you will need to enable them using the 'preferences' bottom item)

Backup: you can backup incrementally whole file trees: faster than an OS copy -r, never deletes a file, keeps track of file content (independently of names)

Deduplication: You can find duplicated files/images (even if they have different names). There is manual mode where you will be asked one picture at a time which copy you want to move to trash, and an automatic mode: (very fast but it may not move to trash the duplicate you would have preferred to delete!-)

Tags: You can assign tags (text strings) to images that are saved in .properties files, one per image, and klik moves this metadata file with the image!

Fusk: you can obfuscate images, for example if you store personal pictures on a network drive. Be careful that if you loose the key, the content will not be recoverable!

You can repair animated gifs.

Image-ML:

ML brings to klik (1) image similarity and (2) face recognition  

All operations are performed over HTTP to python servers that are leveraging free opensource python. 
It requires to have multiple python3 stuff installed: See the dedicated README in the folder python_for_ML.

The main hurdle is that you must "manually" start the python servers, for this, copy/paste the command given in the help menu.

Image search by similarity: Cosine similarity on MobileNetV3 image embeddings. The images are resized to 224x224 and passed through MobileNetV3 (28 layers Convolutional Neural Network) which produces a feature vector with 960 components. 

In a folder, you can sort images by similarity and you can klik on an image and ask for the 5 most similar images in the folder. 

Note that since images are first down-sampled to 224x224 pixels, "similarity" will not "work" on small details. Also note that the first step consists in computing all the feature vectors (960 floats each) and store it in a cache, so if your folder contains many images it will take a while.

Image similarity de-duplication: thanks to ML, klik can find 'similar images' that is: images that look the same but are NOT bit-identical. This includes images that have been rescaled, or cropped, as well as minor variants like when one has been shooting a scene with action very fast, to make sure you could capture something.

Face recognition:

Detect faces in images and recognize them.
It operates in 2 steps:

- You must first TRAIN the system, for that you must provide images that contain the face of the persons to be recognized and a 'tag'. 

The way to "who is who" is to sort your pictures in folders, one folder per person.
Then in each folder, create an empty file named ".folder_name_is_recognition_label", the folder name will become a 'tag' or class name, and images in the folder will become a training sample (assuming a face is detected...). 

- Multiple configurations can be named and saved, and training is fully INCREMENTAL: you can add persons/classes in multiple sessions to a given configuration, without having to retrain the whole system.

- Then you can search for faces in any image. But if an image contains multiple faces, only the first "found" will be recognized, and you have no control over which one will get out "first" of the face detection...

a) MTCNN (Multi-Task Cascaded Convolutional Networks) or 4 variants of Haars Cascades can be used to detect faces in images.

b) then classification uses KNN with cosine similarity on feature vectors extracted using FaceNet 'vggface2' image embeddings. 


Recommended after training: visit the folder in .klik that has the name of your config and browse the "prototypes" i.e. the "faces" that were stored during training: you may find weird ones that are caused by face detection false positives (sometimes things that are detected as faces are not faces). Simply delete these bogus images and the next time you load the config, the vgg19 vector file will be erased too.


# For developers

How to build klik and klik installers.

You need git to get the source code.

Ideally you should use sdkman for gradle and java.


## MacOS and linux

### Install homebrew

**/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"**

### install sdkman

**brew install sdkman**

### install gradle

**sdk install gradle 9.2.0**

### install java24 (with fx)

**sdk install java 25.fx-zulu**

### Run Klik

gradle klik

### Run the launcher

gradle run

### Run the audio player

gradle audio_player

or just:

gradle audio

### Make a MacOS installer (.dmg)

You need a mac.

./jpackage_for_macos.sh

the produced file is named ./Klik-1.0.dmg


## Windows11

You need a PC under Windows11. (maybe windows10 works too, not tested; earlier windows versions used to work with earlier klik+java version, but this is now hard to test as klik now requires a pretty recent java)

### Install git-for-windows

https://gitforwindows.org/

This will give you a major asset (on top of git): the git bash shell MINGw64

MINGw64 gives you a 'nix like' behavior

### Clone this repo

**git clone https://github.com/philgentric/klik.git**

### Install sdkman, gradle, java with fx

**https://sdkman.io/install/#windows-installation**

**sdk install gradle 9.2.0**

**sdk install java 25.fx-zulu**

### Make a Windows11 installer (.msi)

In the git bash shell MINGw64 type:

powershell -ExecutionPolicy Bypass -File jpackage_for_windows.ps1

This will create a msi file in ./build/jpackage





