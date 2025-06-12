package klik.look.my_i18n;

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

    //**********************************************************
    public Locale get_locale()
    //**********************************************************
    {
        switch (this) {
            case Breton:
                return Locale.of("br","FR");
            case Chinese:
                return Locale.of("zh","CN");
            default:
            case English:
                return Locale.of("en","US");
            case French:
                return Locale.of("fr","FR");
            case German:
                return Locale.of("de","DE");
            case Italian:
                return Locale.of("it","IT");
            case Japanese:
                return Locale.of("ja","JP");
            case Korean:
                return Locale.of("ko","KR");
            case Portuguese:
                return Locale.of("pt","PT");
            case Spanish:
                return Locale.of("es","ES");
        }

    }
}
