package klik.files_and_paths;


import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.icons.Error_type;
import klik.util.Threads;
import klik.change.Change_gang;
import klik.deduplicate.console.Deduplication_console_interface;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_writer_actor;
import klik.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Files_and_Paths {

    private static final boolean dbg = false;

    //**********************************************************
    public static void unsafe_delete_files(Stage owner, List<Old_and_new_Path> l, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("unsafe_delete_all: perform_safe_moves_in_a_thread");
        Moving_files.perform_safe_moves_in_a_thread(owner, l, aborter, false, logger);

    }

    //**********************************************************
    public static void unsafe_delete_file(Stage owner, Old_and_new_Path oanp, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("unsafe_delete_all: perform_safe_moves_in_a_thread");
        Moving_files.perform_safe_move_in_a_thread(owner, oanp, aborter, false, true, logger);

    }

    //**********************************************************
    public static void safe_delete_file(Stage owner, Old_and_new_Path oanf, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path trash_dir = Static_application_properties.get_trash_dir(logger);
        List<Old_and_new_Path> l2 = new ArrayList<>();

        Path new_Path = (Paths.get(trash_dir.toString(), oanf.get_old_Path().getFileName().toString()));
        Old_and_new_Path oanf2 = new Old_and_new_Path(oanf.old_Path, new_Path, oanf.cmd, oanf.status);
        l2.add(oanf2);

        logger.log("safe_delete_all: perform_safe_moves_in_a_thread");

        Moving_files.perform_safe_moves_in_a_thread(owner, l2, aborter, true, logger);

    }

    //**********************************************************
    public static void safe_delete_files(Stage owner, List<Old_and_new_Path> l, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path trash_dir = Static_application_properties.get_trash_dir(logger);
        List<Old_and_new_Path> l2 = new ArrayList<>();
        for (Old_and_new_Path oanf : l) {
            Path new_Path = (Paths.get(trash_dir.toString(), oanf.get_old_Path().getFileName().toString()));
            Old_and_new_Path oanf2 = new Old_and_new_Path(oanf.old_Path, new_Path, oanf.cmd, oanf.status);
            l2.add(oanf2);
        }

        logger.log("safe_delete_all: perform_safe_moves_in_a_thread");

        Moving_files.perform_safe_moves_in_a_thread(owner, l2, aborter, true, logger);

    }

    //**********************************************************
    public static void move_to_trash(Stage owner, Path f, Runnable after_the_move, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path trash_dir = Static_application_properties.get_trash_dir(logger);
        if (f.getParent().toAbsolutePath().toString().equals(trash_dir.toAbsolutePath().toString())) {
            Popups.popup_warning(owner, I18n.get_I18n_string("Nothing_done", logger), I18n.get_I18n_string("Nothing_done_explained", logger), false, logger);
            return;
        }
        List<Old_and_new_Path> l2 = new ArrayList<>();
        Path new_Path = (Paths.get(trash_dir.toString(), f.getFileName().toString()));
        Old_and_new_Path oanf2 = new Old_and_new_Path(f, new_Path, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.before_command);
        oanf2.run_after = after_the_move;
        l2.add(oanf2);
        Moving_files.perform_safe_moves_in_a_thread(owner, l2, aborter, true, logger);

    }


    //**********************************************************
    public static void remove_empty_folders(Path p, boolean recursively, Logger logger)
    //**********************************************************
    {
        if (Files.isDirectory(p)) {
            File dir = p.toFile();
            delete_empty_dirs(dir, recursively, logger);
        }
    }

    //**********************************************************
    // returns true if the dir was deleted (because it was empty)
    private static boolean delete_empty_dirs(File dir, boolean recursively, Logger logger)
    //**********************************************************
    {
        int deleted = 0;
        for (; ; ) {
            File[] files = dir.listFiles();
            if ( files == null) return false;
            if (files.length == 0) {
                boolean status = dir.delete();
                if (status) {
                    logger.log("Empty directory deleted: " + dir.getAbsolutePath());
                    return true;
                } else {
                    logger.log("Empty directory NOT deleted: " + dir.getAbsolutePath());
                    return false;
                }
            }
            if (!recursively) return false;
            int count = 0;
            for (File f : files) {
                if (f.isDirectory()) {
                    if (delete_empty_dirs(f, true, logger)) {
                        deleted++;
                        count++;
                    }
                }
            }
            if (count == 0) break;
            // if an empty dir was removed then maybe THIS directory is now empty? need to retry!
            logger.log("Empty sub-directories deleted, will retry " + dir.getAbsolutePath());
        }
        if (deleted > 0) return true;
        return false;
    }



    //**********************************************************
    public static Path get_icon_cache_dir(Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Static_application_properties.get_absolute_dir(logger, Static_application_properties.ICON_CACHE_DIR);
        if (dbg) if (tmp_dir != null) {
            logger.log("icon dir file=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }
    //**********************************************************
    public static Path get_folder_icon_cache_dir(Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Static_application_properties.get_absolute_dir(logger, Static_application_properties.FOLDER_ICON_CACHE_DIR);
        if (dbg) if (tmp_dir != null) {
            logger.log("folder icon dir file=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    public static void clear_one_icon_from_cache_on_disk(Path path, Logger logger)
    //**********************************************************
    {
        Path icon_cache_dir = get_icon_cache_dir(logger);
        int icon_size = Static_application_properties.get_icon_size(logger);
        String name = Icon_writer_actor.make_cache_name(path, String.valueOf(icon_size), Icon_factory_actor.png_extension);
        Path icon_path = Path.of(icon_cache_dir.toAbsolutePath().toString(), name);
        try {
            Files.delete(icon_path);
            logger.log("one icon deleted from cache:" + name);

        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: deleting one icon FAILED: " + e));
        }
    }


    //**********************************************************
    public static void clear_icon_cache_on_disk(Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path icons = get_icon_cache_dir(logger);

        double size = get_size_on_disk(icons, aborter, logger);

        String s1 = I18n.get_I18n_string("Warning_deleting_icon", logger);
        String s2 = size / 1000_000_000.0 + I18n.get_I18n_string("GB_deleted", logger);
        if (size < 1_000_000_000) {
            s2 = size / 1000_000.0 + I18n.get_I18n_string("MB_deleted", logger);
        }
        if (!Popups.popup_ask_for_confirmation(owner, s1, s2, logger)) return;
        delete_for_ever_all_files_in_dir_in_a_thread(icons, logger);
    }

    //**********************************************************
    public static void clear_folder_icon_cache_on_disk(Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path icons = get_folder_icon_cache_dir(logger);

        double size = get_size_on_disk(icons, aborter, logger);

        String s1 = "Warning deleting folder icons";//I18n.get_I18n_string("Warning_deleting_icon", logger);
        String s2 = size / 1000_000_000.0 + I18n.get_I18n_string("GB_deleted", logger);
        if (size < 1_000_000_000) {
            s2 = size / 1000_000.0 + I18n.get_I18n_string("MB_deleted", logger);
        }
        if (!Popups.popup_ask_for_confirmation(owner, s1, s2, logger)) return;
        delete_for_ever_all_files_in_dir_in_a_thread(icons, logger);
    }

    //**********************************************************
    public static void clear_trash(Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path trash = Static_application_properties.get_trash_dir(logger);
        double size = get_size_on_disk(trash, aborter, logger);
        String s1 = I18n.get_I18n_string("Warning_delete", logger);
        String s2 = size / 1000_000 + I18n.get_I18n_string("MB_deleted", logger);//"MB of files will be truly deleted";
        if (size > 1000_000_000) {
            s2 = size / 1000_000_000 + I18n.get_I18n_string("GB_deleted", logger);//"MB of files will be truly deleted";

        }
        if (!Popups.popup_ask_for_confirmation(owner, s1, s2, logger)) return;
        delete_for_ever_all_files_in_dir_in_a_thread(trash, logger);
    }


    //**********************************************************
    public static long get_size_on_disk(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //get_size_on_disk_with_streams(path,logger);
        // the concurrent version is at least 5 times faster!
        return get_size_on_disk_concurrent(path,aborter, logger);
    }

    //**********************************************************
    public static Sizes get_sizes_on_disk2(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        File[] all_files = path.toFile().listFiles();
        if ( all_files == null)
        {
            Error_type error = Files_and_Paths.explain_error(path,logger);
            logger.log(error+ " get_sizes_on_disk2: cannot scan folder "+path);
            return new Sizes(0,0,0,0);
        }
        int folders = 0;
        int files = 0;
        int images = 0;
        long bytes = 0;

        for (File f : all_files)
        {
            if (aborter.should_abort())
            {
                logger.log("ABORTED2: Disk_scanner for "+path);
                break;
            }
            if (f.isDirectory())
            {
                folders++;
            }
            else
            {
                files++;
                if( Guess_file_type.is_file_a_image(f)) images++;
                bytes += f.length();
            }
        }
        return new Sizes(bytes,folders,files,images);

    }
    //**********************************************************
    public static Sizes get_sizes_on_disk(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //get_size_on_disk_with_streams(path,logger);
        // the concurrent version is at least 5 times faster!
        return get_sizes_on_disk_concurrent(path,aborter, logger);
    }
    /*
    //**********************************************************
    public static long get_size_on_disk_with_streams(Path path, Logger logger)
    //**********************************************************
    {
        // the concurrent version is significantly faster
        long start = System.currentTimeMillis();
        try {
            long res = Files.walk(path)
                    .map(f -> f.toFile())
                    .filter(f -> f.isFile())
                    //.mapToDouble(f -> f.length()).sum();
                    .mapToLong(f -> f.length()).sum();

            long now = System.currentTimeMillis();

            logger.log("get_size_on_disk_with_streams: " + (now-start)+" size = "+res);

            return res;
        } catch (Exception e) {
            logger.log("get_size_on_disk failed: " + e);
        }
        return -1;
    }
*/

    //**********************************************************
    public static Sizes get_sizes_on_disk_concurrent(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace("Stupid2: not a folder: "+path));
            return null;
        }
        //long start = System.currentTimeMillis();
        AtomicInteger folders = new AtomicInteger(0);
        AtomicLong bytes = new AtomicLong(0);
        AtomicLong files = new AtomicLong(0);
        AtomicLong images = new AtomicLong(0);

        File_payload fp = f -> {
            bytes.addAndGet(f.length());
            files.incrementAndGet();
            if ( Guess_file_type.is_file_a_image(f)) images.incrementAndGet();
        };
        Dir_payload dp = f -> {
            folders.incrementAndGet();
            //System.out.println("folder:"+f.getName());
        };
        Disk_scanner.process_folder(path, fp,dp, aborter, logger);


        //long now = System.currentTimeMillis();
        //logger.log("get_size_on_disk_concurrent: " + (now-start)+" size=" +bytes.get());
        return new Sizes(bytes.get(),folders.get(),files.get(),images.get());
    }


    //**********************************************************
    public static long get_size_on_disk_concurrent(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace("Stupid3: not a folder: "+path));
            return 0L;
        }
        // this is a blocking call
        //long start = System.currentTimeMillis();
        AtomicLong bytes = new AtomicLong(0);
        File_payload fp = f -> bytes.addAndGet(f.length());
        Disk_scanner.process_folder(path, fp,null,aborter,logger);
        //long now = System.currentTimeMillis();
        //logger.log("get_size_on_disk_concurrent: " + (now-start)+" size=" +size.get());
        return bytes.get();
    }



    //**********************************************************
    public static long get_how_many_files_down_the_tree(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //return get_how_many_files_streams(path,logger);
        return get_how_many_files_concurrent(path,aborter, logger);
    }

    //**********************************************************
    private static long get_how_many_files_concurrent(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace("Stupid4: not a folder: "+path));
            return 0L;
        }
        AtomicLong files = new AtomicLong(0);
        File_payload fp = f -> files.incrementAndGet();
        Disk_scanner.process_folder(path, fp,null, aborter, logger);
        return files.get();
    }

    /*

    //**********************************************************
    public static long get_how_many_folders(Path path, Logger logger)
    //**********************************************************
    {
        try {
            long res = Files.walk(path)
                    .map(f -> f.toFile())
                    .filter(f -> f.isDirectory())
                    .count();
            return res;
        } catch (Exception e) {
            logger.log("get_how_many_folders failed: " + e);
        }
        return -1;
    }

    //**********************************************************
    public static long get_how_many_files_streams(Path path, Logger logger)
    //**********************************************************
    {
        try {
            long res = Files.walk(path)
                    .map(f -> f.toFile())
                    .filter(f -> f.isFile())
                    .count();
            return res;
        } catch (Exception e) {
            logger.log("get_how_many_files failed: " + e);
        }
        return -1;
    }


    //**********************************************************
    public static long get_how_many_images(Path path, Logger logger)
    //**********************************************************
    {
        try {
            long res = Files.walk(path)
                    .filter(f -> Guess_file_type.is_this_path_an_image(f))
                    .count();
            return res;
        } catch (Exception e) {
            logger.log("get_how_many_images failed: " + e);
        }
        return -1;
    }


    //**********************************************************
    public static long get_size_on_disk_excluding_sub_folders(Path path, Logger logger)
    //**********************************************************
    {
        if (!path.toFile().isDirectory()) return 0;
        long returned = 0;
        File files[] = path.toFile().listFiles();
        for (File f : files) {
            if (f.isDirectory()) continue;
            returned += f.length();
        }
        return returned;
    }
     */

    //**********************************************************
    public static String get_1_line_string_for_byte_data_size(double size)
    //**********************************************************
    {
        String returned;
        if (size < 1000) {
            returned = size + " B";
        } else if (size < 1000_000) {
            returned = String.format("%.1f", size / 1000.0) + " kB";
        } else if (size < 1000_000_000) {
            returned = String.format("%.1f", size / 1000_000.0) + " MB";
        } else if (size < 1000_000_000_000.0) {
            returned = String.format("%.1f", size / 1000_000_000.0) + " GB";
        } else {
            returned = String.format("%.1f", size / 1000_000_000_000.0) + " TB";
        }
        return returned;
    }


    //**********************************************************
    public static String get_2_line_string_with_size(Path path, Logger logger)
    //**********************************************************
    {
        long size = path.toFile().length();
        String bytes = I18n.get_I18n_string("Bytes", logger);
        String kbytes = I18n.get_I18n_string("KBytes", logger);
        String mbytes = I18n.get_I18n_string("MBytes", logger);
        String file_size = size + " " + bytes;
        if (size > 1000_000_000) {
            double GB = (double) size / 1000_000_000.0;
            file_size += "\n" + String.format("%.1f", GB) + " " + I18n.get_I18n_string("GBytes", logger);
        } else if (size > 1000_000) {
            double MB = (double) size / 1000_000.0;
            file_size += "\n" + String.format("%.1f", MB) + " " + mbytes;
        } else {
            if (size > 1000) {
                file_size += "\n" + String.format("%.1f", (double) size / 1000.0) + " " + kbytes;
            }
        }
        return file_size;
    }



    //**********************************************************
    private static void delete_for_ever_all_files_in_dir_in_a_thread(Path dir, Logger logger)
    //**********************************************************
    {

        Runnable r = () -> delete_for_ever_all_files_in_dir(dir, logger);

        Threads.execute(r, logger);

    }

    //**********************************************************
    private static void delete_for_ever_all_files_in_dir(Path dir, Logger logger)
    //**********************************************************
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))//, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {
            List<Old_and_new_Path> l = new ArrayList<>();
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    delete_for_ever_all_files_in_dir(p, logger);
                }
                try {
                    Files.delete(p);
                    l.add(new Old_and_new_Path(p, null, Command_old_and_new_Path.command_delete_forever, Status_old_and_new_Path.delete_forever_done));
                } catch (NoSuchFileException x) {
                    logger.log("no such file or directory:" + p);
                } catch (DirectoryNotEmptyException x) {
                    logger.log(p + " directory not empty");
                }

            }
            Change_gang.report_changes(l);

        } catch (IOException x) {
            // File permission problems are caught here.
            logger.log(x.toString());
        }

    }


    //**********************************************************
    public static Path change_dir_name(Path path, Logger logger, String new_name)
    //**********************************************************
    {
        if (dbg) logger.log("change_dir_name, new name: " + new_name);

        try {
            logger.log("trying rename: " + path.getFileName() + " => " + new_name);
            Path new_path = Paths.get(path.getParent().toString(), new_name);
            //Files.move(path, new_path);
            FileUtils.moveFile(path.toFile(),new_path.toFile());
            logger.log("....done");
            return new_path;
        } catch (FileAlreadyExistsException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            Popups.popup_Exception(e, 200, "File already exists", logger);
        } catch (AccessDeniedException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            Popups.popup_Exception(e, 200, "Access Denied", logger);
        } catch (FileSystemException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            Popups.popup_Exception(e, 200, "File System Exception", logger);
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return path;
    }


    //**********************************************************
    public static Path ask_user_for_new_file_name(Stage owner, Path path, Logger logger)
    //**********************************************************
    {
        String old_name = path.getFileName().toString();

        TextInputDialog dialog = new TextInputDialog(old_name);
        dialog.initOwner(owner);
        {
            String text = I18n.get_I18n_string("Rename", logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setTitle(text);
        }
        {
            String text = I18n.get_I18n_string("Rename_explained", logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setHeaderText(text);

        }
        {
            String text = I18n.get_I18n_string("New_name", logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setContentText(text);
        }
        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name -> logger.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            logger.log("ask_user_for_new_file_name: no result? cancel I hope?");
            return null;
        }

        String new_name = result.get();
        if (new_name.equals(old_name)) {
            logger.log("Rename not done as names are same ->" + old_name + "=" + new_name + "<-");
            return null;
        }
        logger.log("ask_user_for_new_file_name ->" + old_name + "<-   ==>   ->" + new_name + "<-");

        if (FilenameUtils.getExtension(new_name).isEmpty()) {
            if (!FilenameUtils.getExtension(old_name).isEmpty()) {
                logger.log("WARNING, should not remove extension");
                if (Guess_file_type.is_this_path_an_image(path) || Guess_file_type.is_this_path_a_video(path)) {
                    logger.log("WARNING, extension restored");
                    new_name = new_name + "." + FilenameUtils.getExtension(old_name);
                    Popups.popup_warning(owner, "extension restored: ", old_name + "=>" + new_name, true, logger);
                } else {
                    logger.log("WARNING, should not remove extension");
                    Popups.popup_warning(owner, "extension lost?", old_name + "had an extension... and you entered a name without extension? :" + new_name, false, logger);
                }

            } else {
                if (!FilenameUtils.getExtension(new_name).equals(FilenameUtils.getExtension(old_name))) {
                    Popups.popup_warning(owner, "extension check:", "you changed the file name extension", false, logger);
                }
            }
        }
        Path returned = Paths.get(path.getParent().toAbsolutePath().toString(), new_name);
        logger.log("ask_user_for_new_file_name returns ->" + returned + "<-");
        return returned;

    }

    //**********************************************************
    public static Path ask_user_for_new_dir_name(Stage owner, Path path, Logger logger)
    //**********************************************************
    {
        String old_name = path.getFileName().toString();
        TextInputDialog dialog = new TextInputDialog(old_name);
        dialog.initOwner(owner);
        {
            String text = I18n.get_I18n_string("Folder_copy_name", logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setTitle(text);
        }
        {
            String text = I18n.get_I18n_string("Folder_copy_name_explained", logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setHeaderText(text);

        }
        {
            String text = I18n.get_I18n_string("Folder_copy_name_explained", logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setContentText(text);
        }
        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name -> logger.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            logger.log("ask_user_for_new_dir_name no result? cancel I hope?");
            return null;
        }

        String new_name = result.get();
        if (new_name.equals(old_name)) {
            logger.log("ask_user_for_new_dir_name: Rename not done as names are same ->" + old_name + "=" + new_name + "<-");
            return null;
        }
        logger.log("ask_user_for_new_dir_name ->" + old_name + "<-   ==>   ->" + new_name + "<-");

        Path returned = Paths.get(path.getParent().toAbsolutePath().toString(), new_name);
        logger.log("ask_user_for_new_dir_name returns ->" + returned + "<-");
        return returned;
    }

    //**********************************************************
    public static boolean is_same_path(Path p1, Path p2, Logger logger)
    //**********************************************************
    {
        if (p2 == null) return false; // happens for delete forever
        try {
            return Files.isSameFile(p1, p2);
        } catch (IOException e) {
            // happens if for one of the path, no file exists
            if (dbg) logger.log_exception("WARNING: Exception in Files.isSameFile() :", e);
        }
        return false;

    }


    //**********************************************************
    public static boolean copy_dir(Path origin, Path new_path, Logger logger)
    //**********************************************************
    {
        try {
            if (origin.toAbsolutePath().toString().equals(new_path.toAbsolutePath().toString())) {
                logger.log("cannot copy: names a same !");
                return false;
            }
            FileUtils.copyDirectory(origin.toFile(), new_path.toFile());

            List<Old_and_new_Path> l = new ArrayList<>();
            Old_and_new_Path oan = new Old_and_new_Path(null, new_path, Command_old_and_new_Path.command_copy, Status_old_and_new_Path.copy_done);
            l.add(oan);
            Change_gang.report_changes(l);
            return true;
        } catch (IOException e) {
            logger.log_stack_trace("copy_dir failed: " + e);
        }
        return false;
    }


    //**********************************************************
    public static List<My_File> get_all_files_down(File cwd, Deduplication_console_interface popup, boolean consider_also_hidden_files, Logger logger)
    //**********************************************************
    {
        List<My_File> returned = new ArrayList<>();
        File[] files = cwd.listFiles();
        if (files == null) return returned;
        for (File f : files) {
            if (f.isDirectory())
            {
                if (popup != null) if (!popup.increment_directory_examined()) return returned;
                returned.addAll(get_all_files_down(f, popup, consider_also_hidden_files, logger));
            }
            else
            {
                if (!consider_also_hidden_files) if (Guess_file_type.ignore(f.toPath())) continue;
                if (f.length() == 0) {
                    logger.log("WARNING: empty file found:" + f.getAbsolutePath());
                    continue;
                }
                My_File mf = new My_File(f, logger);
                returned.add(mf);
            }
        }
        return returned;
    }


    //**********************************************************
    public static void copy_dir_in_a_thread(Stage owner, Path path, Path new_path, Logger logger)
    //**********************************************************
    {
        logger.log("copy_dir_in_a_thread start");
        Runnable r = () -> {
            boolean status = copy_dir(path, new_path, logger);
            if (status) {
                if (dbg) logger.log("Folder copy done: " + new_path);
            } else {
                logger.log("Folder copy error!");
                Platform.runLater(() -> Popups.popup_warning(owner, "copy of dir failed", "see the logs", false, logger));
            }
        };
        try {
            Threads.execute(r, logger);
            logger.log("copy_dir_in_a_thread LAUNCHED");
        } catch (RejectedExecutionException ree) {
            logger.log("copy_dir_in_a_thread()" + ree);
        }

    }

    public static Error_type explain_error(Path dir, Logger logger)
    {
        try
        {
            BasicFileAttributes x = Files.readAttributes(dir, BasicFileAttributes.class);
            logger.log(dir.toAbsolutePath()+": "+ BasicFileAttributes_to_string(x));
        }
        catch (AccessDeniedException e2)
        {
            logger.log(Stack_trace_getter.get_stack_trace("ACCESS DENIED EXCEPTION" + e2));
            return Error_type.DENIED;
        }
        catch (NoSuchFileException e2)
        {
            logger.log(Stack_trace_getter.get_stack_trace("NoSuchFileException" + e2));
            // the DIR is gone !!
            return Error_type.NOT_FOUND;
        }
        catch (IOException e2)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e2.toString()));
            return Error_type.ERROR;
        }
        return Error_type.OK;
    }

    private static String BasicFileAttributes_to_string(BasicFileAttributes x) {
        StringBuilder sb = new StringBuilder();
        sb.append("isDirectory: ").append(x.isDirectory()).append(" isSymbolicLink: ").append(x.isSymbolicLink());
        return sb.toString();
    }

}
