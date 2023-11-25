package klik.browser.icons;

import javafx.scene.image.Image;
import klik.actor.Aborter;
import klik.actor.Message;

import java.nio.file.Path;

//**********************************************************
public class Icon_write_message implements Message
//**********************************************************
{
	public final Image image;
	public final Path original_path;
    public final double icon_size;
    public final String extension;
	public final Aborter aborter;


	//**********************************************************
    public Icon_write_message(Image smaller, double icon_size_, String extension_, Path p_)
	//**********************************************************
	{
		image = smaller;
		icon_size = icon_size_;
		original_path = p_;
		extension = extension_;
		aborter = new Aborter();
	}

	//**********************************************************
	@Override
    public String to_string()
	//**********************************************************
	{
        return "Icon_write_message for"+original_path;
    }

	@Override
	public Aborter get_aborter() {
		return aborter;
	}
}
