package klik.properties;

import klik.browser.Browser;
import klik.look.Look_and_feel;
import klik.look.Look_and_feel_manager;
import klik.look.styles.Look_and_feel_light;
import klik.util.Logger;

//**********************************************************
public class Style
//**********************************************************
{
    public static final String STYLE_KEYWORD = "STYLE";

    //**********************************************************
    public static Look_and_feel read_look_and_feel_from_properties_file(Logger logger)
    //**********************************************************
    {
        Look_and_feel look_and_feel = null;
        String style_s = Static_application_properties.get_properties_manager(logger).get(STYLE_KEYWORD);
        if (style_s == null)
        {
            // DEFAULT STYLE, first time klik is launched on the platform
            look_and_feel = new Look_and_feel_light(logger);
        } else {
            for (Look_and_feel laf : Look_and_feel_manager.registered) {
                if (laf.name.equals(style_s)) {
                    look_and_feel = laf;
                    break;
                }
            }
        }
        if (look_and_feel == null) {
            look_and_feel = new Look_and_feel_light(logger);
        }
        Static_application_properties.get_properties_manager(logger).save_unico(STYLE_KEYWORD, look_and_feel.name,false);
        return look_and_feel;
    }

    //**********************************************************
    public static void set_style(Look_and_feel style,Logger logger)
    //**********************************************************
    {
        Static_application_properties.get_properties_manager(logger).save_unico(STYLE_KEYWORD, style.name,false);
    }


}
