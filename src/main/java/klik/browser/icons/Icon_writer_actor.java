package klik.browser.icons;

import javafx.stage.Window;
import klik.util.execute.actor.Actor;
import klik.util.execute.actor.Actor_engine;
import klik.util.execute.actor.Message;
import klik.util.image.Static_image_utilities;
import klik.util.image.icon_cache.Icon_caching;
import klik.util.log.Logger;

import java.nio.file.Path;


/*
 * this actor accepts icons and writes them to the disk cache
 */

//**********************************************************
public class Icon_writer_actor implements Actor
//**********************************************************
{
	private static final boolean dbg = false;
	// dbg_names is super useful to debug this feature BUT it has a major caveat:
	// folders with [whatever] in the name will not have an animated icon

	Path cache_dir;
	private final Logger logger;
    private final Window owner;
	//**********************************************************
	public Icon_writer_actor(Path cache_dir_, Window owner, Logger logger)
	//**********************************************************
	{
		this.logger = logger;
        this.owner = owner;
		if ( dbg) logger.log("Icon_writer_actor created");
		cache_dir = cache_dir_;
	}


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Icon_writer_actor";
    }


    //**********************************************************
	public void push(Icon_write_message ii)
	//**********************************************************
	{
		Actor_engine.run(this, ii, null,logger);
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
        Path out_path = Icon_caching.path_for_icon_caching(iwm.absolute_path(),String.valueOf(iwm.icon_size()),Icon_caching.png_extension,owner,logger);
        Static_image_utilities.write_png_to_disk(iwm.image(), out_path, logger);
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
