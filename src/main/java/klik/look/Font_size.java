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
    public static void apply_font_size(Node node,  Window owner,Logger logger)
    //**********************************************************
    {
        double size = Non_booleans_properties.get_font_size(owner,logger);
        if (dbg) logger.log("applying font size " + size);
        apply_font_size(node, size, logger);
    }
    //**********************************************************
    public static void apply_font_size(Node node, double size, Logger logger)
    //**********************************************************
    {
        String style = node.getStyle();
        if ( style.isEmpty()|| style.endsWith(";"))
        {
            // shortcut!
            StringBuilder sb = new StringBuilder();
            sb.append(style).append(FX_FONT_SIZE).append(size).append(PX);
            node.setStyle(sb.toString());
            //node.setStyle(style + FX_FONT_SIZE + size + PX);
            if ( dbg)  logger.log("new_style1->" + style + FX_FONT_SIZE + size + PX + "<-");
            return;
        }
        if ( dbg) logger.log("Node style->" + style + "<-");
        int index = style.indexOf(FX_FONT_SIZE);

        if (index < 0) {
            // font not in stlye yet, so we add it
            StringBuilder sb = new StringBuilder();
            sb.append(style).append(FX_FONT_SIZE).append(size).append(PX);
            node.setStyle(sb.toString());
            //node.setStyle(style + FX_FONT_SIZE + size + PX);
            if ( dbg)  logger.log("new_style1->" + style + FX_FONT_SIZE + size + PX + "<-");
            return;
        }
        index += (FX_FONT_SIZE).length();
        int index2 = style.indexOf(PX, index);
        index2 += PX_LENGTH;

        String new_style = style.substring(0, index);
        StringBuilder sb = new StringBuilder();
        sb.append(new_style).append(size).append(PX).append(style.substring(index2));
        node.setStyle(sb.toString());

        if( dbg) logger.log("new_style2->" + new_style + "<-");
    }

    //**********************************************************
    public static void apply_font_size(MenuItem mi, double size, Logger logger)
    //**********************************************************
    {
        String style = mi.getStyle();
        // style is null
        /*
        if ( style.isEmpty()|| style.endsWith(";"))
        {
            // shortcut!
            StringBuilder sb = new StringBuilder();
            sb.append(style).append(FX_FONT_SIZE).append(size).append(PX);
            mi.setStyle(sb.toString());
            //node.setStyle(style + FX_FONT_SIZE + size + PX);
            if ( dbg)  logger.log("new_style1->" + style + FX_FONT_SIZE + size + PX + "<-");
            return;
        }
        if ( dbg) logger.log("Node style->" + style + "<-");
        int index = style.indexOf(FX_FONT_SIZE);

        if (index < 0) {
            // font not in stlye yet, so we add it
            StringBuilder sb = new StringBuilder();
            sb.append(style).append(FX_FONT_SIZE).append(size).append(PX);
            mi.setStyle(sb.toString());
            //node.setStyle(style + FX_FONT_SIZE + size + PX);
            if ( dbg)  logger.log("new_style1->" + style + FX_FONT_SIZE + size + PX + "<-");
            return;
        }
        index += (FX_FONT_SIZE).length();
        int index2 = style.indexOf(PX, index);
        index2 += PX_LENGTH;

        String new_style = style.substring(0, index);
        StringBuilder sb = new StringBuilder();
        sb.append(new_style).append(size).append(PX).append(style.substring(index2));
        mi.setStyle(sb.toString());

        if( dbg) logger.log("new_style2->" + new_style + "<-");

         */
    }


    /*
    //**********************************************************
    public static void apply_font_size(Window owner, Logger logger)
    //**********************************************************
    {
        double size = Non_booleans_properties.get_font_size(owner,logger);
        if (dbg) logger.log("applying font size " + size);
        apply_font_size(size,owner,logger);
    }
*/
    //**********************************************************
    public static void apply_font_size(PopupControl popup_control, Window owner, Logger logger)
    //**********************************************************
    {
        double size = Non_booleans_properties.get_font_size(owner,logger);
        String style = popup_control.getStyle();
        if ( style.isEmpty() || style.endsWith(";"))
        {
            // shortcut!
            StringBuilder sb = new StringBuilder();
            sb.append(style).append(FX_FONT_SIZE).append(size).append(PX);
            popup_control.setStyle(sb.toString());
            //node.setStyle(style + FX_FONT_SIZE + size + PX);
            if ( dbg)  logger.log("new_style1->" + style + FX_FONT_SIZE + size + PX + "<-");
            return;
        }
        if ( dbg) logger.log("PopupControl style->" + style + "<-");
        int index = style.indexOf(FX_FONT_SIZE);

        if (index < 0)
        {
            // font not in style yet, so we add it
            StringBuilder sb = new StringBuilder();
            sb.append(style).append(FX_FONT_SIZE).append(size).append(PX);
            popup_control.setStyle(sb.toString());
            if ( dbg)  logger.log("new_style1->" + sb.toString() + "<-");
            return;
        }
        index += (FX_FONT_SIZE).length();

        int index2 = style.indexOf(PX, index);
        index2 += PX_LENGTH;

        String new_style = style.substring(0, index);
        StringBuilder sb = new StringBuilder();
        sb.append(new_style).append(size).append(PX).append(style.substring(index2));
        popup_control.setStyle(sb.toString());

        if( dbg) logger.log("new_style2->" + sb.toString() + "<-");
    }
}
