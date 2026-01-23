// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.in3D;

import javafx.application.Application;
import javafx.stage.Stage;
import klikr.Window_type;
import klikr.Instructions;
import klikr.util.Shared_services;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.util.log.Exceptions_in_threads_catcher;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;
import klikr.util.perf.Perf;

import java.nio.file.Path;

//*******************************************************
public class The_main_circular_3 extends Application
//*******************************************************
{

    //*******************************************************
    static void main(String[] args) {
        Application.launch(args);
    }
    //*******************************************************

    //*******************************************************
    @Override
    public void start(Stage primaryStage)
    //*******************************************************
    {
        Shared_services.init("main circular 3D", primaryStage);
        Logger logger = new Simple_logger();
        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);
        Perf.monitor(logger);

        Path p = Path.of(System.getProperty("user.home"));
        Instructions.additional_no_past(Window_type.File_system_3D,new Path_list_provider_for_file_system(p,primaryStage,logger),primaryStage,logger);
    }
}
