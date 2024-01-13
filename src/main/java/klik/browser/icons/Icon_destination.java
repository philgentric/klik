package klik.browser.icons;

import javafx.scene.image.Image;
import klik.browser.Image_and_rotation;
import klik.browser.items.Iconifiable_item_type;

import java.nio.file.Path;

public interface Icon_destination
{

    void receive_icon(Image_and_rotation icon);

    Icon_status get_icon_status();

    void set_icon_status(Icon_status s);

    Iconifiable_item_type get_item_type();

    Path get_path_for_display();

    String get_string();

    Path get_item_path();

}
