package klik.images;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Browser;
import klik.change.Change_gang;
import klik.files_and_paths.*;
import klik.search.Finder;
import klik.search.Finder_actor;
import klik.images.decoding.Fast_date_from_OS;
import klik.level2.fusk.Fusk_static_core;
import klik.level2.fusk.Fusk_strings;
import klik.images.decoding.Exif_metadata_extractor;
import klik.look.Look_and_feel_manager;
import klik.properties.Static_application_properties;
import klik.search.Keyword_extractor;
import klik.util.From_disk;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

//**********************************************************
public class Image_context
//**********************************************************
{
    public static final boolean dbg = false;

    public final Path previous_path;
    public final Path path;
    public final Image image;
    public final ImageView the_image_view;
    //private List<String> exifs_tags_list = null;
    private double rotation = 0;
    Logger logger;
    double zoom_factor = 1.0;
    //double scroll_x;
    //double scroll_y;
    public boolean image_is_damaged;
    public String title="";
    public final FileTime creation_time;

    //**********************************************************
    public static Image_context get_Image_context(Path path, Aborter aborter,Logger logger_)
    //**********************************************************
    {
        if ( !Files.exists(path)) return null;
        Image local_image = From_disk.load_image_from_disk(path, true, aborter,logger_);
        if ( local_image == null)
        {
            return null;
        }
        if ( local_image.isError())
        {
            Image broken = Look_and_feel_manager.get_broken_icon(300);
            return new Image_context(path,path,broken,logger_);
        }

        return new Image_context(path,path, local_image,logger_);
    }


    //**********************************************************
    public Image_context(Path current_path,Path previous_path_, Image image_, Logger logger_)
    //**********************************************************
    {
        path = current_path;
        previous_path = previous_path_;
        logger = logger_;
        image = image_;
        the_image_view = new ImageView(image);
        the_image_view.setCacheHint(CacheHint.QUALITY);
        creation_time = Fast_date_from_OS.get_date(current_path,logger);
        //if ( get_rotation) get_rotation();
        if ( dbg)
        {
            if ( path ==null)
            {
                logger.log("NULL file, image loaded:"+image.getWidth()+"x"+image.getHeight());
            }
            else
            {
                logger.log("image loaded:"+ path.getFileName()+" "+image.getWidth()+"x"+image.getHeight());
            }

        }
    }
    //**********************************************************
    public Image_context(Image im_,  Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        image = im_;
        path = null;
        previous_path = null;
        creation_time = null;
        the_image_view = new ImageView(image);
        the_image_view.setCacheHint(CacheHint.QUALITY);
    }

    //**********************************************************
    public static String get_full_path(Path f)
    //**********************************************************
    {
        return f.toAbsolutePath().toString();
    }



    //**********************************************************
    public double get_rotation(Aborter aborter)
    //**********************************************************
    {
        load_exif(aborter);
        return rotation;
    }

    //**********************************************************
    private List<String> load_exif(Aborter aborter)
    //**********************************************************
    {
        List<String> exifs_tags_list = new ArrayList<>();
        image_is_damaged = false;
        try
        {
            Exif_metadata_extractor extractor = new Exif_metadata_extractor(path,logger);
            double how_many_pixels = image.getWidth()*image.getHeight();
            exifs_tags_list = extractor.get_exif_metadata(how_many_pixels,true, aborter,true);
            rotation = extractor.get_rotation(true,aborter);
            if ( dbg) logger.log(path+" rotation="+rotation);
            image_is_damaged = extractor.is_image_damaged();
            title = extractor.title;
        }
        catch (OutOfMemoryError e)
        {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
        }
        catch (Exception e)
        {
            logger.log(Stack_trace_getter.get_stack_trace_for_throwable(e));
        }
        return exifs_tags_list;
    }


    //**********************************************************
    public String get_image_name()
    //**********************************************************
    {
        if ( path == null) return "no file";
        return path.getFileName().toString();
    }

    //**********************************************************
    public void show_exif_stage(Aborter aborter)
    //**********************************************************
    {
        TextFlow textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);
        logger.log("$$$$$$ EXIF $$$$$$$$$$$");
        for (String s : load_exif(new Aborter("EXIF",logger)))
        {
            logger.log(s);
            Text t = new Text(s);
            textFlow.getChildren().add(t);
            textFlow.getChildren().add(new Text(System.lineSeparator()));
        }
        logger.log("$$$$$$$$$$$$$$$$$$$$$$$$");
        ScrollPane sp = new ScrollPane();
        Look_and_feel_manager.set_region_look(sp);
        Look_and_feel_manager.set_region_look(textFlow);
        sp.setPrefSize(1000, 600);
        sp.setContent(textFlow);
        sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        Stage local_stage = new Stage();
        local_stage.setHeight(600);
        local_stage.setWidth(1000);

        Scene scene = new Scene(sp, 1000, 600);

        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        if ( extension.equalsIgnoreCase(Fusk_static_core.FUSK_EXTENSION))
        {
            if (Fusk_static_core.is_fusk(path,aborter, logger))
            {
                String base = FilenameUtils.getBaseName(path.toAbsolutePath().toString());
                local_stage.setTitle(Fusk_strings.defusk_string(base, logger));
            }
            else
            {
                local_stage.setTitle(path.toAbsolutePath()+"(has the extension but IS NOT a fusk!)");
            }
        }
        else
        {
            local_stage.setTitle(path.toAbsolutePath().toString());
        }
        local_stage.setScene(scene);
        local_stage.show();
        local_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE) {
                        local_stage.close();
                        key_event.consume();
                    }
                });
    }


    //**********************************************************
    void edit()
    //**********************************************************
    {
        Desktop d = Desktop.getDesktop();
        logger.log("asking desktop to EDIT: " + path.getFileName());
        try
        {
            d.edit(path.toFile());

            // we want the UI to refresh if the file is modified
            // we do not know when the edition will end so we need to start a watcher
            // with a 10 minute timer

            Filesystem_modification_reporter reporter = () -> {
                List<Old_and_new_Path> oanps = new ArrayList<>();
                Command_old_and_new_Path cmd = Command_old_and_new_Path.command_edit;
                Old_and_new_Path oan = new Old_and_new_Path(path, path, cmd, Status_old_and_new_Path.edition_requested, false);
                oanps.add(oan);
                Change_gang.report_changes(oanps);
            };
            Filesystem_item_modification_watcher ephemeral_filesystem_item_modification_watcher = new Filesystem_item_modification_watcher();
            // will die after 10 minutes
            if ( !ephemeral_filesystem_item_modification_watcher.init(path,reporter,false,10,logger))
            {
                logger.log("Warning: cannot start monitoring: "+path);
            }
        } catch (IOException e)
        {
            logger.log_stack_trace(e.toString());
        }
    }


    //**********************************************************
    void change_zoom_factor(Image_window image_window, double mul)
    //**********************************************************
    {
        image_window.mouse_handling_for_image_window.set_mouse_mode(image_window, Mouse_mode.pix_for_pix);

        // if image is smaller than screen
        if (( the_image_view.getImage().getHeight() < image_window.the_Scene.getHeight())
            &&(the_image_view.getImage().getWidth() < image_window.the_Scene.getWidth()))
        {
            the_image_view.setFitWidth(image_window.the_Scene.getWidth());
            the_image_view.setFitHeight(image_window.the_Scene.getHeight());
            return;
        }


        // depends on aspect ratio
        double image_aspect_ratio = the_image_view.getImage().getHeight()/the_image_view.getImage().getWidth();
        double scene_aspect_ratio =  image_window.the_Scene.getHeight()/ image_window.the_Scene.getWidth();

        if ( scene_aspect_ratio > image_aspect_ratio)
        {
            the_image_view.setFitWidth(image_window.the_Scene.getWidth());
            logger.log("change_zoom_factor setFitWidth"+image_window.the_Scene.getWidth());
        }
        else
        {
            the_image_view.setFitHeight(image_window.the_Scene.getHeight());
            logger.log("change_zoom_factor setFitHeight"+image_window.the_Scene.getHeight());
        }

        zoom_factor *= mul;
        logger.log("mul="+mul+" => new zoom_factor="+zoom_factor);

        double ww = image.getWidth();
        double dx = ww/10* zoom_factor;
        double W = ww - 2 * dx;
        logger.log("change_zoom_factor dx="+dx+" W="+W);
        if ( W < 0) return;

        double hh = image.getHeight();
        double dy = hh/10* zoom_factor;
        double H = hh - 2 * dy;
        logger.log("change_zoom_factor dy="+dy+" H="+H);
        if ( H < 0) return;
        Rectangle2D r = new Rectangle2D(dx, dy,W , H);
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
    private static List<String> load_keyword_exclusion_list(Logger logger)
    //**********************************************************
    {

        List<String> returned = new ArrayList<>();
        int max = Static_application_properties.get_excluded_keyword_list_max_size(logger);
        for (int i = 0; i < max; i++) {
            String key = Static_application_properties.EXCLUDED_KEYWORD_PREFIX + i;
            String kw = Static_application_properties.get_properties_manager(logger).get(key);
            if (kw != null) {
                String lower = kw.toLowerCase();
                returned.add(lower);
                logger.log("excluded key word: ->" + lower + "<-");
            }
        }
        return returned;
    }

    //**********************************************************
    void search_using_keywords_from_the_name(Browser b)
    //**********************************************************
    {
        logger.log("Image_context search_using_keywords_from_the_name");
        List<String> exclusion_list = load_keyword_exclusion_list(logger);
        Keyword_extractor ke = new Keyword_extractor(logger, exclusion_list);
        Set<String> keywords_set = ke.extract_keywords_from_file_and_dir_names(path);
        if (keywords_set == null) {
            logger.log("FATAL null keywords ??? ");
            return;
        }
        if (keywords_set.isEmpty()) {
            logger.log("FATAL no keywords ??? ");
            return;
        }
        List<String> keywords = new ArrayList<>();
        for (String k : keywords_set) {
            keywords.add(k.toLowerCase());
        }

        logger.log("---- looking at keywords -------");
        for (String s : keywords) {
            logger.log("->" + s + "<-");
        }
        logger.log("--------------------------------");

        Finder.find(path,b,keywords,logger);
    }



    List<String> given_keywords = new ArrayList<>();
    //**********************************************************
    void search_using_keywords_given_by_the_user(Browser b)
    //**********************************************************
    {
        logger.log("find()");
        ask_user_and_find(b, path, given_keywords, logger);
    }


    //**********************************************************
    public static void ask_user_and_find(
            Browser browser,
            Path target,
            List<String> keywords,
            Logger logger
    )
    //**********************************************************
    {
        logger.log("ask_user_and_find()");

        Platform.runLater( () -> {
            StringBuilder ttt = new StringBuilder();
            for (String ss : keywords) ttt.append(ss).append(" ");
            TextInputDialog dialog = new TextInputDialog(ttt.toString());
            Look_and_feel_manager.set_dialog_look(dialog);
            dialog.initOwner(browser.my_Stage.the_Stage);
            dialog.setTitle("Keywords");
            dialog.setHeaderText("Enter your keywords, separated by space");
            dialog.setContentText("Keywords:");

            logger.log("dialog !");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent())
            {
                String[] splited = result.get().split("\\s+");// split by any space
                if ( splited.length > 0)
                {
                    keywords.clear();
                    for (String s : splited)
                    {
                        String local = s.toLowerCase();
                        if ( keywords.contains(local)) continue;
                        keywords.add(s);
                    }

                    Finder.find(target,browser,keywords,logger);
                }
            }

        });
    }

    /*
    public void finder_shutdown()
    {
        if (finder_for_k != null) finder_for_k.shutdown();
    }
    */


    //**********************************************************
    void open()
    //**********************************************************
    {
        Desktop d = Desktop.getDesktop();
        logger.log("asking desktop to OPEN: " + path.getFileName());
        try
        {
            d.open(path.toFile());
        } catch (IOException e)
        {
            logger.log("open error:" + e);
        }
    }


    //**********************************************************
    boolean copy(Browser b, Runnable after)
    //**********************************************************
    {
        //if (Popups.popup_ask_for_confirmation(I18n.get_I18n_string("Warning", logger),
        //        I18n.get_I18n_string("Copy_are_you_sure", logger), logger) == false) return;

        Path new_path = null;
        for (int i = 0; i < 40000; i++)
        {
            // add 2 levels of folders names as prefix since a copy is usually moved afterward
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
            logger.log("prefix for copy ="+prefix);
            new_path = Moving_files.generate_new_candidate_name_special(path,prefix,i, logger);
            if (!Files.exists(new_path))
            {
                logger.log("new_path" + new_path+" does not exist");

                break;
            }
            else {
                logger.log("new_path" + new_path+" exists, retrying");

            }
        }
        if (new_path == null)
        {
            logger.log("copy failed: could not create new unused name for" + path.getFileName());
            return false;
        }
        logger.log("copy:" + path.getFileName()+ " copy name= "+new_path);

        try
        {
            Files.copy(path, new_path);
        } catch (IOException e)
        {
            logger.log("copy failed: could not create new file for: " + path.getFileName() + ", Exception:" + e);
            return false;
        }
        Actor_engine.execute(after,b.aborter,logger);
        //Popups.popup_text(I18n.get_I18n_string("Copy_done",logger),I18n.get_I18n_string("New_name",logger)+new_path.getFileName().toString(),false);
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(new Old_and_new_Path(
                path,
                new_path,
                Command_old_and_new_Path.command_copy,
                Status_old_and_new_Path.copy_done,false));
        Change_gang.report_changes(l);
        Image_window orphan = Image_window.get_Image_window(b,new_path, logger);
        return true;
    }




    //**********************************************************
    public Image_context rename_file_for_an_image_window(Image_window image_window)
    //**********************************************************
    {
        Path new_path =  Files_and_Paths.ask_user_for_new_file_name(image_window.the_Stage, path,logger);
        if ( new_path == null) return null;
        return image_window.change_name_of_file(new_path);
    }

    //**********************************************************
    Image_context ultim(Image_window image_stage)
    //**********************************************************
    {
        String old_file_name = path.getFileName().toString().toLowerCase();
        if (old_file_name.contains(Static_application_properties.ULTIM))
        {
            logger.log("no vote, name already contains " + Static_application_properties.ULTIM);
            return null;
        }

        Path new_path = Moving_files.generate_new_candidate_name(path,"",Static_application_properties.ULTIM, logger);
        return image_stage.change_name_of_file(new_path);
    }


}
