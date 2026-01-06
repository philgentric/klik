// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.look.my_i18n;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klikr.look.Jar_utils;
import klikr.util.log.Logger;

import java.util.Locale;

//**********************************************************
public enum Language
//**********************************************************
{
    Breton,
    Chinese,
    English,
    French,
    German,
    Italian,
    Japanese,
    Korean,
    Portuguese,
    Spanish;

    public Image get_icon(Window owner, Logger logger)
    {
        Image icon = Jar_utils.load_jfx_image_from_jar("icons/"+this.name()+".png", 64, owner,logger);
        return icon;
    }
    //**********************************************************
    public Locale get_locale()
    //**********************************************************
    {
        switch (this) {
            case Breton:
                //return Locale.of("br","FR");
                return new Locale("br","FR");
            case Chinese:
                return new Locale("zh","CN");
            default:
            case English:
                return new Locale("en","US");
            case French:
                return new Locale("fr","FR");
            case German:
                return new Locale("de","DE");
            case Italian:
                return new Locale("it","IT");
            case Japanese:
                return new Locale("ja","JP");
            case Korean:
                return new Locale("ko","KR");
            case Portuguese:
                return new Locale("pt","PT");
            case Spanish:
                return new Locale("es","ES");
        }

    }
}
