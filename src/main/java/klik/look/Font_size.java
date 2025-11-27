// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.look;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PopupControl;
import javafx.stage.Window;
import klik.properties.Non_booleans_properties;
import klik.util.log.Logger;

//**********************************************************
public class Font_size
//**********************************************************
{
    public static final String FX_FONT_SIZE = "-fx-font-size:";
    public static final String PX = "px;";
    public static int PX_LENGTH = ("px;").length();


    private final static boolean dbg = false;
    //**********************************************************
    public static String get_font_size(Window owner, Logger logger)
    //**********************************************************
    {
        return FX_FONT_SIZE + Non_booleans_properties.get_font_size(owner,logger) + PX;
    }


    // edit the style to change the font size, without affecting the rest of the style
    //**********************************************************
    public static void apply_global_font_size_to_Node(Node node, Window owner, Logger logger)
    //**********************************************************
    {
        double size = Non_booleans_properties.get_font_size(owner,logger);
        //if (dbg)
            logger.log("applying font size " + size);
        apply_this_font_size_to_Node(node, size, logger);
    }

    //**********************************************************
    public static void apply_this_font_size_to_Node(Node node, double size, Logger logger)
    //**********************************************************
    {
        init(logger);
        String style = node.getStyle();
        if ( style.isEmpty())
        {
            node.setStyle(append_to_old_style(style,size,font_family,logger));
            return;
        }
        if ( dbg) logger.log("\nfound node style->" + style + "<-");

        if ( style.contains(FX_FONT_SIZE))
        {
            String new_style = style.replaceAll("-fx-font-style:\\s*[^;]+",FX_FONT_SIZE+size+PX);
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

            font_family = "'Atkinson Hyperlegible'";
            String font_filename = "AtkinsonHyperlegible-Bold.ttf";

            //font_family = "TRON";
            //String font_filename = "TRON.ttf";

            //font_family = "Roboto";
            //String font_filename = "Roboto-Bold.ttf";

            Look_and_feel_manager.get_instance(null, logger).load_font(font_filename);
        }
    }

    //**********************************************************
    private static String append_to_old_style(String old_style, double size, String font_family, Logger logger)
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        sb.append(old_style).append(FX_FONT_SIZE).append(size).append(PX);
        sb.append("-fx-font-family: "+font_family+";");
        if ( dbg) logger.log("font get_new_style->" + sb + "<-");

        return sb.toString();
    }

}
