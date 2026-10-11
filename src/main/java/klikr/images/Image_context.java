// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../experimental/fusk/Fusk_strings.java
//SOURCES ../search/Finder.java
//SOURCES ../search/Keyword_extractor.java
package klikr.images;

import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Window;
import klikr.Klikr_application;
import klikr.browsers.browser_core.Image_and_properties;
import klikr.browsers.browser_core.icons.image_properties_cache.Rotation;
import klikr.settings.String_constants;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browsers.browser_core.items.Item_file_with_icon;
import klikr.browsers.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.change.Change_gang;
import klikr.change.file_system_monitoring.File_status;
import klikr.change.file_system_monitoring.Filesystem_item_modification_watcher;
import klikr.change.file_system_monitoring.Filesystem_modification_reporter;
import klikr.change.old_and_new.Command;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.image.Full_image_from_disk;
import klikr.util.image.Static_image_utilities;
import klikr.util.files_and_paths.*;
import klikr.util.image.decoding.Fast_date_from_filesystem;
import klikr.util.image.decoding.Fast_rotation_from_exif_metadata_extractor;
import klikr.look.Jar_utils;
import klikr.look.Look_and_feel_manager;
import klikr.search.Finder;
import klikr.search.Keyword_extractor;
import klikr.util.image.rescaling.Image_rescaling_filter;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.log.Logger;
import klikr.util.execute.System_open_actor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

//**********************************************************
public class Image_context
//**********************************************************
{
    public static final boolean dbg = false;
    public static final String DELAY = "Delay: ";

    public final Kontext context;
    public final Path previous_path;
    public final Path path;
    public final Image image;
    public final ImageView the_image_view;
    private Rotation rotation = null;
    double zoom_factor = 1.0;
    public boolean image_is_damaged;
    public String title="";
    public final FileTime creation_time;

    //**********************************************************
    public static Optional<Image_context> build_Image_context(Path path, Image_window image_window)
    //**********************************************************
    {
       if ( image_window.rescaler == Image_rescaling_filter.Native)
       {
           //logger_.log("default (javafx ImageView) rescaler used for :"+path);
           // the image returned is full scale, it will be automagically
           // scaled (down typically) by javafx ImageView
           return get_Image_context(path, image_window.context);

       }
       else
       {
           image_window.context.log("VIPS "+image_window.rescaler+" rescaler used for :"+path);
           // the image returned is rescaled using VIPS-lib,
           // to the target scene dimensions
           // with the specified filter aka 'rescaler'
           return Static_image_utilities.get_Image_context_with_alternate_rescaler(
                   path,
                   image_window.stage.getScene().getWidth(),
                   image_window.stage.getScene().getHeight(),
                   image_window.rescaler,
                   image_window.context);
       }
    }




    //**********************************************************
    public static Optional<Image_context> get_Image_context(Path path, Kontext context)
    //**********************************************************
    {
        if ( !Files.exists(path)) return Optional.empty();
        Image_and_properties iap = Full_image_from_disk.load_native_resolution_image_from_disk(path, true, context);
        if (iap == null) return Optional.empty();
        Image local_image = iap.image();
        if ( local_image.isError())
        {
            Image broken = Jar_utils.get_broken_icon(300,context.logger());
            if ( broken == null) return Optional.empty();
            return Optional.of(new Image_context(path,path,broken,context));
        }

        Optional<Image_context> returned = Optional.of(new Image_context(path, path, local_image,context));
        return returned;
    }


    //**********************************************************
    public Image_context(Path current_path,Path previous_path_, Image image_, Kontext context)
    //**********************************************************
    {
        this.context = context;
        path = current_path;
        previous_path = previous_path_;
        image = image_;
        the_image_view = new ImageView(image);
        the_image_view.setPickOnBounds(true); // allow click on transparent areas
        the_image_view.setCacheHint(CacheHint.QUALITY);


        creation_time = Fast_date_from_filesystem.get_date(current_path,context);
        //if ( get_rotation) get_rotation();
        if ( dbg)
        {
            if ( path ==null)
            {
                context.log("NULL file, image loaded:"+image.getWidth()+"x"+image.getHeight());
            }
            else
            {
                context.log("image loaded:"+ path.getFileName()+" "+image.getWidth()+"x"+image.getHeight());
            }

        }
    }
    //**********************************************************
    public Image_context(Image im_,  Kontext context)
    //**********************************************************
    {
        this.context = context;
        image = im_;
        path = null;
        previous_path = null;
        creation_time = null;
        the_image_view = new ImageView(image);
        the_image_view.setPickOnBounds(true); // allow click on transparent areas
        the_image_view.setCacheHint(CacheHint.QUALITY);
    }

    //**********************************************************
    public static String get_full_path(Path f)
    //**********************************************************
    {
        return f.toAbsolutePath().toString();
    }

    //**********************************************************
    public static Function<Image_context, Long> size_F()
    //**********************************************************
    {
        return ic -> 700L; // super rough estimate ;-)
    }


    //**********************************************************
    public Rotation get_rotation()
    //**********************************************************
    {
        if ( rotation != null) return rotation;
        rotation = Fast_rotation_from_exif_metadata_extractor.get_rotation(path, true, context);
        if (dbg) context.log("get_rotation, ROTATION = "+rotation);
        return rotation;
    }


    //**********************************************************
    public String get_image_name()
    //**********************************************************
    {
        if ( path == null) return "no file";
        return path.getFileName().toString();
    }



    //**********************************************************
    double get_animated_gif_delay()
    //**********************************************************
    {
        StringBuilder sb = Exif_stage.get_graphicsmagick_info(path,context);
        String s = sb.toString();

        String[] lines = s.split("\\R");
        for (String line : lines) {
            line = line.trim();
            context.log("line ->"+line+"<-");
            if (line.startsWith(DELAY)) {
                String delayValue = line.substring(DELAY.length()).trim(); // extract the value after "Delay: "
                context.log(DELAY + delayValue);
                double delay = Double.parseDouble(delayValue);
                context.log(DELAY + delay);
                return delay;
            }
        }
        context.log("no delay found, assuming 10");
        return 10;
    }



    //**********************************************************
    void open()
    //**********************************************************
    {
        //context.log("asking desktop to EDIT: " + path.getFileName());
        context.log("asking desktop to open: " + path.getFileName());
        //try
        {
            System_open_actor.open_with_registered_application(path,  context);

            // dont use this, it is AWT
            //Desktop desktop = Desktop.getDesktop();
            //desktop.edit(path.toFile());

            // we want the UI to refresh if the file is modified
            // we do not know when the edition will end so we need to start a watcher
            // with a 10 minute timer

            Filesystem_modification_reporter reporter = () -> {
                List<Old_and_new_Path> oanps = new ArrayList<>();
                Command cmd = Command.command_edit;
                Old_and_new_Path oan = new Old_and_new_Path(path, path, cmd, Status.edition_requested, false);
                oanps.add(oan);
                Change_gang.report_changes(oanps, context.owner());
            };
            Filesystem_item_modification_watcher ephemeral_filesystem_item_modification_watcher = new Filesystem_item_modification_watcher();
            // will die after 10 minutes
            if ( ephemeral_filesystem_item_modification_watcher.init(path,reporter,false,10, context) != File_status.OK)
            {
                context.log("Warning: cannot start monitoring: "+path);
            }
        }
        /*catch (IOException e)
        {
            context.log_stack_trace(e.toString());
        }*/
    }

    //**********************************************************
    void open_with_registered_application(Kontext context)
    //**********************************************************
    {
        System_open_actor.open_with_registered_application(path,context);

            // we want the UI to refresh if the file is modified
            // we do not know when the edition will end so we need to start a watcher
            // with a 10 minute timer

            Filesystem_modification_reporter reporter = () -> {
                List<Old_and_new_Path> oanps = new ArrayList<>();
                Command cmd = Command.command_edit;
                Old_and_new_Path oan = new Old_and_new_Path(path, path, cmd, Status.edition_requested, false);
                oanps.add(oan);
                Change_gang.report_changes(oanps,context.owner());
            };
            Filesystem_item_modification_watcher ephemeral_filesystem_item_modification_watcher = new Filesystem_item_modification_watcher();
            // will die after 10 minutes
            if ( ephemeral_filesystem_item_modification_watcher.init(path,reporter,false,10,context) != File_status.OK)
            {
                context.log("Warning: cannot start monitoring: "+path);
            }

    }




    //**********************************************************
    void change_zoom_factor(Image_window image_window, double mul)
    //**********************************************************
    {
        //image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.pix_for_pix);
        double image_width = image.getWidth();
        double image_height = image.getHeight();


        zoom_factor /= mul;
        context.log("mul="+mul+" => new zoom_factor="+zoom_factor);

        double image_width2 = image_width*zoom_factor;
        double window_width = image_window.the_Scene.getWidth();
        if ( image_width2 < window_width)
        {
            context.log("image_width2 too small");
            image_width2 = window_width;
        }
        double min_x = (image_width-image_width2);
        if ( min_x < 0)
        {
            context.log("min_x too small");
            min_x = 0;
        }


        double image_height2 = image_height*zoom_factor;
        double window_height = image_window.the_Scene.getHeight();
        if ( image_height2 < window_height)
        {
            context.log("image_height2 too small");
            image_height2 = window_height;
        }
        double min_y = (image_height-image_height2);
        if ( min_y < 0)
        {
            context.log("min_y too small");
            min_y = 0;
        }


        context.log("rectangle = "+min_x+", "+min_y+", "+image_width2+", "+image_height2);
        Rectangle2D r = new Rectangle2D(min_x, min_y,image_width2 , image_height2);
        the_image_view.setViewport(r);
    }


    //**********************************************************
    public void move_viewport(double dx, double dy)
    //**********************************************************
    {
        Rectangle2D r0 = the_image_view.getViewport();
        if ( r0 == null)
        {
            //entire image is displayed
            Rectangle2D r = new Rectangle2D(

                    -dx,
                    -dy,
                    the_image_view.getImage().getWidth(),
                    the_image_view.getImage().getHeight());
            the_image_view.setViewport(r);
            return;
        }
        Rectangle2D r = new Rectangle2D(
                r0.getMinX()-dx,
                r0.getMinY()-dy,
                r0.getWidth(),
                r0.getHeight());
        the_image_view.setViewport(r);
    }

    //**********************************************************
    void search_using_keywords_from_the_name(Path_list_provider path_list_provider, Path_comparator_source path_comparator_source)
    //**********************************************************
    {
        if(dbg) context.log("Image_context search_using_keywords_from_the_name");
        Keyword_extractor ke = new Keyword_extractor(context.logger(), List.of());
        Set<String> keywords_set = ke.extract_keywords_from_file_and_dir_names(path);
        if (keywords_set == null) {
            context.log(Logger.error+"FATAL null keywords ??? ");
            return;
        }
        //String extension = Extensions.get_extension(path.getFileName().toString());
        if (keywords_set.isEmpty())
        {
            // this happens when the image name does not contain text at all
            keywords_set.add(path.getFileName().toString());
            //context.log(Logger.error+"FATAL no keywords ??? ");
            return;
        }
        List<String> keywords = new ArrayList<>();
        for (String k : keywords_set) {
            keywords.add(k.toLowerCase());
        }

        context.log("---- Going to search for keywords: -------");
        for (String s : keywords) {
            context.log("->" + s + "<-");
        }
        context.log("------------------------------------------");

        Finder.find(
                Klikr_application.application,
                path_list_provider,
                path_comparator_source,
                keywords,null,true,context);
    }



    List<String> given_keywords = new ArrayList<>();
    //**********************************************************
    void search_using_keywords_given_by_the_user(
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            boolean search_only_for_images)
    //**********************************************************
    {
        context.log("find()");
        ask_user_and_find( path_list_provider, path_comparator_source,given_keywords, search_only_for_images,context);
    }


    //**********************************************************
    public static void ask_user_and_find(
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            List<String> keywords,
            boolean search_only_for_images,
            Kontext context
    )
    //**********************************************************
    {
        context.log("ask_user_and_find()");

        Jfx_batch_injector.inject( () -> {
            StringBuilder ttt = new StringBuilder();
            for (String ss : keywords) ttt.append(ss).append(" ");
            TextInputDialog dialog = new TextInputDialog(ttt.toString());
            Look_and_feel_manager.set_dialog_look(dialog,context.logger());
            dialog.initOwner(context.owner());
            dialog.setTitle("Keywords");
            dialog.setHeaderText("Enter your keywords, separated by space");
            dialog.setContentText("Keywords:");

            //context.log("dialog !");
            String extension = "";
            Optional<String> op = dialog.showAndWait();
            if (op.isPresent())
            {
                String result = op.get();
                if ( result.contains("."))
                {
                    extension = Extensions.get_extension(result);
                    result = Extensions.get_base_name(result);
                }
                String[] splited = result.split("\\s+");// split by any space
                if ( splited.length > 0)
                {
                    keywords.clear();
                    for (String s : splited)
                    {
                        String local = s.toLowerCase();
                        if ( keywords.contains(local)) continue;
                        keywords.add(s);
                    }

                    Finder.find(
                            Klikr_application.application,
                            path_list_provider,
                            path_comparator_source,
                            keywords,
                            extension,
                            search_only_for_images,context);
                }
            }

        },context);
    }

    /*
    public void finder_shutdown()
    {
        if (finder_for_k != null) finder_for_k.shutdown();
    }
    */



    //**********************************************************
    boolean copy(
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Runnable after,
            Window owner)
    //**********************************************************
    {
        // to get a good (long) prefix, add 2 levels of folders names
        // since a copy is usually moved afterward
        // and you  want to get a good name for the copy

        String prefix = "";
        if ( path.getParent() != null)
        {
            prefix = path.getParent().getFileName()+"_";
            if ( prefix.startsWith(".")) prefix = prefix.substring(1); // avoid to make hidden files in hidden folders
            if (path.getParent().getParent() != null)
            {
                prefix = path.getParent().getParent().getFileName()+"_"+prefix;
            }
        }
        if (path.getFileName().toString().startsWith(prefix)) prefix = ""; // no "recursive" prefix_prefix_prefix ... !!!
        context.log("Image_context COPY prefix ="+prefix);

        Path new_path = null;
        for (int i = 0; i < 40000; i++)
        {

            new_path = Moving_files.generate_new_candidate_name_special(path,prefix,i, context.logger());
            if (!Files.exists(new_path))
            {
                context.log("new_path ->" + new_path+"<- does not exist");
                break;
            }
            else
            {
                context.log("new_path" + new_path+" exists, retrying");
            }
        }
        if (new_path == null)
        {
            context.log("copy failed: could not create new unused name for" + path.getFileName());
            return false;
        }
        context.log("copy:" + path.getFileName()+ " copy name= "+new_path);

        try
        {
            Files.copy(path, new_path);
        } catch (IOException e)
        {
            context.log("copy failed: could not create new file for: " + path.getFileName() + ", Exception:" + e);
            return false;
        }
        Actor_engine.execute(after,"Copy image", context.logger());
        //Popups.popup_text(My_I18n.get_I18n_string("Copy_done",logger),My_I18n.get_I18n_string("New_name",logger)+new_path.getFileName().toString(),false);
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(
                path,
                new_path,
                Command.command_copy,
                Status.copy_done,false));
        Change_gang.report_changes(l,owner);

        Item_file_with_icon.open_an_image(
                path_list_provider,
                path_comparator_source,
                new_path,
                context);
        //Image_window orphan = Image_window.get_Image_window(b,new_path, logger);
        return true;
    }




    //**********************************************************
    public Optional<Image_context> rename_file_for_an_image_window(Image_window image_window)
    //**********************************************************
    {
        Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_file_name(path,context);
        if ( new_path == null) return Optional.empty();
        return image_window.change_name_of_file(new_path);
    }

    //**********************************************************
    Optional<Image_context> ultim(Image_window image_stage)
    //**********************************************************
    {
        String old_file_name = path.getFileName().toString().toLowerCase();
        if (old_file_name.contains(String_constants.ULTIM))
        {
            context.log("no vote, name already contains " + String_constants.ULTIM);
            return Optional.empty();
        }

        Path new_path = Moving_files.generate_new_candidate_name(path,"", String_constants.ULTIM, context.logger());
        return image_stage.change_name_of_file(new_path);
    }


}
