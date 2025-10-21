package klik.util.animated_gifs;

import javafx.stage.Stage;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.properties.Non_booleans_properties;
import klik.properties.boolean_features.Booleans;
import klik.util.files_and_paths.Moving_files;
import klik.images.Image_context;
import klik.util.log.Stack_trace_getter;
import klik.util.execute.Execute_command;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


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
    public static Path extract_all_frames_in_animated_gif(Image_context image_context,
                                                          String uuid,
                                                          Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Path target = image_context.path;
        Path this_dir = target.getParent();

        Path tmp_dir = Non_booleans_properties.get_trash_dir(this_dir,owner,logger);
        if ( tmp_dir == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Weird! could not use tmp directory:"));
            return null;
        }

        Path old_path_for_restore = target;

        // move with a unique name into the trash folder
        Path new_path = Path.of(tmp_dir.toAbsolutePath().toString(),uuid+"_"+target.getFileName().toString());
        Moving_files.safe_move_a_file_or_dir_NOT_in_a_thread(new_path, target.toFile(), 100,100, owner,aborter, logger);


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
        if ( Execute_command.execute_command_list(graphicsMagick_command_line, tmp_dir.toFile(), 2000, sb, logger) ==null)
        {
            Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
            // and restore the file
            Moving_files.safe_move_a_file_or_dir_NOT_in_a_thread(old_path_for_restore,new_path.toFile(), 100,100,owner,aborter, logger);
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
            if ( Execute_command.execute_command_list(graphicsMagick_command_line, tmp_dir.toFile(), 2000, sb,logger) == null)
            {
                Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
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
            if ( Execute_command.execute_command_list(l, tmp_dir.toFile(), 2000, null, logger) == null)
            {
                Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
                return null;
            }

        }
        return target;
    }


}
