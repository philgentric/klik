package klik.util;

import javafx.application.HostServices;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import klik.Shared_services;


//**********************************************************
public class Github_stars
//**********************************************************
{
    private static HostServices host_services;

    //**********************************************************
    public static void init(HostServices hs)
    //**********************************************************
    {
        host_services =  hs;
    }
    //**********************************************************
    public static void ask_for_github_star()
    //**********************************************************
    {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Enjoying this app?");
        alert.setHeaderText("Star us on GitHub!");
        alert.setContentText("If you find this app useful, please consider giving us a star on GitHub. It helps others discover the project!");

        ButtonType starButton = new ButtonType("⭐ Star on GitHub");
        ButtonType laterButton = new ButtonType("Maybe Later");

        alert.getButtonTypes().setAll(starButton, laterButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == starButton) {
                open_github();
            }
        });
    }

    //**********************************************************
    private static void open_github()
    //**********************************************************
    {
        try {
            if ( host_services != null)
            {
                host_services.showDocument("https://github.com/philgentric/klik");
                Shared_services.main_properties().set("GITHUB_STAR_ASK_DONE","true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
