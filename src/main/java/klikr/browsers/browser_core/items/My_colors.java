// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.items;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import klikr.look.my_i18n.My_I18n;
import klikr.util.Kontext;
import klikr.util.execute.actor.Executor;
import klikr.util.execute.actor.My_Runnable;
import klikr.util.log.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;

//**********************************************************
public class My_colors
//**********************************************************
{

    public static final String NO_COLOR = "NO_COLOR";

    public static Map<String,My_color> all_colors = new HashMap<>();

    //**********************************************************
    public static void init_My_colors(CountDownLatch latch, Kontext context)
    //**********************************************************
    {

        all_colors.clear();
        String localized_name;
        {
            localized_name = My_I18n.get_I18n_string(NO_COLOR,context);
            all_colors.put(localized_name,new My_color(null, localized_name));
        }
        {
            localized_name = My_I18n.get_I18n_string("Color_Red",context);
            all_colors.put(localized_name,new My_color(Color.RED, localized_name));
        }
        {
            localized_name = My_I18n.get_I18n_string("Color_Green", context);
            all_colors.put(localized_name, new My_color(Color.GREEN, localized_name));
        }
        {
            localized_name = My_I18n.get_I18n_string("Color_Blue",context);
            all_colors.put(localized_name,new My_color(Color.BLUE, localized_name));
        }
        {
            localized_name = "Chartreuse";
            all_colors.put(localized_name,new My_color(Color.CHARTREUSE, localized_name));
        }
        {
            localized_name = "Cyan";
            all_colors.put(localized_name,new My_color(Color.CYAN, localized_name));
        }
        {
            localized_name = "Bisque";
            all_colors.put(localized_name,new My_color(Color.BISQUE, localized_name));
        }
        {
            localized_name = "Coral";
            all_colors.put(localized_name,new My_color(Color.CORAL, localized_name));
        }
        {
        localized_name = "Chocolate";
        all_colors.put(localized_name,new My_color(Color.CHOCOLATE, localized_name));
        }
        {
            localized_name = "Noir";
            all_colors.put(localized_name,new My_color(Color.BLACK, localized_name));
        }

        latch.countDown();
    }


    //**********************************************************
    public static Circle get_circle(String localized_name, double radius, Kontext context)
    //**********************************************************
    {
        My_color my_color = my_color_from_localized_name(localized_name,context);
        if ( my_color == null) return null;
        return new Circle(radius, my_color.color());
    }

    //**********************************************************
    public static Collection<My_color> get_all_colors(Kontext context)
    //**********************************************************
    {
        if ( !all_colors.isEmpty()) return all_colors.values();
        CountDownLatch latch = new CountDownLatch(1);
        My_Runnable r = new My_Runnable() {
            @Override
            public void run() throws Exception {
                init_My_colors(latch, context);
            }
        };
        Executor.execute(r,"loading colors", context.logger());
        try {
            latch.await();
        } catch (InterruptedException e) {
            context.log_with_stack_trace_from_throwable("",e);
        }
        return all_colors.values();
    }

    //**********************************************************
    public static My_color my_color_from_localized_name(String localized_name, Kontext context)
    //**********************************************************
    {
        for ( My_color my_color : get_all_colors(context))
        {
            if ( my_color.localized_name().equals(localized_name)) return my_color;
        }
        // not found , return no-color
        for ( My_color my_color : get_all_colors(context))
        {
            if ( my_color.color() ==null) return my_color;
        }
        return null;//bad
    }



    //**********************************************************
    public static Color load_color_for_path(Path folderPath, Kontext context)
    //**********************************************************
    {
        Path color_file = Path.of(folderPath.toAbsolutePath().toString(),".color");
        try {
            List<String> lines = Files.readAllLines(color_file, StandardCharsets.UTF_8);
            if ( lines.isEmpty()) return null;
            Collection<My_color> all_colors = My_colors.get_all_colors(context);
            for ( My_color my_color: all_colors)
            {
                if ( my_color == null) continue;
                if ( my_color.toString().equals(lines.get(0)))
                {
                    return my_color.color();
                }
            }
            try {
                Color c = Color.valueOf(lines.get(0));
                context.log(Logger.ok+" OK: color  identified =>"+lines.get(0)+" as" + c.toString()+"<= for path: "+folderPath);
                return c;
            } catch ( Exception e) {
                context.log(Logger.error+"WARNING: color not identified  =>"+lines.get(0)+"<= for path: "+folderPath);
                context.log_with_stack_trace_from_throwable("",e);
                return null;
            }
        } catch (IOException e) {
            // this is OK, no file = no color
            //context.log_with_stack_trace(""+e));
        }
        return null;
    }

    //**********************************************************
    public static void save_color(Path folderPath, String color_java_name, Logger logger)
    //**********************************************************
    {
        Path color_file = Path.of(folderPath.toAbsolutePath().toString(),".color");
        if ( color_java_name == null)
        {
            try {
                Files.delete(color_file);
            } catch (IOException e) {
                logger.log_with_stack_trace_from_throwable("",e);
            }
            //logger.log("removed "+color_file);
            return;
        }

        try {
            FileWriter writer = new FileWriter(color_file.toFile(),StandardCharsets.UTF_8,false);
            writer.write(color_java_name);
            writer.close();
            //logger.log("saved "+color_file+" "+color_java_name);

        } catch(IOException e){
            logger.log_with_stack_trace_from_throwable("",e);
        }
    }


}
