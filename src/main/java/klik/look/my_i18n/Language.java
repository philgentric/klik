package klik.look.my_i18n;

import java.util.Locale;

public class Language
{
    public final Locale locale;

    public Language(Locale l) {
        locale = l;
        //print_all();
    }

    public void print_all()
    {
        System.out.println("language 2 letter code="+locale.getLanguage());
        System.out.println("display-language="+locale.getDisplayLanguage(Locale.ENGLISH));
        System.out.println("country 2 letter code="+locale.getCountry());
        System.out.println("display-country="+locale.getDisplayCountry(Locale.ENGLISH));
        System.out.println("display-name (display-language + display-country)="+locale.getDisplayName());
        System.out.println("\n");
    }
    public String language()
    {
        return locale.getLanguage();
    }
    public String country()
    {
        return locale.getCountry();
    }
    public String language_key()
    {
        return locale.getDisplayLanguage(Locale.ENGLISH);
    }
}
