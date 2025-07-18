package klik.util;

import javafx.stage.Window;
import klik.browser.Shared_services;
import klik.properties.Non_booleans;
import klik.util.log.Logger_factory;

public class Sys_init
{
    public static void init(String name, Window owner)
    {
        Shared_services.aborter = Non_booleans.init_main_properties_manager(name,owner);
        Shared_services.logger = Logger_factory.get(name);
    }
}
