package klik.animated_gifs_from_videos;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.virtual_threads.Concurency_limiter;
import klik.change.Change_gang;
import klik.files_and_paths.*;
import klik.browser.icons.Icon_factory_actor;
import klik.properties.Static_application_properties;
import klik.util.Execute_command;
import klik.util.From_disk;
import klik.util.Logger;
import klik.util.Popups;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Animated_gif_generator
//**********************************************************
{
    private static final int HUNDRED =100;

    //**********************************************************
    public static void generate_many_gifs(Stage owner, Path video_path, int clip_lenght, int skip_to_next, Logger logger)
    //**********************************************************
    {

        int duration_in_seconds = get_video_duration(owner, video_path, logger);
        if ( duration_in_seconds > 3*3600)
        {
            logger.log("WARNING: ffprobe reports duration that looks wrong?"+duration_in_seconds+" in hours="+duration_in_seconds/3600+ "... going to assume 30 minutes");
            duration_in_seconds = 3600/2; // assume half an hour ...
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
            c.add(new Old_and_new_Path(null,dir.toPath(),Command_old_and_new_Path.command_move,Status_old_and_new_Path.before_command));
            Change_gang.report_changes(c);
        }
        for ( int start = 0 ; start < duration_in_seconds; start+=skip_to_next)
        {
            String name = video_path.getFileName().toString()+"_part_"+String.format("%07d",start)+".gif";
            Path destination_gif_full_path = Path.of(dir.getAbsolutePath(),name);

            Actor_engine.run(
                    new Animated_gif_generation_actor(logger), // need on actor instance per task because we want to be able to abort
                    new Animated_gif_generation_message(owner,video_path,destination_gif_full_path,clip_lenght,start,logger),
                    null,
                    logger);
        }
    }


    //**********************************************************
    public static int get_video_duration(
            Stage owner,
            Path video_path,
            Logger logger)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add("ffprobe");
        list.add("-i");
        list.add(video_path.getFileName().toString());
        list.add("-show_format");
        StringBuilder sb = new StringBuilder();
        File wd = video_path.getParent().toFile();
        if (!Execute_command.execute_command_list(list, wd, 2000, sb, logger)) {
            Static_application_properties.manage_show_ffmpeg_install_warning(owner,logger);
        }
        //logger.log("->"+sb.toString()+"<-");

        int duration = -1;
        String[] x = sb.toString().split("\\R");
        for (String l : x) {
            if (l.startsWith("duration=")) {
                String sub = l.substring(9);
                double dd = Double.parseDouble(sub);
                duration = (int) dd;
                logger.log("FOUND DURATION" + duration + "seconds");
                break;
            }
        }
        return duration;
    }

    //**********************************************************
    public static void video_to_gif(
            Stage owner,
            Path video_path,
            Path destination_gif_full_path,
            double clip_duration_in_seconds,
            double start_time_in_seconds,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add("ffmpeg");
        list.add("-y"); // force overwrite of output without asking
        // skip some time at the beginning
        if (start_time_in_seconds >= 0)
        {
            list.add("-ss");
            list.add(convert_to_video_time_string(start_time_in_seconds));
        }
        //list.add("00:02:36.000");

        list.add("-i");
        list.add(video_path.getFileName().toString());
        list.add("-r");
        list.add("10"); // -r 10 ==> 10 fps
        list.add("-vf");
        list.add("scale=512:-1,setsar=1.1");
        list.add("-t");
        list.add(convert_to_video_time_string(clip_duration_in_seconds));
        list.add(destination_gif_full_path.toAbsolutePath().toString());
        File wd = video_path.getParent().toFile();
        if (aborter.should_abort())
        {
            logger.log("video_to_gif aborted");
            return;
        }
        // Output file is empty
        StringBuilder sb = new StringBuilder();
        if (!Execute_command.execute_command_list(list, wd, 2000, sb, logger))
        {
            Static_application_properties.manage_show_ffmpeg_install_warning(owner,logger);
        }
        logger.log("\n\n\n ffmpeg output :\n"+ sb +"\n\n\n");



        if (sb.toString().contains("Output file is empty"))
        {
            //retry without delay
            video_to_gif(owner, video_path, destination_gif_full_path, clip_duration_in_seconds, 0, aborter,logger);

        }
    }

    //**********************************************************
    public static String convert_to_video_time_string(double seconds)
    //**********************************************************
    {
        if ( seconds < 60)
        {
            String f = String.format("%2.3f",seconds);
            return "00:00:" + f;//seconds + ".000";
        }
        else
        {
            int minutes = (int)(seconds /60.0);
            double remaining_seconds = seconds -minutes*60;
            String f = String.format("%2.3f",remaining_seconds);
            if ( minutes < 60)
            {
                return "00:"+minutes+":" + f;//seconds + ".000";
            }
            else
            {
                int hours = minutes/60;
                minutes = minutes-hours*60;
                return hours+":"+minutes+":" + f;//seconds + ".000";
            }
        }
    }

    private static Stage the_stage;
    private static ImageView the_imageview;

    public static final int Mini_console_width = 1000;
    public static final int Mini_console_height = 200;

    public static TextField tf_start;
    public static TextField tf_duration;
    static double start_time_seconds;
    static double duration_seconds;
    static Path gif_full_path;
    static File gif_saving_dir = null;

    //**********************************************************
    public static void interactive(Path path, Logger logger)
    //**********************************************************
    {
        start_time_seconds = 0;
        duration_seconds =  5;
        Path icon_cache_dir = Files_and_Paths.get_icon_cache_dir(logger);

        Platform.runLater(() -> {
            the_stage = new Stage();
            the_stage.setTitle("Animated gif maker for :"+path.getFileName().toString());
            the_stage.setMinWidth(Mini_console_width);
            the_stage.setMinHeight(Mini_console_height);
            the_imageview = new ImageView();

            set_image(start_time_seconds,duration_seconds, path, logger, icon_cache_dir);
            Pane vb = new VBox();
            vb.getChildren().add(the_imageview);
            Button save = new Button("Choose folder & save");
            vb.getChildren().add(save);
            Button save_same = new Button("Save in previously chosen folder");
            save_same.setDisable(true);
            vb.getChildren().add(save_same);

            save.setOnAction(actionEvent -> SwingUtilities.invokeLater(() -> {
                JFileChooser  dir_chooser = new JFileChooser();
                dir_chooser.setFileHidingEnabled(false);
                dir_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if ( gif_saving_dir != null)
                {
                    dir_chooser.setCurrentDirectory(gif_saving_dir.getParentFile());
                    dir_chooser.setSelectedFile(gif_saving_dir);
                }int status = dir_chooser.showOpenDialog(null);
                if (status == JFileChooser.APPROVE_OPTION)
                {
                    gif_saving_dir = dir_chooser.getSelectedFile();
                    save_same.setDisable(false);
                    save_now(new Aborter(),logger);
                }
            }));
            save_same.setOnAction(actionEvent -> {

                if (gif_saving_dir== null) return;
                SwingUtilities.invokeLater(() -> save_now(new Aborter(),logger));
            });

            {
                HBox hb = new HBox();
                Label label = new Label("Start");
                label.setPrefWidth(HUNDRED);
                label.setMinWidth(HUNDRED);
                label.setMaxWidth(HUNDRED);
                hb.getChildren().add(label);
                tf_start = new TextField(String.valueOf(start_time_seconds));
                tf_start.setPrefWidth(3*HUNDRED);
                tf_start.setMinWidth(3*HUNDRED);
                tf_start.setMaxWidth(3*HUNDRED);
                EventHandler<ActionEvent> start_change = actionEvent -> {
                    start_time_seconds = Double.parseDouble(tf_start.getText());
                    logger.log(" START  ="+ start_time_seconds);
                    set_image(start_time_seconds,duration_seconds,path, logger, icon_cache_dir);

                };
                tf_start.setOnAction(start_change);
                hb.getChildren().add(tf_start);
                vb.getChildren().add(hb);

                HBox hbplus = new HBox();
                add_plus_button(1,path, logger, icon_cache_dir, hbplus);
                add_plus_button(5,path, logger, icon_cache_dir, hbplus);
                add_plus_button(10,path, logger, icon_cache_dir, hbplus);
                add_plus_button(30,path, logger, icon_cache_dir, hbplus);
                add_plus_button(60,path, logger, icon_cache_dir, hbplus);
                add_plus_button(180,path, logger, icon_cache_dir, hbplus);
                vb.getChildren().add(hbplus);

                HBox hbminus = new HBox();
                add_minus_button(1, path, logger, icon_cache_dir, hbminus);
                add_minus_button(5, path, logger, icon_cache_dir, hbminus);
                add_minus_button(10, path, logger, icon_cache_dir, hbminus);
                add_minus_button(30, path, logger, icon_cache_dir, hbminus);
                add_minus_button(60, path, logger, icon_cache_dir, hbminus);
                add_minus_button(180, path, logger, icon_cache_dir, hbminus);
                vb.getChildren().add(hbminus);
            }

            {
                HBox hb = new HBox();
                Label label = new Label("Duration");
                label.setPrefWidth(HUNDRED);
                label.setMinWidth(HUNDRED);
                label.setMaxWidth(HUNDRED);
                hb.getChildren().add(label);
                tf_duration = new TextField(String.valueOf(duration_seconds));

                EventHandler<ActionEvent> duration_change = actionEvent -> {
                    duration_seconds = Double.parseDouble(tf_duration.getText());
                    logger.log(" DURATION  ="+duration_seconds);
                    set_image(start_time_seconds,duration_seconds,path, logger, icon_cache_dir);

                };
                tf_duration.setOnAction(duration_change);
                tf_duration.setPrefWidth(3*HUNDRED);
                tf_duration.setMinWidth(3*HUNDRED);
                tf_duration.setMaxWidth(3*HUNDRED);
                hb.getChildren().add(tf_duration);
                vb.getChildren().add(hb);
            }

            Scene the_scene = new Scene(vb);
            the_stage.setScene(the_scene);
            the_stage.setX(0);
            the_stage.setY(0);
            the_stage.show();
        });

    }

    //**********************************************************
    private static void save_now(Aborter aborter, Logger logger)
    //**********************************************************
    {
        String new_name = gif_full_path.getFileName().toString();
        //if (new_name.length() > 24) new_name = new_name.substring(new_name.length() - 12);
        Path new_path = Path.of(gif_saving_dir.getAbsolutePath().toString(), new_name);
        Old_and_new_Path oandnp = new Old_and_new_Path(gif_full_path, new_path, Command_old_and_new_Path.command_move, Status_old_and_new_Path.before_command);
        Moving_files.perform_safe_move_in_a_thread(the_stage, oandnp, aborter, false, true, logger);
    }

    //**********************************************************
    private static void add_minus_button(int amount, Path path, Logger logger, Path icon_cache_dir, HBox hb)
    //**********************************************************
    {
        Button minus = new Button(" - "+amount+" s");
        minus.setPrefWidth(HUNDRED);
        minus.setMinWidth(HUNDRED);
        minus.setMaxWidth(HUNDRED);
        EventHandler<ActionEvent> minus_action = actionEvent -> {
            start_time_seconds -= amount;
            tf_start.setText(String.valueOf(start_time_seconds));
            logger.log(" START  =" + start_time_seconds);
            set_image(start_time_seconds, duration_seconds, path, logger, icon_cache_dir);
        };
        minus.setOnAction(minus_action);
        hb.getChildren().add(minus);
    }

    //**********************************************************
    private static void add_plus_button(int amount, Path path, Logger logger, Path icon_cache_dir, HBox hb)
    //**********************************************************
    {
        Button plus = new Button(" + "+amount+" s");
        plus.setPrefWidth(HUNDRED);
        plus.setMinWidth(HUNDRED);
        plus.setMaxWidth(HUNDRED);
        EventHandler<ActionEvent> plus_action = actionEvent -> {
            start_time_seconds += amount;
            tf_start.setText(String.valueOf(start_time_seconds));
            logger.log(" START  =" + start_time_seconds);
            set_image(start_time_seconds, duration_seconds, path, logger, icon_cache_dir);
        };
        plus.setOnAction(plus_action);
        hb.getChildren().add(plus);
    }

    //**********************************************************
    private static void set_image(double start_time, double clip_duration, Path path, Logger logger, Path icon_cache_dir)
    //**********************************************************
    {
        logger.log("path="+ path);
        int icon_size = 500;

        File icon_file = From_disk.file_for_icon_cache(icon_cache_dir, path, String.valueOf(icon_size), Icon_factory_actor.gif_extension);
        logger.log("icon_file="+icon_file.getAbsolutePath());


        gif_full_path = icon_file.toPath();//Paths.get(icon_cache_dir.toAbsolutePath().toString(), icon_file.getName());

        logger.log("gif_full_path="+gif_full_path);
        video_to_gif(
                the_stage,
                path,
                gif_full_path,
                clip_duration,
                start_time,
                new Aborter(),
                logger);


        Image image = From_disk.load_icon_from_disk_cache(path, icon_cache_dir, icon_size, String.valueOf(icon_size), Icon_factory_actor.gif_extension, false, logger);

        if ( image == null) logger.log("shit image==null");
        else the_imageview.setImage(image);
    }
}
