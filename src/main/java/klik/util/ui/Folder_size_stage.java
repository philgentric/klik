// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.ui;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.look.Font_size;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.files_and_paths.Sizes;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.ui.progress.Progress;

import java.nio.file.Path;

//**********************************************************
public class Folder_size_stage
//**********************************************************
{

    private static final double icon_height = 100;
    private static final double stage_x_start = 10;
    private static final double stage_y_start = 10;
    private static final double size_stage_height = 3*icon_height;
    private static final double size_stage_width = 2*size_stage_height;
    private static double stage_x = stage_x_start;
    private static double stage_y = stage_y_start;



    //**********************************************************
    public static void get_folder_size(Path path,
                                       Window owner,
                                       Logger logger)
    //**********************************************************
    {
        // open a window to display what is going on and the final result
        Stage local_stage = new Stage();
        local_stage.initOwner(owner);
        local_stage.setX(stage_x);
        local_stage.setY(stage_y);
        stage_y += size_stage_height;
        if ( stage_y > 1000)
        {
            stage_y = stage_y_start;
            stage_x += 100;
            if ( stage_x > 1000) stage_x = stage_x_start;
        }

        local_stage.setHeight(size_stage_height);
        local_stage.setWidth(size_stage_width);
        VBox vbox = new VBox();
        Look_and_feel_manager.set_region_look(vbox,owner,logger);

        vbox.setAlignment(javafx.geometry.Pos.CENTER);

        Progress progress = Progress.start(vbox,owner,logger);

        TextArea textarea2 = new TextArea();
        vbox.getChildren().add(textarea2);
        textarea2.setMinHeight(icon_height);
        Font_size.apply_this_font_size_to_Node(textarea2,20,logger);

        Scene scene = new Scene(vbox, Color.WHITE);
        local_stage.setTitle(path.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();
        //local_stage.setAlwaysOnTop(true);


        Aborter local_aborter = new Aborter("get_folder_size",logger);
        local_stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                local_aborter.abort("folder size window closing");
            }
        });

        local_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE) {
                        local_stage.close();
                        local_aborter.abort("folder size window closing2");
                        key_event.consume();
                    }
                });
        Runnable r = () -> {
            // this call is blocking until tree has been explored
            Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(path,local_aborter, logger);

            Jfx_batch_injector.inject(() -> {
                String bytes = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(sizes.bytes(),owner,logger);

                progress.remove();
                if (sizes.bytes() < 0)
                {
                    textarea2.setText(path+ "\nAn error occurred, probably Access Denied, check the logs");
                }

                String folders_s = My_I18n.get_I18n_string("Folders", owner,logger);
                String files_s = My_I18n.get_I18n_string("Files", owner,logger);
                String image_s = My_I18n.get_I18n_string("Images", owner,logger);
                String bytes_s = My_I18n.get_I18n_string("Bytes", owner,logger);

                textarea2.setText(folders_s+":\t\t\t"+ sizes.folders() + "\n"+
                        files_s+":\t\t\t" + sizes.files() + "\n" +
                        image_s+":\t\t\t"+sizes.images()+ "\n" +
                        bytes_s+":\t\t\t"+bytes);
                logger.log(path + " :  " + sizes.folders() + " " + folders_s + " , " + sizes.files() + " " + files_s + " , " + sizes.images() + " " + image_s+" , "+bytes+" "+bytes_s);

            },logger);
        };
        Actor_engine.execute(r, "Explore tree",logger);

    }

}
