// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.look;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;
import klikr.settings.Non_booleans_properties;
import klikr.util.log.Logger;

//**********************************************************
public class Font_size
//**********************************************************
{
    private final static boolean dbg = false;
    public static final String FX_FONT_SIZE = "-fx-font-size:";
    public static final String PT = "pt;";


    //**********************************************************
    public static String get_font_size(Window owner, Logger logger)
    //**********************************************************
    {
        return FX_FONT_SIZE + Non_booleans_properties.get_font_size(owner,logger) + PT;
    }


    //**********************************************************
    public static void apply_global_font_size_to_MenuItem(MenuItem menu_item, Window owner, Logger logger)
    //**********************************************************
    {
        int size = Non_booleans_properties.get_font_size(owner,logger);
        init(logger);
        String style = menu_item.getStyle();
        if ( style == null)
        {
            menu_item.setStyle(append_to_old_style(style,size,font_family,logger));
            //logger.log("1 applying font length to MenuItem " + length+ " "+menu_item.getText());
            return;
        }
        if ( style.isEmpty())
        {
            menu_item.setStyle(append_to_old_style(style,size,font_family,logger));
            //logger.log("2 applying font length to MenuItem " + length+ " "+menu_item.getText());
            return;
        }
        if ( dbg) logger.log("\nfound node style->" + style + "<-");

        if ( style.contains(FX_FONT_SIZE))
        {
            String new_style = style.replaceAll("-fx-font-style:\\s*[^;]+",FX_FONT_SIZE+size+ PT);
            menu_item.setStyle(new_style);
            //logger.log("3 applying font length to MenuItem " + length+ " "+menu_item.getText());
            return;
        }

        menu_item.setStyle(append_to_old_style(style,size,font_family,logger));
        //logger.log("4 applying font length to MenuItem " + length+ " "+menu_item.getText());

    }


    // edit the style to change the font length, without affecting the rest of the style
    //**********************************************************
    public static void apply_global_font_size_to_Node(Node node, Window owner, Logger logger)
    //**********************************************************
    {
        int size = Non_booleans_properties.get_font_size(owner,logger);
        if (dbg)
            logger.log("applying font size " + size);
        apply_this_font_size_to_Node(node, size, logger);
    }

    //**********************************************************
    public static void apply_this_font_size_to_Node(Node node, int size, Logger logger)
    //**********************************************************
    {
        init(logger);
        String style = node.getStyle();
        if ( style.isEmpty())
        {
            node.setStyle(append_to_old_style(style,size,font_family,logger));
            return;
        }
        if ( dbg)
            logger.log("\nfound node style->" + style + "<-");

        if ( style.contains(FX_FONT_SIZE))
        {
            String new_style = style.replaceAll("-fx-font-style:\\s*[^;]+",FX_FONT_SIZE+size+ PT);
            node.setStyle(new_style);
            return;
        }

        node.setStyle(append_to_old_style(style,size,font_family,logger));
    }

    private static boolean font_loaded = false;
    public static String font_family;

    //**********************************************************
    public static void init(Logger logger)
    //**********************************************************
    {
        if ( !font_loaded)
        {
            font_loaded = true;
            // this one is default:
            //font_family = "Papyrus";

            font_family = "Atkinson Hyperlegible";
            String font_filename = "AtkinsonHyperlegible-Bold.ttf";

            //font_family = "TRON";
            //String font_filename = "TRON.ttf";

            //font_family = "Roboto";
            //String font_filename = "Roboto-Bold.ttf";

            Look_and_feel_manager.get_instance(null, logger).load_font(font_filename);
        }
    }

    //**********************************************************
    private static String append_to_old_style(String old_style, int size, String font_family, Logger logger)
    //**********************************************************
    {
        /*
        double quotes are strongly recommended** in JavaFX to avoid parsing failures:
        ```css
        -fx-font-size: 30.0pt;
        -fx-font-family: "Atkinson Hyperlegible";
        ```
        1. **Font Fallback**: Always include a fallback family in case the custom font isn't installed:
           ```css
           -fx-font-family: "Atkinson Hyperlegible", sans-serif;
           ```

         */
        StringBuilder sb = new StringBuilder();
        if ( old_style != null) sb.append(old_style);
        sb.append(FX_FONT_SIZE).append(size).append(PT);
        sb.append(" -fx-font-family: \""+font_family+"\", sans-serif;");
        if ( dbg) logger.log("font get_new_style->" + sb + "<-");

        return sb.toString();
    }

}
