//SOURCES ../../experimental/metadata/Metadata_handler.java
//SOURCES ../../change/Redo_same_move_engine.java

package klik.util.files_and_paths;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Or_aborter;
import klik.browser.icons.Icon_factory_actor;
import klik.change.Change_gang;
import klik.change.undo.Undo_for_moves;
import klik.change.Redo_same_move_engine;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Feature;
import klik.properties.Cache_folder;
import klik.experimental.metadata.Metadata_handler;
import klik.look.my_i18n.My_I18n;
import klik.properties.boolean_features.Booleans;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Progress_window;
import klik.util.ui.Popups;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.apache.commons.io.FileUtils;

//**********************************************************
public class Moving_files
//**********************************************************
{

    private static final boolean moving_files_dbg = false;

    //**********************************************************
    public static void safe_move_files_or_dirs(Path destination_dir,
                                               boolean destination_is_trash,
                                               List<File> the_files_being_moved,
                                               Window owner, double x, double y,
                                               Aborter aborter,
                                               Logger logger)
    //**********************************************************
    {
        List<Old_and_new_Path> oan_list = new ArrayList<>();
        boolean popup = false;
        for (File the_file_being_moved : the_files_being_moved)
        {
            Path old_Path_ = the_file_being_moved.toPath();
            Path new_Path_ = Paths.get(destination_dir.toAbsolutePath().toString(), the_file_being_moved.getName());

            if (old_Path_.compareTo(new_Path_) == 0) {
                logger.log("WARNING illegal move ignored" + old_Path_.toAbsolutePath() + " == " + new_Path_.toAbsolutePath());
                popup = true;
                continue;
            }
            Command cmd_ = Command.command_move;
            if (destination_is_trash) cmd_ = Command.command_move_to_trash;
            Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status.rename_done,false);
            oan_list.add(oan);
        }

        if (popup) {
            Popups.popup_warning( "Stupid move ignored", "Check the folders in the window title, it seems you are trying to move files from one folder to the SAME folder!?", false, owner,logger);
        }
        perform_safe_moves_in_a_thread(oan_list,  true, x,y,owner, aborter,logger);
    }



    //**********************************************************
    public static void safe_move_a_file_or_dir_NOT_in_a_thread(Path new_Path_, File the_file_being_moved, double x, double y, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path old_Path_ = the_file_being_moved.toPath();
        Command cmd_ = Command.command_move;
        Old_and_new_Path oan = new Old_and_new_Path(old_Path_, new_Path_, cmd_, Status.move_done,false);

        List<Old_and_new_Path> oanl = new ArrayList<>();
        oanl.add(oan);
        actual_safe_moves(oanl, true,x,y,owner, aborter,logger);
    }



    //**********************************************************
    public static void perform_safe_moves_in_a_thread(
            List<Old_and_new_Path> the_list,
            boolean and_list_for_undo,
            double x, double y, Window owner,
            Aborter aborter, Logger logger)
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
        Runnable r = () -> actual_safe_moves(the_list, and_list_for_undo, x, y, owner, aborter, logger);
        try {
            Actor_engine.execute(r, logger);
            if (moving_files_dbg) logger.log("perform_safe_moves_in_a_thread LAUNCHED, thread COUNT=" + Thread.activeCount());
        } catch (RejectedExecutionException ree) {
            logger.log("perform_safe_moves_in_a_thread()" + ree);

        }


    }

    //**********************************************************
    public static void safe_delete_files(List<Old_and_new_Path> l, double x, double y, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        // WHY create a copy of the list ?????
        List<Old_and_new_Path> l2 = new ArrayList<>();
        for (Old_and_new_Path oanf : l) {
            Path trash_dir = Non_booleans_properties.get_trash_dir(oanf.old_Path,owner,logger);
            Path new_Path = (Paths.get(trash_dir.toString(), oanf.old_Path.getFileName().toString()));
            Old_and_new_Path oanf2 = new Old_and_new_Path(oanf.old_Path, new_Path, oanf.cmd, oanf.status,false);
            l2.add(oanf2);
        }

        logger.log("safe_delete_all: perform_safe_moves_in_a_thread");

        Moving_files.perform_safe_moves_in_a_thread(l2,  true,x,y, owner, aborter, logger);

    }

    //**********************************************************
    public static List<Old_and_new_Path> actual_safe_moves(
            List<Old_and_new_Path> the_list,
            boolean and_list_for_undo,
            double x, double y,
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        Progress_window progress_window = check(the_list,x, y,  owner, logger);
        Aborter local = null;
        if ( progress_window == null) local = aborter;
        else local = new Or_aborter(aborter,progress_window.aborter,logger);
        List<Old_and_new_Path> done = new ArrayList<>();
        List<Old_and_new_Path> not_done = new ArrayList<>();
        for (Old_and_new_Path oandn : the_list)
        {
            if ( local.should_abort())
            {
                logger.log("file move aborted by user");
                break;
            }
            // record (last) move destination folder
            Redo_same_move_engine.last_destination_folder = oandn.new_Path.getParent();
            // we also move meta data
            Path meta_old = Metadata_handler.make_metadata_path(oandn.old_Path);
            if (meta_old.toFile().exists()) {
                // if there is an associated metadata file, move it too
                Path meta_new = Metadata_handler.make_metadata_path(oandn.new_Path);
                process_one_move(new Old_and_new_Path(meta_old, meta_new, Command.command_rename, Status.before_command,false), owner, local, logger);
            }

            {
                // we rename the ICON to avoid remaking one

                Path icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir(Cache_folder.klik_icon_cache,owner,logger);
                int icon_size = Non_booleans_properties.get_icon_size(owner);
                File current_icon = From_disk.file_for_icon_caching(icon_cache_dir, oandn.old_Path,String.valueOf(icon_size), Icon_factory_actor.png_extension);
                if (current_icon.exists()) {
                    File new_icon = From_disk.file_for_icon_caching(icon_cache_dir, oandn.new_Path, String.valueOf(icon_size), Icon_factory_actor.png_extension);

                    //Files.move(current_icon.toPath(), new_icon.toPath());
                    //FileUtils.moveFile(current_icon, new_icon, StandardCopyOption.REPLACE_EXISTING);
                    if ( !move_file(current_icon.toPath(),new_icon.toPath(),logger))
                    {
                        logger.log("icon move failed");
                    }
                }
                //logger.log("renaming icon :"+current_icon.getName()+"==>"+new_icon.getName());
            }

            // then we move the actual file
            Old_and_new_Path actual = process_one_move(oandn, owner, local, logger);
            if ( actual==null)
            {
                logger.log(Stack_trace_getter.get_stack_trace("move has failed for "+oandn.old_Path));
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
            Change_gang.report_changes(done,owner);
            if ( and_list_for_undo)
            {
                Undo_for_moves.add(done, owner, logger);
            }
        }

        if (!not_done.isEmpty()) {
            Change_gang.report_changes(not_done,owner);
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
            Popups.popup_warning( "Moves not done?", sb.toString(), for_3seconds, owner,logger);
            logger.log(Stack_trace_getter.get_stack_trace("Moves not done? " + sb));
        }

        if ( progress_window != null) progress_window.close();


        return done;
    }

    // determine if the operation may take a long time
    // if yes, return a progress window
    //**********************************************************
    private static Progress_window check(
            List<Old_and_new_Path> the_list,
            double x, double y,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        if ( the_list.isEmpty()) return null;

        boolean show_progress_window = false;
        if ( the_list.size() > 2 )
        {
            show_progress_window = true;
        }
        else
        {
            Old_and_new_Path oand = the_list.get(0);
            if ( oand.old_Path.toFile().isDirectory())
            {
                Sizes sizes =   Static_files_and_paths_utilities.get_sizes_on_disk_deep_concurrent(oand.old_Path, new Aborter("dummy",logger), logger);
                if ( sizes.bytes() > 10_000_000) show_progress_window = true;
            }
            else
            {
                if (oand.old_Path.toFile().length() > 10_000_000) show_progress_window = true;
            }
        }
        if ( show_progress_window)
        {
            return Progress_window.show(
                    true,
                    "File(s) are being moved",
                    20000,
                    x,
                    y,
                    owner,
                    logger);
        }
        return null;
    }

    // warning: this is a low level function,
    // it does not check for overwrite etc
    // prefer Moving_files.actual_safe_moves
    //**********************************************************
    public static boolean move_file(Path old_path, Path new_path, Logger logger)
    //**********************************************************
    {
        if (! old_path.toFile().exists())
        {
            logger.log("cannot move, file does not exists: "+old_path);
            return false;
        }
        // move a file, if the destination path contains folders that do not exist yet, create them

        Path parent = new_path.getParent();
        if ( parent != null && !parent.toFile().exists())
        {
            if ( !parent.toFile().mkdirs())
            {
                logger.log("cannot create folders for new path "+new_path.toAbsolutePath());
                return  false;
            }
        }

        try
        {
            Files.move(old_path,new_path);
        }
        catch (FileAlreadyExistsException e)
        {
            logger.log("FileAlreadyExistsException Files.move() cannot move "+old_path+" => "+ new_path);
            return false;
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("IOException Files.move() cannot move "+old_path+" => "+ new_path));
            try
            {
                Files.copy(old_path,new_path);
                logger.log("file/folder was copied instead "+old_path+" => "+ new_path);
            }
            catch (IOException ee)
            {
                logger.log("cannot copy "+old_path+" => "+ new_path+ " "+ee);
                return false;

            }
            try
            {
                Files.delete(old_path);
            }
            catch (IOException eee)
            {
                logger.log("cannot delete "+old_path+ " "+eee);
                return false;
            }
        }
        return true;
    }

    //**********************************************************
    private static Old_and_new_Path process_one_move(Old_and_new_Path oandn, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        boolean is_same_path = Static_files_and_paths_utilities.is_same_path(oandn.old_Path,oandn.new_Path,logger);
        if ( is_same_path)
        {
            logger.log("Move not done : identical paths " + oandn.old_Path + "=>" + oandn.new_Path);
            return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.same_path,false);
        }
        boolean destination_exists = destination_exists(oandn.new_Path, logger);
        boolean content_is_same = file_contents_are_identical(oandn, aborter, logger);
        if ( destination_exists)
        {
            if ( oandn.cmd == Command.command_move)
            {
                if (content_is_same) {

                    // the contents are the same
                    // then the "old" file goes to trash
                    Path new_path = generate_safe_name(Non_booleans_properties.get_trash_dir(oandn.old_Path, owner, logger), oandn.old_Path.getFileName().toString(), logger);
                    Old_and_new_Path oandn2 = new Old_and_new_Path(
                            oandn.old_Path,
                            new_path,
                            Command.command_move_to_trash,
                            Status.before_command,
                            false);
                    logger.log("--Move to trash as destination folder contains a copy--\nRequested:" + oandn.old_Path + "=>" + oandn.new_Path+"\nPerformed:" + oandn2.old_Path + "=>" + oandn2.new_Path);
                    return do_the_move_or_delete(oandn2, owner, aborter, logger);
                }
            }
            // we don't trust the developper to correctly set the command to Command.command_rename
            if ( is_a_rename(oandn,logger))
            {
                // "destination exists" on macos maybe the case that we want to rename
                // my_file.txt into My_File.txt and macos says My_File.txt exists because it
                // "sees" it as my_file.txt, which does exist, of course
                if ( names_differ_only_by_case(oandn, aborter, logger))
                {
                    if (content_is_same)
                    {
                        logger.log("Rename in 2 steps" + oandn.old_Path + "=>" + oandn.new_Path);

                        // the trick is to move in 2 steps with a temporary name
                        try {
                            String tmp_name = oandn.old_Path.getFileName().toString()+"___";
                            File tmp = oandn.old_Path.getParent().resolve(tmp_name).toFile();
                            FileUtils.moveFile(oandn.old_Path.toFile(), tmp);
                            FileUtils.moveFile(tmp, oandn.new_Path.toFile());
                            return move_success(oandn, logger);
                        }
                        catch( IOException e)
                        {
                            logger.log(Stack_trace_getter.get_stack_trace(""+e));
                            return move_failed(oandn,e,owner,aborter,logger);
                        }
                    }
                }

            }

            // in any case we MUST NOT overwrite the destination
            Path new_path = generate_safe_name(oandn.new_Path.getParent(), oandn.new_Path.getFileName().toString(),logger);
            Old_and_new_Path oandn2 = new Old_and_new_Path(
                    oandn.old_Path,
                    new_path,
                    Command.command_move,
                    Status.before_command,
                    false);
            logger.log("--Move--\nRequested:" + oandn.old_Path + "=>" + oandn.new_Path+"\nPerformed:" + oandn2.old_Path + "=>" + oandn2.new_Path);

            return do_the_move_or_delete(oandn2,owner,aborter,logger);
        }
        else
        {
            // no problemo
            logger.log("no problemo move:" + oandn.old_Path + "=>" + oandn.new_Path);
            logger.log("--Move (no problemo)--\nPerformed:" + oandn.old_Path + "=>" + oandn.new_Path);

            return do_the_move_or_delete(oandn,owner,aborter,logger);
        }

    }

    //**********************************************************
    private static boolean is_a_rename(Old_and_new_Path oandn, Logger logger)
    //**********************************************************
    {
        return Static_files_and_paths_utilities.is_same_path(oandn.old_Path.getParent(),oandn.new_Path.getParent(),logger);
    }

    //**********************************************************
    private static Path generate_safe_name(Path folder, String original_name, Logger logger)
    //**********************************************************
    {
        String base_name = Extensions.get_base_name(original_name);
        String extension = Extensions.get_extension(original_name);

        String candidate_name = original_name;
        for (int index = 1; index < 100000; index++)
        {
            Path candidate_path = Path.of(folder.toAbsolutePath().toString(),candidate_name);
            if (!destination_exists(candidate_path,logger))
            {
                return candidate_path;
            }
            candidate_name = increment_if_name_is_already_a_count(base_name,index,logger);
            if ( candidate_name == null) break;
            if ( ! extension.isEmpty()) candidate_name = Extensions.add(candidate_name,extension);
        }
        candidate_name = base_name+ UUID.randomUUID();
        if ( ! extension.isEmpty()) candidate_name = Extensions.add(candidate_name,extension);
        return Path.of(folder.toAbsolutePath().toString(), candidate_name);
    }

    //**********************************************************
    private static String increment_if_name_is_already_a_count(String base_name, int index, Logger logger)
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

        String new_integer_with_leading_zeroes = String.format("%0"+lenght_of_trailing_numbers+"d",(k+index));
        String new_name = base_name.substring(0, base_name.length() - lenght_of_trailing_numbers) + new_integer_with_leading_zeroes;
        logger.log("candidate new_name? ->" + new_name+"<-");
        return new_name;
    }


    //**********************************************************
    private static boolean destination_exists(Path path, Logger logger)
    //**********************************************************
    {
        File destination = path.toFile();
        if ( destination.isDirectory())
        {
            return destination.exists();
        }
        return check_file_really_exists(destination,logger);
    }

    //**********************************************************
    private static boolean names_differ_only_by_case(Old_and_new_Path oandn, Aborter aborter, Logger logger)
    //**********************************************************
    {
        String proposed_new_name_string = oandn.new_Path.getFileName().toString();

        if( proposed_new_name_string.toLowerCase().equals(oandn.old_Path.getFileName().toString().toLowerCase()))
        {
            return true;
        }
        return false;
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
        if (Files.exists(oandn.new_Path)) {
            File_with_a_few_bytes mf1 = new File_with_a_few_bytes(oandn.old_Path.toFile(), logger);
            File_with_a_few_bytes mf2 = new File_with_a_few_bytes(oandn.new_Path.toFile(), logger);

            // identical or not ?
            return File_with_a_few_bytes.files_have_same_content(mf1, mf2, aborter, logger);
        }
        if (moving_files_dbg) logger.log("new path does not exist, so not identical " + oandn.to_string());
        return false;
    }


    //**********************************************************
    private static Old_and_new_Path do_the_move_or_delete(Old_and_new_Path oandn, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        try {
            if (oandn.cmd == Command.command_delete_forever)
            {
                logger.log("delete for ever issued for : " + oandn.old_Path);
                Files.delete(oandn.old_Path);
                logger.log("delete for ever DONE for : " + oandn.old_Path);
                return move_success(oandn, logger);
            }

            {
                if (Static_files_and_paths_utilities.is_same_path(oandn.old_Path, oandn.new_Path, logger))
                {
                    logger.log("WARNING !!! do_the_move not performed : identical paths " + oandn.old_Path + "=>" + oandn.new_Path);
                    return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.same_path,false);
                }
                long start = System.currentTimeMillis();
                if (oandn.old_Path.toFile().isFile())
                {
                    logger.log("move FILE command issued : " + oandn.old_Path + "=>" + oandn.new_Path);
                    // preserves attributes by default:
                    FileUtils.moveFile(oandn.old_Path.toFile(), oandn.new_Path.toFile());
                }
                else
                {
                    logger.log("move FOLDER command issued : " + oandn.old_Path + "=>" + oandn.new_Path);
                    FileUtils.moveDirectory(oandn.old_Path.toFile(), oandn.new_Path.toFile());
                }

                if ( System.currentTimeMillis()-start > 5_000)
                {
                    if (Booleans.get_boolean(Feature.Play_ding_after_long_processes.name(),owner))
                    {
                        Ding.play("file moving takes more than 5s", logger);
                    }
                }
            }
            return move_success(oandn, logger);

        }
        catch (AccessDeniedException x)
        {
            logger.log("Move failed " + oandn.old_Path + " ACCESS DENIED exception ");
            return move_failed(oandn, x, owner, aborter,logger);
        }

        catch (FileNotFoundException x) {
            logger.log("Move failed " + oandn.old_Path + " FILE NOT FOUND exception, the source does not exists?"+oandn.old_Path);
            return move_failed( oandn, x, owner,aborter,logger);
        }
        catch (DirectoryNotEmptyException x)
        {
            if (oandn.old_Path.toFile().isDirectory())
            {
                // (on Macos for sure; other OS I dont know) when moving a directory across file systems
                // e.g. from main drive to external USB drive
                // DirectoryNotEmptyException is raised ....
                // hypothesis: Files.move(x) is implemented as first a copy (which succeeds!)
                // and then when a delete of the source folder is attempted... DirectoryNotEmptyException
                //
                logger.log("Folder move failed " + oandn.old_Path + " DIRECTORY NOT EMPTY exception\nThis may happen when moving a folder across filesystems: the origin is still there!");
                Popups.popup_warning( "Directory was COPIED", "..instead of moved because it was across 2 different filesystems", true, owner,logger);
                return move_failed( oandn, x, owner,aborter,logger);
            }

            logger.log(oandn.old_Path + " DIRECTORY NOT EMPTY, not allowed!");
            Popups.popup_Exception(x, 200, "DIRECTORY NOT EMPTY", owner, logger);
            return move_failed( oandn, x, owner,aborter,logger);
        }
        catch (IOException e)
        {
            logger.log("IO EXCEPTION "+oandn.old_Path + " " + e);
            if ( !oandn.old_Path.toFile().canWrite())
            {
                logger.log("File is not writeable: "+oandn.old_Path + " " + e);
                Popups.popup_warning("File is not writeable:"+oandn.old_Path, "This file cannot be moved because its file-system properties do not allow it", false, owner,logger);

            }
            return move_failed( oandn, e, owner,aborter,logger);
        }
    }


    //**********************************************************
    private static Old_and_new_Path move_failed(Old_and_new_Path oandn, IOException e0, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("******* move failed for: *********\n" +
                "->" + oandn.old_Path + "<-\n" +
                "==>\n" +
                "->" + oandn.new_Path + "<-\n" +
                "" + e0 + "\n Note this error may show when moving directories across file systems e.g. USB drive\n" +
                "***********************************");

        if (oandn.new_Path == null) {
            return new Old_and_new_Path(oandn.old_Path, null, oandn.cmd, Status.command_failed,false);
        }
        if (!Files.exists(oandn.new_Path.getParent()))
        {
            logger.log("FAILED to move file, target dir does not exists->" + oandn.new_Path.getParent() + "<-" + e0);
            Path path = oandn.new_Path.getParent();
            Non_booleans_properties.get_main_properties_manager(owner).remove(path.toAbsolutePath().toString());

            return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.target_dir_does_not_exist,false);
        } else {
            logger.log("destination folder exists but ... FAILED to move file for some other reason->" + oandn.old_Path.toAbsolutePath() +
                    "<- into ->" + oandn.new_Path.toAbsolutePath() + "<-\n" + e0);

            // ok so we try to COPY instead
            // for external drives (e.g. USB) it may make a difference for FOLDERS
            // that is: move works for individual files, but nt for folders...
            // for reasons that are a bit mysterious to me?
            try {
                if (oandn.old_Path.toFile().isFile()) {
                    Files.copy(oandn.old_Path, oandn.new_Path);

                } else {
                    //Static_files_and_paths_utilities.copy_dir(oandn.old_Path, oandn.new_Path,logger);
                    FileUtils.copyDirectory(oandn.old_Path.toFile(), oandn.new_Path.toFile());
                }


                String local_string =
                        My_I18n.get_I18n_string("We_tried_moving", owner,logger)
                                + oandn.old_Path.toAbsolutePath()
                                + My_I18n.get_I18n_string("Into", owner,logger)
                                + oandn.new_Path.toAbsolutePath()
                                + My_I18n.get_I18n_string("And_it_worked", owner,logger);
                if (moving_files_dbg) Popups.popup_warning( "Move success (dbg is on)", local_string, false, owner,logger);
                logger.log(local_string + "<-\n" + e0);
                return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, Command.command_copy, Status.copy_done,false);
            }
            catch (FileAlreadyExistsException ex) {
                logger.log("FATAL! we tried moving a file/dir and it failed, so we tried to copy instead and is ALSO failed!" + oandn.old_Path.toAbsolutePath() +
                        "<- into ->" + oandn.new_Path.toAbsolutePath() + "<-\n" + ex);
            }
            catch (IOException ex) {
                logger.log("FATAL! we tried moving a file/dir and it failed, so we tried to copy instead and is ALSO failed!" + oandn.old_Path.toAbsolutePath() +
                        "<- into ->" + oandn.new_Path.toAbsolutePath() + "<-\n" + ex);
            }

        }
        return new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.command_failed,false);

    }


    //**********************************************************
    private static Old_and_new_Path move_success(Old_and_new_Path oandn, Logger logger)
    //**********************************************************
    {
        if (moving_files_dbg) {
            String txt = "move_success() cmd:" + oandn.cmd + ":\nold:" + oandn.old_Path.toAbsolutePath();
            if (oandn.new_Path != null) {
                txt += "\nnew:" + oandn.new_Path.toAbsolutePath();
            } else {
                txt += "\nnew: null (delete forever)";
            }
            logger.log(txt);
        }
        if (oandn.run_after != null) oandn.run_after.run();

        return switch (oandn.cmd) {
            case command_move_to_trash ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.move_to_trash_done,false);
            case command_delete_forever ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.delete_forever_done,false);
            case command_edit ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.edition_requested,false);
            case command_move ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.move_done,false);
            case command_rename ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.rename_done,false);
            default ->
                    new Old_and_new_Path(oandn.old_Path, oandn.new_Path, oandn.cmd, Status.command_failed,false);
        };
    }

    //**********************************************************
    public static Path change_dir_name(Path old_path, String new_name,Window owner, Aborter aborter , Logger logger)
    //**********************************************************
    {
        logger.log("change_dir_name, new name: " + new_name);

        logger.log("trying rename: " + old_path.getFileName() + " => " + new_name);
        Path new_path = Paths.get(old_path.getParent().toString(), new_name);
        //Files.move(path, new_path);
        //FileUtils.moveDirectory(old_path.toFile(),new_path.toFile());
        if ( !move_file(old_path,new_path,logger))
        {
            return null;
        }
        logger.log("....done");
        Old_and_new_Path oan = new Old_and_new_Path(old_path,new_path, Command.command_rename, Status.rename_done,false);
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(oan);
        Undo_for_moves.add(l, owner,logger);
        return new_path;

    }
















































    //**********************************************************
    public static Path generate_new_candidate_name(Path old_path, String prefix, String postfix, Logger logger)
    //**********************************************************
    {
        String base_name = Extensions.get_base_name(old_path.getFileName().toString());
        String extension = Extensions.get_extension(old_path.getFileName().toString());
        String new_name = Extensions.add(prefix + base_name + postfix,extension);
        if (moving_files_dbg) logger.log("generate_new_candidate_name=" + new_name);
        return Paths.get(old_path.getParent().toString(), new_name);
    }
    //**********************************************************
    public static Path generate_new_candidate_name_special(Path old_path, String prefix, int index, Logger logger)
    //**********************************************************
    {
        String base_name = Extensions.get_base_name(old_path.getFileName().toString());
        String extension = Extensions.get_extension(old_path.getFileName().toString());

        {
            Path path = name_is_already_a_count2(old_path,prefix,base_name,extension,logger);
            if ( path != null)
            {
                return path;
            }

        }
        String new_name = Extensions.add(prefix + base_name + index , extension);
        //if (dbg)
            logger.log("generate_new_candidate_name_special=" + new_name);
        return Paths.get(old_path.getParent().toString(), new_name);

    }

    private static final Random random = new Random();
    //**********************************************************
    private static Path name_is_already_a_count2(Path path, String prefix, String base_name, String extension, Logger logger)
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
            String new_name = Extensions.add(prefix+base_name.substring(0, base_name.length() - lenght_of_trailing_numbers) + new_integer_with_leading_zeroes,extension);
            if (moving_files_dbg) logger.log("candidate new_name? ->" + new_name+"<-");
            Path local = Paths.get(path.getParent().toString(), new_name);
            if ( !local.toFile().exists())
            {
                logger.log("NEW NAME ->"+local.toAbsolutePath()+"<-");
                return local;
            }
        }
        return null;
    }


}
