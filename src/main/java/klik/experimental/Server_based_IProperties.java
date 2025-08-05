package klik.experimental;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.properties.IProperties;
import klik.properties.Non_booleans_properties;
import klik.properties.Properties_server;
import klik.util.log.Logger;
import klik.util.tcp.TCP_client;
import klik.util.tcp.TCP_client_out;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Server_based_IProperties implements IProperties
{
    private final static boolean dbg = true;
    private final Logger logger;
    private static Properties_server server = null;

    public Server_based_IProperties(Window owner, Aborter aborter, Logger logger)
    {
        this.logger = logger;
        if (server == null)
        {

            String home = System.getProperty(Non_booleans_properties.USER_HOME);
            System.out.println("home="+home);
            Path p = Paths.get(home, Non_booleans_properties.CONF_DIR, Non_booleans_properties.PROPERTIES_FILENAME);
            server = new Properties_server(p, "Preferences DB", owner, aborter,logger);
        }
    }

    @Override
    public boolean set(String key, String value)
    {
        if( dbg) logger.log("Server_based_IProperties set()"+key+"-"+value);
        TCP_client_out r = TCP_client.request2("localhost", Properties_server.PROPERTY_PORT_for_set, key, value, logger);
        return r.status();
    }
    @Override
    public String get(String key)
    {
        if( dbg) logger.log("Server_based_IProperties get()"+key);
        TCP_client_out r = TCP_client.request("localhost", Properties_server.PROPERTY_PORT_for_get, key, logger);
        if ( r.reply().equals(Properties_server.eNcoDeD_aS_nUlL)) return null;
        return r.reply();
    }
    @Override
    public void remove(String key)
    {
        if( dbg) logger.log("Server_based_IProperties remove() "+key);
        TCP_client_out r = TCP_client.request2("localhost", Properties_server.PROPERTY_PORT_for_set, key, Properties_server.eNcoDeD_aS_nUlL,logger);
    }
    @Override
    public List<String> get_all_keys()
    {
        if( dbg) logger.log("Server_based_IProperties get_all_keys()");
        return TCP_client.request_all_keys("localhost", Properties_server.PROPERTY_PORT_for_all, logger);
    }

    @Override
    public String get_tag() {
        return "Server_based_IProperties";
    }

    @Override
    public void clear() {
        List<String> keys = get_all_keys();
        for (String k : keys)
        {
            remove(k);
        }
    }

    @Override
    public void force_reload_from_disk()
    {
        logger.log("WARNING: Server_based_IProperties force_reload_from_disk() NOT IMPLEMENTED");

    }
}
