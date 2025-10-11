package klik.browser.icons;

import klik.actor.Actor;
import klik.actor.Actor_engine;
import klik.actor.Message;
import klik.util.image.Static_image_utilities;
import klik.util.log.Logger;

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
	// dbg_names is super useful to debug this feature BUT it has a major caveat:
	// folders with [whaztever] in the name will not have an animated icon
	private static final boolean dbg_names = false;

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
	public static String make_cache_name(String tag, String icon_size_tag, String extension)
	//**********************************************************
	{
		if ( tag == null) return null;
		StringBuilder sb = new StringBuilder();
		sb.append(make_cache_name_raw(tag));
		sb.append("_");
		sb.append(icon_size_tag);
		sb.append(".");
		sb.append(extension);
		return sb.toString();
//		return clean_name(full_name) + "_"+tag + "."+extension;
	}
	//**********************************************************
	public static String make_cache_name_raw(String tag)
	//**********************************************************
	{
		if ( tag == null) return null;

		StringBuilder sb = new StringBuilder();
		if ( dbg_names)
		{
			sb.append(clean_name(tag));
		}
		else
		{
			sb.append(UUID.nameUUIDFromBytes(tag.getBytes())); // the name is always the same length and is obfuscated
		}
		return sb.toString();
//		return clean_name(full_name) + "_"+tag + "."+extension;
	}



	//**********************************************************
	public static String clean_name(String s)
	//**********************************************************
	{
		s = s.replace("/", "_");
		s = s.replace(".", "_");
		s = s.replace("\\[", "_");
		s = s.replace("]", "_");
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

    //**********************************************************
    public void write_icon_to_cache_on_disk(Icon_write_message iwm)
    //**********************************************************
    {
        File out_file = new File(cache_dir.toFile(),
                make_cache_name(iwm.tag,String.valueOf(iwm.icon_size), iwm.extension));
        Static_image_utilities.write_png_to_disk(iwm.image, out_file, iwm.tag, logger);
    }


    /*
    old way to do it: involved using AWT/SWING components
    which does nt work with gluonfx native,
    replaced with pure java png : ar.com.hjg.pngj



    //**********************************************************
    public void write_icon_to_cache_on_disk(Icon_write_message iwm)
    //**********************************************************
    {
		try
		{
			BufferedImage bim = JavaFX_to_Swing.fromFXImage(iwm.image, null, logger);
			if ( bim == null)
			{
				logger.log("write_icon_to_cache_on_disk JavaFX_to_Swing.fromFXImage failed for "+iwm.tag);
				return;
			}
			if (iwm.get_aborter().should_abort()) return;
			boolean status = ImageIO.write(bim, "png", new File(
					cache_dir.toFile(),
					make_cache_name(iwm.tag,String.valueOf(iwm.icon_size), iwm.extension))
			);

			if ( !status )
			{
				logger.log("Icon_writer: ImageIO.write returns false for: "+iwm.tag);
			}
		}
		catch(Exception e)
		{
			logger.log("Icon_writer exception (1) ="+e+" path="+iwm.tag);
		}
		//logger.log("Icon_writer OK for path="+iwm.original_path+" png done");
	}
*/
}
