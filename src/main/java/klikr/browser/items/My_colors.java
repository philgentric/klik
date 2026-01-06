// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.items;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import klikr.look.my_i18n.My_I18n;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class My_colors
//**********************************************************
{

    public static final String NO_COLOR = "NO_COLOR";

    public static Map<String,My_color> all_colors = new HashMap();

    //**********************************************************
    public static synchronized void init_My_colors(Window owner, Logger logger)
    //**********************************************************
    {
        all_colors.clear();
        Color col;
        String localized_name;
        {
            localized_name = My_I18n.get_I18n_string(NO_COLOR,owner,logger);
            col = null;
            all_colors.put(localized_name,new My_color(col, localized_name,null));
        }
        {
            localized_name = My_I18n.get_I18n_string("Color_Red",owner,logger);
            col = Color.RED;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
        {
            localized_name = My_I18n.get_I18n_string("Color_Green", owner,logger);
            col = Color.GREEN;
            all_colors.put(localized_name, new My_color(col, localized_name, col.toString()));
        }
        {
            localized_name = My_I18n.get_I18n_string("Color_Blue",owner,logger);
            col = Color.BLUE;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
        {
            localized_name = "Chartreuse";
            col = Color.CHARTREUSE;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
        {
            localized_name = "Cyan";
            col = Color.CYAN;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
        {
            localized_name = "Bisque";
            col = Color.BISQUE;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
        {
            localized_name = "Coral";
            col = Color.CORAL;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
        {
        localized_name = "Chocolate";
        col = Color.CHOCOLATE;
        all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
        {
            localized_name = "Noir";
            col = Color.BLACK;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
    }


    //**********************************************************
    public static Circle get_circle(String localized_name, double radius, Window owner, Logger logger)
    //**********************************************************
    {
        My_color my_color = my_color_from_localized_name(localized_name,owner,logger);
        if ( my_color == null) return null;
        return new Circle(radius, my_color.color());
    }

    //**********************************************************
    public static Collection<My_color> get_all_colors(Window owner,Logger logger)
    //**********************************************************
    {
        if ( all_colors.isEmpty()) init_My_colors(owner,logger);
        return all_colors.values();
    }

    //**********************************************************
    public static My_color my_color_from_localized_name(String localized_name, Window owner,Logger logger)
    //**********************************************************
    {
        for ( My_color my_color : get_all_colors(owner,logger))
        {
            if ( my_color.localized_name().equals(localized_name)) return my_color;
        }
        return null;
    }



    //**********************************************************
    public static Color load_color_for_path(Path folderPath, Window owner,Logger logger)
    //**********************************************************
    {
        Path color_file = Path.of(folderPath.toAbsolutePath().toString(),".color");
        try {
            List<String> lines = Files.readAllLines(color_file, StandardCharsets.UTF_8);
            Collection<My_color> all_colors = My_colors.get_all_colors(owner,logger);
            for ( My_color my_color: all_colors)
            {
                if ( my_color.java_name() == null) continue;
                if ( my_color.java_name().equals(lines.get(0)))
                {
                    return Color.valueOf(my_color.java_name());
                }
            }
            try {
                Color c = Color.valueOf(lines.get(0));
                logger.log("✅ WARNING: color not found in predefined list =>"+lines.get(0)+"<= for path: "+folderPath);
                return c;
            } catch ( Exception e) {
                logger.log("❌ FATAL: color not found in predefined list =>"+lines.get(0)+"<= for path: "+folderPath);
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
                return null;
            }
        } catch (IOException e) {
            // this is OK, no file = no color
            //logger.log(Stack_trace_getter.get_stack_trace(""+e));
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
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
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
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
    }


}
