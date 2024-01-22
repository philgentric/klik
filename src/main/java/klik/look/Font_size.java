package klik.look;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import klik.properties.Static_application_properties;
import klik.util.Logger;

//**********************************************************
public class Font_size
//**********************************************************
{
    public static final String FX_FONT_SIZE = "-fx-font-size:";
    public static final String PX = "px;";

    private final static boolean dbg = false;
    //**********************************************************
    public static String get_font_size(Logger logger)
    //**********************************************************
    {
        return FX_FONT_SIZE + Static_application_properties.get_font_size( logger) + PX;
    }

    // brutally set the font size by setting the whole style
    // i.e. the font could be reverted to the default one
    //**********************************************************
    public static void set_preferred_font_size(Node x, Logger logger)
    //**********************************************************
    {
        set_font_size(x,Static_application_properties.get_font_size(logger),logger);
    }
    //**********************************************************
    public static void set_font_size(Node x, double size, Logger logger)
    //**********************************************************
    {
        if ( x.getStyle().contains(FX_FONT_SIZE))
        {
            x.setStyle(FX_FONT_SIZE + size + PX);
        }
        else
        {
            x.setStyle(x.getStyle()+FX_FONT_SIZE + size + PX);

        }
        //logger.log("set_font_size->"+x.getStyle()+"<-");
    }

    //**********************************************************
    public static void set_preferred_font_size(ContextMenu x, Logger logger)
    //**********************************************************
    {
        set_font_size(x,Static_application_properties.get_font_size(logger),logger);
    }
    //**********************************************************
    public static void set_font_size(ContextMenu x, double size, Logger logger)
    //**********************************************************
    {
        x.setStyle(FX_FONT_SIZE + size + PX);
    }


    // edit the style to change the font size, without affecting the rest of the style
    //**********************************************************
    public static void apply_font_size(Node node, Logger logger)
    //**********************************************************
    {
        double size = Static_application_properties.get_font_size(logger);
        if (dbg) System.out.println("applying font size " + size);
        apply_font_size(node, size, logger);
    }
    //**********************************************************
    public static void apply_font_size(Node node, double size, Logger logger)
    //**********************************************************
    {
        String style = node.getStyle();
        if ( dbg) System.out.println("style->" + style + "<-");
        int index = style.indexOf(FX_FONT_SIZE);

        if (index < 0) {
            node.setStyle(style + FX_FONT_SIZE + size + PX);
            if ( dbg)  System.out.println("new_style1->" + style + FX_FONT_SIZE + size + PX + "<-");
            return;
        }
        index += (FX_FONT_SIZE).length();

        int index2 = style.indexOf(PX, index);
        index2 += ("px;").length();

        String new_style = style.substring(0, index);
        new_style += size + PX;
        new_style += style.substring(index2);
        //new_style += "-fx-wrap-text:true;";
        //System.out.println("WRAP2!");
        node.setStyle(new_style);

        if ( dbg) System.out.println("new_style2->" + new_style + "<-");
    }
}
