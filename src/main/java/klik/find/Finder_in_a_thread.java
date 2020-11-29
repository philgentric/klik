package klik.find;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import klik.images.Image_context;
import klik.images.Image_stage;
import klik.properties.Properties;
import klik.util.Logger;
import klik.util.Tool_box;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

//**********************************************************
public class Finder_in_a_thread implements Callback_for_image_found_publish
//**********************************************************
{
	public static final boolean dbg = false;
	private static final int MAX_MENU_ITEMS = 23;
	//private List<String> exclusion_list;
	private Logger logger;
	final Path target;
	HashMap<String, Set<Path>> recorded_similar_files = new HashMap<>();
	int displayed = 0;
	final Pane pane;
	final Stage from_stage;
	Finder_popup popup = null;
	Finder finder;


	//**********************************************************
	public Finder_in_a_thread(
			Path target_,
			Stage from_stage_,
			Pane pane_,
			Logger logger_
			)
	//**********************************************************
	{
		target = target_;
		logger = logger_;
		from_stage = from_stage_;
		pane = pane_;
	}

	// find similar images
	// given is the set of strings used to find files by name
	// if null, that set will be built from the file name
	//**********************************************************
	public void find_image_files_from_keywords(Set<String> given_keywords, Logger logger) 
	//**********************************************************
	{

		logger.log("1 extract_keywords_from_name_and_find find_similar given_keywords == null");


		List<String> exclusion_list =  Finder.load_keyword_exclusion_list(logger);

		if ( given_keywords == null)
		{
			Keyword_extractor ke = new Keyword_extractor(logger, exclusion_list);
			given_keywords = ke.extract_keywords_from_file_and_dir_names(target);
		}
		find_image_files_from_keywords_internal(given_keywords, logger );

	}

	//**********************************************************
	private void find_image_files_from_keywords_internal(Set<String> given_keywords, Logger logger) 
	//**********************************************************
	{
		recorded_similar_files.clear();
		if ( given_keywords == null)
		{
			logger.log("FATAL no keywords ??? ");
			return;
		}
		logger.log("---- looking at keywords -------");
		for ( String s : given_keywords)
		{
			logger.log("->"+s+"<-");

		}
		logger.log("--------------------------------");			

		List<String> given_keywords_list = new ArrayList<>(given_keywords);
		popup = new Finder_popup(
				"Please wait... looking at keywords:", 
				given_keywords_list,
				500,
				500, logger);


		Callback_for_image_found_publish this_as_callback = this;

		Thread thread  = new Thread(new Runnable()
		{	
			@Override
			public void run() {
				launch_a_Finder(given_keywords, this_as_callback,logger);
			}
		});
		thread.start();
	}
	//**********************************************************
	private void launch_a_Finder(
			Set<String> given_keywords, 
			Callback_for_image_found_publish this_as_callback,
			Logger logger)
	//**********************************************************
	{

		finder= new Finder(target, given_keywords,  true,logger);
		recorded_similar_files = finder.get_similar_files(this_as_callback);
		//if ( dbg)
		{
			int size = 0;
			for ( Entry <String, Set<Path>> e : recorded_similar_files.entrySet())
			{
				Set<Path> set = e.getValue();
				size += set.size();
			}
			logger.log(" results number of Paths="+size);
		}
	}

	//**********************************************************
	@Override
	public void add_one_Search_result(Search_result sr)
	//**********************************************************
	{
		Set<Path> set = recorded_similar_files.get(sr.keyword);
		if ( set == null)
		{
			set = new TreeSet<Path>();
			recorded_similar_files.put(sr.keyword, set);
		}
		set.add(sr.file);
		popup.ping();
	}	



	//**********************************************************
	@Override
	public void update_display_in_FX_thread() 
	//**********************************************************
	{

		Platform.runLater(new Runnable() 
		{	
			@Override
			public void run() 
			{
				show_similars(recorded_similar_files);
			}
		});
	}


	//**********************************************************
	private void show_similars(HashMap<String, Set<Path>> similars)
	//**********************************************************
	{
		final ContextMenu contextMenu = new ContextMenu();
		for( Entry<String, Set<Path>> e : similars.entrySet())
		{
			String name = e.getKey();
			Set<Path> files = e.getValue();
			String found = "keyword:" +name+", "+files.size()+" items";
			displayed = 0;
			Menu one_item = create_one_menu_item_for_keyword(found, files);
			contextMenu.getItems().add(one_item);
			if ( dbg) logger.log("items before: "+files.size()+" files,  displayed="+displayed);
		}
		contextMenu.show(pane, 20, 20);
	}

	//**********************************************************
	private Menu create_one_menu_item_for_keyword(String found, Set<Path> files)
	//**********************************************************
	{
		Menu one_item = new Menu(found);
		Iterator<Path> it = files.iterator();
		int remaining = files.size();
		if ( files.size() <= MAX_MENU_ITEMS)
		{
			remaining -= create_menu_items_for(one_item, it);
			return one_item;
		}
		if ( files.size() > MAX_MENU_ITEMS*MAX_MENU_ITEMS)
		{
			int max_level = 1;
			int possible = MAX_MENU_ITEMS;
			for(;max_level < 12; max_level++)
			{
				possible *= MAX_MENU_ITEMS;
				if ( possible >= remaining) break;
				max_level++;
			}
			max_level--;
			logger.log("possible="+possible);
			logger.log("max_level="+max_level);
			remaining -= create_menu_items_recursive(one_item, it, remaining, max_level);
			return one_item;
		}
		for ( int k = 0; k< MAX_MENU_ITEMS; k++)
		{
			Menu sub_menu = new Menu("part"+k);
			remaining -= create_menu_items_for(sub_menu, it);
			one_item.getItems().add(sub_menu );
			if ( remaining <= 0) break;				
		}
		return one_item;

	}


	//**********************************************************
	private int create_menu_items_recursive(
			Menu one_item,
			Iterator<Path> it,
			int remaining_, 
			int max_level) 
	//**********************************************************
	{
		int local_remaining = remaining_;
		if (local_remaining <= 0) return 0;

		max_level--;
		if ( max_level == 0)
		{
			//logger.log("NO recurse because max_level = 0, remaining="+local_remaining);
			int done = create_menu_items_for(one_item, it);
			return done;
		}

		if ( local_remaining <=  MAX_MENU_ITEMS)
		{
			//logger.log("NO recurse, remaining ="+local_remaining);
			int done = create_menu_items_for(one_item, it);
			return done;
		}
		logger.log("recurse at level "+max_level+" for remaining = "+local_remaining);
		int done = 0;
		for ( int i = 0 ; i < MAX_MENU_ITEMS ; i++)
		{
			//String text = "subpart "+i+" remaining:"+local_remaining;
			String text = ""+displayed;
			Menu one_sub_item = new Menu(text);
			int done_local = create_menu_items_recursive(one_sub_item,it,local_remaining, max_level);
			if ( done_local == 0) break;
			local_remaining -= done_local;
			//text += " after="+local_remaining;
			text += "-"+displayed;
			one_sub_item.setText(text);
			one_item.getItems().add(one_sub_item);
			done += done_local;
		}	
		return done;
	}

	//**********************************************************
	private int create_menu_items_for(Menu one_item, Iterator<Path> it)//, int remaining_)
	//**********************************************************
	{
		int local =  0;
		//logger.log(name)
		for ( int i = 0 ; i < MAX_MENU_ITEMS ; i++)
		{
			if ( it.hasNext() == false) break;
			Path f = it.next();
			//String p = f.getAbsolutePath()+ " "+local;
			String p = f.getFileName().toString();
			MenuItem one_sub_item = new MenuItem(p);
			local++;
			displayed++;
			one_item.getItems().add(one_sub_item);
			one_sub_item.setOnAction(new EventHandler<ActionEvent>() 
			{


				@Override public void handle(ActionEvent e) 
				{
					logger.log("going to open on menu select: "+p);
					Image_stage is = Image_stage.get_Image_stage(from_stage,f,  true,logger);
				}
			});
		}	
		return local;
	}


	public void shutdown()
	{
		logger.log("asking findor to shutdown");
		if (finder !=null) finder.please_stop = true;
		if ( popup != null) popup.stage.close();
	}
}
