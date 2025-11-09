// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.properties;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.Shared_services;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.tcp.*;
import klik.util.ui.Popups;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Properties_server
//**********************************************************
{
    private static final boolean dbg = false;
    public static final int PROPERTY_PORT_for_get = 64912;
    public static final int PROPERTY_PORT_for_set = 64913;
    public static final int PROPERTY_PORT_for_all = 64914;
    public static final String E_ND_O_F_K_EYS = "eND_oF_kEYS";
    public static final String eNcoDeD_aS_nUlL = "eNcoDeD_aS_nUlL";


    private final Properties the_Properties;
    private final Path the_properties_path;
    private final Logger logger;
    private final Aborter aborter;
    private final String tag;

    // saving to file is done in a separate thread:
    public final BlockingQueue<Boolean> disk_store_request_queue = new LinkedBlockingQueue<>();

    //**********************************************************
    public Properties_server(Path f_, String tag, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Objects.requireNonNull(aborter);
        this.tag = tag;
        this.aborter = aborter;
        this.logger = logger;
        the_properties_path = f_;
        the_Properties = new Properties();
        load_properties();
        start_store_engine(owner, aborter,  logger);
        start_get_server();
        start_set_server();
        start_all_keys_server();

        //for ( String k : get_all_keys()) logger.log("property: " + k + " = " + get(k));
    }

    //**********************************************************
    private boolean start_get_server()
    //**********************************************************
    {
        // start the server to receive subsequent requests

        Session_factory session_factory = () -> new Session() {
            @Override
            public boolean on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    String request = TCP_util.read_string(dis);
                    String value = the_Properties.getProperty(request);
                    if ( value == null) value = eNcoDeD_aS_nUlL;
                    TCP_util.write_string(value, dos);
                    dos.flush();
                    if ( dbg) logger.log("Properties server GET done for " + request);
                    return true;
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                    return false;
                }

            }

            @Override
            public String name() {
                return "";
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter("Properties GET TCP server", logger), logger);
        return tcp_server.start(PROPERTY_PORT_for_get,"Properties server listening for 'get' requests",false);
    }

    //**********************************************************
    private boolean start_set_server()
    //**********************************************************
    {
        // start the server to receive subsequent requests

        Session_factory session_factory = () -> new Session() {
            @Override
            public boolean on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try
                {
                    String key = TCP_util.read_string(dis);
                    String value = TCP_util.read_string(dis);
                    if ( value.equals(eNcoDeD_aS_nUlL))
                    {
                        the_Properties.remove(key);
                    }
                    else
                    {
                        the_Properties.setProperty(key, value);
                    }
                    disk_store_request_queue.add(true);
                    TCP_util.write_string("ok", dos);
                    dos.flush();
                    if ( dbg) logger.log("Properties server SET done for " + key+" "+value);
                    return true;
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                    return false;
                }
            }
            @Override
            public String name() {
                return "";
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter("Properties SET TCP server", logger), logger);
        return tcp_server.start(PROPERTY_PORT_for_set,"Properties server listening for 'set' requests",false);
    }


    //**********************************************************
    private boolean start_all_keys_server()
    //**********************************************************
    {
        // start the server to receive subsequent requests

        Session_factory session_factory = () -> new Session() {
            @Override
            public boolean on_client_connection(DataInputStream dis, DataOutputStream dos)
            {
                try {
                    Enumeration<Object> e = the_Properties.keys();
                    int count = 0;
                    while(e.hasMoreElements())
                    {
                        String k = (String) e.nextElement();
                        if ( dbg) logger.log("Properties server GET key ="+k);
                        TCP_util.write_string(k, dos);
                        count++;
                    }
                    TCP_util.write_string(E_ND_O_F_K_EYS, dos);
                    dos.flush();
                    if ( dbg) logger.log("Properties server GET done for ALL "+count+" keys");
                }
                catch (IOException e)
                {
                    logger.log(Stack_trace_getter.get_stack_trace(""+e));
                }
                return false;
            }
            @Override
            public String name() {
                return "";
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter("Properties ALL KEYS TCP server", logger), logger);
        return tcp_server.start(PROPERTY_PORT_for_all,"Properties server listening for 'get_all_keys' requests",false);
    }



    // trying to limit disk writes for source that can be super active
    // like the image properties cache or image feature vectors etc
    // but keep as safe as possible, especially always saved on clean exit (with aborter)
    //**********************************************************
    private void start_store_engine(Window owner,Aborter aborter, Logger logger)
    //**********************************************************
    {
        Runnable r = () -> {
            for(;;)
            {
                try {
                    Boolean b = disk_store_request_queue.poll(20, TimeUnit.SECONDS);
                    if ( b == null)
                    {
                        // this is a time out (20 seconds), nothing to save
                        if (aborter.should_abort()) return;
                        continue;
                    }
                    if ( disk_store_request_queue.peek() != null)
                    {
                        //logger.log("ignoring as there are more requests for saving Properties store engine : " + tag + " " + the_properties_path);
                        // if another request is already in flight, we will have an opportunity to save very soon
                        continue;
                    }
                    save(owner);
                    if (aborter.should_abort())
                    {
                        logger.log("aborting (after saving) Properties store engine : " + tag + " " + the_properties_path);
                        return;
                    }
                }
                catch (InterruptedException e)
                {
                    logger.log("INTERRUPTED Properties store engine : " + tag + " " + the_properties_path);
                    return;
                }
            }
        };
        Actor_engine.execute(r, "Properties store engine (Properties server)",logger);
    }

    //**********************************************************
    private void save(Window owner)
    //**********************************************************
    {
       //if (dbg)
            logger.log("Properties_server: save "+the_properties_path.toAbsolutePath());

        if (!Files.exists(the_properties_path))
        {
            try {
                Files.createDirectories(the_properties_path.getParent());
                Files.createFile(the_properties_path);
            }
            catch (FileAlreadyExistsException e)
            {
                if (dbg) logger.log("Warning: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
            }
            catch (IOException e) {
                logger.log("❌ FATAL: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
                return;
            }
            if (dbg) logger.log("created file:"+ the_properties_path);
        }
        else
        {
            if (dbg) logger.log(" file exists:"+ the_properties_path);
        }

        if (!Files.isWritable(the_properties_path))
        {
            Popups.popup_Exception(new AccessDeniedException(the_properties_path.toAbsolutePath().toString()), 200, "Cannot store properties ", owner,logger);
            logger.log("❌ FATAL: cannot write properties in:" + the_properties_path.toAbsolutePath());
            return;
        }
        else
        {
            if (dbg) logger.log(" file is writable:"+ the_properties_path);
        }

        try
        {
            FileOutputStream fos = new FileOutputStream(the_properties_path.toFile());
            the_Properties.store(fos, "no comment");
            fos.close();
            if (dbg) logger.log(("ALL properties stored in:" + the_properties_path.toAbsolutePath()));
        }
        catch (Exception e)
        {
            //logger.log("store_properties Exception: " + Stack_trace_getter.get_stack_trace_for_throwable(e));
            Popups.popup_Exception(new AccessDeniedException(the_properties_path.toAbsolutePath().toString()), 200, "Cannot store properties due to: "+e, owner,logger);

        }
    }

    //**********************************************************
    private void load_properties()
    //**********************************************************
    {
        if (dbg) logger.log("load_properties()");
        FileInputStream fis;
        try
        {

            if (Files.exists(the_properties_path))
            {
                if (!Files.isReadable(the_properties_path))
                {
                    logger.log("cannot read properties from:" + the_properties_path.toAbsolutePath());
                    return;
                }
                fis = new FileInputStream(the_properties_path.toFile());
                the_Properties.load(fis);
                if (dbg) logger.log("properties loaded from:" + the_properties_path.toAbsolutePath());
                fis.close();
            }

        } catch (Exception e)
        {
            logger.log("load_properties Exception: " + e);
        }
    }



    //**********************************************************
    public static void main(String[] deb)
    //**********************************************************
    {
        Logger logger = Shared_services.logger();

        File f_ = new File("test.txt");
        Properties_server ps = new Properties_server(f_.toPath(), "unit test",null,new Aborter("dummy",logger),logger);

        String key = "toto";
        String value = "tata";
        {
            TCP_client_out reply = TCP_client.request2("localhost", PROPERTY_PORT_for_set, key, value, logger);
            if ( reply.status()) logger.log("status OK");
            else logger.log("status FAIL");
            logger.log(reply.reply());
        }
        {
            TCP_client_out reply = TCP_client.request("localhost", PROPERTY_PORT_for_get, key, logger);
            if ( reply.status()) logger.log("status OK");
            else logger.log("status FAIL");

            String value2 = reply.reply();
            if ( value2.equals(value))
            {
                logger.log("TEST OK");
            }
            else
            {
                logger.log("TEST FAIL");
            }

        }




    }



}