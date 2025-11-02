package klik.browser;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.look.Jar_utils;
import klik.look.Look_and_feel_manager;
import klik.properties.Non_booleans_properties;
import klik.util.info_stage.Info_stage;
import klik.util.info_stage.Line_for_info_stage;
import klik.util.log.Logger;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class About_klik_stage
//**********************************************************
{

    //**********************************************************
    public static void show_about_klik_stage(Window owner, Logger logger)
    //**********************************************************
    {
        List<Line_for_info_stage> l = new ArrayList<>();

        l.add(new Line_for_info_stage(true,"Klik"));
        l.add(new Line_for_info_stage(false,"    Klik is a file system explorer with a strong focus on images."));
        l.add(new Line_for_info_stage(false,"    The original purpose of Klik was to enable to sort pictures into folders,"));
        l.add(new Line_for_info_stage(false,"    but it is a pretty good File Manager "));
        l.add(new Line_for_info_stage(true,"Intuitive"));
        l.add(new Line_for_info_stage(false,"    Klik has been designed to be very intuitive"));
        l.add(new Line_for_info_stage(false,"    Play with Drag & Drop, you will see!"));
        l.add(new Line_for_info_stage(false,"    Moving files and folders around has never been easier"));
        l.add(new Line_for_info_stage(true,"Transparency"));
        l.add(new Line_for_info_stage(false,"    Contrarily to a number of other products, Klik does not hide your images"));
        l.add(new Line_for_info_stage(false,"    Klik does not use hidden folders or whatever \"Libraries\"!"));
        l.add(new Line_for_info_stage(false,"    Klik only uses 100% transparent file system operations"));
        l.add(new Line_for_info_stage(false,"    Klik never modifies a file, it only creates folders at your will"));
        l.add(new Line_for_info_stage(false,"    and enables you to move files from folder to folder"));
        l.add(new Line_for_info_stage(true,"Safety"));
        l.add(new Line_for_info_stage(false,"    Klik has UNDO, and it is stored on file i.e. it survives crashes and closing the application"));
        l.add(new Line_for_info_stage(false,"    Klik never deletes a file without asking you for confirmation"));
        l.add(new Line_for_info_stage(false,"    In Klik, \"delete\" actually means moving the file into the \"klik_trash\" folder"));
        l.add(new Line_for_info_stage(false,"    (ultimately, you can visit the \"klik_trash\" folder and recover any \"deleted\" file or folder)"));
        l.add(new Line_for_info_stage(false,"    Only clearing the \"klik_trash\" folder is final, and you will be asked for confirmation"));
 //       l.add(new Line_for_info_stage(false,"    If you are really sure you want to delete something forever, fast, the short cut \"SHIFT D\" will do that."));
        l.add(new Line_for_info_stage(true,"Windows"));
        l.add(new Line_for_info_stage(false,"     Klik has 2 types of windows: \"Browser\" and \"Image\" "));
        l.add(new Line_for_info_stage(false,"     You can open has many windows as you want, the limit is your machine's RAM"));
        l.add(new Line_for_info_stage(true,"Browser Windows"));
        l.add(new Line_for_info_stage(false,"    = displays the content of a folder"));
        l.add(new Line_for_info_stage(false,"    Uses icons for images and movies, and buttons for everything else"));
        l.add(new Line_for_info_stage(false,"    Clicking on an icon will popup a new \"Image\" window displaying that image "));
        l.add(new Line_for_info_stage(false,"    Clicking on an file-button will open that file (with the default application) "));
        l.add(new Line_for_info_stage(false,"    Clicking on an folder-button will open that folder, replacing the current one "));
        l.add(new Line_for_info_stage(false,"    Has a slideshow mode (press s) with variable direction (press s again) & speed (press w/x for slower/faster)"));
        l.add(new Line_for_info_stage(true,"Image Windows"));
        l.add(new Line_for_info_stage(false,"    = displays one image at a time"));
        l.add(new Line_for_info_stage(false,"    Can load images one after the other very fast to explore a folder (using the space bar or the left/right arrows)"));
        l.add(new Line_for_info_stage(false,"    Has a slideshow mode (press s) with variable direction & speed (press w/x for slower/faster)"));
        l.add(new Line_for_info_stage(true,"Top Buttons"));
        l.add(new Line_for_info_stage(false,"    Klik \"Browser window\" has 6 top buttons"));
        l.add(new Line_for_info_stage(false,"        Up (Parent Folder) button: will open the parent directory"));
        l.add(new Line_for_info_stage(false,"        Bookmark & History: for faster navigation"));
        l.add(new Line_for_info_stage(false,"        Files: everything related to files, for example: create an empty folder"));
        l.add(new Line_for_info_stage(false,"        View: visualize things, for example: open a new browser window"));
        l.add(new Line_for_info_stage(false,"        Preferences: all settings"));
        l.add(new Line_for_info_stage(false,"        Trash button: drop trash on it, or click it and see the \"klik_trash\" folder"));
        l.add(new Line_for_info_stage(true,"Drag & drop (D&D)"));
        l.add(new Line_for_info_stage(false,"    In Klik, you can Drag-and-Drop (almost) everything!"));
        l.add(new Line_for_info_stage(false,"        In a Browser window: "));
        l.add(new Line_for_info_stage(false,"           D&D works for icons representing images in a folder ,"));
        l.add(new Line_for_info_stage(false,"           D&D works for buttons representing non-image files in a folder ,"));
        l.add(new Line_for_info_stage(false,"           D&D works for buttons representing folders"));
        l.add(new Line_for_info_stage(false,"        In an Image window:  D&D enables to move the image by dropping it in a browser window"));
        l.add(new Line_for_info_stage(false,"        D&D reception spots (where you can drop something) include:"));
        l.add(new Line_for_info_stage(false,"          Browser window: the file will be moved into the corresponding folder"));
        l.add(new Line_for_info_stage(false,"          Folder buttons: the file will be moved into the corresponding folder"));
        l.add(new Line_for_info_stage(false,"          Trash button: the file will be moved into the trash folder"));
        l.add(new Line_for_info_stage(false,"          Up button: the file will be moved into the parent folder"));
        l.add(new Line_for_info_stage(true,"Performance"));
        l.add(new Line_for_info_stage(false,"    Klik has been designed for speed"));
        l.add(new Line_for_info_stage(false,"    Klik can display with fluidity folders that contain tens of thousands of images"));
        l.add(new Line_for_info_stage(false,"    Klik can display huge images (tested up to 14000x10000 pixels)"));
        l.add(new Line_for_info_stage(false,"    Time consuming operations are performed asynchronously so that the UI remains fluid"));
        l.add(new Line_for_info_stage(true,"The little features that make Klik great:"));
        l.add(new Line_for_info_stage(false,"     You can easily rename things (folders and files)"));
        l.add(new Line_for_info_stage(false,"     Klik remembers all settings (in a human readable file called "+ Non_booleans_properties.PROPERTIES_FILENAME +")"));
        l.add(new Line_for_info_stage(false,"     You can visualise how much room a folder takes on disk (folder size = everything including all sub-folder's content)"));
        l.add(new Line_for_info_stage(false,"     Klik tells you how many pictures a folder contains"));
        l.add(new Line_for_info_stage(false,"     Klik history remembers the folders you visited, so you can shortcut. It can be cleared."));
        l.add(new Line_for_info_stage(false,"     Klik bookmarks enable to short cut"));
        l.add(new Line_for_info_stage(false,"     Klik undo enable to undo any past action, including after a crash or application closed"));
        l.add(new Line_for_info_stage(false,"     Klik uses system defaults to open files: you can play music, open sheets etc"));
        l.add(new Line_for_info_stage(false,"     Klik uses system defaults to edit files: you can start the system-configured default editor for anything, from Klik"));
        l.add(new Line_for_info_stage(false,"     Klik displays file name and pixel sizes in the title of \"Image\" windows"));
        l.add(new Line_for_info_stage(false,"     You can sort folders alphabetically or by file/folder size"));
        l.add(new Line_for_info_stage(false,"     You can find images by keywords (it assumes file names are made by keywords concatenation)"));
        l.add(new Line_for_info_stage(false,"     You can find duplicated files/images (even if they have different names)"));
        l.add(new Line_for_info_stage(false,"     You can see the full EXIF metadata of the pictures (if any)"));
        l.add(new Line_for_info_stage(false,"     You can generate animated gifs icons from videos, you can generate a 5s GIF for each 5s sequence n a movie"));
        l.add(new Line_for_info_stage(false,"     You can close Klik windows with a single Escape key stroke"));
        l.add(new Line_for_info_stage(true,"How to get klik :"));
        l.add(new Line_for_info_stage(false,"     Everything (source code executable, docs) is on github:"));
        l.add(new Line_for_info_stage(false,"     https://github.com/philgentric/klik"));

        l.add(new Line_for_info_stage(true,"Copyrigth"));
        l.add(new Line_for_info_stage(false,"Copyrigth 2000-"+ Year.now().getValue()+" Philippe Gentric"));

        //l.add(new Line_for_info_stage(true,"Performance"));
        //String pref_string = "Image production performance = "+ Icon_factory_actor.sample_collector.get_Mpixel_per_second()+" Mpix/s";
        //l.add(new Line_for_info_stage(false,pref_string));

        String icon_path = Look_and_feel_manager.get_main_window_icon_path(Look_and_feel_manager.get_instance(owner,logger), Look_and_feel_manager.Icon_type.KLIK);
        Image icon = Jar_utils.load_jfx_image_from_jar(icon_path, 128, owner,logger);
        Info_stage.show_info_stage("About Klik",l, icon, null);

    }



}
