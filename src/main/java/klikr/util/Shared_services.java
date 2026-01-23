// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util;

import javafx.stage.Stage;
import klikr.util.execute.actor.Aborter;
import klikr.properties.File_based_IProperties;
import klikr.properties.IProperties;
import klikr.properties.boolean_features.Booleans;
import klikr.properties.boolean_features.Feature;
import klikr.util.log.File_logger;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;

//**********************************************************
public class Shared_services
//**********************************************************
{
    private static Aborter aborter;
    private static Logger logger;
    private static IProperties main_properties;

    public static Logger logger()
    {
        return logger;
    }
    public static Aborter aborter()
    {
        return aborter;
    }
    //**********************************************************
    public static IProperties main_properties()
    //**********************************************************
    {
        return main_properties;
    }
    //**********************************************************
    public static void init(String name, Stage owner)
    //**********************************************************
    {
        if (main_properties != null)
        {
            logger.log(" DONT create Shared_services more than once !");
            return;
        }

        Logger tmp_logger = new Simple_logger();
        aborter = new Aborter("Shared_services", tmp_logger);
        // this properties file holds both the Non-booleans AND the Booleans
        main_properties = new File_based_IProperties(name+" main properties", "klikr", true,owner, aborter, tmp_logger);
        logger = get_logger(name);
    }

    //**********************************************************
    public static Logger get_logger(String tag)
    //**********************************************************
    {
        Logger logger;
        if (Booleans.get_boolean_defaults_to_false(Feature.Log_to_file.name()))
        {
            logger = new File_logger(tag);
        }
        else
        {
            logger = new Simple_logger();
        }
        return logger;
    }

}
