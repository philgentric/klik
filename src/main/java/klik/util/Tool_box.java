package klik.util;


import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import klik.change.Command_old_and_new_Path;
import klik.change.Old_and_new_Path;
import klik.change.Status_old_and_new_Path;
import klik.change.Change_gang;
import klik.browser.Browser;
import klik.browser.Icon_factory;
import klik.images.Decoded_image_engine;
import klik.images.Image_decode_request;
import klik.look.Look_and_feel_manager;
import klik.properties.Properties;
import klik.properties.Properties_manager;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;


//**********************************************************
public class Tool_box {
    private static final boolean dbg = false;
    public static final String VERTICAL_SCROLL = "vertical_scroll";
    public static final String SHOW_ICONS = "show_icons";

    private static LinkedBlockingQueue<Runnable> lbq;
    private static ExecutorService executor;
    static Icon_factory icon_factory = null;

    static Decoded_image_engine decoded_image_cache = null;


    private final static int pool_max = 200;




    //**********************************************************
    public static void safe_delete_all(List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {
        Path trash_dir = Properties.get_trash_dir(logger);
        List<Old_and_new_Path> l2 = new ArrayList<>();
        for (Old_and_new_Path oanf : l) {
            Path new_Path = (Paths.get(trash_dir.toString(), oanf.get_old_Path().getFileName().toString()));
            Old_and_new_Path oanf2 = new Old_and_new_Path(oanf.old_Path, new_Path, oanf.cmd, oanf.status);
            l2.add(oanf2);
        }

        logger.log("delete_fx: perform_the_move_in_a_javafx_Task1");

        Tool_box.perform_the_safe_moves_in_a_thread(l2, logger);

    }

    //**********************************************************
    public static void safe_delete_one(Path f, Logger logger)
    //**********************************************************
    {
        Path trash_dir = Properties.get_trash_dir(logger);
        List<Old_and_new_Path> l2 = new ArrayList<>();
        Path new_Path = (Paths.get(trash_dir.toString(), f.getFileName().toString()));
        Old_and_new_Path oanf2 = new Old_and_new_Path(f, new_Path, Command_old_and_new_Path.command_delete, Status_old_and_new_Path.before_command);
        l2.add(oanf2);
        Tool_box.perform_the_safe_moves_in_a_thread(l2, logger);

    }


    //**********************************************************
    public static Path safe_rename(Logger logger,
                                   Path target_file_to_be_renamed,
                                   String new_name)
    //**********************************************************
    {
        Path new_file = Paths.get(target_file_to_be_renamed.getParent().toAbsolutePath().toString(), new_name);
        List<Old_and_new_Path> l = new ArrayList<>();
        Command_old_and_new_Path cmd = Command_old_and_new_Path.command_rename;
        l.add(new Old_and_new_Path(target_file_to_be_renamed, new_file, cmd, Status_old_and_new_Path.before_command));

        logger.log("perform_the_move_in_a_javafx_Task6");
        Tool_box.perform_the_safe_moves_in_a_thread(l, logger);

        return new_file;
    }


    //**********************************************************
    public static void safe_move_files_or_dirs(Path destination_dir, Logger logger, List<File> the_files_being_moved)
    //**********************************************************
    {
        List<Old_and_new_Path> oanl = new ArrayList<Old_and_new_Path>();
        for (File the_file_being_moved : the_files_being_moved) {
            Path old_Path_ = the_file_being_moved.toPath();
            Path new_Path_ = Paths.get(destination_dir.toAbsolutePath().toString(), the_file_being_moved.getName());
            Command_old_and_new_Path cmd_ = Command_old_and_new_Path.command_move;
            Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status_old_and_new_Path.rename_done);
            oanl.add(oan);
        }
        Tool_box.perform_the_safe_moves_in_a_thread(oanl, logger);
    }

    //**********************************************************
    public static void safe_move_a_file_or_dir(Path destination_dir, Logger logger, File the_file_being_moved)
    //**********************************************************
    {
        List<Old_and_new_Path> oanl = new ArrayList<Old_and_new_Path>();
        Path old_Path_ = the_file_being_moved.toPath();
        Path new_Path_ = Paths.get(destination_dir.toAbsolutePath().toString(), the_file_being_moved.getName());
        Command_old_and_new_Path cmd_ = Command_old_and_new_Path.command_move;
        Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status_old_and_new_Path.rename_done);
        oanl.add(oan);
        Tool_box.perform_the_safe_moves_in_a_thread(oanl, logger);
    }

    //**********************************************************
    public static void perform_the_safe_moves_in_a_thread(
            List<Old_and_new_Path> the_list,
            Logger logger)
    //**********************************************************
    {
        if (the_list == null) {
            logger.log("FATAL1 in perform_the_move_in_a_thread()");
            return;

        }
        if (the_list.isEmpty()) {
            logger.log("FATAL2 in perform_the_move_in_a_thread()");
            return;

        }
        logger.log("perform_the_move_in_a_thread()");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                actual_safe_move(the_list, logger);
            }
        };
        try {
            execute(r, logger);
            logger.log("thread COUNT=" + Thread.activeCount());
        } catch (RejectedExecutionException ree) {
            logger.log("perform_the_move_in_a_thread()" + ree);

        }


    }

    //**********************************************************
    private static void actual_safe_move(
            List<Old_and_new_Path> the_list,
            Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> the_list2 = new ArrayList<>();
        for (Old_and_new_Path oandn : the_list) {
            logger.log("Toolbox::actual_move() +"+oandn.old_Path.getFileName()+" changed into "+oandn.new_Path.getFileName());
            //File new_file_local = oandn.new_file;
            // MAGIC: try up to 2000 new names
            for (int i = 0; i < 2000; i++) {
                if (Files.exists(oandn.get_new_Path()) == false) {
                    Old_and_new_Path oandn2 = do_the_rename(logger, oandn);
                    the_list2.add(oandn2);
                    break;
                }
                Path new_path = generate_new_candidate_name(oandn.get_new_Path(), i, logger);
                oandn = new Old_and_new_Path(oandn.old_Path, new_path, oandn.cmd, oandn.status);
            }
        }
        Change_gang.report_event(the_list2);
    }

    //**********************************************************
    public static Path generate_new_candidate_name(Path new_file_local, int i, Logger logger)
    //**********************************************************
    {
        logger.log("looking for a new name for:" + new_file_local.toAbsolutePath());
        String new_name = new_file_local.getFileName().toString();
        {
            int last_index = new_file_local.getFileName().toString().lastIndexOf('.');
            // capture extension, if any
            String extension = "";
            if (last_index >= 0) {
                extension = new_file_local.getFileName().toString().substring(last_index, new_file_local.getFileName().toString().length());
                new_name = new_file_local.getFileName().toString().substring(0, last_index);
            }

            new_name += "_" + i;
            // restore extension
            new_name += extension;
        }

        return Paths.get(new_file_local.getParent().toString(), new_name);
    }


    //**********************************************************
    private static Old_and_new_Path do_the_rename(Logger logger, Old_and_new_Path oandn)
    //**********************************************************
    {
        if ( oandn.get_old_Path().toAbsolutePath().toString().equals( oandn.get_new_Path().toAbsolutePath().toString()))
        {
            return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.same_path);

        }
        try {
            Files.move(oandn.get_old_Path(), oandn.get_new_Path());
            return rename_success(logger, oandn);

        } catch (IOException e) {
            return rename_failed(logger, oandn, e);
        }
    }


    //**********************************************************
    private static Old_and_new_Path rename_failed(Logger logger,
                                                  //Properties_manager the_properties_manager,
                                                  Old_and_new_Path oandn,
                                                  IOException e)
    //**********************************************************
    {
        if (Files.exists(oandn.get_new_Path().getParent()) == false) {
            logger.log("FAILED to move file, target dir does not exists->" + oandn.get_new_Path().getParent() + "<-" + e);


            Path path = oandn.get_new_Path().getParent();
            if (Properties.get_properties_manager().remove_invalid_dir(path)) {
                logger.log("rename_failed because dir does not exist, so it has been removed from properties : " + path);
            } else {
                logger.log("WARNING: rename_failed because dir does not exist, but it could NOT be removed from properties : " + path);
            }

            return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.target_dir_does_not_exist);
        } else {
            logger.log("destination folder exists but ... FAILED to move file for some other reason->" + oandn.get_old_Path().toAbsolutePath() +
                    "<- into ->" + oandn.get_new_Path().toAbsolutePath() + "<-\n" + e);

            {
                // ok so we try to COPY instead
                // for external drives (e.g. USB) it may make a difference for FOLDERS
                // that is: move works for individual files, but nt for folders...
                // for reasons that are a bit mysterious to me?
                // surely related to filesystem's item's ownership stuff?
                try
                {
                    Files.copy(oandn.get_old_Path(), oandn.get_new_Path());
                    popup_text("WARNING", "We tried MOVING:" + oandn.get_old_Path().toAbsolutePath()
                                    +"<- into ->" + oandn.get_new_Path().toAbsolutePath() + "<- and it FAILED, so we tried to COPY instead and THAT WORKED!" );
                    logger.log("YOUHOU! we tried moving a file/dir and it failed, so we tried to copy instead and worked!" + oandn.get_old_Path().toAbsolutePath() +
                            "<- into ->" + oandn.get_new_Path().toAbsolutePath() + "<-\n" + e);
                    return new Old_and_new_Path(oandn.old_Path, oandn.new_Path,Command_old_and_new_Path.command_copy , Status_old_and_new_Path.copy_done);

                } catch (IOException ex)
                {
                    logger.log("FATAL! we tried moving a file/dir and it failed, so we tried to copy instead and is ALSO failed!" + oandn.get_old_Path().toAbsolutePath() +
                            "<- into ->" + oandn.get_new_Path().toAbsolutePath() + "<-\n" + e);
                }

            }
            return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.command_failed);

        }
    }


    //**********************************************************
    private static Old_and_new_Path rename_success(Logger logger,
                                                   //Properties_manager the_properties_manager,
                                                   Old_and_new_Path oandn)
    //**********************************************************
    {
        logger.log("OK cmd:" + oandn.get_cmd() + ":\nold:" + oandn.get_old_Path().toAbsolutePath() +
                "\nnew:" + oandn.get_new_Path().toAbsolutePath());
        /*
        if (oandn.get_cmd() != Command_old_and_new_Path.command_rename) {
            the_properties_manager.save_multiple(Constants.MOVE_DIR, oandn.get_new_Path().getParent().toAbsolutePath().toString());
        }*/
        switch (oandn.get_cmd()) {
            case command_delete:
                return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.deletion_done);
            case command_edit:
                return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.edition_requested);
            case command_move:
                return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.move_done);
            case command_rename:
                return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.rename_done);
            case command_unknown:
            default:
                return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.command_failed);

        }
    }



    //**********************************************************
    public static void list_runnables(Logger logger)
    //**********************************************************
    {
        logger.log("executor service: currently listed runnables");
        for (Runnable r : lbq) {
            logger.log("runnable:        " + r.toString());
        }
    }


    //**********************************************************
    public static void execute(Runnable r, Logger logger)
    //**********************************************************
    {
        if (executor == null) create_executor();
        executor.execute(r);
        if (lbq.size() >= pool_max) {
            logger.log("WARNING: thread pool too small, pool max =" + pool_max);
            if (dbg) list_runnables(logger);
        }
    }


    //**********************************************************
    private static void create_executor()
    //**********************************************************
    {
        int n_threads = 10 * Runtime.getRuntime().availableProcessors();
        lbq = new LinkedBlockingQueue<>();
        executor = new ThreadPoolExecutor(n_threads, pool_max, 10, TimeUnit.SECONDS, lbq);
    }

    //**********************************************************
    public static int how_many_runnables()
    //**********************************************************
    {
        if (lbq == null) return 0;
        return lbq.size();
    }






    //**********************************************************
    public static void accept_drag_dropped_as_a_move_in(
            DragEvent event,
            Path destination_dir,
            Scene excluded,
            String origin,
            Logger logger)
    //**********************************************************
    {
        /*
        Object source = event.getGestureSource();
        if (source == null) {
            logger.log("source class is null for" + event.toString());
        } else {
            if ( source instanceof Item)
            {
                //logger.log("source class is:" + source.getClass().getName());
                Item item = (Item) source;
                Scene scene_of_source = item.getScene();

                // data is dragged over the target
                // accept it only if it is not dragged from the same node
                if (scene_of_source == excluded) {
                    logger.log("drag reception for stage: same scene, giving up<<");
                    event.consume();
                    return;
                }
            }

        }*/

        Dragboard db = event.getDragboard();


        String s = db.getString();
        if (s != null) {
            logger.log(origin + " drag ACCEPTED for STRING: " + s);
            List<File> list = new ArrayList<>();
            for (String ss : s.split("\\r?\\n")) {
                if (ss.isBlank()) continue;
                logger.log(origin + " drag ACCEPTED for additional file: " + ss);
                list.add(new File(ss));
            }
            Tool_box.safe_move_files_or_dirs(destination_dir, logger, list);

            Tool_box.popup_text("special multi Drag and Drop", list.size() + " files moved!");
        } else {
            List<File> l = db.getFiles();
            for (File fff : l) {
                logger.log(origin + " drag ACCEPTED for: " + fff.getAbsolutePath());
                Tool_box.safe_move_a_file_or_dir(destination_dir, logger, fff);
            }
        }


        // tell the source
        event.setDropCompleted(true);
        event.consume();
    }



    //**********************************************************
    private static void rename_file_or_dir(Browser browser, Path path, Logger logger)
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog(path.getFileName().toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Enter new name here:");
        dialog.setContentText(null);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Rename");

        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            change_dir_name(//browser,
                    path, logger, result.get());

        }

    }

    //**********************************************************
    public static Path change_dir_name(Path path, Logger logger, String result)
    //**********************************************************
    {
        if (dbg) logger.log("change_dir_name, new name: " + result);

        try {
            logger.log("trying rename: " + path.getFileName() + " => " + result);
            Path new_path = Paths.get(path.getParent().toString(), result);
            Files.move(path, new_path);
            logger.log("....done");
            return new_path;
        }
        catch (AccessDeniedException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            Tool_box.popup_Exception(e, 200, logger);
        } catch (FileSystemException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            Tool_box.popup_Exception(e, 200, logger);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return path;
    }

    //**********************************************************
    public static void popup_Exception(Exception e, double icon_size, Logger logger)
    //**********************************************************
    {
        logger.log(Stack_trace_getter.get_stack_trace("Going to popup exception(1): "+e));
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Informative");
                alert.setHeaderText("Operation was denied");
                alert.setContentText(
                        "The error was: \n" + e);

                logger.log("Going to popup exception(2): "+e);
                alert.setGraphic(new ImageView(Look_and_feel_manager.get_denied_icon(icon_size)));
                alert.showAndWait();
            }
        });
    }

    //**********************************************************
    public static void popup_text(String header, String content)
    //**********************************************************
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Informative");
                alert.setHeaderText(header);
                alert.setContentText(content);
                Optional<ButtonType> sre = alert.showAndWait();
            }
        });
    }

    //**********************************************************
    public static boolean popup_ask_for_confirmation(String header, String content)
    //**********************************************************
    {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Please confirm!");
        alert.setHeaderText(header);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            // ... user chose OK
            return true;
        } else {
            // ... user chose CANCEL or closed the dialog
            return false;
        }

    }


    //**********************************************************
    public static Icon_factory get_icon_factory(Logger logger)
    //**********************************************************
    {
        if (icon_factory == null) {
            icon_factory = new Icon_factory(logger);
        }
        return icon_factory;
    }


    //**********************************************************
    public static void inject_image_decode_request(Image_decode_request r, Logger logger)
    //**********************************************************
    {
        if (decoded_image_cache == null) decoded_image_cache = new Decoded_image_engine(logger);

        decoded_image_cache.inject(r);
        //logger.log("injected image decoding request for:"+r.get_string() );
    }


    //**********************************************************
    public static Path get_icon_cache_dir(Logger logger)
    //**********************************************************
    {
        Properties_manager pm = Properties.get_properties_manager();
        Path tmp_dir = null;
        String icon_cache_dir_name = pm.get(Constants.ICON_CACHE_DIR_KEY);
        if (dbg) logger.log("icon cache dir name=" + icon_cache_dir_name);
        if (icon_cache_dir_name == null) {
            // inject default
            tmp_dir = Properties.get_absolute_dir(logger, Constants.ICON_CACHE_DIR_KEY);
            pm.imperative_store(Constants.ICON_CACHE_DIR_KEY, tmp_dir.toAbsolutePath().toString(), true);
        } else {
            tmp_dir = (new File(icon_cache_dir_name)).toPath();
        }
        if (dbg) logger.log("icon dir file=" + tmp_dir.toAbsolutePath());
        if (Files.exists(tmp_dir) == false) {

            try {
                Files.createDirectories(tmp_dir);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        " Attempt to create icon cache dir named->" + tmp_dir.toAbsolutePath() + "<- failed",
                        "Failed", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        }
        return tmp_dir;
    }




    //**********************************************************
    public static long get_size_on_disk(Path path,Logger logger)
    //**********************************************************
    {
        try {
            long res = Files.walk(path)
                    .map(f -> f.toFile())
                    .filter(f -> f.isFile())
                    .mapToLong(f -> f.length()).sum();
            return res;
        }
        catch(Exception e)
        {
            logger.log("get_size_on_disk failed: "+e);
        }
        return -1;
    }


    //**********************************************************
    public static long get_how_many_files(Path path,Logger logger)
    //**********************************************************
    {
        try {
            long res = Files.walk(path)
                    .map(f -> f.toFile())
                    .filter(f -> f.isFile())
                    .count();
            return res;
        }
        catch(Exception e)
        {
            logger.log("get_how_many_files failed: "+e);
        }
        return -1;
    }
    //**********************************************************
    public static long get_how_many_images(Path path,Logger logger)
    //**********************************************************
    {
        try {
            long res = Files.walk(path)
                    .filter(f -> Guess_file_type_from_extension.is_this_path_an_image(f))
                    .count();
            return res;
        }
        catch(Exception e)
        {
            logger.log("get_how_many_images failed: "+e);
        }
        return -1;
    }

    //**********************************************************
    public static long get_how_many_folders(Path path,Logger logger)
    //**********************************************************
    {
        try {
            long res = Files.walk(path)
                    .map(f -> f.toFile())
                    .filter(f -> f.isDirectory())
                    .count();
            return res;
        }
        catch(Exception e)
        {
            logger.log("get_how_many_folders failed: "+e);
        }
        return -1;
    }

    //**********************************************************
    public static void clear_trash(Logger logger)
    //**********************************************************
    {
        Path trash = Properties.get_trash_dir(logger);

        long size =  get_size_on_disk(trash,logger);

        if (false == popup_ask_for_confirmation("Warning: no recovery after delete", size/1000000+"MB of files will be truly deleted")) return;

        delete_all_files_in(trash, logger);
    }

    //**********************************************************
    private static void delete_all_files_in(Path dir, Logger logger)
    //**********************************************************
    {

        Runnable r = new Runnable() {
            @Override
            public void run() {
                delete_all_files_in_core(dir, logger);
            }
        };

        execute(r, logger);

    }

    //**********************************************************
    private static void delete_all_files_in_core(Path dir, Logger logger)
    //**********************************************************
    {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))//, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {
            for (Path p : stream) {
                if (Files.isDirectory(p))
                {
                    delete_all_files_in_core(p,logger);
                }
                try {
                    Files.delete(p);
                } catch (NoSuchFileException x) {
                    logger.log("no such file or directory:" + p);
                } catch (DirectoryNotEmptyException x) {
                    logger.log(p + " directory not empty");
                }

            }
        } catch (IOException x) {
            // File permission problems are caught here.
            logger.log("" + x);
        }

    }

    //**********************************************************
    public static void clear_icon_cache(Logger logger)
    //**********************************************************
    {
        Path icons = get_icon_cache_dir(logger);

        long size = Tool_box.get_size_on_disk(icons,logger);

        if (false == popup_ask_for_confirmation("Warning: \nDeleting icons will slow display the next time the directory is re-visited,\nsize on disk to be reclaimed :",size/1000000L+" MB of files "))
            return;


        delete_all_files_in(icons, logger);

    }

    //**********************************************************
    public static boolean get_sort_files_by_name()
    //**********************************************************
    {
    String s = Properties.get_properties_manager().get("sort_files_by_name");
        if (s == null) {
            Properties.get_properties_manager().save_unico("sort_files_by_name", "false");
        return false;
    } else {
        return Boolean.valueOf(s);
    }
}

    //**********************************************************
    public static void set_sort_files_by_name(boolean b)
    //**********************************************************
    {
        Properties.get_properties_manager().save_unico("sort_files_by_name", "" + b);
    }

    //**********************************************************
    public static boolean get_show_icons()
    //**********************************************************
    {
        String s = Properties.get_properties_manager().get(SHOW_ICONS);
        if (s == null) {
            Properties.get_properties_manager().save_unico(SHOW_ICONS, "false");
            return false;
        } else {
            return Boolean.valueOf(s);
        }
    }

    //**********************************************************
    public static void set_show_icons(boolean b)
    //**********************************************************
    {
        Properties.get_properties_manager().save_unico(SHOW_ICONS, "" + b);

    }

    //**********************************************************
    public static void set_vertical_scroll(boolean b)
    //**********************************************************
    {
        Properties.get_properties_manager().save_unico(VERTICAL_SCROLL, "" + b);
    }

    //**********************************************************
    public static boolean get_vertical_scroll()
    //**********************************************************
    {
        String s = Properties.get_properties_manager().get(VERTICAL_SCROLL);
        if (s == null) {
            Properties.get_properties_manager().save_unico(VERTICAL_SCROLL, "true");
            return true;
        } else {
            return Boolean.valueOf(s);
        }
    }

    //**********************************************************
    public static long get_remaining_memory()
    //**********************************************************
    {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        long remaining = max - used;
        return remaining;
    }


    //**********************************************************
    public static String MAKE_CACHE_NAME(Path f, int icon_size_)
    //**********************************************************
    {
        String full_name = f.toAbsolutePath().toString();
        return CLEAN_NAME(full_name) + icon_size_+".png";
    }
    //**********************************************************
    public static String CLEAN_NAME(String s)
    //**********************************************************
    {
        s = s.replace("/", "_");
        s = s.replace(".", "_");
        s = s.replace(" ", "_");
        return s;
    }

    //**********************************************************
    public static String get_2_line_string_with_size(Path path)
    //**********************************************************
    {
        long size = path.toFile().length();
        String file_size = size + " Bytes";
        double MB = (double) size / 1000000.0;
        if (size > 1000000)
        {
            file_size += "\n" + String.format("%.1f",MB) + " MB";
        }
        else
        {
            if (size > 1000)
            {
                file_size += "\n" + String.format("%.1f", (double)size / 1000.0) + " kB";
            }
        }
        return file_size;
    }

    public static String get_1_line_string_with_size(long size)
    {
        String returned;
        if ( size < 10000)
        {
            returned = size+" B";
        }
        else if ( size < 1000000)
        {
            returned = String.format("%.1f",(double)size/1000.0)+" kB";
        }
        else if ( size < 1000000000)
        {
            returned = String.format("%.1f",(double)size/1000000.0)+" MB";
        }
        else
        {
            returned = String.format("%.1f",(double)size/1000000000.0)+" GB";
        }
        return returned;
    }





    /*
        //**********************************************************
    static public void create_dir(
            Directory_display directory_display,
            File ff,
            Logger logger)
    //**********************************************************
    {
        TextInputDialog dialog = new TextInputDialog("newDir");
        dialog.setTitle("New directory ");
        dialog.setHeaderText("Please give the name of the New Directory here below:");
        dialog.setContentText(null);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Create");

        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            logger.log("new dir: " + result.get());

            File new_dir = new File(ff, result.get());
            boolean status = new_dir.mkdir();
            if (status) {
                directory_display.set_target_dir_to_display(ff);
            } else {
                Alert a = new Alert(AlertType.ERROR, "mkdir failed for " + new_dir.getAbsolutePath() + ", probably you do not have the rights?");
                a.showAndWait();
            }
        }

    }

     */

}
