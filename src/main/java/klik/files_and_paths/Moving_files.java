package klik.files_and_paths;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.Icon_factory_actor;
import klik.change.Change_gang;
import klik.change.undo.Undo_engine;
import klik.images.Same_move_engine;
import klik.level2.metadata.Metadata_handler;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.*;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

//**********************************************************
public class Moving_files
//**********************************************************
{
    public static final String SP_EZ_IA_L = "_copy_made_by_klik_";

    private static final boolean dbg = false;

    //**********************************************************
    public static void safe_move_files_or_dirs(Stage owner,
                                               Path destination_dir,
                                               boolean destination_is_trash,
                                               List<File> the_files_being_moved,
                                               Aborter aborter,
                                               Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> oanl = new ArrayList<>();
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
            Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status_old_and_new_Path.rename_done);
            oanl.add(oan);
        }

        if (popup) {
            Popups.popup_warning(owner, "Stupid move ignored", "Check the folders in the window title, it seems you are trying to move files from one folder to the SAME folder!?", false, logger);
        }
        perform_safe_moves_in_a_thread(owner, oanl, aborter, true, logger);
    }

    //**********************************************************
    public static void safe_move_a_file_or_dir(Stage owner, Path destination_dir, File the_file_being_moved, Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> oanl = new ArrayList<>();
        Path old_Path_ = the_file_being_moved.toPath();
        Path new_Path_ = Paths.get(destination_dir.toAbsolutePath().toString(), the_file_being_moved.getName());
        Command_old_and_new_Path cmd_ = Command_old_and_new_Path.command_move;
        Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status_old_and_new_Path.move_done);
        oanl.add(oan);
        perform_safe_moves_in_a_thread(owner, oanl, aborter, true, logger);
    }

    //**********************************************************
    public static void perform_safe_moves_in_a_thread(Stage owner, List<Old_and_new_Path> the_list,  Aborter aborter,boolean and_list_for_undo, Logger logger)
    //**********************************************************
    {
        perform_safe_moves_in_a_thread(owner, the_list, aborter, true, and_list_for_undo,logger);
    }

    //**********************************************************
    public static void perform_safe_moves_in_a_thread(Stage owner, List<Old_and_new_Path> the_list, Aborter aborter, boolean JFX, boolean and_list_for_undo, Logger logger)
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
        if ( dbg) logger.log("perform_safe_moves_in_a_thread()");
        Runnable r = () -> actual_safe_moves(owner, the_list, aborter, JFX, and_list_for_undo, logger);
        try {
            Actor_engine.execute(r,logger);
            if ( dbg) logger.log("perform_safe_moves_in_a_thread LAUNCHED, thread COUNT=" + Thread.activeCount());
        } catch (RejectedExecutionException ree) {
            logger.log("perform_safe_moves_in_a_thread()" + ree);

        }


    }

    //**********************************************************
    public static void perform_safe_move_in_a_thread(Stage owner, Old_and_new_Path oanp, Aborter aborter, boolean JFX, boolean and_list_for_undo, Logger logger)
    //**********************************************************
    {
        if (oanp == null) {
            logger.log("FATAL perform_safe_move_in_a_thread() oanp is null");
            return;

        }

        logger.log("perform_safe_move_in_a_thread()");
        Runnable r = () -> actual_safe_move(owner, oanp, JFX, aborter, logger);
        try {
            Actor_engine.execute(r,logger);
            logger.log("actual_safe_move LAUNCHED, thread COUNT=" + Thread.activeCount());
        } catch (RejectedExecutionException ree) {
            logger.log("perform_safe_move_in_a_thread()" + ree);

        }


    }

    //**********************************************************
    private static void actual_safe_move(Stage owner, Old_and_new_Path oandn, boolean JFX,
                                         //boolean and_list_for_undo,
                                         Aborter aborter, Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> done = new ArrayList<>();
        List<Old_and_new_Path> not_done = new ArrayList<>();

        // we also move meta data
        Path meta_old = Metadata_handler.make_metadata_path(oandn.old_Path);
        if (meta_old.toFile().exists()) {
            // if there is an associated metadata file, move it too
            Path meta_new = Metadata_handler.make_metadata_path(oandn.new_Path);
            process_one_move(owner, new Old_and_new_Path(meta_old, meta_new, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command), aborter, logger);
        }

        {
            // we rename the icon to avoid remaking one
            Path icon_cache_dir = Files_and_Paths.get_icon_cache_dir(logger);
            int icon_size = Static_application_properties.get_icon_size(logger);
            File current_icon = From_disk.file_for_icon_cache(icon_cache_dir, oandn.old_Path, String.valueOf(icon_size), Icon_factory_actor.png_extension);
            if (current_icon.exists()) {
                File new_icon = From_disk.file_for_icon_cache(icon_cache_dir, oandn.new_Path,  String.valueOf(icon_size), Icon_factory_actor.png_extension);
                if (new_icon == null) {
                    logger.log("icon move failed: cannot make icon ?");
                } else {
                    try {
                        //Files.move(current_icon.toPath(), new_icon.toPath());
                        FileUtils.moveFile(current_icon, new_icon, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        logger.log("icon move failed: " + e);
                    }
                }
            }
            //logger.log("renaming icon :"+current_icon.getName()+"==>"+new_icon.getName());
        }

        // then we move the actual file
        Old_and_new_Path actual = process_one_move(owner, oandn, aborter, logger);
        if ( actual==null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("move has failed??"));
            return;
        }
        if ( dbg) logger.log("A move has been completed and the status is: " + actual.status);

        switch (actual.status) {
            case move_done, rename_done, move_to_trash_done, identical_file_moved_to_klik_trash, identical_file_deleted, delete_forever_done, copy_done ->
                    done.add(actual);
            default -> {
                not_done.add(actual);
                logger.log("WARNING status is weird:" + actual.status);
            }
        }


        Change_gang.report_changes(done);
        Change_gang.report_changes(not_done);

        if (!not_done.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            for (Old_and_new_Path i : not_done)
            {
                sb.append(i.old_Path.toAbsolutePath());
                sb.append("  ==> ");
                sb.append(i.new_Path.toAbsolutePath());
                sb.append("   ");
                sb.append(i.status);

            }
            boolean for_3seconds = true;
            if (not_done.size() >= 2) for_3seconds = false;
            if (JFX)
                Popups.popup_warning(owner, "Move not done?", sb.toString(), for_3seconds, logger);
            logger.log(Stack_trace_getter.get_stack_trace("Move not done? " + sb));
        }

    }

    //**********************************************************
    private static void actual_safe_moves(Stage owner, List<Old_and_new_Path> the_list, Aborter aborter, boolean JFX, boolean and_list_for_undo, Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> done = new ArrayList<>();
        List<Old_and_new_Path> not_done = new ArrayList<>();
        for (Old_and_new_Path oandn : the_list) {
            // record (last) move destination folder
            Same_move_engine.last_destination_folder = oandn.new_Path.getParent();
            // we also move meta data
            Path meta_old = Metadata_handler.make_metadata_path(oandn.old_Path);
            if (meta_old.toFile().exists()) {
                // if there is an associated metadata file, move it too
                Path meta_new = Metadata_handler.make_metadata_path(oandn.new_Path);
                process_one_move(owner, new Old_and_new_Path(meta_old, meta_new, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command), aborter, logger);
            }

            {
                // we rename the ICON to avoid remaking one
                Path icon_cache_dir = Files_and_Paths.get_icon_cache_dir(logger);
                int icon_size = Static_application_properties.get_icon_size(logger);
                File current_icon = From_disk.file_for_icon_cache(icon_cache_dir, oandn.old_Path,String.valueOf(icon_size), Icon_factory_actor.png_extension);
                if (current_icon.exists()) {
                    File new_icon = From_disk.file_for_icon_cache(icon_cache_dir, oandn.new_Path, String.valueOf(icon_size), Icon_factory_actor.png_extension);
                    try {
                        //Files.move(current_icon.toPath(), new_icon.toPath());
                        FileUtils.moveFile(current_icon, new_icon, StandardCopyOption.REPLACE_EXISTING);
                    } catch (FileAlreadyExistsException e) {
                        logger.log("icon move failed: " + e);
                    }
                    catch (IOException e) {
                        logger.log("icon move failed: " + e);
                    }
                }
                //logger.log("renaming icon :"+current_icon.getName()+"==>"+new_icon.getName());
            }

            // then we move the actual file
            Old_and_new_Path actual = process_one_move(owner, oandn, aborter, logger);
            if ( actual==null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("move has failed??"));
                return;
            }
            if (dbg) logger.log("A move has been completed and the status is: " + actual.status);

            switch (actual.status) {
                case move_done, rename_done, move_to_trash_done, identical_file_moved_to_klik_trash, identical_file_deleted, delete_forever_done, copy_done ->
                        done.add(actual);
                default -> {
                    not_done.add(actual);
                    logger.log("WARNING status is weird:" + actual.status);
                }
            }


        }

        Change_gang.report_changes(done);
        Change_gang.report_changes(not_done);

        if (!not_done.isEmpty()) {
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
            if (JFX)
                Popups.popup_warning(owner, "Moves not done?", sb.toString(), for_3seconds, logger);
            logger.log(Stack_trace_getter.get_stack_trace("Moves not done? " + sb));
        }

    }

    private static final boolean unsafe = true;

    //**********************************************************
    private static Old_and_new_Path process_one_move(Stage owner, Old_and_new_Path oandn, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (oandn.cmd == Command_old_and_new_Path.command_move) {
            // when this is NOT a move to trash,
            // we check if the destination file is already there
            if (file_contents_are_identical(oandn, aborter, logger)) {
                // Yes ! the use case is a reconciliation of multiple copies
                // in that case trying to "overwrite" is counterproductive (and depending on OS may fail in different ways)
                // so instead we have 2 choices
                // 1. move to klik_trash aka "safe"
                // 2. delete
                if (unsafe) {
                    // TRANSFORM the command to "delete_for_ever"
                    Old_and_new_Path new_ = new Old_and_new_Path(oandn.old_Path, null, Command_old_and_new_Path.command_delete_forever, Status_old_and_new_Path.identical_file_deleted);
                    logger.log(oandn.get_old_Path() + " deleted because a file at destination has exactly the same content");
                    return process_one_move(owner, new_, aborter, logger);
                } else {
                    Path new_path = Paths.get(Static_application_properties.get_trash_dir(logger).toAbsolutePath().toString(), oandn.old_Path.getFileName().toString());
                    new_path = generate_new_candidate_name(new_path, "", "_identical_file", logger);
                    Old_and_new_Path new_ = new Old_and_new_Path(oandn.old_Path, new_path, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.identical_file_moved_to_klik_trash);
                    logger.log(oandn.get_old_Path() + " moved to klik_trash because a file at destination has exactly the same content");
                    return process_one_move(owner, new_, aborter, logger);
                }
            }
            //logger.log(oandn.get_old_Path() + " files are not identical");

        }

        if (oandn.get_new_Path() == null) {
            logger.log("oandn.get_new_Path() is null, this is a delete forever of:" + oandn.get_old_Path());
            return do_the_move_or_delete(owner, oandn,logger);
        }
        // MAGIC: try up to 42000 new names
        for (int i = 0; i < 42000; i++) {
            //logger.log("oandn.get_new_Path() = " + oandn.get_new_Path());

            // ths trick is to make sure we do not have case problems e.g. depending on file system
            File proposed_new_name = oandn.get_new_Path().toFile();
            String proposed_new_name_string = proposed_new_name.getName();

            File test = new File(oandn.get_new_Path().getParent().toFile(),proposed_new_name_string);
            if ( test.isDirectory())
            {
                // DIRECTORY
                if (test.exists())
                {
                    if( dbg) logger.log("DIRECTORY "+ proposed_new_name_string+", this name is NOT ok, there is a file with that name in the folder: "+oandn.get_new_Path().getParent());
                    Path new_path = generate_new_candidate_name_special(oandn.get_new_Path(), "",  i, logger);
                    oandn = new Old_and_new_Path(oandn.old_Path, new_path, oandn.cmd, Status_old_and_new_Path.name_augmented);
                }
                else
                {
                    if( dbg) logger.log("DIRECTORY "+ proposed_new_name_string+", this name is OK, no file with that name in the folder: "+oandn.get_new_Path().getParent());
                    return do_the_move_or_delete(owner, oandn, logger);
                }
            }
            else
            {
                //FILE
                if ( check_file_really_exists(test,logger))
                {
                    if( dbg) logger.log("FILE "+proposed_new_name_string+" new name NOT ok, there is a file with that name");
                    Path new_path = generate_new_candidate_name_special(oandn.get_new_Path(), "", i, logger);
                    oandn = new Old_and_new_Path(oandn.old_Path, new_path, oandn.cmd, Status_old_and_new_Path.name_augmented);
                } else {
                    if( dbg) logger.log("FILE " + proposed_new_name_string + " this name is OK, no file with that name");
                    return do_the_move_or_delete(owner, oandn, logger);
                }
            }


        }
        return null; // will cause a crash
    }
    //**********************************************************
    public static boolean check_file_really_exists(File f, Logger logger)
    //**********************************************************
    {
        if (f.isDirectory()) {
            logger.log(Stack_trace_getter.get_stack_trace("WARNING: dont use check_file_really_exists on a folder"));
            return true;
        }
        //if ( f.length() == 0) return true; DONT DO THAT, if the file does not exists length is zero !


        //return f.exists();

        // it seems that sometimes, when a network or USB drive is "under the water" for example heavy traffic for USB drives,
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
                if ( dbg) logger.log("Warning: the file exists but is empty");
                return true;
            }
            if ( dbg) logger.log("can read 1 byte, this is file " + f.getAbsolutePath());
            is.close();
            return true;
        } catch (java.io.FileNotFoundException e) {
            if ( dbg) logger.log("check_file_really_exists() ?... seems that it does not exist: " + e);
            return false;
        }
        catch (java.io.IOException e) {
            if ( dbg) logger.log("cannot read 1 byte, it seems this: " + f.getAbsolutePath()+" is NOT file, exists()="+f.exists()+", isDirectory()="+f.isDirectory());
            return false;
        }
    }


    //**********************************************************
    static boolean file_contents_are_identical(Old_and_new_Path oandn, Aborter aborter, Logger logger)
    //**********************************************************
    {
        if (Files.exists(oandn.get_new_Path())) {
            My_File mf1 = new My_File(oandn.old_Path.toFile(), logger);
            My_File mf2 = new My_File(oandn.new_Path.toFile(), logger);

            // identical or not ?
            return My_File.files_have_same_content(mf1, mf2, aborter, logger);
        }
        if ( dbg) logger.log("new path does not exist, so not identical " + oandn.get_string());
        return false;
    }


    //**********************************************************
    private static Old_and_new_Path do_the_move_or_delete(Stage owner, Old_and_new_Path oandn, Logger logger)
    //**********************************************************
    {
        try {
            if (oandn.cmd == Command_old_and_new_Path.command_delete_forever)
            {
                if (dbg) logger.log("delete for ever issued for : " + oandn.get_old_Path());
                Files.delete(oandn.get_old_Path());
                if (dbg) logger.log("delete for ever DONE for : " + oandn.get_old_Path());
            }
            else
            {
                if (Files_and_Paths.is_same_path(oandn.get_old_Path(), oandn.get_new_Path(), logger)) {
                    if (dbg)
                        logger.log("WARNING !!! do_the_move not performed : identical paths " + oandn.get_old_Path() + "=>" + oandn.get_new_Path());
                    return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.same_path);
                }
                if (dbg) logger.log("move command issued : " + oandn.get_old_Path() + "=>" + oandn.get_new_Path());
                if (oandn.get_old_Path().toFile().isFile())
                {
                    // preserves attributes by default:
                    FileUtils.moveFile(oandn.get_old_Path().toFile(), oandn.get_new_Path().toFile());
                }
                else
                {
                    FileUtils.moveDirectory(oandn.get_old_Path().toFile(), oandn.get_new_Path().toFile());
                }

                {
                    List<Old_and_new_Path> l = new ArrayList<>();
                    l.add(oandn);
                    Undo_engine.add(l, logger);
                }
            }
            return move_success(oandn, logger);

        }
        catch (FileExistsException x) {
            logger.log("WARNING: move failed " + oandn.get_old_Path() + " file exists exception, the destination already exists"+oandn.get_new_Path());
            return move_failed(owner, oandn, x, logger);
        }
        catch (FileNotFoundException x) {
            logger.log("WARNING: move failed " + oandn.get_old_Path() + " file not found exception, the source does not exists?"+oandn.get_old_Path());
            return move_failed(owner, oandn, x, logger);
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
                logger.log("WARNING: move failed " + oandn.get_old_Path() + " directory not empty exception\nThis may happen when moving a folder across filesystems: the origin is still there!");
                Popups.popup_warning(owner, "Directory was COPIED", "..instead of moved because it was across 2 different filesystems", true, logger);
                return move_failed(owner, oandn, x, logger);
            }

            logger.log(oandn.get_old_Path() + " directory not empty: it is not allowed!");
            Popups.popup_Exception(x, 200, "Directory is not empty", logger);
            return move_failed(owner, oandn, x, logger);
        } catch (IOException e) {
            logger.log(oandn.get_old_Path() + " " + e);
            return move_failed(owner, oandn, e, logger);
        }
    }


    //**********************************************************
    private static Old_and_new_Path move_failed(Stage owner, Old_and_new_Path oandn, IOException e0, Logger logger)
    //**********************************************************
    {
        logger.log("******* move failed for: *********\n" +
                "->" + oandn.get_old_Path() + "<-\n" +
                "==>\n" +
                "->" + oandn.get_new_Path() + "<-\n" +
                "" + e0 + "\n Note this error may show when moving directories across file systems e.g. USB drive\n" +
                "***********************************");

        if (oandn.get_new_Path() == null) {
            return new Old_and_new_Path(oandn.old_Path, null, oandn.cmd, Status_old_and_new_Path.command_failed);
        }
        if (!Files.exists(oandn.get_new_Path().getParent()))
        {
            logger.log("FAILED to move file, target dir does not exists->" + oandn.get_new_Path().getParent() + "<-" + e0);
            Path path = oandn.get_new_Path().getParent();
            if (Static_application_properties.get_properties_manager(logger).remove_invalid_dir(path)) {
                logger.log("move failed because dir does not exist, so it has been removed from properties : " + path);
            } else {
                logger.log("WARNING: move failed because dir does not exist, but it could NOT be removed from properties : " + path);
            }
            return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.target_dir_does_not_exist);
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
                    FileUtils.copyDirectory(oandn.get_old_Path().toFile(), oandn.get_new_Path().toFile());
                }


                String local_string =
                        I18n.get_I18n_string("We_tried_moving", logger)
                                + oandn.get_old_Path().toAbsolutePath()
                                + I18n.get_I18n_string("Into", logger)
                                + oandn.get_new_Path().toAbsolutePath()
                                + I18n.get_I18n_string("And_it_worked", logger);
                if (dbg) Popups.popup_warning(owner, "Move success (dbg is on)", local_string, false, logger);
                logger.log(local_string + "<-\n" + e0);
                return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, Command_old_and_new_Path.command_copy, Status_old_and_new_Path.copy_done);
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
        return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.command_failed);

    }


    //**********************************************************
    private static Old_and_new_Path move_success(Old_and_new_Path oandn, Logger logger)
    //**********************************************************
    {
        if (dbg) {
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
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.move_to_trash_done);
            case command_delete_forever ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.delete_forever_done);
            case command_edit ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.edition_requested);
            case command_move ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.move_done);
            case command_rename ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.rename_done);
            default ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status_old_and_new_Path.command_failed);
        };
    }

    //**********************************************************
    public static Path generate_new_candidate_name(Path old_path, String prefix, String postfix, Logger logger)
    //**********************************************************
    {
        String base_name = FilenameUtils.getBaseName(old_path.getFileName().toString());
        String extension = FilenameUtils.getExtension(old_path.getFileName().toString());
        String new_name = prefix + base_name + postfix + "." + extension;
        if (dbg) logger.log("generate_new_candidate_name=" + new_name);
        return Paths.get(old_path.getParent().toString(), new_name);
    }
    //**********************************************************
    public static Path generate_new_candidate_name_special(Path old_path, String prefix, int index, Logger logger)
    //**********************************************************
    {
        String base_name = FilenameUtils.getBaseName(old_path.getFileName().toString());
        String extension = FilenameUtils.getExtension(old_path.getFileName().toString());

        {
            Path path = name_is_alredy_a_count(old_path,base_name,extension,logger);
            if ( path != null)
            {
                return path;
            }

        }
        int k = base_name.lastIndexOf(SP_EZ_IA_L);
        if ( k <0)
        {
            String new_name = prefix + base_name + SP_EZ_IA_L + index + "." + extension;
            if (dbg) logger.log("generate_new_candidate_name=" + new_name);
            return Paths.get(old_path.getParent().toString(), new_name);

        }
        else
        {
            String new_name = prefix + base_name.substring(0, k) + SP_EZ_IA_L + index + "." + extension;
            if (dbg) logger.log("generate_new_candidate_name=" + new_name);
            return Paths.get(old_path.getParent().toString(), new_name);
        }
    }

    //**********************************************************
    private static Path name_is_alredy_a_count(Path old_path, String base_name, String extension, Logger logger)
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
        logger.log(base_name+" ends with a number of length "+lenght_of_trailing_numbers);
        String number =  base_name.substring(base_name.length()-lenght_of_trailing_numbers);
        logger.log(base_name+" ends with a number = "+number);
        long k = Long.parseLong(number);

        for(int i = 1; i< 10000; i++) {
            String new_integer_with_leading_zeroes = String.format("%0"+lenght_of_trailing_numbers+"d",(k+i));
            String new_name = base_name.substring(0, base_name.length() - lenght_of_trailing_numbers) + new_integer_with_leading_zeroes+ "." + extension;
            logger.log("new_name=" + new_name);
            Path path = Paths.get(old_path.getParent().toString(), new_name);
            if ( !path.toFile().exists()) return path;
        }
        return null;
    }

    /*
    public static Path try_to_prune(Path path, Logger logger)
    {
        // if the file name ends with SP_EZ_IA_L + N,
        // try to check if e can remove this postfix i.e. no file with the same name exist
        String raw = path.getFileName().toString();
        String base_name = FilenameUtils.getBaseName(raw);
        String extension = FilenameUtils.getExtension(raw);
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
