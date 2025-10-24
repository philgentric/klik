package klik.util.animated_gifs;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Shared_services;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.actor.Job_termination_reporter;
import klik.change.Change_gang;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.properties.Cache_folder;
import klik.util.files_and_paths.*;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.image.Icons_from_disk;
import klik.util.image.icon_cache.Icon_caching;
import klik.util.log.Logger;
import klik.util.ui.Folder_chooser;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Popups;
import klik.util.ui.Progress_window;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Animated_gifs_from_video
//**********************************************************
{
    private static Stage the_stage;
    private static ImageView the_imageview;

    public static final int Mini_console_width = 1000;
    public static final int Mini_console_height = 200;

    public static TextField tf_start;
    public static TextField tf_duration;
    static double start_time_seconds;
    static double duration_seconds;
    static Path temporary_gif_full_path;
    static File gif_saving_dir = null;
    static Path icon_cache_dir = null;
    static Path video_path;
    static Logger logger;
    static final int[] HUNDRED = {100};

    //**********************************************************
    public static void generate_many_gifs(Window owner, Path video_path, int clip_lenght, int skip_to_next, Logger logger)
    //**********************************************************
    {
        Double duration_in_seconds = Ffmpeg_utils.get_media_duration(video_path,owner, logger);
        if ( duration_in_seconds == null)
        {
            logger.log("❌ FATAL: ffprobe cannot find duration of "+video_path);
            return;
        }
        if ( duration_in_seconds > 3*3600)
        {
            logger.log("WARNING: ffprobe reports duration that looks wrong?"+duration_in_seconds+" in hours="+duration_in_seconds/3600+ "... going to assume 30 minutes");
            duration_in_seconds = Double.valueOf(1800.0); // assume half an hour ...
        }
        String folder_name = video_path.getFileName().toString()+"_anim";
        File dir = new File(video_path.getParent().toFile(),folder_name);
        if ( !dir.exists())
        {
            if (!dir.mkdir())
            {
                logger.log("WARNING: creating dir failed for "+dir.getAbsolutePath());
                return;
            }
            List<Old_and_new_Path> c = new ArrayList<>();
            c.add(new Old_and_new_Path(null,dir.toPath(), Command.command_move, Status.before_command,false));
            Change_gang.report_changes(c, owner);
        }
        AtomicBoolean abort_reported = new AtomicBoolean(false);
        Animated_gif_generation_actor actor = new Animated_gif_generation_actor(logger);
        AtomicInteger in_flight = new AtomicInteger(0);
        double x = owner.getX()+100;
        double y = owner.getY()+100;
        Progress_window progress_window = Progress_window.show(
                in_flight,
                "Wait for animated gifs to be generated",
                20*60,
                x,
                y,
                owner,
                logger);
        for ( int start = 0 ; start < duration_in_seconds; start+=skip_to_next)
        {
            if (progress_window.aborter.should_abort())
            {
                Jfx_batch_injector.inject(() -> Popups.popup_warning("❗ ABORTING MASSIVE GIF GENERATION for "+video_path, "On abort request",true,owner,logger), logger);
                return;
            }
            String name = video_path.getFileName().toString()+"_part_"+String.format(Ffmpeg_utils.us_locale,"%07d",(Integer)start)+".gif";
            Path destination_gif_full_path = Path.of(dir.getAbsolutePath(),name);

            Job_termination_reporter tr = (message, job) -> in_flight.decrementAndGet();
            in_flight.incrementAndGet();
            Actor_engine.run(actor,
                    new Animated_gif_generation_message(owner,video_path,512,50,destination_gif_full_path,clip_lenght,start,progress_window.aborter,abort_reported,logger),
                    tr,
                    logger);
        }

        //running_film.report_progress_and_close_when_finished(in_flight);
    }

    //**********************************************************
    public static void interactive(Path video_path_, Window owner, Logger logger_)
    //**********************************************************
    {
        video_path = video_path_;
        logger = logger_;
        start_time_seconds = 0;
        duration_seconds =  5;
        final int[] icon_height = {256};
        final int[] fps = {50};

        Platform.runLater(() -> {
            the_stage = new Stage();
            Look_and_feel look_and_feel = Look_and_feel_manager.get_instance(the_stage,logger);
            HUNDRED[0] = (int)look_and_feel.estimate_text_width("Start time in s");

            icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir( Cache_folder.klik_icon_cache,owner,logger);
            the_stage.setTitle("Animated gif maker for :"+video_path.getFileName().toString());
            the_stage.setMinWidth(Mini_console_width);
            the_stage.setMinHeight(Mini_console_height);
            the_imageview = new ImageView();
            the_imageview.setPreserveRatio(true);
            the_imageview.setFitHeight(icon_height[0]);
            Double full_clip_duration_in_seconds = Ffmpeg_utils.get_media_duration(video_path, the_stage, logger);

            if ( full_clip_duration_in_seconds == null)
            {
                logger.log("❌ FATAL: ffprobe cannot find duration of "+video_path);
                return;
            }
            make_animated_gif_in_tmp_folder(icon_height[0],fps[0], owner);//start_time_seconds,duration_seconds, video_path, logger, icon_cache_dir);
            Pane vb = new VBox();
            Look_and_feel_manager.set_region_look(vb,owner,logger);
            {
                HBox hb =  new HBox();
                {
                    VBox vbb = new VBox();
                    vbb.getChildren().add(new Label("GIF height (pixel)"));
                    TextField icon_height_tf = new TextField(""+icon_height[0]);
                    vbb.getChildren().add(icon_height_tf);
                    icon_height_tf.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            icon_height[0] = Integer.valueOf(icon_height_tf.getText());
                            the_imageview.setFitHeight(icon_height[0]);
                            make_animated_gif_in_tmp_folder(icon_height[0],fps[0], owner);

                        }
                    });
                    vbb.getChildren().add(new Label("Frame rate (per second)"));
                    TextField fps_tf = new TextField(""+fps[0]);
                    vbb.getChildren().add(fps_tf);
                    fps_tf.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            fps[0] = Integer.valueOf(fps_tf.getText());
                            //logger.log("fps="+fps[0]);
                            make_animated_gif_in_tmp_folder(icon_height[0],fps[0], owner);
                        }
                    });
                    hb.getChildren().add(vbb);
                }
                {
                    Region spacer = new Region();
                    Look_and_feel_manager.set_region_look(spacer,owner,logger);
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    hb.getChildren().add(spacer);
                }
                hb.getChildren().add(the_imageview);
                vb.getChildren().add(hb);
                {
                    Region spacer = new Region();
                    Look_and_feel_manager.set_region_look(spacer,owner,logger);
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    hb.getChildren().add(spacer);
                }
            }
            Button save = new Button("Choose folder & save");
            vb.getChildren().add(save);
            Button save_same = new Button("Save in ...");
            save_same.setDisable(true);
            vb.getChildren().add(save_same);

            save.setOnAction(actionEvent ->
            {
                if ( gif_saving_dir == null) gif_saving_dir = new File(System.getProperty("user.home"));
                gif_saving_dir = Folder_chooser.show_dialog_for_folder_selection("Choose folder to save animated gifs", gif_saving_dir.toPath(), the_stage, logger).toFile();
                if ( gif_saving_dir == null) return;
                save_same.setDisable(false);
                save_same.setText("Save in "+gif_saving_dir.getAbsolutePath());
                save_now(icon_height[0],fps[0], the_stage, logger);
            });

            save_same.setOnAction(actionEvent -> {
                if (gif_saving_dir== null) return;
                save_now(icon_height[0],fps[0], owner, logger);
            });

            {
                {
                    HBox hb = new HBox();
                    Label label = new Label("Start time: ");
                    label.setPrefWidth(HUNDRED[0]);
                    label.setMinWidth(HUNDRED[0]);
                    label.setMaxWidth(HUNDRED[0]);
                    hb.getChildren().add(label);
                    tf_start = new TextField(String.valueOf(start_time_seconds));
                    tf_start.setPrefWidth(3 * HUNDRED[0]);
                    tf_start.setMinWidth(3 * HUNDRED[0]);
                    tf_start.setMaxWidth(3 * HUNDRED[0]);
                    EventHandler<ActionEvent> start_change = actionEvent -> {
                        start_time_seconds = Double.parseDouble(tf_start.getText());
                        logger.log(" START  =" + start_time_seconds);
                        make_animated_gif_in_tmp_folder(icon_height[0],fps[0], owner);//start_time_seconds,duration_seconds,video_path, logger, icon_cache_dir);

                    };
                    tf_start.setOnAction(start_change);
                    hb.getChildren().add(tf_start);
                    {
                        Label label2 = new Label("Total time: " + full_clip_duration_in_seconds);
                        label2.setPrefWidth(HUNDRED[0]);
                        label2.setMinWidth(HUNDRED[0]);
                        label2.setMaxWidth(HUNDRED[0]);
                        hb.getChildren().add(label2);
                    }
                    vb.getChildren().add(hb);
                    Button jump =  new Button("Jump to next (add current duration)");
                    hb.getChildren().add(jump);
                    jump.setOnAction(actionEvent -> {
                        change_start_time(start_time_seconds+duration_seconds);
                        make_animated_gif_in_tmp_folder(icon_height[0],fps[0], owner);
                    });
                }




                {
                    HBox hb = new HBox();
                    double[] values ={0.1,0.5,1,5,10,30,60,180};
                    for ( double val : values) add_change_start_time_button(val,  hb,icon_height,fps, owner,logger);
                    vb.getChildren().add(hb);
                }

                {
                    HBox hb = new HBox();
                    double[] values ={-0.1,-0.5,-1,-5,-10,-30,-60,-180};
                    for ( double val : values) add_change_start_time_button(val, hb,icon_height,fps,owner,logger);
                    vb.getChildren().add(hb);
                }
            }

            {
                HBox hb_dur = new HBox();
                Label label = new Label("Duration");
                label.setPrefWidth(HUNDRED[0]);
                label.setMinWidth(HUNDRED[0]);
                label.setMaxWidth(HUNDRED[0]);
                hb_dur.getChildren().add(label);
                tf_duration = new TextField(String.valueOf(duration_seconds));

                EventHandler<ActionEvent> duration_change = actionEvent -> {
                    duration_seconds = Double.parseDouble(tf_duration.getText());
                    logger.log(" DURATION  ="+duration_seconds);
                    make_animated_gif_in_tmp_folder(icon_height[0],fps[0], owner);//start_time_seconds,duration_seconds,video_path, logger, icon_cache_dir);

                };
                tf_duration.setOnAction(duration_change);
                tf_duration.setPrefWidth(3*HUNDRED[0]);
                tf_duration.setMinWidth(3*HUNDRED[0]);
                tf_duration.setMaxWidth(3*HUNDRED[0]);
                hb_dur.getChildren().add(tf_duration);
                vb.getChildren().add(hb_dur);

                {
                    HBox hb = new HBox();
                    double[] values ={0.1,0.5,1,5,10,30,60,180};
                    for ( double val : values) add_change_duration_button(val,hb, icon_height,fps,owner,logger);
                    vb.getChildren().add(hb);
                }

                {
                    HBox hb = new HBox();
                    double[] values ={-0.1,-0.5,-1,-5,-10,-30,-60,-180};
                    for ( double val : values) add_change_duration_button(val, hb, icon_height,fps,owner,logger);
                    vb.getChildren().add(hb);
                }
            }

            Scene the_scene = new Scene(vb);
            the_stage.setScene(the_scene);
            the_stage.setX(0);
            the_stage.setY(0);
            the_stage.sizeToScene();
            the_stage.show();
        });

    }

    //**********************************************************
    private static void save_now(int icon_height, int fps, Window owner,Logger logger)
    //**********************************************************
    {
        // if the user already saved, the file has been moved to the target folder
        // so we need to re-generate (use case is: user saved, changed her mind, erased the result, wants to redo it)
        if ( !temporary_gif_full_path.toFile().exists())
        {
            make_animated_gif_in_tmp_folder(icon_height, fps, owner);
        }
        String new_name = temporary_gif_full_path.getFileName().toString();

        //if (new_name.length() > 24) new_name = new_name.substring(new_name.length() - 12);
        Path new_path = Path.of(gif_saving_dir.getAbsolutePath(), new_name);
        Old_and_new_Path oandnp = new Old_and_new_Path(temporary_gif_full_path, new_path, Command.command_move, Status.before_command,false);
        List<Old_and_new_Path> ll = new ArrayList<>();
        ll.add(oandnp);
        Moving_files.perform_safe_moves_in_a_thread(ll,false, 100,100, the_stage, Shared_services.aborter(), logger);
    }

    //**********************************************************
    private static void change_start_time(double new_val)
    //**********************************************************
    {
        start_time_seconds = new_val;
        tf_start.setText(String.valueOf(start_time_seconds));
        logger.log(" START  =" + start_time_seconds);
    }

    //**********************************************************
    private static void change_duration(double new_val)
    //**********************************************************
    {
        duration_seconds = new_val;
        tf_duration.setText(String.valueOf(duration_seconds));
        logger.log(" DURATION  =" + duration_seconds);
    }



    //**********************************************************
    private static void add_change_start_time_button(double amount, HBox hb,int height[], int fps[], Window owner,Logger logger)
    //**********************************************************
    {
        String d = amount+" s";
        if( amount > 0) d = " + "+d;
        Button button = new Button(d);
        button.setPrefWidth(HUNDRED[0]);
        button.setMinWidth(HUNDRED[0]);
        button.setMaxWidth(HUNDRED[0]);
        EventHandler<ActionEvent> plus_action = actionEvent -> {
            change_start_time(start_time_seconds+amount);
            make_animated_gif_in_tmp_folder( height[0], fps[0], owner);//start_time_seconds, duration_seconds, path, logger, icon_cache_dir);
        };
        button.setOnAction(plus_action);
        hb.getChildren().add(button);
    }
    //**********************************************************
    private static void add_change_duration_button(double amount, HBox hb, int height[], int fps[], Window owner, Logger logger)
    //**********************************************************
    {
        String d = amount+" s";
        if( amount > 0) d = " + "+d;
        Button button = new Button(d);
        button.setPrefWidth(HUNDRED[0]);
        button.setMinWidth(HUNDRED[0]);
        button.setMaxWidth(HUNDRED[0]);
        EventHandler<ActionEvent> plus_action = actionEvent -> {
            change_duration(duration_seconds+amount);
            make_animated_gif_in_tmp_folder(height[0],fps[0], owner);//start_time_seconds, duration_seconds, path, logger, icon_cache_dir);
        };
        button.setOnAction(plus_action);
        hb.getChildren().add(button);
    }

    //**********************************************************
    private static void make_animated_gif_in_tmp_folder(int height, int fps, Window owner)
    //**********************************************************
    {
        logger.log("make_animated_gif_in_tmp_folder, video path="+ video_path);

        Path temporary_gif_full_path = Icon_caching.path_for_icon_caching( video_path, String.valueOf(height), Icon_caching.gif_extension, owner,logger);
        logger.log("make_animated_gif_in_tmp_folder, icon_file="+temporary_gif_full_path.toAbsolutePath());

        Ffmpeg_utils.video_to_gif(
                video_path,
                height,
                fps,
                temporary_gif_full_path,
                duration_seconds,
                start_time_seconds,
                0,
                Shared_services.aborter(),
                the_stage,
                logger);


        Image image = Icons_from_disk.load_icon_from_disk_cache(video_path, height, String.valueOf(height),Icon_caching.gif_extension, Icons_from_disk.dbg, owner,logger);

        if ( image == null) logger.log("image==null");
        else the_imageview.setImage(image);
    }


}
