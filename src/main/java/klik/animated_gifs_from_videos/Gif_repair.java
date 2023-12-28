package klik.animated_gifs_from_videos;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.files_and_paths.Moving_files;
import klik.images.Image_context;
import klik.properties.Static_application_properties;
import klik.util.Execute_command;
import klik.util.Logger;
import klik.util.Popups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static klik.animated_gifs_from_videos.Animated_gif_from_folder.warning_IMAGEMAGIC;

/*
animated-GIF repairing utility: recover the frames and re-assemble them in a "more standard" format
often works on animated-gifs that are behaving strangely like playing to fats or too slow
because the format is weird/wrong
 */
// TODO: consider using Gifsicle?

public class Gif_repair
{
    private static final boolean cleanup = true;

    //**********************************************************
    public static Path extract_all_frames_in_animated_gif(Stage owner, Image_context image_context, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path target = image_context.path;
        Path this_dir = target.getParent();
        Path new_dir = Path.of(this_dir.toString(), "tmp_" + target.getFileName().toString());
        Path tmp_dir = null;
        for(int safe = 0; safe < 3; safe++) {
            try {
                tmp_dir = Files.createDirectory(new_dir);
            } catch (IOException e) {
                new_dir = Path.of(this_dir.toString(), "tmp_"+UUID.randomUUID()+"_"+ target.getFileName().toString());
                continue;
            }
            break;
        }
        if ( tmp_dir == null)
        {
            logger.log("Weird! could not create directory:"+new_dir);
            return null;
        }

        Moving_files.safe_move_a_file_or_dir(owner, tmp_dir, target.toFile(), aborter, logger);

        List<String> l = new ArrayList<>();
        // convert XXX -scene 1 +adjoin frame_%03d.gif
        l.add("convert");
        l.add(image_context.path.getFileName().toString());
        l.add("-scene");
        l.add("1");
        l.add("+adjoin");
        l.add("frame_%03d.gif");
        if ( !Execute_command.execute_command_list(l, tmp_dir.toFile(), 2000, null, logger))
        {

            Static_application_properties.manage_show_imagemagick_install_warning(owner,logger);

            Popups.popup_warning(owner, "Repair part1 command failed:",warning_IMAGEMAGIC,false,logger);
            // and restore the file !
            Path to_be_restored = Path.of(new_dir.toString(),target.getFileName().toString());
            Moving_files.safe_move_a_file_or_dir(owner, this_dir, to_be_restored.toFile(), aborter, logger);

            if (!remove_tmp_dir(this_dir, tmp_dir.toFile().toString(),logger) )
            {
                logger.log("warming: tmp directory not removed: "+tmp_dir);
            }
            return null;
        }

        return tmp_dir;
    }

    //**********************************************************
    private static boolean remove_tmp_dir(Path this_dir, String target_tmp_dir, Logger logger)
    //**********************************************************
    {
        List<String> l2 = new ArrayList<>();
        // rm XXX
        l2.add("rm");
        l2.add("-R");
        l2.add(target_tmp_dir);
        if (!Execute_command.execute_command_list(l2, this_dir.toFile(), 2000, null,logger))
        {
            return false;
        }
        logger.log("remove_tmp_dir: ");
        return true;
    }

    //**********************************************************
    public static Path reassemble_all_frames(Stage owner, Image_context image_context, Path tmp_dir, Logger logger)
    //**********************************************************
    {
        Path target = image_context.path;
        Path this_dir = target.getParent();
/*
        if (perform_rm)
        {
            List<String> l = new ArrayList<>();
            // remove the (supposedly) damaged file
            Files_and_Paths.move_to_trash(image_context.path,null,logger);
        }
*/
        {
            List<String> l = new ArrayList<>();
            // convert frame_0??.gif rebuilt.gif
            l.add("convert");
            l.add("frame_0??.gif");
            l.add("../" + image_context.path.getFileName().toString());
            if ( ! Execute_command.execute_command_list(l, tmp_dir.toFile(), 2000, null,logger))
            {
                Static_application_properties.manage_show_imagemagick_install_warning(owner,logger);

                logger.log(warning_IMAGEMAGIC);
                return null;
            }
        }
        if (cleanup)
        {
            List<String> l = new ArrayList<>();
            // rm frame_0*
            l.add("rm");
            l.add("frame_0*.gif");
            l.add(image_context.path.getFileName().toString());
            if ( ! Execute_command.execute_command_list(l, tmp_dir.toFile(), 2000, null, logger))
            {
                Static_application_properties.manage_show_imagemagick_install_warning(owner,logger);

                logger.log(warning_IMAGEMAGIC);
                return null;
            }

            remove_tmp_dir(this_dir, tmp_dir.toFile().toString(), logger);
        }
        return target;
    }


}
