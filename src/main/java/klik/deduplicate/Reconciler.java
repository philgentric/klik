package klik.deduplicate;

import klik.actor.Aborter;
import klik.deduplicate.console.Deduplication_console_interface;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.My_File;
import klik.util.Logger;
import klik.util.System_out_logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Reconciler
{
    /*
    GIVEN A REFERENCE FOLDER/tree and a SEARCH folder/tree
    this looks for "missing" files in the SEARCH folder i.e. files that are NOT in the reference
     */
    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        Logger logger = new System_out_logger();

        String target = "/Volumes/TOSHIBA4T/";
        String reference =    "/Volumes/TOSHIBA4T/";
        Deduplication_console_interface pinger = new Deduplication_console_interface(null,logger);

        List<My_File> reference_files = Files_and_Paths.get_all_files_down(new File(reference), pinger, true, logger);
        List<My_File> target_files = Files_and_Paths.get_all_files_down(new File(target), pinger, true, logger);

        Aborter stop = new Aborter();

        //List<Old_and_new_Path> l = new ArrayList<>();
        List<File> missing = new ArrayList<>();
        for (My_File t : target_files)
        {
            boolean found = false;
            for (My_File r : reference_files)
            {
                if (My_File.files_have_same_content(r, t, stop, logger))
                {
                    // found !
                    found = true;
                    /*
                    String ref = r.f.getAbsolutePath().substring(reference.length());
                    String tar = t.f.getAbsolutePath().substring(target.length());
                    logger.log("          found MATCHING file : "+tar+ " as reference: "+ref);

                    Path old  = t.f.toPath();
                    Path new_p = new File(poubelle,t.f.getName()).toPath();
                    l.add(new Old_and_new_Path(old,new_p, false, Command_old_and_new_Path.command_unknown, Status_old_and_new_Path.before_command));

                    Files_and_Paths.perform_safe_moves_in_a_thread(l,logger,false);
                    */

                    break;
                }
            }
            if ( !found)
            {
                missing.add(t.file);
                logger.log(missing.size()+" total missing, new missing file : "+t.file.getAbsolutePath());
            }
        }

        logger.log("\n done ! "+missing.size()+" total missing");

    }
}
