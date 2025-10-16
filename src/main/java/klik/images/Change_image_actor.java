package klik.images;

import klik.actor.Actor;
import klik.actor.Message;
import klik.change.Change_gang;
import klik.image_indexer.Image_indexer;
import klik.util.ui.Jfx_batch_injector;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

//**********************************************************
public class Change_image_actor implements Actor
//**********************************************************
{
    private static final boolean dbg = false;
    private static Change_image_actor instance;

    //**********************************************************
    public static Actor get_instance()
    //**********************************************************
    {
        if ( instance == null) instance = new Change_image_actor();
        return instance;
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Change_image_message change_image_message = (Change_image_message) m;

        if ( dbg) change_image_message.logger.log("delta=" + change_image_message.delta);

        if (change_image_message.image_window == null)
        {
            if ( dbg) change_image_message.logger.log("FATAL change_image_message.image_stage == null");
            return "Failed change_image_message.image_stage == null";
        }
        if (change_image_message.image_window.image_display_handler == null)
        {
            if ( dbg) change_image_message.logger.log("FATAL change_image_message.image_stage.image_display_handler == null");
            return "Failed change_image_message.image_stage.image_display_handler == null";
        }
        if (change_image_message.image_window.image_display_handler.image_indexer == null)
        {
            if ( dbg) change_image_message.logger.log("warning: change_image_message.image_stage.image_display_handler.image_indexer  == null (probably in the making)");
            if (change_image_message.input_image_context.path == null)
            {
                if ( dbg) change_image_message.logger.log("change_image_message.input_image_context.path == null");
                change_image_message.image_window.set_nothing_to_display(null);
                return "Failed change_image_message.image_stage.image_display_handler.image_indexer == null";
            }
            return display_target_path(change_image_message.input_image_context.path, change_image_message);

        }
        if (change_image_message.input_image_context == null)
        {
            if ( dbg) change_image_message.logger.log( "change_image_message.input_image_context == null");
            change_image_message.image_window.set_nothing_to_display(null);
            return "Failed change_image_message.input_image_context == null";
        }
        if (change_image_message.input_image_context.previous_path == null)
        {
            if ( dbg) change_image_message.logger.log("change_image_message.input_image_context.previous_path == null");
            change_image_message.image_window.set_nothing_to_display(null);
            return "Failed change_image_message.input_image_context.previous_path == null";
        }

        if ( change_image_message.get_aborter().should_abort())
        {
            change_image_message.logger.log("Change_image_actor aborted from: " +change_image_message.get_aborter().name);
            return "aborted";
        }
        // the safe way is to get the index of the current image from its path
        // however, when renaming a sequence of image this is annoying
        // since when you press next, you are in the new name context...

        Path target_path;
        if ( change_image_message.delta != 0)
        {
            // wait if needed for the image_indexer to be there
            for (;;)
            {
                if (change_image_message.image_window.image_display_handler.image_indexer.isPresent()) break;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            Image_indexer x = change_image_message.image_window.image_display_handler.image_indexer.get();
            target_path = x.get_new_path_relative(
                    change_image_message.input_image_context.previous_path, change_image_message.delta, change_image_message.ultimate);
        }
        else
        {
            target_path = change_image_message.input_image_context.path;
        }

        if ( dbg) change_image_message.image_window.logger.log("Change_image_actor target_path="+target_path);
        if ( target_path == null)
        {
            if ( dbg) change_image_message.logger.log("Change_image_actor change_image_relative something really bad happened, like the whole dir was deleted behind the scene");
            change_image_message.image_window.set_nothing_to_display(null);
            //cim.image_stage.restore_cursor();
            return "BAD";
        }
        if ( change_image_message.get_aborter().should_abort()) return "aborted";

        String returned =  display_target_path(target_path, change_image_message);

        return returned;
    }

    //**********************************************************
    private static String display_target_path(Path target_image_path, Change_image_message change_image_message)
    //**********************************************************
    {
        if ( dbg) change_image_message.logger.log("Change_image_actor change_image_relative target = "+ target_image_path);
        String full_path = Image_context.get_full_path(target_image_path);
        Image_context image_context = change_image_message.image_window.image_display_handler.image_cache.get(full_path);

        boolean forward = true;
        if ( change_image_message.delta < 0) forward = false;
        if (image_context != null)
        {
            change_image_message.logger.log("\nimage FOUND in cache");
            // image was found in cache
            change_image_message.output_image_context[0] = image_context;
            if ( dbg) change_image_message.logger.log("\nChange_image_actor FOUND in CACHE: " + full_path);
            Jfx_batch_injector.inject(() -> change_image_message.image_window.set_image_internal(change_image_message.output_image_context[0]), change_image_message.logger);
            //cim.image_stage.restore_cursor();
            change_image_message.image_window.image_display_handler.preload( change_image_message.ultimate, forward);
            return "found in cache";
        }
        if ( change_image_message.get_aborter().should_abort()) return "aborted";

        change_image_message.logger.log("\nimage NOT found in cache: " + full_path);
        Optional<Image_context> option = Image_context.build_Image_context(
                    target_image_path,
                    change_image_message.image_window,
                    change_image_message.aborter,
                    change_image_message.logger);

        if (option.isEmpty())
        {
            if ( dbg) change_image_message.logger.log("Change_image_actor null image (1) in change_image_relative");
            Change_gang.report_anomaly(Objects.requireNonNull(change_image_message.input_image_context.path).getParent(), change_image_message.image_window.stage);
            //cim.image_stage.restore_cursor();
            return "Failed";
        }
        image_context = option.get();
        change_image_message.image_window.image_display_handler.save_in_cache(full_path,option.get());
        change_image_message.output_image_context[0] = option.get();

        if (change_image_message.image_window.mouse_handling_for_image_window.something_is_wrong_with_image_size())
        {
            if ( dbg) change_image_message.logger.log("Change_image_actor something_is_wrong_with_image_size in change_image_relative");
            //image_stage.restore_cursor();
            //return;
        }
        else
        {
            if ( dbg)
            {
                change_image_message.logger.log(
                        "Change_image_actor change_image_relative OK! target_image_path is:" + target_image_path
                                +" image_context.path :"+image_context.path
                                + " for file:" + change_image_message.input_image_context.path.getFileName());
            }
            change_image_message.image_window.set_image_internal(image_context);
        }

        //cim.image_stage.restore_cursor();

        change_image_message.image_window.image_display_handler.preload(change_image_message.ultimate, forward);
        return "OK";
    }


}
