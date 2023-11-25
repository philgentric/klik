package klik.look;

import javafx.scene.Node;
import klik.properties.Static_application_properties;
import klik.util.Logger;

import java.util.List;

public class Font_name {

    List<String> get_all_font_names() {
        return javafx.scene.text.Font.getFamilies();
    }

    public static void print_all_font_families() {
        System.out.println("*********FONT FAMILIES***********");
        for (String ff : javafx.scene.text.Font.getFamilies())
        {
            System.out.println(ff);
        }
        System.out.println("*********FONT NAMES***********");
        for (String ff : javafx.scene.text.Font.getFontNames())
        {
            System.out.println(ff);
        }
    }


    public static final String FX_FONT_SIZE = "-fx-font-size:";
    public static final String PX = "px;";

    private final static boolean dbg = false;

    public static void set_font_style(Node x, Logger logger) {
        x.setStyle(FX_FONT_SIZE + Static_application_properties.get_font_size(logger) + PX);
    }

    public static void apply_font_size(Node x, Logger logger) {
        double size = Static_application_properties.get_font_size(logger);
        if (dbg) System.out.println("applying font size " + size);

        String style = x.getStyle();
        if (dbg) System.out.println("style->" + style + "<-");
        int index = style.indexOf(FX_FONT_SIZE);

        if (index < 0) {
            x.setStyle(style + FX_FONT_SIZE + size + PX);
            if (dbg) System.out.println("new_style1->" + style + FX_FONT_SIZE + size + PX + "<-");
            return;
        }
        index += (FX_FONT_SIZE).length();

        int index2 = style.indexOf(PX, index);
        index2 += ("px;").length();

        String new_style = style.substring(0, index);
        new_style += "" + size + PX;
        new_style += style.substring(index2);
        x.setStyle(new_style);

        if (dbg) System.out.println("new_style2->" + new_style + "<-");
    }
}
