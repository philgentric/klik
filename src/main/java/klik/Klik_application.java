

//FILES ../../resources/klik/denied.png
//FILES ../../resources/klik/ding.mp3
//FILES ../../resources/klik/dummy.png
//FILES ../../resources/klik/haarcascade_frontalface_default.xml
//FILES ../../resources/klik/lazy.png
//FILES ../../resources/klik/not-found.png
//FILES ../../resources/klik/speaker.png
//FILES ../../resources/klik/unknown-error.png
//FILES ../../resources/klik/running_film.gif



//FILES dark/bookmarks.png=../../resources/klik/dark/bookmarks.png
//FILES dark/dark.css=../../resources/klik/dark/dark.css
//FILES dark/folder.png=../../resources/klik/dark/folder.png
//FILES dark/image.png=../../resources/klik/dark/image.png
//FILES dark/preferences.png=../../resources/klik/dark/preferences.png
//FILES dark/trash.png=../../resources/klik/dark/trash.png
//FILES dark/up.png=../../resources/klik/dark/up.png
//FILES dark/view.png=../../resources/klik/dark/view.png
//FILES ../../resources/klik/dark/lazy_dark.png

//FILES light/bookmarks.png=../../resources/klik/light/bookmarks.png
//FILES broken.png=../../resources/klik/broken.png
//FILES light/camera.png=../../resources/klik/light/camera.png
//FILES light/denied.png=../../resources/klik/light/denied.png
//FILES light/folder.png=../../resources/klik/light/folder.png
//FILES light/image.png=../../resources/klik/light/image.png
//FILES klik.png=../../resources/klik/klik.png
//FILES light/light.css=../../resources/klik/light/light.css
//FILES light/preferences.png=../../resources/klik/light/preferences.png
//FILES light/trash.png=../../resources/klik/light/trash.png
//FILES light/up.png=../../resources/klik/light/up.png
//FILES light/view.png=../../resources/klik/light/view.png


//FILES wood/bookmarks.png=../../resources/klik/wood/bookmarks.png
//FILES wood/preferences.png=../../resources/klik/wood/preferences.png
//FILES wood/view.png=../../resources/klik/wood/view.png
//FILES wood/wood.css=../../resources/klik/wood/wood.css
//FILES wood/wooden_camera.png=../../resources/klik/wood/wooden_camera.png
//FILES wood/wooden_folder.png=../../resources/klik/wood/wooden_folder.png
//FILES wood/wooden_trash.png=../../resources/klik/wood/wooden_trash.png
//FILES wood/wooden_up.png=../../resources/klik/wood/wooden_up.png


//FILES MessagesBundle_en_US.properties=../../resources/klik/MessagesBundle_en_US.properties
//FILES MessagesBundle_fr_FR.properties=../../resources/klik/MessagesBundle_fr_FR.properties

//DEPS com.github.ben-manes.caffeine:caffeine:3.1.8
//DEPS commons-io:commons-io:2.16.1
//DEPS com.drewnoakes:metadata-extractor:2.19.0
//DEPS com.google.code.gson:gson:2.8.6
//DEPS org.openjfx:javafx-controls:21.0.3:${os.detected.jfxname}
//DEPS org.openjfx:javafx-graphics:21.0.3:${os.detected.jfxname}
//DEPS org.openjfx:javafx-media:21.0.3:${os.detected.jfxname}


//SOURCES ./Print_system_info.java
//SOURCES actor/Aborter.java
//SOURCES browser/classic/Browser.java
//SOURCES New_window_context.java
//SOURCES browser/My_Stage.java
//SOURCES change/history/History_auto_clean.java
//SOURCES look/Look_and_feel_manager.java
//SOURCES properties/Non_booleans.java
//SOURCES util/log/Exceptions_in_threads_catcher.java
//SOURCES util/cache_auto_clean/Monitor.java
//SOURCES util/log/File_logger.java
//SOURCES util/log/Logger_factory.java
//SOURCES util/log/Logger.java
//SOURCES util/info_stage/*.java
//SOURCES util/tcp/TCP_client.java
//SOURCES util/tcp/TCP_client_out.java
//SOURCES properties/boolean_features/Booleans.java
//SOURCES actor/Actor.java
//SOURCES util/execute/Scheduled_thread_pool.java
//SOURCES browser/virtual_landscape/Virtual_landscape.java
//SOURCES properties/File_sort_by.java
//SOURCES properties/Properties_manager.java
//SOURCES properties/Cache_folder.java
//SOURCES browser/virtual_landscape/Vertical_slider.java
//SOURCES browser/virtual_landscape/Virtual_landscape_menus.java
//SOURCES browser/items/Item_file_no_icon.java
//SOURCES change/active_list_stage/Active_list_stage.java
//SOURCES change/active_list_stage/Active_list_stage_action.java
//SOURCES change/active_list_stage/Datetime_to_signature_source.java
//SOURCES change/history/History_item.java
//SOURCES change/undo/Undo_for_moves.java
//SOURCES change/undo/Undo_item.java
//SOURCES image_ml/face_recognition/Face_recognition_service.java
//SOURCES images/*.java
//SOURCES images/decoding/Exif_metadata_extractor.java
//SOURCES change/bookmarks/Bookmarks.java
//SOURCES actor/Message.java
//SOURCES actor/Job.java
//SOURCES util/Sys_init.java
//SOURCES ./Start_context.java

package klik;

import javafx.application.Application;
import javafx.stage.Stage;
import klik.browser.*;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.Sys_init;
import klik.util.cache_auto_clean.Monitor;
import klik.util.log.Exceptions_in_threads_catcher;
import klik.util.log.File_logger;
import klik.util.log.Logger;
import klik.util.log.Logger_factory;
import klik.util.tcp.TCP_client;

import java.nio.file.Path;

//**********************************************************
public class Klik_application extends Application
//**********************************************************
{
    private final static String name = "Klik_application";

    public static Integer ui_change_report_port_at_launcher; // port on which the launcher will LISTEN for UI_CHANGED messages
    public static Stage primary_stage;
    //**********************************************************
    public static void main(String[] args)
    {
        launch(args);
    }
    //**********************************************************

    //**********************************************************
    @Override
    public void start(Stage primary_stage_) throws Exception
    //**********************************************************
    {
        Sys_init.init(name, primary_stage_);
        Logger logger = Logger_factory.get(name);

        primary_stage = primary_stage_;
        Start_context context = Start_context.get_context_and_args(this);

        logger.log("Klik_application Start_context= " + context.args());

        primary_stage.setOnCloseRequest(event -> {
            System.out.println("Klik_application primary_stage setOnCloseRequest exit");
            System.exit(0);
        });

        Print_system_info.print();

        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);

        ui_change_report_port_at_launcher = context.extract_ui_change_report_port();
        if ( ui_change_report_port_at_launcher == null)
        {
            logger.log("Klik_application: ui_change_report_port_at_launcher=null ");
        }
        else
        {
            logger.log("Klik_application ui_change_report_port_at_launcher= " + ui_change_report_port_at_launcher);
        }
        Path path = context.extract_path();
        if ( path != null)
        {
            logger.log("Starting browser on path ->" + path+"<-");
        }
        Window_provider window_provider = New_window_context.additional_no_past(path,primary_stage_,logger);
        new Monitor(window_provider, logger).start();

        if ( context.extract_reply_port() != null) // is null when launched from the audio player
        {
            TCP_client.send_in_a_thread("localhost", context.extract_reply_port(), Launcher.STARTED, logger);
        }
    }



}
