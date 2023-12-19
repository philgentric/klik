package klik.images;

import javafx.application.Platform;
import klik.actor.Actor;
import klik.actor.Message;
import klik.change.Change_gang;

import java.nio.file.Path;
import java.util.Objects;

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

        if (change_image_message.image_stage == null)
        {
            if ( dbg) change_image_message.logger.log("FATAL change_image_message.image_stage == null");
            return "Failed change_image_message.image_stage == null";
        }
        if (change_image_message.image_stage.image_display_handler == null)
        {
            if ( dbg) change_image_message.logger.log("FATAL change_image_message.image_stage.image_display_handler == null");
            return "Failed change_image_message.image_stage.image_display_handler == null";
        }
        if (change_image_message.image_stage.image_display_handler.image_indexer == null)
        {
            if ( dbg) change_image_message.logger.log("warning: change_image_message.image_stage.image_display_handler.image_indexer  == null (probably in the making)");
            if (change_image_message.input_image_context.path == null)
            {
                if ( dbg) change_image_message.logger.log("change_image_message.input_image_context.path == null");
                change_image_message.image_stage.set_nothing_to_display(null);
                return "Failed change_image_message.image_stage.image_display_handler.image_indexer == null";
            }
            return display_target_path(change_image_message.input_image_context.path, change_image_message);

        }
        if (change_image_message.input_image_context == null)
        {
            if ( dbg) change_image_message.logger.log( "change_image_message.input_image_context == null");
            change_image_message.image_stage.set_nothing_to_display(null);
            return "Failed change_image_message.input_image_context == null";
        }
        if (change_image_message.input_image_context.previous_path == null)
        {
            //if ( dbg)
                change_image_message.logger.log("change_image_message.input_image_context.previous_path == null");
            change_image_message.image_stage.set_nothing_to_display(null);
            return "Failed change_image_message.input_image_context.previous_path == null";
        }

        if ( change_image_message.get_aborter().should_abort()) return "aborted";
        // the safe way is to get the index of the current image from its path
        // however, when renaming a sequence of image this is annoying
        // since when you press next, you are in the new name context...

        //cim.image_stage.show_wait_cursor();
       //if ( dbg) change_image_message.image_stage.logger.log("Change_image_actor current OLD path="+change_image_message.input_image_context.path);
        Path target_path = change_image_message.image_stage.image_display_handler.image_indexer.get_new_path_relative(
                change_image_message.input_image_context.previous_path,change_image_message.delta,change_image_message.ultimate);
        if ( dbg) change_image_message.image_stage.logger.log("Change_image_actor current NEW path="+target_path);
        if ( target_path == null)
        {
            if ( dbg) change_image_message.logger.log("Change_image_actor change_image_relative something really bad happened, like the whole dir was deleted behind the scene");
            change_image_message.image_stage.set_nothing_to_display(null);
            //cim.image_stage.restore_cursor();
            return "BAD";
        }
        if ( change_image_message.get_aborter().should_abort()) return "aborted";

        return display_target_path(target_path, change_image_message);


    }

    //**********************************************************
    private static String display_target_path( Path target_image_path, Change_image_message change_image_message)
    //**********************************************************
    {
        if ( dbg) change_image_message.logger.log("Change_image_actor change_image_relative target = "+ target_image_path);
        String skey = Image_context.get_full_path(target_image_path);
        Image_context iai = change_image_message.image_stage.image_display_handler.try_to_get_from_cache(skey);
        boolean forward = true;
        if ( change_image_message.delta < 0) forward = false;
        if (iai != null)
        {
            // image was found in cache
            change_image_message.output_image_context[0] = iai;
            if ( dbg) change_image_message.logger.log("\nChange_image_actor FOUND in CACHE: " + skey);
            Platform.runLater(() -> change_image_message.image_stage.set_image(change_image_message.output_image_context[0],false));
            //cim.image_stage.restore_cursor();
            change_image_message.image_stage.image_display_handler.preload(change_image_message.image_stage.image_display_handler, change_image_message.ultimate, forward, change_image_message.image_stage.image_display_handler.alternate_rescaler);
            return "found in cache";
        }
        if ( change_image_message.get_aborter().should_abort()) return "aborted";

        if ( dbg) change_image_message.logger.log("\n Change_image_actorNOT found in cache: " + skey);
        iai = change_image_message.image_stage.image_display_handler.local_getImage_context(target_image_path, change_image_message.aborter);
        if (iai == null)
        {
            if ( dbg) change_image_message.logger.log("Change_image_actor null image (1) in change_image_relative");
            Change_gang.report_anomaly(Objects.requireNonNull(change_image_message.input_image_context.path).getParent());
            //cim.image_stage.restore_cursor();
            return "Failed";
        }

        change_image_message.image_stage.image_display_handler.save_in_cache(skey,iai);
        change_image_message.output_image_context[0] = iai;

        if (Objects.requireNonNull(change_image_message.image_stage.mouse_handling_for_image_stage).something_is_wrong_with_image_size())
        {
            if ( dbg) change_image_message.logger.log("Change_image_actor something_is_wrong_with_image_size in change_image_relative");
            //image_stage.restore_cursor();
            //return;
        }
        else
        {
            if ( dbg) change_image_message.logger.log("Change_image_actor change_image_relative OK! index is:" + target_image_path + " for file:" + Objects.requireNonNull(change_image_message.input_image_context.path).getFileName());
        }

        change_image_message.image_stage.set_image(change_image_message.output_image_context[0],false);
        //cim.image_stage.restore_cursor();

        change_image_message.image_stage.image_display_handler.preload(change_image_message.image_stage.image_display_handler, change_image_message.ultimate, forward, change_image_message.image_stage.image_display_handler.alternate_rescaler);
        return "OK";
    }


}
