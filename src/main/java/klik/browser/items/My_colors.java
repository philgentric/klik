package klik.browser.items;

import javafx.scene.paint.Color;
import klik.look.my_i18n.I18n;
import klik.util.Logger;

import java.util.*;

public class My_colors {

    public static final String NO_COLOR = "NO_COLOR";

    public static Map<String,My_color> all_colors = new HashMap();

    public static void init_My_colors(Logger logger)
    {
        Color col;
        String localized_name;
        {
            localized_name = I18n.get_I18n_string(NO_COLOR,logger);
            col = null;
            all_colors.put(localized_name,new My_color(col, localized_name,null));
        }
        {
            localized_name = I18n.get_I18n_string("Color_Red",logger);
            col = Color.RED;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
        {
            localized_name = I18n.get_I18n_string("Color_Green", logger);
            col = Color.GREEN;
            all_colors.put(localized_name, new My_color(col, localized_name, col.toString()));
        }
        {
            localized_name = I18n.get_I18n_string("Color_Blue",logger);
            col = Color.BLUE;
            all_colors.put(localized_name,new My_color(col, localized_name,col.toString()));
        }
    }

    public static Collection<My_color> get_all_colors(Logger logger)
    {
        if ( all_colors != null) init_My_colors(logger);
        return all_colors.values();
    }

    public static My_color my_color_from_localized_name(String localized_name, Logger logger)
    {
        for ( My_color my_color : get_all_colors(logger))
        {
            if ( my_color.localized_name().equals(localized_name)) return my_color;
        }
        return null;
    }
}
