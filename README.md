# Klik: a file system explorer/manager with a strong focus on images

The purpose of Klik is to enable to sort pictures into folders, as a consequence it is a pretty good general purpose "File Manager": you can visualize files and folders, and organize information into a file/folder tree.

Klik has been designed to be intuitive and fast.

Klik reproduces best-of drag-and-drop features found in good file managers, with a plus: it displays images as icons (adjustable size), and yet it remains _extremely fast and stable even with folders that contains ten of thousands of images._

Time-consuming operations are performed asynchronously so that the UI remains fluid.

Klik can display all standard image formats, up to huge images (tested up to 14000x10000 pixels).

Klik User Interface is designed to be translated in any language. By the way, traductors wanted! 

![Alt text](https://user-images.githubusercontent.com/4594245/152138959-18fe11c9-9fe3-4f19-8c82-9999f75fa071.png)


# Easiest way to try klik 

install git (https://git-scm.com/)

on a mac, install brew then git: 

/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

brew install git

Open a terminal and type:

git clone https://github.com/philgentric/klik.git

cd klik

./gradlew run

# Intuitive

Klik has been designed to be very intuitive.
Play with Drag & Drop, you will see!
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

Openning multiple Browser-windows is very handy to sort images from one folder to multiple destination folders: just open one browser-window per folder.
If you have really many destination folders, the easiest way is to open a browser-window in the parent folder and then drag images into the buttons representing each destination folders.

## Browser Windows = displays the content of a folder

Uses icons for images, PDFs and movies, and buttons for everything else.
Icons size is your preference: from very small to very large
Clicking on an icon will popup either a new "Image" window displaying that image, if it is an image, or use the System default application to open the file.
Clicking on an file-button will open that file with the System default application.
Clicking on an folder-button will open that folder, replacing the current one.
The slideshow mode will scroll through the folder.

## Image Windows = displays one image at a time

Can load images one after the other very fast to explore a folder (using the space bar or the left/right arrows).
Speed comes from a cache with pre-loading, play with it, you will see that preloading can be "forward" if you use the right arrow to scan one image after the other or "backward" if you use the left arrow.
The slideshow mode has variable speed.

## Top Buttons

Klik "Browser window" has top buttons/menus that are always present (even if the folder is empty).

Up/Parent button: will open the parent directory.

Bookmark & History menu

Files menu : enables to create a new empty folder in the current folder, and much more...

View Menu: enables to open a new browsing window (for the current folder), and more ...

Preferences menu for preferences

Trash button: when you click on it, it will display the content of the "klik_trash" folder. If you drag a file over it, the file is moved to klik_trash.

## Drag & drop (Drag&Drop)

In Klik, you can Drag-and-Drop (almost) everything!

In a Browser window:

Drag&Drop works for icons representing images in a folder

Drag&Drop works for buttons representing non-image files in a folder

Drag&Drop works for buttons folders

In an Image window:  Drag&Drop enables to move the image, for example droping an image from a Image window to a Browser window

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

You can find images by keywords (it assumes keywords compose file names).

## The experimental features that make Klik fun

You can find duplicated files/images (even if they have different names). There is manual mode where you will be asked one picture at a time which copy you want to move to klik-trash, and an automatic mode: very fast but it does not always move to klik-trash the copy you would have preferred to delete!-)

You can see the full EXIF metadata of the pictures (if any).

You can repair animated gifs.

You can close Klik windows with a single Escape key stroke.

You can assign tags (text strings) to images that are saved in .properties files, one per image, and klik moves this metadata file with the image!

Note: in order for movies to be displayed as animated gifs, you must have ffmpeg installed and in your path
