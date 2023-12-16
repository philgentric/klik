package klik.browser.icons;

import klik.actor.Actor;
import klik.actor.Actor_engine;
import klik.actor.Message;
import klik.util.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

/*
 * this actor accepts icons and writes them to the disk cache
 */

//**********************************************************
public class Icon_writer_actor implements Actor
//**********************************************************
{
	private static final boolean dbg = false;
	Path cache_dir;
	Logger logger;
	//**********************************************************
	public Icon_writer_actor(Path cache_dir_, Logger l)
	//**********************************************************
	{
		logger = l;
		if ( dbg) logger.log("Icon_writer_actor created");
		cache_dir = cache_dir_;
	}

	//**********************************************************
	public void push(Icon_write_message ii)
	//**********************************************************
	{
		Actor_engine.run(this, ii, null,logger);
	}


	//**********************************************************
	private void write_icon_to_cache_on_disk(Icon_write_message iwm)
	//**********************************************************
	{
		try
		{
			//	BufferedImage bim = SwingFXUtils.fromFXImage(iwm.image, null);
			BufferedImage bim = JavaFX_to_Swing.fromFXImage(iwm.image, null, logger);
			if ( bim == null)
			{
				logger.log("write_icon_to_cache_on_disk JavaFX_to_Swing.fromFXImage failed for "+iwm.original_path);
				return;
			}
			if (iwm.get_aborter().should_abort()) return;
			boolean status = ImageIO.write(bim, "png", new File(
					cache_dir.toFile(),
					make_cache_name(iwm.original_path,String.valueOf(iwm.icon_size), iwm.extension))
			);

			if ( !status )
			{
				logger.log("Icon_writer: ImageIO.write returns false for: "+iwm.original_path);
			}
		}
		catch(Exception e)
		{
			logger.log("Icon_writer exception (1) ="+e+" path="+iwm.original_path);
		}
		//logger.log("Icon_writer OK for path="+iwm.original_path+" png done");
	}


	//**********************************************************
	public static String make_cache_name(Path path, String tag, String extension)
	//**********************************************************
	{
		if ( path == null) return null;
		String full_name = path.toAbsolutePath().toString();
		StringBuilder sb = new StringBuilder();
		//sb.append(clean_name(full_name));
		sb.append(UUID.nameUUIDFromBytes(full_name.getBytes())); // the name is always the same length
		sb.append("_");
		sb.append(tag);
		sb.append(".");
		sb.append(extension);
		return sb.toString();
//		return clean_name(full_name) + "_"+tag + "."+extension;
	}


	//**********************************************************
	public static String clean_name(String s)
	//**********************************************************
	{
		s = s.replace("/", "_");
		s = s.replace(".", "_");
		//s = s.replace(" ", "_"); this is a bug: files named "xxx_yyy" and "xxx yyy" get the same icon!, sometimes no icon e.g. pdf
		return s;
	}

	//**********************************************************
	@Override
	public String run(Message m)
	//**********************************************************
	{
		Icon_write_message mm = (Icon_write_message) m;

		write_icon_to_cache_on_disk(mm);
		return "icon written";
	}


}
