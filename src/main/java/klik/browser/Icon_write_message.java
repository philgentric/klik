package klik.browser;

import javafx.scene.image.Image;

import java.nio.file.Path;

public class Icon_write_message 
{
	public final Image image;
	public final Path original_path;
    public final double icon_size;

    public Icon_write_message(Image smaller, double icon_size_, Path p_)
	{
		image = smaller;
		icon_size = icon_size_;
		original_path = p_;
	}

}
