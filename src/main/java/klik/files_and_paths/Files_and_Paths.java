package klik.files_and_paths;


import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.Error_type;
import klik.change.undo.Undo_engine;
import klik.look.Look_and_feel_manager;
import klik.change.Change_gang;
import klik.browser.icons.Icon_factory_actor;
import klik.browser.icons.Icon_writer_actor;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Fx_batch_injector;
import klik.util.Logger;
import klik.util.Popups;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileExistsException;



import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        Moving_files.perform_safe_moves_in_a_thread(owner, l,  false, aborter,logger);
    }

    /*
    //**********************************************************
    public static void unsafe_delete_file(Stage owner, Old_and_new_Path oanp, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("unsafe_delete_all: perform_safe_moves_in_a_thread");
        Moving_files.perform_safe_moves_in_a_thread(owner, oanp, aborter, logger);

    }
*/

    /*
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

    }*/


    //**********************************************************
    public static void move_to_trash(Stage owner, Path path, Runnable after_the_move, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path trash_dir = Static_application_properties.get_trash_dir(path,logger);
        if (path.getParent().toAbsolutePath().toString().equals(trash_dir.toAbsolutePath().toString())) {
            Popups.popup_warning(owner, I18n.get_I18n_string("Nothing_done", logger), I18n.get_I18n_string("Nothing_done_explained", logger), false, logger);
            return;
        }
        List<Old_and_new_Path> l2 = new ArrayList<>();
        Path new_Path = (Paths.get(trash_dir.toString(), path.getFileName().toString()));
        Old_and_new_Path oanf2 = new Old_and_new_Path(path, new_Path, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.before_command,false);
        oanf2.run_after = after_the_move;
        l2.add(oanf2);
        Moving_files.perform_safe_moves_in_a_thread(owner, l2, true, aborter, logger);

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
    public static Path get_aspect_ratio_and_rotation_caches_dir(Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Static_application_properties.get_absolute_dir_on_user_home(Static_application_properties.ASPECT_RATIO_AND_ROTATION_CACHES_DIR, false,logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("Aspect ratio and rotation cache folder=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    public static Path get_icon_cache_dir(Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Static_application_properties.get_absolute_dir_on_user_home(Static_application_properties.ICON_CACHE_DIR, false, logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("icon dir file=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    public static Path get_folder_icon_cache_dir(Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Static_application_properties.get_absolute_dir_on_user_home(Static_application_properties.FOLDER_ICON_CACHE_DIR, false, logger);
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
    public static void clear_icon_cache_on_disk_with_warning_fx(Stage owner, Aborter aborter, Logger logger)
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

        delete_for_ever_all_files_in_dir_in_a_thread(icons, true,aborter, logger);
    }

    //**********************************************************
    public static void clear_icon_cache_on_disk_no_warning(Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path icons = get_icon_cache_dir(logger);
        delete_for_ever_all_files_in_dir_in_a_thread(icons, false, aborter, logger);
    }


    //**********************************************************
    public static void clear_aspect_ratio_and_rotation_caches_on_disk_no_warning_fx(Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path icons = get_aspect_ratio_and_rotation_caches_dir(logger);
        delete_for_ever_all_files_in_dir_in_a_thread(icons, false,aborter, logger);
    }

    //**********************************************************
    public static void clear_folder_icon_cache_on_disk_with_warning_fx(Stage owner, Aborter aborter, Logger logger)
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
        delete_for_ever_all_files_in_dir_in_a_thread(icons, true,aborter,logger);
    }

    //**********************************************************
    public static void clear_folder_icon_cache_no_warning_fx(Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path icons = get_folder_icon_cache_dir(logger);
        delete_for_ever_all_files_in_dir_in_a_thread(icons, false, aborter,logger);
    }


    //**********************************************************
    public static void clear_trash_with_warning_fx(Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            List<Path> trashes = Static_application_properties.get_existing_trash_dirs(logger);
            String s1 = I18n.get_I18n_string("Warning_delete", logger);
            double size = 0;
            for (Path trash : trashes) {
                long tmp = get_size_on_disk(trash, aborter, logger);
                logger.log("trash dir: "+trash+" "+tmp+" bytes");
                size += tmp;
            }
            String s2 = (int) (size / 1000_000.0) + I18n.get_I18n_string("MB_deleted", logger);//"MB of files will be truly deleted";
            if (size > 1000_000_000) {
                double r1 = Math.round(size / 1000_000_00);
                double s = r1 / 10.0;
                s2 = (s) + I18n.get_I18n_string("GB_deleted", logger);//"MB of files will be truly deleted";

            }
            String finalS = s2;
            Runnable r2 = () -> {
                if (!Popups.popup_ask_for_confirmation(owner, s1, finalS, logger)) return;
                for (Path trash : trashes) {
                    delete_for_ever_all_files_in_dir_in_a_thread(trash, true, aborter,logger);
                    logger.log("deletion ongoing: "+trash);
                }
            };
            Fx_batch_injector.inject(r2,logger);
        };
        Actor_engine.execute(r,new Aborter("clean trash",logger),logger);

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
    public static Sizes get_sizes_on_disk_shallow(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        File[] all_files = path.toFile().listFiles();
        if ( all_files == null)
        {
            Error_type error = Files_and_Paths.explain_error(path,logger);
            logger.log(error+ " get_sizes_on_disk_shallow: cannot scan folder "+path);
            return new Sizes(0,0,0,0,null);
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
                if( Guess_file_type.is_file_an_image(f)) images++;
                bytes += f.length();
            }
        }
        return new Sizes(bytes,folders,files,images,null);

    }
    //**********************************************************
    public static Sizes get_sizes_on_disk_deep(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //get_size_on_disk_with_streams(path,logger);
        // the concurrent version is at least 5 times faster!
        return get_sizes_on_disk_deep_concurrent(path,aborter, logger);
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
    public static Sizes get_sizes_on_disk_deep_concurrent(Path path, Aborter aborter, Logger logger)
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
            if ( Guess_file_type.is_file_an_image(f)) images.incrementAndGet();
        };
        Dir_payload dp = f -> {
            folders.incrementAndGet();
            //System.out.println("folder:"+f.getName());
        };
        ConcurrentLinkedQueue<String> warnings = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(path, "get_sizes_on_disk_deep_concurrent", fp,dp, warnings, aborter, logger);


        //long now = System.currentTimeMillis();
        //logger.log("get_size_on_disk_concurrent: " + (now-start)+" size=" +bytes.get());
        return new Sizes(bytes.get(),folders.get(),files.get(),images.get(),warnings);
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
        ConcurrentLinkedQueue<String> wp = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(path, "get_size_on_disk_concurrent", fp,null,wp,aborter,logger);
        //long now = System.currentTimeMillis();
        //logger.log("get_size_on_disk_concurrent: " + (now-start)+" size=" +size.get());
        return bytes.get();
    }



    //**********************************************************
    public static long get_how_many_files_deep(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        //return get_how_many_files_streams(path,logger);
        return get_how_many_files_deep_concurrent(path,aborter, logger);
    }

    //**********************************************************
    private static long get_how_many_files_deep_concurrent(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace("Stupid4: not a folder: "+path));
            return 0L;
        }
        AtomicLong files = new AtomicLong(0);
        File_payload fp = f -> files.incrementAndGet();
        ConcurrentLinkedQueue<String> wp = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(path, "get_how_many_files_deep_concurrent", fp,null, wp,aborter, logger);
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
    public static String get_1_line_string_for_byte_data_size(double size,Logger logger)
    //**********************************************************
    {
        String returned;
        if (size < 1000) {
            String bytes = I18n.get_I18n_string("Bytes", logger);
            returned = size + " "+bytes;
        } else if (size < 1000_000) {
            String kBytes = I18n.get_I18n_string("kBytes", logger);
            returned = String.format("%.1f", size / 1000.0) + " "+kBytes;
        } else if (size < 1000_000_000) {
            String MBytes = I18n.get_I18n_string("MBytes", logger);
            returned = String.format("%.1f", size / 1000_000.0) + " "+MBytes;
        } else if (size < 1000_000_000_000.0) {
            String GBytes = I18n.get_I18n_string("GBytes", logger);
            returned = String.format("%.1f", size / 1000_000_000.0) + " "+GBytes;
        } else {
            String TBytes = I18n.get_I18n_string("TBytes", logger);
            returned = String.format("%.1f", size / 1000_000_000_000.0) + " "+TBytes;
        }
        return returned;
    }

    //**********************************************************
    public static String get_1_line_string_with_size(Path path, Logger logger)
    //**********************************************************
    {
        long BYTES = path.toFile().length();
        StringBuilder sb = new StringBuilder();
        if (BYTES > 1000_000_000)
        {
            double GB = (double) BYTES / 1000_000_000.0;
            String GBytes = I18n.get_I18n_string("GBytes", logger);
            sb.append(String.format("%.1f", GB)).append(" ").append(GBytes);
        }
        else if (BYTES > 1000_000)
        {
            double MB = (double) BYTES / 1000_000.0;
            String MBytes = I18n.get_I18n_string("MBytes", logger);
            sb.append(String.format("%.1f", MB)).append(" ").append(MBytes);
        }
        else if (BYTES > 1000)
        {
            String kBytes = I18n.get_I18n_string("kBytes", logger);
            sb.append(String.format("%.1f", (double) BYTES / 1000.0)).append(" ").append(kBytes);
        }
        else
        {
            String bytes = I18n.get_I18n_string("Bytes", logger);
            sb.append(BYTES).append(" ").append(bytes);
        }

        return sb.toString();
    }

    //**********************************************************
    private static void delete_for_ever_all_files_in_dir_in_a_thread(Path dir, boolean also_folders, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            String s = delete_for_ever_all_files_in_dir(dir, also_folders,logger);
            if ( s != null)
            {
                Runnable rr = new Runnable() {
                    @Override
                    public void run() {
                        if ( s.contains("AccessDeniedException") && s.contains(Static_application_properties.TRASH_DIR))
                        {
                            Popups.popup_warning(null,"There is a permission issue in the TRASH folder, did you move in the trash a folder that you do not own?\nYou will have to fix that manually",s,false,logger);
                        }
                        else {
                            Popups.popup_warning(null, "Error", s, false, logger);
                        }
                    }
                };
                Fx_batch_injector.inject(rr,logger);
            }
        };

        Actor_engine.execute(r,aborter,logger);

    }

    //**********************************************************
    private static String delete_for_ever_all_files_in_dir(Path dir, boolean also_folders, Logger logger)
    //**********************************************************
    {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))//, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {

            List<Old_and_new_Path> l = new ArrayList<>();
            for (Path p : stream)
            {
                if (Files.isDirectory(p))
                {
                    String s = delete_for_ever_all_files_in_dir(p, also_folders, logger);
                    if ( s != null) logger.log(s);
                    if (also_folders) Files.delete(p);
                }
                else
                {
                    String s = delete_for_ever_a_file(p,l,logger);
                    if ( s != null) logger.log(s);

                }

            }
            Change_gang.report_changes(l);

        }
        catch (IOException x)
        {
            // directory permission problems are caught here.
            if ( x.toString().contains("AccessDeniedException"))
            {
                try {
                    Set<PosixFilePermission> permissions = new TreeSet<>();
                    permissions.add(PosixFilePermission.OWNER_WRITE);
                    permissions.add(PosixFilePermission.OTHERS_WRITE);
                    permissions.add(PosixFilePermission.GROUP_WRITE);
                    Files.setPosixFilePermissions(dir, permissions);
                }
                catch (AccessDeniedException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                } catch (IOException e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
                try
                {
                    Files.delete(dir);
                }
                catch (AccessDeniedException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
                catch (IOException e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }

            }
            logger.log(Stack_trace_getter.get_stack_trace(x.toString()));
        }
        return null;
    }

    //**********************************************************
    private static String delete_for_ever_a_file(Path p, List<Old_and_new_Path> l, Logger logger)
    //**********************************************************
    {
        try
        {
            Files.delete(p);
            l.add(new Old_and_new_Path(p, null, Command_old_and_new_Path.command_delete_forever, Status_old_and_new_Path.delete_forever_done, false));
        } catch (NoSuchFileException x) {
            logger.log(Stack_trace_getter.get_stack_trace(x.toString()));
            return x.toString();
        } catch (DirectoryNotEmptyException x) {
            logger.log(Stack_trace_getter.get_stack_trace(x.toString()));
            return x.toString();
        }
        catch (IOException x) {
            if ( x.toString().contains("AccessDeniedException"))
            {
                try {
                    Set<PosixFilePermission> permissions = new TreeSet<>();
                    permissions.add(PosixFilePermission.OWNER_WRITE);
                    permissions.add(PosixFilePermission.OWNER_READ);
                    permissions.add(PosixFilePermission.OWNER_EXECUTE);
                    permissions.add(PosixFilePermission.OTHERS_WRITE);
                    permissions.add(PosixFilePermission.OTHERS_READ);
                    permissions.add(PosixFilePermission.OTHERS_EXECUTE);
                    permissions.add(PosixFilePermission.GROUP_WRITE);
                    permissions.add(PosixFilePermission.GROUP_READ);
                    permissions.add(PosixFilePermission.GROUP_EXECUTE);
                    Files.setPosixFilePermissions(p, permissions);
                }
                catch (UnsupportedOperationException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
                catch (AccessDeniedException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
                try {
                    Files.delete(p);
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
            }
            else {
                logger.log(Stack_trace_getter.get_stack_trace(x.toString()));
                return x.toString();

            }
        }
        return null;// OK
    }


    //**********************************************************
    public static Path change_file_name(Path old_path, String new_name, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("change_file_name, new name: " + new_name);

        try {
            logger.log("trying rename: " + old_path.getFileName() + " => " + new_name);
            Path new_path = Paths.get(old_path.getParent().toString(), new_name);
            //Files.move(path, new_path);
            FileUtils.moveFile(old_path.toFile(),new_path.toFile());
            logger.log("....done");
            Old_and_new_Path oan = new Old_and_new_Path(old_path,new_path,Command_old_and_new_Path.command_rename,Status_old_and_new_Path.rename_done,false);
            List<Old_and_new_Path> l = new ArrayList<>();
            l.add(oan);
            Undo_engine.add(l,aborter,logger);
            return new_path;
        } catch (FileExistsException e) {
            Popups.popup_Exception(e, 200, "File already exists", logger);
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return null;
    }


    //**********************************************************
    public static Path change_dir_name(Path old_path, String new_name,Aborter aborter , Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("change_dir_name, new name: " + new_name);

        try {
            logger.log("trying rename: " + old_path.getFileName() + " => " + new_name);
            Path new_path = Paths.get(old_path.getParent().toString(), new_name);
            //Files.move(path, new_path);
            FileUtils.moveDirectory(old_path.toFile(),new_path.toFile());
            logger.log("....done");
            Old_and_new_Path oan = new Old_and_new_Path(old_path,new_path,Command_old_and_new_Path.command_rename,Status_old_and_new_Path.rename_done,false);
            List<Old_and_new_Path> l = new ArrayList<>();
            l.add(oan);
            Undo_engine.add(l,aborter, logger);
            return new_path;
        } catch (FileAlreadyExistsException e) {
            Popups.popup_Exception(e, 200, "File already exists", logger);
        } catch (AccessDeniedException e) {
            Popups.popup_Exception(e, 200, "Access Denied", logger);
        } catch (FileSystemException e) {
            Popups.popup_Exception(e, 200, "File System Exception", logger);
        } catch (IOException e) {
            Popups.popup_Exception(e, 200, "IO Exception", logger);
        }
        return null;
    }


    //**********************************************************
    public static Path ask_user_for_new_file_name(Stage owner, Path path, Logger logger)
    //**********************************************************
    {
        String old_name = path.getFileName().toString();

        TextInputDialog dialog = new TextInputDialog(old_name);
        Look_and_feel_manager.set_dialog_look(dialog);
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
        Look_and_feel_manager.set_dialog_look(dialog);
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
            //logger.log("ask_user_for_new_dir_name no result? cancel I hope?");
            return null;
        }

        String new_name = result.get();
        if (new_name.equals(old_name)) {
            logger.log("ask_user_for_new_dir_name: Rename not done as names are same ->" + old_name + "=" + new_name + "<-");
            return null;
        }
        //logger.log("ask_user_for_new_dir_name ->" + old_name + "<-   ==>   ->" + new_name + "<-");

        Path returned = Paths.get(path.getParent().toAbsolutePath().toString(), new_name);
        //logger.log("ask_user_for_new_dir_name returns ->" + returned + "<-");
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
            Old_and_new_Path oan = new Old_and_new_Path(null, new_path, Command_old_and_new_Path.command_copy, Status_old_and_new_Path.copy_done, false);
            l.add(oan);
            Change_gang.report_changes(l);
            return true;
        } catch (IOException e) {
            logger.log_stack_trace("copy_dir failed: " + e);
        }
        return false;
    }



    //**********************************************************
    public static void copy_dir_in_a_thread(Stage owner, Path path, Path new_path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( dbg) logger.log("copy_dir_in_a_thread start");
        Runnable r = () -> {
            boolean status = copy_dir(path, new_path, logger);
            if (status) {
                if (dbg) logger.log("Folder copy done: " + new_path);
            } else {
                logger.log("Folder copy error!");
                Fx_batch_injector.inject(() -> Popups.popup_warning(owner, "copy of dir failed", "see the logs", false, logger),logger);
            }
        };
        try {
            Actor_engine.execute(r,aborter,logger);
            if ( dbg) logger.log("copy_dir_in_a_thread LAUNCHED");
        } catch (RejectedExecutionException ree) {
            logger.log("copy_dir_in_a_thread()" + ree);
        }

    }

    //**********************************************************
    public static Error_type explain_error(Path dir, Logger logger)
    //**********************************************************
    {
        try
        {
            BasicFileAttributes x = Files.readAttributes(dir, BasicFileAttributes.class);
            logger.log(dir.toAbsolutePath()+", BasicFileAttributes: "+ BasicFileAttributes_to_string(x));
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

    //**********************************************************
    private static String BasicFileAttributes_to_string(BasicFileAttributes x)
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        sb.append("isDirectory: ").append(x.isDirectory()).append(" isSymbolicLink: ").append(x.isSymbolicLink());
        return sb.toString();
    }

    //**********************************************************
    public static long get_file_age_in_hours(File f, Logger logger)
    //**********************************************************
    {
        Duration age = get_file_age(f,logger);
        if ( age == null) return Integer.MAX_VALUE;
        return age.toHours();
    }
    //**********************************************************
    public static long get_file_age_in_days(File f, Logger logger)
    //**********************************************************
    {
        Duration age = get_file_age(f,logger);
        if ( age == null) return Integer.MAX_VALUE;
        return age.toDays();
    }
    //**********************************************************
    public static Duration get_file_age(File f, Logger logger)
    //**********************************************************
    {
        BasicFileAttributes x = null;
        try {
            x = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
            logger.log_exception("get_file_age",e);
            return null;
        }
        FileTime creation= x.creationTime();
        Instant c = creation.toInstant();
        Instant now = Instant.now();

        Duration age = Duration.between(c,now);
        return age;
    }
}
