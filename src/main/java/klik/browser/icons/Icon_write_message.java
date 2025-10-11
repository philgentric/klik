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
	//public final Path original_path;
	public final String tag;
    public final int icon_size;
    public final String extension;
	public final Aborter aborter;


	//**********************************************************
    public Icon_write_message(Image smaller, int icon_size_, String extension_,
							  String tag_,
							  Aborter aborter)
	//**********************************************************
	{
		image = smaller;
		icon_size = icon_size_;
		tag = tag_;
		extension = extension_;
		this.aborter = aborter;
	}

	//**********************************************************
	@Override
    public String to_string()
	//**********************************************************
	{
        return "Icon_write_message for"+tag;
    }

	@Override
	public Aborter get_aborter() {
		return aborter;
	}


}
