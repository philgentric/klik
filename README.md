


[En Français](file:LISEZ_MOI.md)

# Klik: a file system explorer/manager with a strong focus on images

Sort files by displaying pictures and documents as icons, into folders, intuitive and fast, drag-and-drop anything.

# Try klik now!

To get klik running on your system, **you need to know how to open a terminal on your system**.

(the recipe looks complicated, but it is reliable and fast)

[Installation pour MacOS](file:MACOS_en.md)

[Installation pour Windows](file:WINDOWS_en.md)


### Get klik updates

To get the latest version of klik, just go in the klik folder and type:

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

Klik never deletes a file without asking you for confirmation.
In Klik, "delete" actually means moving the file into the (klik) "trash" folder (down in .klik)
There are 3 ways to recover a deleted file or folder: (1) use the undo menu item (2) visit the "trash" folder using klik (3) using the trash folder in .klik using your favorite file manager.
Only clearing the "trash" folder is final, and you will be asked for confirmation.
If you move a file into a folder where there is already a file with the same name, it is renamed with a postfix.
When you have duplicates and you merge by moving files, Klik will detect identical files with the same name and move the redundant copy into the trash folder.

# Numerous formats supported

Klik supports all major image & video file formats, as well a PDF (for icons).

Klik browser window display icons for still images, animated gifs are displayed animated, PDF documents are displayed as a icon-size image of the first page, movies are displayed as a few second of animated gif (this feature requires to have ffmpeg installed).

Klik image windows supports jpeg, png, gif, animated gif, bmp, wbmp.

Klik includes a (minimal) audio player with play lists!-)

Klik relies on your default system applications for:
- video play
- image edition
- in general opening any file that is not an image or a sound, or not in a supported format

# Windows

Klik has 2 types of windows: "Browser" and "Image".

You can open as many windows as you want, the limit is your machine's RAM.

(Klik also includes a music player, single instance/window.)

## Browser Windows = displays the content of a folder

Uses icons for images, PDFs and movies, and buttons for everything else. Has a slide show mode.

### Top Buttons

Klik "Browser window" has top buttons/menus that are always present (even if the folder is empty).

Up/Parent button: will open the parent directory.

Bookmark & History menu: Bookmarks, history and Undo

Files menu : enables to create a new empty folder in the current folder, and much more...

View Menu: enables to open a new browsing window (for the current folder), and more ...

Preferences menu for preferences

Trash button: when you click on it, it will display the content of the (klik specific) trash folder. If you drag a file over it, the file is moved to the trash.

### mutiple browsers

Opening multiple Browser-windows is very handy to sort images from one folder to multiple destination folders: just open one browser-window per folder, then move images or files using drag-and-drop.

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

Drag&Drop enables to move the image, for example dropping an image from a Image window to a Browser window

Type "y" to move an image into the same folder as the previous move.

### Summary:

Drag&Drop drop areas (where you can drop something) include:

Browser window: the file will be moved into the corresponding folder

Folder buttons: the file will be moved into the corresponding folder

Trash button: the file will be moved into the trash folder

Up button: the file will be moved into the parent folder


## Customizable look-and-feel

Klik comes with a few look-and-feels, and they are customisable using CSS (Cascaded Style Sheets) like web pages.
(The CSS files and the icons are in src/main/resources

## The little features that make Klik great

You can easily rename things (folders and files). In Image mode type "r".

Klik remembers all settings (in a human-readable file called klik_properties.txt).

Klik tells you how many files, folders and pictures a folder contains.

Klik displays file name and pixel sizes in the title of "Image" windows.

You can sort folders in many different ways:
- by name (alphabetically)
- by date
- by file size
- by image width
- by image height
- by image aspect ratio (most compact)
- random
- mix of aspect ratio & random
- image similarity (this option requires a computer with a GPU)

You can visualise how much room a folder takes on disk (folder size = everything including all sub-folder's content).

Klik history remembers the folders you visited.

Klik history can be cleared (and it effectively erases forever the history).

Klik uses system defaults to open files: you can play music, open sheets etc.

Klik uses system defaults to edit files: you can start the system-configured default editor for anything, from Klik, or an application of your choice

You can see the FULL metadata of the pictures; including EXIF, etc

Search: You can find files/images by keywords (it looks for your keywords in file names).

You can close Klik windows with a single Escape key stroke.



# FAQ

Q: What is the smallest/weakest hardware/software configuration klik has been proven to run onto?  
A: It is a HP mini-PC, Celeron N2840 2.16GHz with 2GB of RAM, with a nominal memory bandwidth of 16GB/s, Windows 8.1 (2013)

Q: When I type "git clone...", it says: **git** not found?  
A: You need to install git, it is a safe open source code management tool used by all developers on earth.

Q: It says **java** not found?  
A: klik requires java, you need to install it, version 23

Q: It says **gradle** not found?  
A: klik requires gradle,  **./gradlew** may work your you, YMMV

Q: Where am I?  
A: klik navigates your storage following the **directory structure**. The top left button makes you go up the tree (yes, computers are crazy, they have upside-down trees).

Q: Ok, but where am I?  
A: klik starts in your home folder. On mac it is /Users/yourname. Often people drop things on the desktop, it is located there: /Users/yourname/Desktop

Q: Where are my images?  
A: To find where your images are on your disk, use klik: the "Files" menu has a "Search" item. Then use klik bookmarks to save the paths.

Q: Can klik make a slide show?  
A: Yes, both the image display window and the Browser window support slide shows.

Q: What is klik made of?  
A: klik is written 100% in a computer language named "java", and uses the javafx graphic system.

Q: Why is klik so incredibly fast?  
A: klik uses background worker threads to avoid slowing down the User Interface.

Q: What is a "background worker thread"?  
A: It is a way to execute code on the different cores of your computer. When processing is spread other multiple cores, the job gets done faster, and it does not slow down the user interface; it reacts very fast, this is what you perceive as "speed". The more cores your machine has, the faster klik is.

Q: Why is klik so slow on large images?  
A: klik uses java ImageIO library, which is pretty fast but on very large images (say more than 100 MegaPixel) it simply requires more RAM and computing power to be decoded, if your computer is super recent and has a lot of RAM, it should be OK, but yes, on older machines it is going to take up to several seconds... and the garbage collection will cause hickups... on the other hand, I doubt you will find another tool that can do it faster?

Q: Why is the "sort files by aspect ratio" mode slow the first time I open a folder?  
A: Because the aspect ratio of every image in the folder has to be computed, before they can be sorted. Computing the aspect ratio involves opening the file and reading metadata, this is not a lot of work but if you have many images (say more than 200) in a folder it will take several seconds on an old machine... then klik caches these values and the second time you open the folder it should be quite fast.

Q: A popup tells me the cache is getting pretty large, what should I do?  
A: To make browsing faster klik uses several caches in RAM and on your disk. If you have a lot of images, the caches on disk can grow so large your main storage could become full, which is a bad thing. For this reason klik monitors its disk caches and will warn you when they get larger than a configurable limit. Use the dedicated menu to clear the caches. If you have a lot of spare room, you can change the configuration to increase the limit, to get rid of the annoying popup when you start klik. Note that klik automatically erases items that are older than 2 days from the disk caches .

Q: Should I edit the configuration file?  
A: Be careful when editing the configuration file, because it is super hard to make a config file reader 100% fool-proof. The good news is: if things go wrong, just erase the file, klik will create a fresh clean one, but of course you will lose preferences.

Q: Why should I edit the configuration file?  
A: To look for easter eggs.

Q: How can I edit the configuration file?  
A: The file name is klik_properties.txt, it is located in the .klik folder in your home folder.


## Dependencies

FFMPEG: in order for movies to be displayed as animated gifs, you must have ffmpeg installed and in your path

graphicsMagick: in order to generate PDF contact sheets, you must have graphicsMagick installed and in your path. graphicsMagick is also used to extract metadata from images (gm identify)



## The experimental features that make Klik fun

(to access experimental feature you will need to edit the properties file and change levelX=false to true)

Backup: you can backup incrementally whole file trees: faster than an OS copy -r, never deletes a file, keep track of file content (independently of names)

Deduplication: You can find duplicated files/images (even if they have different names). There is manual mode where you will be asked one picture at a time which copy you want to move to trash, and an automatic mode: (very fast but it may not move to trash the duplicate you would have preferred to delete!-)

Tags: You can assign tags (text strings) to images that are saved in .properties files, one per image, and klik moves this metadata file with the image!

Fusk: you can obfuscate images, for example if you store personal pictures on a network drive. Be careful that if you loose the key, the content will not be recoverable!

You can repair animated gifs.

Image-ML:

ML brings to klik (1) image similarity and (2) face recognition  

All operations are performed over HTTP to python servers that are leveraging free opensource opencv + tensorflow python bindings. 
It requires to have multiple python3 stuff installed: See the dedicated README in the folder python_for_image_ML.

The main hurdle is that you must "manually" start the python servers, for this, copy/paste the command given in the help menu.

Image search by similarity: 
Cosine similarity on VGG19 image embeddings. In a folder, you can sort images by similarity and you can klik on an image and ask for the 5 most similar images in the folder. Note that images are first down-sampled to the default vgg19 size of 224x224 pixels, so you can guess that "similarity" will not "work" on details!!! Also note that the first step consists in computing all the vgg19 feature vectors (4096 floats each) and store it in a cache, so if your folder contains many images it will take a while.


Face recognition:

Detect faces in images and recognize them.
It operates in 2 steps:

- You must first TRAIN the system, for that you must provide images that contain the face of the persons to be recognized. The way to "who is who" is to sort your pictures in folders, one folder per person.
Then if you create an empty file named ".folder_name_is_recognition_label" in the enclosing folder, each folder name will become a class, each image in each folder will become a training sample (note it requires that a face is detected...). 

- Multiple configurations can be named and saved, and training is fully INCREMENTAL: you can add persons/classes in multiple sessions to a given configuration, without having to retrain the whole system.

- Then you can search for faces in any image. But if an image contains multiple faces, only the first "found" will be recognized, and you have no control over which one will get out "first" out of HC.

a) Haars Cascades is used to detect faces in images.

b) then classification uses KNN with cosine similarity on feature vectors extracted using VGG19 image embeddings. 


Recommended after training: visit the folder in .klik that has the name of your config and browse the "prototypes" i.e. the "faces" that were stored during training: you may find weird ones that are caused by Haars-Cascade false positives (sometimes things that are detected as faces are not faces). Simply delete these bogus images and the next time you load the config, the vgg19 vector file will be erased too.



## TODO list

Try faster jpeg decoding like twelveMonkey

Try supporting more formats via Java Advanced Imaging

Try better down-scaling filters (not needed on mac screens, it is built-in) for example with imgscalr

Native ? (you can install grallVM )

## Abandoned TODO list

Make an installer for Mac,Linux and windows: too much work to maintain, not competitive with the "from source" methods (gradle and jbang)

