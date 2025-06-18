package klik.util;

import javafx.stage.Window;
import klik.browser.Shared_services;
import klik.properties.Non_booleans;
import klik.util.log.System_logger;

public class Sys_init
{
    public static void init(String name, Window owner)
    {
        Shared_services.shared_services_aborter = Non_booleans.init_main_properties_manager(name,owner);
        Shared_services.shared_services_logger = System_logger.get_system_logger(name);
    }
}
