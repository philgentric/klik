

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
//SOURCES browser/Browser.java
//SOURCES browser/Browser_creation_context.java
//SOURCES browser/My_Stage.java
//SOURCES change/history/History_auto_clean.java
//SOURCES look/Look_and_feel_manager.java
//SOURCES look/my_i18n/Language_manager.java
//SOURCES properties/Static_application_properties.java
//SOURCES util/log/Exceptions_in_threads_catcher.java
//SOURCES util/cache_auto_clean/Monitor.java
//SOURCES util/log/System_out_logger.java
//SOURCES util/log/Logger.java
//SOURCES util/info_stage/*.java
//SOURCES util/performance_monitor/Performance_monitor.java
//SOURCES util/tcp/TCP_client.java
//SOURCES util/tcp/TCP_client_out.java

//SOURCES actor/Actor.java
//SOURCES util/execute/Scheduled_thread_pool.java
//SOURCES browser/icons/Virtual_landscape.java
//SOURCES properties/File_sort_by.java
//SOURCES properties/Properties_manager.java
//SOURCES properties/Cache_folders.java
//SOURCES browser/Vertical_slider.java
//SOURCES browser/Browser_menus.java
//SOURCES browser/items/Item_button.java
//SOURCES browser/meter/Meters_stage.java
//SOURCES change/active_list_stage/Active_list_stage.java
//SOURCES change/active_list_stage/Active_list_stage_action.java
//SOURCES change/active_list_stage/Datetime_to_signature_source.java
//SOURCES change/history/History_item.java
//SOURCES change/undo/Undo_engine.java
//SOURCES change/undo/Undo_item.java
//SOURCES image_ml/face_recognition/Face_recognition_service.java
//SOURCES images/*.java
//SOURCES images/decoding/Exif_metadata_extractor.java
//SOURCES level3/metadata/Tag_items_management_stage.java
//SOURCES properties/Bookmarks.java
//SOURCES actor/Message.java
//SOURCES actor/Job.java
package klik;

import javafx.application.Application;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.My_Stage;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.Language_manager;
import klik.properties.Static_application_properties;
import klik.util.cache_auto_clean.Monitor;
import klik.util.log.Logger;
import klik.util.log.Exceptions_in_threads_catcher;
import klik.util.log.System_out_logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Klik_application extends Application
//**********************************************************
{

    //**********************************************************
    public static void main(String[] args)
    {
        launch(args);
    }
    //**********************************************************

    //**********************************************************
    @Override
    public void start(Stage primary_stage) throws Exception
    //**********************************************************
    {

        Print_system_info.print();

        //setUserAgentStylesheet(STYLESHEET_MODENA);

        Logger logger = new System_out_logger("Klik");

        Language_manager.init_registered_languages(logger);

        Browser.monitoring_aborter = new Aborter("Monitoring", logger);
        new Monitor(Browser.monitoring_aborter,logger).start();

        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);
        Look_and_feel_manager.init_Look_and_feel(logger);

        Parameters params = getParameters();
        List<String> list = params.getRaw();
        Path path;
        if ( list.isEmpty())
        {
            path = (new File(System.getProperty(Static_application_properties.USER_HOME))).toPath();
        }
        else
        {
            path = Path.of(list.get(0));
        }

        Browser_creation_context.first(new My_Stage(primary_stage,logger),path,logger);
    }




}
