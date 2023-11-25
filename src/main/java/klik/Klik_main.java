package klik;

//**********************************************************
public class Klik_main
//**********************************************************
{
    //**********************************************************
    public static void main(String[] args)
    {
        System.setProperty("apple.laf.useScreenMenuBar", "true");

// set the name of the application menu item
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "AppName");

// set the look and feel

        Klik_application app = new Klik_application();
        app.launch(args);
    }
}
