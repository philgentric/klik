// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr;

//SOURCES ./util/tcp/TCP_util.java

import javafx.application.Platform;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.properties.Non_booleans_properties;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.tcp.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public interface UI_change
//**********************************************************
{
    String UI_CHANGED = "UI_CHANGED";
    String THIS_IS_THE_PORT_I_LISTEN_TO_FOR_UI_CHANGES = "THIS_IS_MY_UI_CHANGE_PORT";

    void define_UI();


    //**********************************************************
    static int start_UI_change_server(ConcurrentLinkedQueue<Integer> propagate_to, UI_change ui_change_target, String app_name, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {

        Session_factory session_factory = () -> new Session() {
            @Override
            public boolean on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    String msg = TCP_util.read_string(dis);
                    if (msg.startsWith(UI_CHANGED))
                    {
                        Non_booleans_properties.force_reload_from_disk(owner);
                        String new_ui_option = msg.split(" ")[1];
                        logger.log(app_name+": UI_CHANGED RECEIVED, msg is "+new_ui_option);
                        My_I18n.reset();
                        Look_and_feel_manager.reset();
                        Platform.runLater(() -> ui_change_target.define_UI());
                        if ( propagate_to != null) {
                            for (int p : propagate_to) {
                                logger.log(app_name+": propagating UI_CHANGED to port: " + p);
                                TCP_client.send("127.0.0.1", p, msg, logger);
                            }
                        }
                    }
                    else if ( msg.startsWith(THIS_IS_THE_PORT_I_LISTEN_TO_FOR_UI_CHANGES))
                    {
                        int port = Integer.parseInt(msg.split(" ")[1]);
                        logger.log(app_name+": received THIS_IS_MY_UI_CHANGE_PORT "+port);
                        if ( propagate_to == null)
                        {
                            logger.log("❌ "+app_name+": BAD WARNING cannot add port to 'propagate_to'");
                        }
                        else
                        {
                            propagate_to.add(port);
                        }
                    }
                    else
                    {
                        logger.log("❌ "+app_name+": UI_change_server received UNKNOWN message: "+msg);
                    }

                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }

                return true;
            }

            @Override
            public String name() {
                return app_name+": UI_change_server";
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,aborter, logger);
        int port = tcp_server.start_zero(app_name+" listening for UI change signal",false);
        if (port < 0)
        {
            logger.log(app_name+": ERROR starting TCP server for UI_change_server failed");

        }
        else
        {
            logger.log(app_name + ": started UI_change_server at: " + port);
        }

        return port;

    }

}
