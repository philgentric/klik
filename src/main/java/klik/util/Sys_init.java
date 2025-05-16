package klik.util;

import klik.actor.Aborter;
import klik.properties.Non_booleans;
import klik.util.log.Logger;
import klik.util.log.System_logger;

public record Sys_init(Logger logger, Aborter aborter) {

    public static Sys_init get(String name)
    {
        Aborter shared_services_aborter = Non_booleans.init_main_properties_manager( name);
        Logger logger = System_logger.get_system_logger(name);

        return new Sys_init(logger,shared_services_aborter);
    }
}
