package klik.util;

import klik.browser.Shared_services;
import klik.properties.Non_zooleans;
import klik.util.log.System_logger;

public class Sys_init
{
    public static void init(String name)
    {
        Shared_services.shared_services_aborter = Non_zooleans.init_main_properties_manager(name);
        Shared_services.shared_services_logger = System_logger.get_system_logger(name);
    }
}
