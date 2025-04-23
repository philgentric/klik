package klik.browser.icons.animated_gifs;

import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.properties.Booleans;
import klik.properties.Non_booleans;
import klik.util.files_and_paths.Moving_files;
import klik.images.Image_context;
import klik.util.log.Stack_trace_getter;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;
import klik.util.ui.Popups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static klik.browser.icons.animated_gifs.Animated_gif_from_folder.warning_GraphicsMagick;

/*
animated-GIF repairing utility: recover the frames and re-assemble them in a "more standard" format
often works on animated-gifs that are behaving strangely like playing to fats or too slow
because the format is weird/wrong
 */
// TODO: consider using Gifsicle?

//**********************************************************
public class Gif_repair
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean cleanup = false;

    //**********************************************************
    public static Path extract_all_frames_in_animated_gif(Stage owner, Image_context image_context,
                                                          String uuid,
                                                          Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path target = image_context.path;
        Path this_dir = target.getParent();

        Path tmp_dir = Non_booleans.get_trash_dir(this_dir,logger);
        if ( tmp_dir == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Weird! could not use tmp directory:"));
            return null;
        }

        Path old_path_for_restore = target;

        // move with a unique name into the trash folder
        Path new_path = Path.of(tmp_dir.toAbsolutePath().toString(),uuid+"_"+target.getFileName().toString());
        Moving_files.safe_move_a_file_or_dir_NOT_in_a_thread(owner, new_path, target.toFile(), aborter, logger);


        List<String> graphicsMagick_command_line = new ArrayList<>();
        // user GraphicsMagick to extract all gif frames
        graphicsMagick_command_line.add("gm");
        graphicsMagick_command_line.add("convert");
        graphicsMagick_command_line.add(new_path.toAbsolutePath().toString());//+"[0--1]");
        //l.add("-scene");
        //l.add("1");
        graphicsMagick_command_line.add("+adjoin");
        graphicsMagick_command_line.add(uuid+"_frame_%03d.gif");
        StringBuilder sb = null;
        if ( dbg) sb = new StringBuilder();
        if ( !Execute_command.execute_command_list(graphicsMagick_command_line, tmp_dir.toFile(), 2000, sb, logger))
        {

            Booleans.manage_show_GraphicsMagick_install_warning(owner,logger);

            Popups.popup_warning(owner, "Repair part1 command failed:", warning_GraphicsMagick,false,logger);
            // and restore the file
            Moving_files.safe_move_a_file_or_dir_NOT_in_a_thread(owner, old_path_for_restore,new_path.toFile(), aborter, logger);
            return null;
        }
        if ( dbg) logger.log(sb.toString());
        return tmp_dir;
    }

    //**********************************************************
    // new_delay is inter-frame delay in hundredths of second : 10 means 10fps, 1 means 100 fps, 50 means 2fps
    public static Path reassemble_all_frames(double new_delay, Stage owner, Image_context image_context, Path tmp_dir, Path final_dest, String uuid, Logger logger)
    //**********************************************************
    {
        Path target = image_context.path;

        {
            List<String> graphicsMagick_command_line = new ArrayList<>();
            graphicsMagick_command_line.add("gm");
            graphicsMagick_command_line.add("convert");
            graphicsMagick_command_line.add("-delay");
            graphicsMagick_command_line.add(""+new_delay);
            // -delay is in hundredths of second : 10 means 10fps, 1 means 100 fps, 50 means 2fps
            graphicsMagick_command_line.add(uuid+"_frame_*?.gif");
            graphicsMagick_command_line.add(final_dest.toAbsolutePath().toString());
            StringBuilder sb = null;
            if ( dbg) sb = new StringBuilder();
            if ( ! Execute_command.execute_command_list(graphicsMagick_command_line, tmp_dir.toFile(), 2000, sb,logger))
            {
                Booleans.manage_show_GraphicsMagick_install_warning(owner,logger);
                logger.log(warning_GraphicsMagick);
                return null;
            }
            if ( dbg) logger.log(sb.toString());
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
                Booleans.manage_show_GraphicsMagick_install_warning(owner,logger);

                logger.log(warning_GraphicsMagick);
                return null;
            }

        }
        return target;
    }


}
