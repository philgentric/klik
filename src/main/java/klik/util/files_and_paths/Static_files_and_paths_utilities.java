// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Sizes.java
//SOURCES ./disk_scanner/File_payload.java
//SOURCES ./disk_scanner/Dir_payload.java
//SOURCES ./disk_scanner/Disk_scanner.java

package klik.util.files_and_paths;


import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.browser.icons.Error_type;
import klik.look.my_i18n.My_I18n;
import klik.properties.Non_booleans_properties;
import klik.properties.Cache_folder;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.disk_scanner.Dir_payload;
import klik.util.files_and_paths.disk_scanner.Disk_scanner;
import klik.util.files_and_paths.disk_scanner.File_payload;
import klik.look.Look_and_feel_manager;
import klik.change.Change_gang;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.image.icon_cache.Icon_caching;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.ui.Popups;
import klik.util.log.Stack_trace_getter;
import org.apache.commons.io.FileUtils;


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

//**********************************************************
public class Static_files_and_paths_utilities
//**********************************************************
{

    private static final boolean dbg = true;





    //**********************************************************
    private static boolean copy_directory(Path old_path, Path new_path, Logger logger)
    //**********************************************************
    {
        if (! old_path.toFile().exists())
        {
            logger.log("cannot move, file does not exists: "+old_path);
            return false;
        }
        // move a file, if the destination path contains folders that do not exist yet, create them

        Path parent = new_path.getParent();
        logger.log("copy_directory parent = "+parent.toAbsolutePath());
        if ( parent != null && !parent.toFile().exists())
        {
            if ( !parent.toFile().mkdirs())
            {
                logger.log("could not create folders for new path "+new_path.toAbsolutePath());
                return  false;
            }
            else
            {
                logger.log("created folders for new path "+new_path.toAbsolutePath());
            }
        }

        try
        {
            Files.copy(old_path,new_path);
        }
        catch (IOException e)
        {
            logger.log("could not move "+old_path+" => "+ new_path);
            return false;
        }

        // copy_folder_content
        boolean returned = true;
        for (File f : old_path.toFile().listFiles())
        {
            Path ff = Path.of(new_path.toAbsolutePath().toString(),f.getName());
            if ( f.isDirectory())
            {
                if ( !copy_directory(f.toPath(),ff,logger))
                {
                    logger.log("could not copy folder: "+f.getAbsolutePath());
                    returned = false;
                }
            }
            try
            {
                Files.copy(f.toPath(), ff);
            }
            catch (IOException e)
            {
                logger.log("copy_folder_content failed : "+f.getAbsolutePath()+ " "+e);
                returned = false;
            }
        }
        return returned;
    }


    //**********************************************************
    public static void move_to_trash_multiple(List<Path> paths, Window owner, double x, double y, Runnable after_the_move, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( paths.size() == 0)
        {
            logger.log("nothing to delete");
            return;
        }
        Path trash_dir = Non_booleans_properties.get_trash_dir(paths.get(0),owner,logger);
        if (paths.get(0).getParent().toAbsolutePath().toString().equals(trash_dir.toAbsolutePath().toString()))
        {
            Popups.popup_warning("❗" + My_I18n.get_I18n_string("Nothing_Done", owner,logger), My_I18n.get_I18n_string("Nothing_Done_Explanation",owner,logger), false, owner,logger);
            return;
        }
        List<Old_and_new_Path> l2 = new ArrayList<>();
        for ( Path p : paths) {
            Path new_Path = (Paths.get(trash_dir.toString(), p.getFileName().toString()));
            Old_and_new_Path oanf2 = new Old_and_new_Path(p, new_Path, Command.command_move_to_trash, Status.before_command, false);
            oanf2.run_after = after_the_move;
            l2.add(oanf2);
        }
        Moving_files.perform_safe_moves_in_a_thread(l2, true, x,y,owner, aborter, logger);

    }

    //**********************************************************
    public static void move_to_trash(Path path, Window owner, double x, double y, Runnable after_the_move, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path trash_dir = Non_booleans_properties.get_trash_dir(path,owner,logger);
        if (path.getParent().toAbsolutePath().toString().equals(trash_dir.toAbsolutePath().toString())) {
            Popups.popup_warning("❗ "+ My_I18n.get_I18n_string("Nothing_Done", owner,logger), My_I18n.get_I18n_string("Nothing_Done_explanation", owner,logger), false, owner,logger);
            return;
        }
        List<Old_and_new_Path> l2 = new ArrayList<>();
        Path new_Path = (Paths.get(trash_dir.toString(), path.getFileName().toString()));
        Old_and_new_Path oanf2 = new Old_and_new_Path(path, new_Path, Command.command_move_to_trash, Status.before_command,false);
        oanf2.run_after = after_the_move;
        l2.add(oanf2);
        Moving_files.perform_safe_moves_in_a_thread(l2, true, x,y,owner, aborter, logger);

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
    public static Path get_cache_dir(Cache_folder cache_folder, Window owner,Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Non_booleans_properties.get_absolute_hidden_dir_on_user_home(cache_folder.name(), false, owner,logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("icon cache dir=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    /*
    //**********************************************************
    public static Path get_icons_cache_dir(Window owner, Logger logger)
    //**********************************************************
    {


        Path tmp_dir = Non_booleans_properties.get_absolute_dir_on_user_home(Cache_folder.klik_icon_cache.name(), false, logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("icon cache dir=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    public static Path get_folders_icons_cache_dir(Logger logger)
    //**********************************************************
    {
        Path tmp_dir = Non_booleans_properties.get_absolute_dir_on_user_home(Cache_folder.klik_folder_icon_cache.name(), false, logger);
        if (dbg) if (tmp_dir != null) {
            logger.log("folder icon dir file=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }



    //**********************************************************
    public static double clear_icon_DISK_cache(boolean show_popup,Stage owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path icons = get_icons_cache_dir(owner, logger);
        return clear_folder(icons, "Image icons' cache on disk", show_popup, owner, aborter, logger);
    }
*/

    //**********************************************************
    public static void clear_one_icon_from_cache_on_disk(Path path, Window owner,Logger logger)
    //**********************************************************
    {
        Path icon_cache_dir = get_cache_dir( Cache_folder.klik_icon_cache,owner,logger);
        int icon_size = Non_booleans_properties.get_icon_size(owner);

        Path icon_path = Icon_caching.path_for_icon_caching(path,String.valueOf(icon_size),Icon_caching.png_extension,owner,logger);
        try {
            Files.delete(icon_path);
            logger.log("one icon deleted from cache:" + icon_path);

        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: deleting one icon FAILED: " + e));
        }
    }

    //**********************************************************
    public static double clear_DISK_cache(Cache_folder cache_folder, boolean show_popup, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path icons = get_cache_dir(cache_folder,owner,logger);
        return clear_folder(icons, cache_folder.name()+" cache on disk", show_popup, owner, aborter, logger);
    }
    //**********************************************************
    public static void clear_all_DISK_caches(Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        double size = 0.0;
        for ( Cache_folder cache_folder : Cache_folder.values()) {
            size += Static_files_and_paths_utilities.clear_DISK_cache(cache_folder, false,owner,aborter,logger);
        }

        logger.log(size +" total bytes erased");
        //Static_files_and_paths_utilities.user_cancel("All disk caches",size,owner,logger);
    }

    //**********************************************************
    public static double clear_folder(Path folder, String tag, boolean show_popup,Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        double size = get_size_on_disk(folder, aborter, logger);
        if ( show_popup)
        {
            if (user_cancel(tag, size, owner, logger)) return 0.0;
        }
        String text = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(size,owner,logger);
        logger.log(tag+", folder cleared: "+folder.toAbsolutePath()+" "+text+" bytes");
        delete_for_ever_all_files_in_dir_in_a_thread(folder, true, owner,logger);
        return size;
    }


    //**********************************************************
    public static boolean user_cancel(String tag, double size, Window owner, Logger logger)
    //**********************************************************
    {
        String s1 = "❗ Deleting: "+tag;//My_I18n.get_I18n_string("Warning_deleting_icon", logger);
        String s2 = size / 1000_000_000.0 + My_I18n.get_I18n_string("GB_deleted", owner,logger);
        if (size < 1_000_000_000) {
            s2 = size / 1000_000.0 + My_I18n.get_I18n_string("MB_deleted", owner,logger);
        }
        return !Popups.popup_ask_for_confirmation( s1, s2, owner,logger);
    }


    //**********************************************************
    public static void clear_trash(boolean show_popup,Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            List<Path> trashes = Non_booleans_properties.get_existing_trash_dirs(owner,logger);
            String s1 = My_I18n.get_I18n_string("Warning_delete", owner,logger);
            double size = 0;
            for (Path trash : trashes) {
                long tmp = get_size_on_disk(trash, aborter, logger);
                logger.log("trash dir: "+trash+" "+tmp+" bytes");
                size += tmp;
            }
            String s2 = (int) (size / 1000_000.0) + My_I18n.get_I18n_string("MB_deleted", owner,logger);//"MB of files will be truly deleted";
            if (size > 1000_000_000) {
                double r1 = Math.round(size / 1000_000_00);
                double s = r1 / 10.0;
                s2 = (s) + My_I18n.get_I18n_string("GB_deleted", owner,logger);//"MB of files will be truly deleted";

            }
            String finalS = s2;
            Runnable r2 = () -> {
                if ( show_popup)
                {
                    if (!Popups.popup_ask_for_confirmation("❗"+s1, finalS, owner,logger)) return;
                }
                for (Path trash : trashes) {
                    delete_for_ever_all_files_in_dir_in_a_thread(trash, true,owner,logger);
                    logger.log("deletion ongoing: "+trash);
                }
                logger.log("Clearing trash folders (one per drive): done");
            };
            Jfx_batch_injector.inject(r2,logger);
        };
        Actor_engine.execute(r,"Clear trash",logger);

    }


    //**********************************************************
    public static long get_size_on_disk(Path path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        // the concurrent version is at least 5 times faster!
        return get_size_on_disk_concurrent(path,aborter, logger);
    }

    //**********************************************************
    public static Sizes get_sizes_on_disk_shallow(Path path, Aborter aborter, Window owner,Logger logger)
    //**********************************************************
    {
        File[] all_files = path.toFile().listFiles();
        if ( all_files == null)
        {
            Error_type error = Static_files_and_paths_utilities.explain_error(path,logger);
            logger.log(error+ " get_sizes_on_disk_shallow: cannot scan folder "+path);
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
                if( Guess_file_type.is_this_file_an_image(f,owner,logger)) images++;
                bytes += f.length();
            }
        }
        return new Sizes(bytes,folders,files,images);

    }
    //**********************************************************
    public static Sizes get_sizes_on_disk_deep(Path path, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        //get_size_on_disk_with_streams(path,logger);
        // the concurrent version is at least 5 times faster!
        return get_sizes_on_disk_deep_concurrent(path,aborter,owner, logger);
    }


    //**********************************************************
    public static Sizes get_sizes_on_disk_deep_concurrent(Path path, Aborter aborter, Window owner, Logger logger)
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

        File_payload fp = (f, file_count_stop_counter) -> {
            bytes.addAndGet(f.length());
            files.incrementAndGet();
            if ( Guess_file_type.is_this_file_an_image(f,owner,logger)) images.incrementAndGet();
            file_count_stop_counter.decrement();
        };
        Dir_payload dp = f -> {
            folders.incrementAndGet();
            //System.out.println("folder:"+f.getName());
        };
        ConcurrentLinkedQueue<String> warnings = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(path, "get_sizes_on_disk_deep_concurrent", fp,dp, warnings, aborter, logger);
        if (! warnings.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            for ( String w : warnings)
            {
                sb.append(w).append("\n");
            }
            logger.log("❗ Disk_scanner.process_folder, warnings:\n"+sb);
        }

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
            logger.log(Stack_trace_getter.get_stack_trace("get_size_on_disk_concurrent: not a folder: "+path));
            return 0L;
        }
        // this is a blocking call
        //long start = System.currentTimeMillis();
        AtomicLong bytes = new AtomicLong(0);
        File_payload fp = (f, file_count_stop_counter) ->
        {
            bytes.addAndGet(f.length());
            file_count_stop_counter.decrement();
        };
        ConcurrentLinkedQueue<String> wp = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(path, "get_size_on_disk_concurrent", fp,null,wp,aborter,logger);
        //long now = System.currentTimeMillis();
        //logger.log("get_size_on_disk_concurrent: " + (now-start)+" size=" +size.get());
        return bytes.get();
    }



    //**********************************************************
    public static long get_how_many_files_deep(Path path, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        //return get_how_many_files_streams(path,logger);
        return get_how_many_files_deep_concurrent(path,aborter, owner, logger);
    }

    //**********************************************************
    private static long get_how_many_files_deep_concurrent(Path path, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace("Stupid4: not a folder: "+path));
            return 0L;
        }
        AtomicLong files = new AtomicLong(0);
        File_payload fp = (f, file_count_stop_counter) ->
        {
            if (Guess_file_type.is_this_path_invisible_when_browsing(f.toPath(),owner,logger))
            {
                if ( !Feature_cache.get(Feature.Show_hidden_files))
                {
                    file_count_stop_counter.decrement();
                    return;
                }
            }
            files.incrementAndGet();
            file_count_stop_counter.decrement();
        };
        ConcurrentLinkedQueue<String> wp = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(path, "get_how_many_files_deep_concurrent", fp,null, wp,aborter, logger);
        return files.get();
    }


    //**********************************************************
    public static String get_1_line_string_for_byte_data_size(double size,Window owner,Logger logger)
    //**********************************************************
    {
        String returned;
        if (size < 1000) {
            String bytes = My_I18n.get_I18n_string("Bytes", owner,logger);
            returned = size + " "+bytes;
        } else if (size < 1000_000) {
            String kBytes = My_I18n.get_I18n_string("kBytes", owner,logger);
            returned = String.format("%.1f", size / 1000.0) + " "+kBytes;
        } else if (size < 1000_000_000) {
            String MBytes = My_I18n.get_I18n_string("MBytes", owner,logger);
            returned = String.format("%.1f", size / 1000_000.0) + " "+MBytes;
        } else if (size < 1000_000_000_000.0) {
            String GBytes = My_I18n.get_I18n_string("GBytes", owner,logger);
            returned = String.format("%.1f", size / 1000_000_000.0) + " "+GBytes;
        } else {
            String TBytes = My_I18n.get_I18n_string("TBytes", owner,logger);
            returned = String.format("%.1f", size / 1000_000_000_000.0) + " "+TBytes;
        }
        return returned;
    }

    //**********************************************************
    public static String get_1_line_string_with_size(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        long BYTES = path.toFile().length();
        StringBuilder sb = new StringBuilder();
        if (BYTES > 1000_000_000)
        {
            double GB = (double) BYTES / 1000_000_000.0;
            String GBytes = My_I18n.get_I18n_string("GBytes", owner,logger);
            sb.append(String.format("%.1f", GB)).append(" ").append(GBytes);
        }
        else if (BYTES > 1000_000)
        {
            double MB = (double) BYTES / 1000_000.0;
            String MBytes = My_I18n.get_I18n_string("MBytes", owner,logger);
            sb.append(String.format("%.1f", MB)).append(" ").append(MBytes);
        }
        else if (BYTES > 1000)
        {
            String kBytes = My_I18n.get_I18n_string("kBytes", owner,logger);
            sb.append(String.format("%.1f", (double) BYTES / 1000.0)).append(" ").append(kBytes);
        }
        else
        {
            String bytes = My_I18n.get_I18n_string("Bytes", owner,logger);
            sb.append(BYTES).append(" ").append(bytes);
        }

        return sb.toString();
    }

    //**********************************************************
    private static void delete_for_ever_all_files_in_dir_in_a_thread(Path dir, boolean also_folders, Window owner,Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            String s = delete_for_ever_all_files_in_dir(dir, also_folders,owner,logger);
            if ( s != null)
            {
                Runnable rr = new Runnable() {
                    @Override
                    public void run() {
                        if ( s.contains("AccessDeniedException") && s.contains(Non_booleans_properties.TRASH_DIR))
                        {
                            Popups.popup_warning("❌ There is a permission issue in the TRASH folder, did you move in the trash a folder that you do not own?\nYou will have to fix that manually",s,false,owner,logger);
                        }
                        else {
                            Popups.popup_warning( "❌ Error", s, false, owner,logger);
                        }
                    }
                };
                Jfx_batch_injector.inject(rr,logger);
            }
        };

        Actor_engine.execute(r,"Delete forever files in trash",logger);

    }

    //**********************************************************
    private static String delete_for_ever_all_files_in_dir(Path dir, boolean also_folders, Window owner, Logger logger)
    //**********************************************************
    {
        File files[] = dir.toFile().listFiles();
        if ( files == null)
        {
            return "cannot list dir ->"+dir.toAbsolutePath()+"<-";
        }
        List<Old_and_new_Path> l = new ArrayList<>();
        for ( File f : files)
        {
            if (f.isDirectory())
            {
                String s = delete_for_ever_all_files_in_dir(f.toPath(), also_folders, owner,logger);
                if ( s != null) logger.log(s);
                if (also_folders) f.delete();
            }
            else
            {
                String s = delete_for_ever_a_file(f.toPath(),logger);
                if ( s != null) logger.log(s);
            }

        }
        Change_gang.report_changes(l,owner);

        /*
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
        }*/
        return null;
    }

    //**********************************************************
    private static String delete_for_ever_all_files_in_dir2(Path dir, boolean also_folders, Window owner, Logger logger)
    //**********************************************************
    {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))//, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {

            List<Old_and_new_Path> l = new ArrayList<>();
            for (Path p : stream)
            {
                if (Files.isDirectory(p))
                {
                    String s = delete_for_ever_all_files_in_dir(p, also_folders, owner,logger);
                    if ( s != null) logger.log(s);
                    if (also_folders) Files.delete(p);
                }
                else
                {
                    String s = delete_for_ever_a_file(p,logger);
                    if ( s != null) logger.log(s);

                }

            }
            Change_gang.report_changes(l,owner);

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
        return null;// OK
    }

    //**********************************************************
    private static String delete_for_ever_a_file(Path p, Logger logger)
    //**********************************************************
    {
        try
        {
            Files.delete(p);
            if ( dbg) logger.log("Deleted for ever: " + p);
        }
        catch (NoSuchFileException x)
        {
            if ( p.getFileName().toString().startsWith("._"))
            {
                // this is a macOS file found on external drives, if the file was deleted, then this file was deleted too,
                // so the original list is "wrong", it is not an error, just ignore it
                return null;
            }
            logger.log(Stack_trace_getter.get_stack_trace(x.toString()));
            return x.toString();
        }
        catch (DirectoryNotEmptyException x) {
            logger.log(Stack_trace_getter.get_stack_trace(x.toString()));
            return x.toString();
        }
        catch (FileSystemException x)
        {
            logger.log(Stack_trace_getter.get_stack_trace(x.toString()));
            return x.toString();
        }
        catch (IOException x)
        {
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
                try
                {
                    Files.delete(p);
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
            }
            else
            {
                logger.log(Stack_trace_getter.get_stack_trace(x.toString()));
                return x.toString();

            }
        }
        return null;// OK
    }


    //**********************************************************
    public static Path change_file_name(Path old_path, String new_name, Window owner, double x, double y,Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (dbg) logger.log("change_file_name, new name: " + new_name);
       // logger.log("trying rename: " + old_path.getFileName() + " => " + new_name);
        Path new_path = Paths.get(old_path.getParent().toString(), new_name);

        Old_and_new_Path oan = new Old_and_new_Path(old_path, new_path, Command.command_rename, Status.before_command, false);
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(oan);
        List<Old_and_new_Path> done = Moving_files.actual_safe_moves(l, true, x, y, owner, aborter, logger);
        if ( done.isEmpty()) return null;

        return new_path;
    }




    //**********************************************************
    public static Path ask_user_for_new_file_name(Window owner, Path path, Logger logger)
    //**********************************************************
    {
        String old_name = path.getFileName().toString();

        TextInputDialog dialog = new TextInputDialog(old_name);
        Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
        dialog.getDialogPane().setMinWidth(800);
        dialog.initOwner(owner);
        {
            String text = My_I18n.get_I18n_string("Rename", owner,logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setTitle(text);
        }
        {
            String text = My_I18n.get_I18n_string("Rename_Explanation", owner,logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setHeaderText(text);

        }
        {
            String text = My_I18n.get_I18n_string("New_name", owner,logger);// to: " + parent.toAbsolutePath().toString();
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

        if (Extensions.get_extension(new_name).isEmpty()) {
            if (!Extensions.get_extension(old_name).isEmpty()) {
                logger.log("❗ WARNING, should not remove extension");
                if (Guess_file_type.is_this_path_an_image(path,owner,logger) || Guess_file_type.is_this_path_a_video(path,owner,logger)) {
                    logger.log("❗ WARNING, extension restored");
                    new_name = Extensions.add(new_name ,Extensions.get_extension(old_name));
                    Popups.popup_warning( "❗ extension restored: ", old_name + "=>" + new_name, true, owner,logger);
                } else {
                    logger.log("❗ WARNING, should not remove extension");
                    Popups.popup_warning( "❗ extension lost?", old_name + "had an extension... and you entered a name without extension? :" + new_name, false, owner,logger);
                }

            } else {
                if (!Extensions.get_extension(new_name).equals(Extensions.get_extension(old_name))) {
                    Popups.popup_warning( "❗ extension check:", "you changed the file name extension", false, owner,logger);
                }
            }
        }
        Path returned = Paths.get(path.getParent().toAbsolutePath().toString(), new_name);
        logger.log("ask_user_for_new_file_name returns ->" + returned + "<-");
        return returned;

    }

    //**********************************************************
    public static Path ask_user_for_new_dir_name(Window owner, Path path, Logger logger)
    //**********************************************************
    {
        String old_name = path.getFileName().toString();
        TextInputDialog dialog = new TextInputDialog(old_name);
        Look_and_feel_manager.set_dialog_look(dialog,owner,logger);
        dialog.initOwner(owner);
        {
            String text = My_I18n.get_I18n_string("Folder_Copy_Name", owner,logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setTitle(text);
        }
        {
            String text = My_I18n.get_I18n_string("Folder_Copy_Name_Explanation", owner,logger);// to: " + parent.toAbsolutePath().toString();
            dialog.setHeaderText(text);

        }
        {
            String text = My_I18n.get_I18n_string("Folder_Copy_Name_Explanation", owner,logger);// to: " + parent.toAbsolutePath().toString();
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
        if (p1 == null) return false;
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
    public static void copy_dir_in_a_thread(Window owner, Path path, Path new_path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( dbg) logger.log("copy_dir_in_a_thread start");
        Runnable r = () -> {
            boolean status = copy_dir(path, new_path, owner, logger);
            if (status) {
                if (dbg) logger.log("✅ Folder copy done: " + new_path);
            }
            else
            {
                logger.log("❌ Folder copy error!");
                Jfx_batch_injector.inject(() -> Popups.popup_warning("❌ copy of folder failed", "see the logs", false, owner,logger),logger);
            }
        };
        try {
            Actor_engine.execute(r,"Copy folder",logger);
            if ( dbg) logger.log("✅ copy_dir_in_a_thread LAUNCHED");
        } catch (RejectedExecutionException ree) {
            logger.log("❌ copy_dir_in_a_thread()" + ree);
        }

    }

    //**********************************************************
    public static boolean copy_dir(Path origin, Path new_path, Window owner, Logger logger)
    //**********************************************************
    {
        File src = origin.toFile();
        File dst = new_path.toFile();
        try
        {
            FileUtils.copyDirectory(src,dst);
            return true;
        }
        catch (IOException e)
        {
            logger.log("❌ Folder copy failed "+e);
            Popups.popup_warning( My_I18n.get_I18n_string("❌ Folder copy failed", owner,logger), "Folder copy failed: "+e, false, owner,logger);
        }

        return false;
    }

    //**********************************************************
    private static boolean copy_dir2(Path origin, Path new_path, Window owner, Logger logger)
    //**********************************************************
    {

        if (origin.toAbsolutePath().toString().equals(new_path.toAbsolutePath().toString())) {
            logger.log("❌cannot copy: names are same !");
            return false;
        }
        if ( !copy_directory(origin, new_path,logger))
        {
            return false;
        }

        List<Old_and_new_Path> l = new ArrayList<>();
        Old_and_new_Path oan = new Old_and_new_Path(null, new_path, Command.command_copy, Status.copy_done, false);
        l.add(oan);
        Change_gang.report_changes(l,owner);
        return true;

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
            logger.log(Stack_trace_getter.get_stack_trace("❌ ACCESS DENIED EXCEPTION" + e2));
            return Error_type.DENIED;
        }
        catch (NoSuchFileException e2)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ NoSuchFileException" + e2));
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

    public static Path get_cache_folder(Cache_folder cache_folder, Window owner,Logger logger)
    {
        return Non_booleans_properties.get_absolute_hidden_dir_on_user_home(cache_folder.name(), false, owner,logger);
    }

    public static Path get_face_reco_folder(Window owner,Logger logger)
    {
        return Non_booleans_properties.get_absolute_hidden_dir_on_user_home(Non_booleans_properties.FACE_RECO_DIR, false, owner,logger);
    }



}
