//SOURCES ../../unstable/metadata/Metadata_handler.java
package klik.util.files_and_paths;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.Icon_factory_actor;
import klik.change.Change_gang;
import klik.change.undo.Undo_for_moves;
import klik.images.Redo_same_move_engine;
import klik.properties.Cache_folder;
import klik.properties.Non_zooleans;
import klik.experimental.metadata.Metadata_handler;
import klik.look.my_i18n.My_I18n;
import klik.properties.Zooleans;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Show_running_film_frame;
import klik.util.ui.Hourglass;
import klik.util.ui.Popups;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;
import org.apache.commons.io.FileUtils;

//**********************************************************
public class Moving_files
//**********************************************************
{

    private static final boolean moving_files_dbg = false;

    //**********************************************************
    public static void safe_move_files_or_dirs(Window owner, double x, double y,
                                               Path destination_dir,
                                               boolean destination_is_trash,
                                               List<File> the_files_being_moved,
                                               Aborter aborter,
                                               Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> oan_list = new ArrayList<>();
        boolean popup = false;
        for (File the_file_being_moved : the_files_being_moved) {
            Path old_Path_ = the_file_being_moved.toPath();
            Path new_Path_ = Paths.get(destination_dir.toAbsolutePath().toString(), the_file_being_moved.getName());

            if (old_Path_.compareTo(new_Path_) == 0) {
                logger.log("WARNING illegal move ignored" + old_Path_.toAbsolutePath() + " == " + new_Path_.toAbsolutePath());
                popup = true;
                continue;
            }
            Command_old_and_new_Path cmd_ = Command_old_and_new_Path.command_move;
            if (destination_is_trash) cmd_ = Command_old_and_new_Path.command_move_to_trash;
            Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status_old_and_new_Path.rename_done,false);
            oan_list.add(oan);
        }

        if (popup) {
            Popups.popup_warning(owner, "Stupid move ignored", "Check the folders in the window title, it seems you are trying to move files from one folder to the SAME folder!?", false, logger);
        }
        perform_safe_moves_in_a_thread(owner, x,y,oan_list,  true, aborter,logger);
    }

    //**********************************************************
    public static void safe_move_a_file_or_dir_in_a_thread(Window owner, double x, double y, Path destination_dir, File the_file_being_moved, Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> oanl = new ArrayList<>();
        Path old_Path_ = the_file_being_moved.toPath();
        Path new_Path_ = Paths.get(destination_dir.toAbsolutePath().toString(), the_file_being_moved.getName());
        Command_old_and_new_Path cmd_ = Command_old_and_new_Path.command_move;
        Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status_old_and_new_Path.move_done,false);
        oanl.add(oan);
        perform_safe_moves_in_a_thread(owner,x,y, oanl,  true, aborter,logger);
    }

    //**********************************************************
    public static void safe_move_a_file_or_dir_NOT_in_a_thread(Window owner, double x, double y, Path new_Path_, File the_file_being_moved, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path old_Path_ = the_file_being_moved.toPath();
        Command_old_and_new_Path cmd_ = Command_old_and_new_Path.command_move;
        Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status_old_and_new_Path.move_done,false);

        List<Old_and_new_Path> oanl = new ArrayList<>();
        oanl.add(oan);
        actual_safe_moves(owner, x,y,oanl, true,aborter,logger);
    }



    //**********************************************************
    public static void perform_safe_moves_in_a_thread(Window owner, double x, double y, List<Old_and_new_Path> the_list,  boolean and_list_for_undo, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (the_list == null) {
            logger.log("FATAL perform_safe_moves_in_a_thread() list is null");
            return;

        }
        if (the_list.isEmpty()) {
            logger.log("warning:  perform_safe_moves_in_a_thread() list is empty");
            return;

        }
        if (moving_files_dbg) logger.log("perform_safe_moves_in_a_thread()");
        Runnable r = () -> actual_safe_moves(owner, x, y, the_list, and_list_for_undo, aborter, logger);
        try {
            Actor_engine.execute(r, logger);
            if (moving_files_dbg) logger.log("perform_safe_moves_in_a_thread LAUNCHED, thread COUNT=" + Thread.activeCount());
        } catch (RejectedExecutionException ree) {
            logger.log("perform_safe_moves_in_a_thread()" + ree);

        }


    }

    //**********************************************************
    public static void safe_delete_files(Window owner, double x, double y, List<Old_and_new_Path> l, Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> l2 = new ArrayList<>();
        for (Old_and_new_Path oanf : l) {
            Path trash_dir = Non_zooleans.get_trash_dir(oanf.old_Path,logger);
            Path new_Path = (Paths.get(trash_dir.toString(), oanf.get_old_Path().getFileName().toString()));
            Old_and_new_Path oanf2 = new Old_and_new_Path(oanf.old_Path, new_Path, oanf.cmd, oanf.status,false);
            l2.add(oanf2);
        }

        logger.log("safe_delete_all: perform_safe_moves_in_a_thread");

        Moving_files.perform_safe_moves_in_a_thread(owner,x,y,  l2,  true,aborter, logger);

    }

    //**********************************************************
    public static List<Old_and_new_Path> actual_safe_moves(Window owner, double x, double y, List<Old_and_new_Path> the_list,
                                          boolean and_list_for_undo, Aborter aborter , Logger logger)
    //**********************************************************
    {
        Hourglass hourglass = check_show_running_film(owner, x, y, the_list, aborter, logger);

        List<Old_and_new_Path> done = new ArrayList<>();
        List<Old_and_new_Path> not_done = new ArrayList<>();
        for (Old_and_new_Path oandn : the_list) {
            // record (last) move destination folder
            Redo_same_move_engine.last_destination_folder = oandn.new_Path.getParent();
            // we also move meta data
            Path meta_old = Metadata_handler.make_metadata_path(oandn.old_Path);
            if (meta_old.toFile().exists()) {
                // if there is an associated metadata file, move it too
                Path meta_new = Metadata_handler.make_metadata_path(oandn.new_Path);
                process_one_move(owner, new Old_and_new_Path(meta_old, meta_new, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command,false), aborter, logger);
            }

            {
                // we rename the ICON to avoid remaking one

                Path icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir(owner, Cache_folder.klik_icon_cache,logger);
                int icon_size = Non_zooleans.get_icon_size();
                File current_icon = From_disk.file_for_icon_caching(icon_cache_dir, oandn.old_Path,String.valueOf(icon_size), Icon_factory_actor.png_extension);
                if (current_icon.exists()) {
                    File new_icon = From_disk.file_for_icon_caching(icon_cache_dir, oandn.new_Path, String.valueOf(icon_size), Icon_factory_actor.png_extension);

                    //Files.move(current_icon.toPath(), new_icon.toPath());
                    //FileUtils.moveFile(current_icon, new_icon, StandardCopyOption.REPLACE_EXISTING);
                    if ( !Static_files_and_paths_utilities.move_file(current_icon.toPath(),new_icon.toPath(),logger))
                    {
                        logger.log("icon move failed");
                    }
                }
                //logger.log("renaming icon :"+current_icon.getName()+"==>"+new_icon.getName());
            }

            // then we move the actual file
            Old_and_new_Path actual = process_one_move(owner, oandn, aborter, logger);
            if ( actual==null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("move has failed for "+oandn.get_old_Path()));
                continue;
            }
            if (moving_files_dbg) logger.log("A move has been completed and the status is: " + actual.status);

            switch (actual.status) {
                case move_done, rename_done, move_to_trash_done, identical_file_moved_to_klik_trash, identical_file_deleted, delete_forever_done, copy_done ->
                        done.add(actual);
                default -> {
                    not_done.add(actual);
                    logger.log("WARNING status is weird:" + actual.status);
                }
            }


        }

        if ( !done.isEmpty())
        {
            Change_gang.report_changes(done);
            if ( and_list_for_undo)
            {
                Undo_for_moves.add(done, logger);
            }
        }

        if (!not_done.isEmpty()) {
            Change_gang.report_changes(not_done);
            StringBuilder sb = new StringBuilder();
            for (Old_and_new_Path i : not_done) {
                sb.append(i.old_Path.toAbsolutePath());
                sb.append("  ==> ");
                sb.append(i.new_Path.toAbsolutePath());
                sb.append("   ");
                sb.append(i.status);

            }
            boolean for_3seconds = true;
            if (not_done.size() >= 2) for_3seconds = false;
            Popups.popup_warning(owner, "Moves not done?", sb.toString(), for_3seconds, logger);
            logger.log(Stack_trace_getter.get_stack_trace("Moves not done? " + sb));
        }

        if ( hourglass != null) hourglass.close();


        return done;
    }

    //**********************************************************
    private static Hourglass check_show_running_film(Window owner, double x, double y, List<Old_and_new_Path> the_list, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if ( the_list.isEmpty()) return null;
        boolean show_running_film = false;

        if ( the_list.size() > 2 ) show_running_film = true;
        else {
            Old_and_new_Path oand = the_list.getFirst();

            if ( oand.old_Path.toFile().isDirectory())
            {
                Sizes sizes =   Static_files_and_paths_utilities.get_sizes_on_disk_deep_concurrent(oand.old_Path, aborter, logger);
                if ( sizes.bytes() > 10_000_000) show_running_film = true;
            }
            else
            {
                if (oand.old_Path.toFile().length() > 10_000_000) show_running_film = true;
            }
        }
        if ( show_running_film)
        {
            return Show_running_film_frame.show_running_film(owner,x,y, "File(s) are being moved", 20000, aborter, logger);
        }
        return null;
    }

    private static final boolean unsafe = true;

    //**********************************************************
    private static Old_and_new_Path process_one_move(Window owner, Old_and_new_Path oandn, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (oandn.cmd == Command_old_and_new_Path.command_move)
        {
            // when this is NOT a move to trash,
            // we check if the destination file is already there
            if (file_contents_are_identical(oandn, aborter, logger))
            {
                // Yes the destination is there AND identical content
                // the use case is a reconciliation of multiple copies with different NAMES
                // in that case trying to "overwrite" is counterproductive (and depending on OS may fail in different ways)
                // so instead we have 2 choices
                // 1. move the source file to klik_trash aka "safe"
                // 2. delete the source file, since we have it already in the destination
                // IN BOTH CASES WE CALL THIS ROUTINE AGAIN BUT THE COMMAND IS CHANGED
                if (unsafe)
                {
                    // TRANSFORM the command to "delete_for_ever"
                    Old_and_new_Path new_ = new Old_and_new_Path(oandn.old_Path, null, Command_old_and_new_Path.command_delete_forever, Status_old_and_new_Path.identical_file_deleted,false);
                    logger.log(oandn.get_old_Path() + " deleted because a file at destination has exactly the same content");
                    return process_one_move(owner, new_, aborter, logger);
                }
                else
                {
                    Path new_path = Paths.get(Non_zooleans.get_trash_dir(oandn.old_Path, logger).toAbsolutePath().toString(), oandn.old_Path.getFileName().toString());
                    new_path = generate_new_candidate_name(new_path, "", "_identical_file", logger);
                    Old_and_new_Path new_ = new Old_and_new_Path(oandn.old_Path, new_path, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.identical_file_moved_to_klik_trash,false);
                    logger.log(oandn.get_old_Path() + " moved to klik_trash because a file at destination has exactly the same content");
                    return process_one_move(owner, new_, aborter, logger);
                }
            }
            else
            {
                //logger.log(oandn.get_old_Path() + " files are not identical");
            }

        }

        if (oandn.get_new_Path() == null)
        {
            logger.log("oandn.get_new_Path() is null, this is a delete forever of:" + oandn.get_old_Path());
            return do_the_move_or_delete(owner, oandn,aborter,logger);
        }

        // this is a MOVE, and there is a risk that the destination FILE exists
        // to avoid erase the destination file, we try to find a new name
        // MAGIC: try up to 42000 new names
        for (int i = 0; i < 42_000; i++) {
            //logger.log("oandn.get_new_Path() = " + oandn.get_new_Path());

            // one trick is to make sure we do not have case problems e.g. depending on file system
            File proposed_new_name = oandn.get_new_Path().toFile();
            String proposed_new_name_string = proposed_new_name.getName();

            File test = new File(oandn.get_new_Path().getParent().toFile(),proposed_new_name_string);
            if ( test.isDirectory())
            {
                // DIRECTORY
                if (test.exists())
                {
                    if(moving_files_dbg) logger.log("DIRECTORY "+ proposed_new_name_string+", this name is NOT ok, there is a file with that name in the folder: "+oandn.get_new_Path().getParent());
                    Path new_path = generate_new_candidate_name_special(oandn.get_new_Path(), "",  i, logger);
                    oandn = new Old_and_new_Path(oandn.old_Path, new_path, oandn.cmd, Status_old_and_new_Path.name_augmented,false);
                }
                else
                {
                    if(moving_files_dbg) logger.log("DIRECTORY "+ proposed_new_name_string+", this name is OK, no file with that name in the folder: "+oandn.get_new_Path().getParent());
                    return do_the_move_or_delete(owner, oandn, aborter,logger);
                }
            }
            else
            {
                //FILE
                if ( check_file_really_exists(test,logger))
                {
                    if(moving_files_dbg) logger.log("FILE "+proposed_new_name_string+" new name NOT ok, there is a file with that name");
                    Path new_path = generate_new_candidate_name_special(oandn.get_new_Path(), "", i, logger);
                    oandn = new Old_and_new_Path(oandn.old_Path, new_path, oandn.cmd, Status_old_and_new_Path.name_augmented,false);
                }
                else
                {
                    if(moving_files_dbg) logger.log("FILE " + proposed_new_name_string + " this name is OK, no file with that name");
                    return do_the_move_or_delete(owner, oandn, aborter,logger);
                }
            }


        }
        return null; // will cause a crash
    }
    //**********************************************************
    private static boolean check_file_really_exists(File f, Logger logger)
    //**********************************************************
    {
        if (f.isDirectory()) {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: dont use check_file_really_exists on a folder"));
            return true;
        }
        //if ( f.length() == 0) return true; DONT DO THAT, if the file does not exists length is zero !

        //return f.exists();

        // it seems that sometimes, when a network or USB drive is "under the water"
        // for example heavy traffic for USB drives,
        // or shaky network like "mobile in the countryside",
        // the call File::exists() return false... but the file actually exists!
        // this is a problem when we do not want to overwrite ANY file
        // since we would prefer to rename that file before moving it
        //
        // a workaround tried here is to try to open the file and read one byte...
        // clearly, this is shaky since reading from the file may also fail on the same
        // saturated USB or network conditions ...

        try {
            byte[] buffer = new byte[1];
            InputStream is = new FileInputStream(f);
            if (is.read(buffer) != buffer.length) {
                if (moving_files_dbg) logger.log("Warning: the file exists but is empty");
                return true;
            }
            if (moving_files_dbg) logger.log("can read 1 byte, this is file " + f.getAbsolutePath());
            is.close();
            return true;
        } catch (java.io.FileNotFoundException e) {
            if (moving_files_dbg) logger.log("check_file_really_exists() ?... seems that it does not exist: " + e);
            return false;
        }
        catch (java.io.IOException e) {
            if (moving_files_dbg) logger.log("cannot read 1 byte, it seems this: " + f.getAbsolutePath()+" is NOT file, exists()="+f.exists()+", isDirectory()="+f.isDirectory());
            return false;
        }
    }


    //**********************************************************
    private static boolean file_contents_are_identical(Old_and_new_Path oandn, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (Files.exists(oandn.get_new_Path())) {
            File_with_a_few_bytes mf1 = new File_with_a_few_bytes(oandn.old_Path.toFile(), logger);
            File_with_a_few_bytes mf2 = new File_with_a_few_bytes(oandn.new_Path.toFile(), logger);

            // identical or not ?
            return File_with_a_few_bytes.files_have_same_content(mf1, mf2, aborter, logger);
        }
        if (moving_files_dbg) logger.log("new path does not exist, so not identical " + oandn.get_string());
        return false;
    }


    //**********************************************************
    private static Old_and_new_Path do_the_move_or_delete(Window owner, Old_and_new_Path oandn, Aborter aborter, Logger logger)
    //**********************************************************
    {
        try {
            if (oandn.cmd == Command_old_and_new_Path.command_delete_forever)
            {
                if (moving_files_dbg) logger.log("delete for ever issued for : " + oandn.get_old_Path());
                Files.delete(oandn.get_old_Path());
                if (moving_files_dbg) logger.log("delete for ever DONE for : " + oandn.get_old_Path());
            }
            else
            {
                if (Static_files_and_paths_utilities.is_same_path(oandn.get_old_Path(), oandn.get_new_Path(), logger)) {
                    if (moving_files_dbg)
                        logger.log("WARNING !!! do_the_move not performed : identical paths " + oandn.get_old_Path() + "=>" + oandn.get_new_Path());
                    return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.same_path,false);
                }
                if (moving_files_dbg) logger.log("move command issued : " + oandn.get_old_Path() + "=>" + oandn.get_new_Path());
                long start = System.currentTimeMillis();
                if (oandn.get_old_Path().toFile().isFile())
                {
                    // preserves attributes by default:
                    FileUtils.moveFile(oandn.get_old_Path().toFile(), oandn.get_new_Path().toFile());
                }
                else
                {
                    //Static_files_and_paths_utilities.move_file(oandn.get_old_Path(),oandn.get_new_Path(),logger);

                    FileUtils.moveDirectory(oandn.get_old_Path().toFile(), oandn.get_new_Path().toFile());
                }

                if ( System.currentTimeMillis()-start > 5_000)
                {
                    if (Zooleans.get_boolean(Zooleans.DING_IS_ON))
                    {
                        Ding.play("file moving takes more than 5s", logger);
                    }
                }
            }
            return move_success(oandn, logger);

        }
        catch (AccessDeniedException x)
        {
            logger.log("WARNING1: move failed " + oandn.get_old_Path() + " ACCESS DENIED exception ");
            return move_failed(owner, oandn, x, aborter,logger);
        }

        catch (FileNotFoundException x) {
            logger.log("WARNING3: move failed " + oandn.get_old_Path() + " file not found exception, the source does not exists?"+oandn.get_old_Path());
            return move_failed(owner, oandn, x, aborter,logger);
        }
        catch (DirectoryNotEmptyException x)
        {
            if (oandn.old_Path.toFile().isDirectory()) {
                // (on Macos for sure; other OS I dont know) when moving a directory across file systems
                // e.g. from main drive to external USB drive
                // DirectoryNotEmptyException is raised ....
                // hypothesis: Files.move(x) is implemented as first a copy (which succeeds!)
                // and then when a delete of the source folder is attempted... DirectoryNotEmptyException
                //
                logger.log("WARNING4: move failed " + oandn.get_old_Path() + " directory not empty exception\nThis may happen when moving a folder across filesystems: the origin is still there!");
                Popups.popup_warning(owner, "Directory was COPIED", "..instead of moved because it was across 2 different filesystems", true, logger);
                return move_failed(owner, oandn, x, aborter,logger);
            }

            logger.log(oandn.get_old_Path() + " directory not empty: it is not allowed!");
            Popups.popup_Exception(x, 200, "Directory is not empty", logger);
            return move_failed(owner, oandn, x, aborter,logger);
        } catch (IOException e) {
            logger.log("WARNING5 "+oandn.get_old_Path() + " " + e);
            if ( !oandn.get_old_Path().toFile().canWrite())
            {
                logger.log("cannot write "+oandn.get_old_Path() + " " + e);
                Popups.popup_warning(owner, "File is not writeable:"+oandn.get_old_Path(), "This file cannot be moved because its file-system properties do not allow it", false, logger);

            }
            return move_failed(owner, oandn, e, aborter,logger);
        }
    }


    //**********************************************************
    private static Old_and_new_Path move_failed(Window owner, Old_and_new_Path oandn, IOException e0, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("******* move failed for: *********\n" +
                "->" + oandn.get_old_Path() + "<-\n" +
                "==>\n" +
                "->" + oandn.get_new_Path() + "<-\n" +
                "" + e0 + "\n Note this error may show when moving directories across file systems e.g. USB drive\n" +
                "***********************************");

        if (oandn.get_new_Path() == null) {
            return new Old_and_new_Path(oandn.old_Path, null, oandn.cmd, Status_old_and_new_Path.command_failed,false);
        }
        if (!Files.exists(oandn.get_new_Path().getParent()))
        {
            logger.log("FAILED to move file, target dir does not exists->" + oandn.get_new_Path().getParent() + "<-" + e0);
            Path path = oandn.get_new_Path().getParent();
            Non_zooleans.get_main_properties_manager().remove(path.toAbsolutePath().toString());

            return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.target_dir_does_not_exist,false);
        } else {
            logger.log("destination folder exists but ... FAILED to move file for some other reason->" + oandn.get_old_Path().toAbsolutePath() +
                    "<- into ->" + oandn.get_new_Path().toAbsolutePath() + "<-\n" + e0);

            // ok so we try to COPY instead
            // for external drives (e.g. USB) it may make a difference for FOLDERS
            // that is: move works for individual files, but nt for folders...
            // for reasons that are a bit mysterious to me?
            try {
                if (oandn.get_old_Path().toFile().isFile()) {
                    Files.copy(oandn.get_old_Path(), oandn.get_new_Path());

                } else {
                    //Static_files_and_paths_utilities.copy_dir(oandn.get_old_Path(), oandn.get_new_Path(),logger);
                    FileUtils.copyDirectory(oandn.get_old_Path().toFile(), oandn.get_new_Path().toFile());
                }


                String local_string =
                        My_I18n.get_I18n_string("We_tried_moving", logger)
                                + oandn.get_old_Path().toAbsolutePath()
                                + My_I18n.get_I18n_string("Into", logger)
                                + oandn.get_new_Path().toAbsolutePath()
                                + My_I18n.get_I18n_string("And_it_worked", logger);
                if (moving_files_dbg) Popups.popup_warning(owner, "Move success (dbg is on)", local_string, false, logger);
                logger.log(local_string + "<-\n" + e0);
                return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, Command_old_and_new_Path.command_copy, Status_old_and_new_Path.copy_done,false);
            }
            catch (FileAlreadyExistsException ex) {
                logger.log("FATAL! we tried moving a file/dir and it failed, so we tried to copy instead and is ALSO failed!" + oandn.get_old_Path().toAbsolutePath() +
                        "<- into ->" + oandn.get_new_Path().toAbsolutePath() + "<-\n" + ex);
            }
            catch (IOException ex) {
                logger.log("FATAL! we tried moving a file/dir and it failed, so we tried to copy instead and is ALSO failed!" + oandn.get_old_Path().toAbsolutePath() +
                        "<- into ->" + oandn.get_new_Path().toAbsolutePath() + "<-\n" + ex);
            }

        }
        return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.command_failed,false);

    }


    //**********************************************************
    private static Old_and_new_Path move_success(Old_and_new_Path oandn, Logger logger)
    //**********************************************************
    {
        if (moving_files_dbg) {
            String txt = "move_success() cmd:" + oandn.get_cmd() + ":\nold:" + oandn.get_old_Path().toAbsolutePath();
            if (oandn.get_new_Path() != null) {
                txt += "\nnew:" + oandn.get_new_Path().toAbsolutePath();
            } else {
                txt += "\nnew: null (delete forever)";
            }
            logger.log(txt);
        }
        if (oandn.run_after != null) oandn.run_after.run();

        return switch (oandn.get_cmd()) {
            case command_move_to_trash ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.move_to_trash_done,false);
            case command_delete_forever ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.delete_forever_done,false);
            case command_edit ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.edition_requested,false);
            case command_move ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.move_done,false);
            case command_rename ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.rename_done,false);
            default ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.command_failed,false);
        };
    }

    //**********************************************************
    public static Path generate_new_candidate_name(Path old_path, String prefix, String postfix, Logger logger)
    //**********************************************************
    {
        String base_name = Static_files_and_paths_utilities.get_base_name(old_path.getFileName().toString());
        String extension = Static_files_and_paths_utilities.get_extension(old_path.getFileName().toString());
        String new_name = prefix + base_name + postfix + "." + extension;
        if (moving_files_dbg) logger.log("generate_new_candidate_name=" + new_name);
        return Paths.get(old_path.getParent().toString(), new_name);
    }
    //**********************************************************
    public static Path generate_new_candidate_name_special(Path old_path, String prefix, int index, Logger logger)
    //**********************************************************
    {
        String base_name = Static_files_and_paths_utilities.get_base_name(old_path.getFileName().toString());
        String extension = Static_files_and_paths_utilities.get_extension(old_path.getFileName().toString());

        {
            Path path = name_is_alredy_a_count(old_path,prefix,base_name,extension,logger);
            if ( path != null)
            {
                return path;
            }

        }
        String new_name = prefix + base_name + index + "." + extension;
        //if (dbg)
            logger.log("generate_new_candidate_name_special=" + new_name);
        return Paths.get(old_path.getParent().toString(), new_name);

    }

    private static final Random random = new Random();
    //**********************************************************
    private static Path name_is_alredy_a_count(Path old_path, String prefix, String base_name, String extension, Logger logger)
    //**********************************************************
    {
        int lenght_of_trailing_numbers = 0;
        for(int i = base_name.length()-1;i >=0; i--)
        {
            char c = base_name.charAt(i);
            if (Character.isDigit(c))
            {
                lenght_of_trailing_numbers++;
            }
            else {
                break;
            }
        }
        if (lenght_of_trailing_numbers == 0) return null; // nope
        if (moving_files_dbg) logger.log(base_name+" ends with a number of length "+lenght_of_trailing_numbers);
        String number =  base_name.substring(base_name.length()-lenght_of_trailing_numbers);
        if (moving_files_dbg) logger.log(base_name+" ends with a number = "+number);
        long k = 0;
        try
        {
            k = Long.parseLong(number);
        }
        catch (NumberFormatException e)
        {
            return null;
        }

        for(int i = 1; i< 10000; i++)
        {
            int ii = i;
            if (i > 500) ii = random.nextInt(10000000);
            String new_integer_with_leading_zeroes = String.format("%0"+lenght_of_trailing_numbers+"d",(k+ii));
            String new_name = prefix+base_name.substring(0, base_name.length() - lenght_of_trailing_numbers) + new_integer_with_leading_zeroes+ "." + extension;
            if (moving_files_dbg) logger.log("candidate new_name? ->" + new_name+"<-");
            Path path = Paths.get(old_path.getParent().toString(), new_name);
            if ( !path.toFile().exists())
            {
                logger.log("NEW NAME ->"+path.toAbsolutePath()+"<-");
                return path;
            }
        }
        return null;
    }

    /*
    public static Path try_to_prune(Path path, Logger logger)
    {
        // if the file name ends with SP_EZ_IA_L + N,
        // try to check if e can remove this postfix i.e. no file with the same name exist
        String raw = path.getFileName().toString();
        String base_name = Static_files_and_paths_utilities.get_base_name(raw);
        String extension = Static_files_and_paths_utilities.get_extension(raw);
        logger.log(base_name + " extension->" + extension + "<-");
        int i = base_name.lastIndexOf(SP_EZ_IA_L);
        if (i < 0) {
            logger.log("no remainer");
            return null;
        }
        String pruned_name = base_name.substring(0, i);
        String remainer = base_name.substring(i + SP_EZ_IA_L.length());
        logger.log("remainer->" + remainer + "<-");
        try {
            int N = Integer.valueOf(remainer);
        } catch (NumberFormatException e) {
            logger.log("remainer->" + remainer + "<- not a number, not pruned");
            return null;
        }
        Path new_path = Paths.get(path.getParent().toAbsolutePath().toString(), pruned_name);
        if (extension.isEmpty()) {
            new_path = Paths.get(path.getParent().toAbsolutePath().toString(), pruned_name + ".jpg");
        } else {
            new_path = Paths.get(path.getParent().toAbsolutePath().toString(), pruned_name + "." + extension);
        }
        File candidate = new_path.toFile();
        if (candidate.exists()) return null;
        return Paths.get(path.getParent().toAbsolutePath().toString(), pruned_name);
    }
*/

}
