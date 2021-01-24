package klik.find;

import klik.properties.Properties;
import klik.properties.Properties_manager;
import klik.util.Guess_file_type_from_extension;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


// finds similar files
//**********************************************************
public class Finder
//**********************************************************
{
	private static final boolean dbg = false;
	private static boolean go_up = false;
	private static Logger logger; // can be null (no debug)
	//Image_context ic;
	Path target_file;
	Set<String> keywords = null;//new TreeSet<>();
	public volatile boolean please_stop = false;

	// It looks for images in the "area" of the target image
	// "area" means:
	// - its directory, and directories DOWN
	// - and then optionally going up
	// and down in each sub directory (divergence!) 
	// the results are held in the image context
	
	//**********************************************************
	public Finder(
			Path target_file_,
			Set<String> given,
			boolean also_go_up,
			Logger logger_)
	//**********************************************************
	{
		target_file = target_file_;
		keywords = given;
		logger = logger_;
		go_up = also_go_up;
	}

	//**********************************************************
	public HashMap <String,Set<Path>> get_similar_files(Callback_for_image_found_publish callback)
	//**********************************************************
	{
		HashMap <String,Set<Path>> search_results = new HashMap<>();

		print_keywords();

		if ( keywords.isEmpty()) return search_results; // empty

		find_similar_files(callback, search_results, keywords);


		callback.update_display_in_FX_thread();
		return search_results;
	}

	//**********************************************************
	private void find_similar_files(
			Callback_for_image_found_publish callback_for_image_found_publish, 
			HashMap<String, Set<Path>> returned, 
			Set<String> local_keywords)
	//**********************************************************
	{
		logger.log("find_similar_files()");
		// first look into the directory of the current file
		Path dir = target_file.getParent();
		extract_dir(local_keywords, returned, dir, callback_for_image_found_publish);

		Path top = Paths.get(System.getProperty("user.home"));//(new File (System.getProperty("user.home"))).toPath();

		if ( go_up == false) return;
		if ( dir.toAbsolutePath().toString().equals(top.toAbsolutePath().toString()) )
		{
			return;
		}

			// then look directories up the hierarchy
		Path up = dir;
		for(;;) 
		{
			up = up.getParent();
			if ( up.toAbsolutePath().toString().equals(top.toAbsolutePath().toString()) )
			{
				if ( dbg) logger.log("giving up at top");
				break;
			}
			extract_dir(local_keywords, returned, up, callback_for_image_found_publish);
			if ( please_stop )
			{
				logger.log("finder shutdown");
				return;
			}
		}
	}


	//**********************************************************
	public static int complexity(HashMap<String, Set<File>> returned) 
	//**********************************************************
	{
		return returned.keySet().size();
	}

	//**********************************************************
	private void extract_dir(
			Set<String> local_keywords,
			HashMap<String, Set<Path>> returned, 
			Path dir,
			Callback_for_image_found_publish cfp
			)
	//**********************************************************
	{
		if ( please_stop )
		{
			logger.log("finder shutdown");
			return;
		}
		if ( Files.isDirectory(dir) == false ) return;

		if ( dbg) 
			logger.log("looking into dir:"+dir.toAbsolutePath());

		// we also look for directories
		check_if_file_name_matches_keywords(local_keywords, dir, returned, cfp);


		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))
		{
			for (Path entry : stream)
			{
				if ( please_stop )
				{
					logger.log("findor shutdown");
					return;
				}
				if ( Files.isDirectory(entry))
				{
					if ( dbg) logger.log("diving DOWN for ALL keywords");
					extract_dir(local_keywords, returned, entry, cfp);
					continue;
				}
				check_if_file_name_matches_keywords(local_keywords, entry, returned, cfp);
			}
		} catch (IOException e)
		{
			logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
		}
	}

	//**********************************************************
	private static void check_if_file_name_matches_keywords(
			Set<String> local_keywords,
			Path fff,
			HashMap<String, Set<Path>> returned, 
			Callback_for_image_found_publish cfp) 
	//**********************************************************
	{
		if ( dbg) logger.log("check_if_file_name_matches_keywords() for "+fff.getFileName());
		if ( local_keywords.contains("&"))
		{
			String cumul = "";
			for ( String s : local_keywords)
			{
				if ( s.equals("&")) continue;
				if ( fff.getFileName().toString().toLowerCase().contains(s.toLowerCase()) == false)
				{
					return;
				}
				if ( cumul.isEmpty()) cumul = s;
				else cumul += " & "+s;
			}
			
			process_name_selected(fff, returned, cfp, cumul);	
		}
		else
		{
			if ( dbg) logger.log("TRYING any keywords for "+fff.getFileName());
			for ( String s : local_keywords)
			{
				if ( fff.getFileName().toString().toLowerCase().contains(s.toLowerCase()))
				{
					process_name_selected(fff, returned, cfp, s);	
				}
			}		
			if (local_keywords.size() <= 1) return;
			if ( dbg) logger.log("TRYING all keywords for "+fff.getFileName());
			// look for ALL of them
			String all_s = "";
			for ( String s : local_keywords)
			{
				if ( fff.getFileName().toString().toLowerCase().contains(s.toLowerCase()) == false) return;
				all_s += s+ " ";
			}			
			// all selected !!
			logger.log("all keywords "+all_s+" found for "+fff.getFileName());
			process_name_selected(fff, returned, cfp, all_s);			}

	}

	//**********************************************************
	private static void process_name_selected(
			Path fff, 
			HashMap<String, Set<Path>> returned,
			Callback_for_image_found_publish call_back,
			String keyword) 
	//**********************************************************
	{
		if ( Files.isDirectory(fff))
		{
			if ( dbg) 
				logger.log("keyword is ->"+keyword+"<- matched for DIR "+fff.toAbsolutePath());
			Finder.record_found_dir(returned, call_back, fff, keyword);
		}
		else
		{
			if ( Guess_file_type_from_extension.is_this_path_an_image (fff))
			{
				if ( dbg) 
					logger.log("keyword is ->"+keyword+"<- matched for IMAGE "+fff.toAbsolutePath());
				record_found_file(returned, call_back, fff, keyword);					
			}
		}
	}

	//**********************************************************
	private static void record_found_dir(
			HashMap<String, Set<Path>> returned,
			Callback_for_image_found_publish cfp,
			Path dir,
			String s) 
	//**********************************************************
	{
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))
		{
			for (Path entry : stream)
			{
				if ( Files.isDirectory(entry))
				{
					Finder.record_found_dir(returned, cfp, entry, s);
					continue;
				}
				if ( Guess_file_type_from_extension.is_this_path_an_image(entry) == false) continue;
				record_found_file(returned, cfp, entry, s);
			}
		} catch (IOException e)
		{
			logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
		}
	}

	//**********************************************************
	private static void record_found_file(
			HashMap<String, Set<Path>> returned,
			Callback_for_image_found_publish callback_for_image_found_publish,
			Path fff, String keyword)
	//**********************************************************
	{
		Set<Path> set = returned.get(keyword);
		if ( set == null)
		{
			set = new TreeSet<Path>();
			returned.put(keyword, set);
		}
		set.add(fff);
		if ( dbg) logger.log("found "+fff.toAbsolutePath());
		if ( callback_for_image_found_publish != null)
		{
			callback_for_image_found_publish.add_one_Search_result(new Search_result(fff,keyword));
		}
	}

	//**********************************************************
	private void print_keywords() 
	//**********************************************************
	{
		if ( dbg == false) return;

		logger.log("---Finder keywords------");
		for( String s: keywords)
		{
			logger.log("->"+s+"<-");
		}
		logger.log("------------------------");

	}

	//**********************************************************
	public static List<String> load_keyword_exclusion_list(Logger logger)
	//**********************************************************
	{
		List<String> returned = new ArrayList<>();
		int max = Properties.get_excluded_keyword_list_max_size(logger);
		for (int i = 0; i < max; i++) {
			String key = Properties.EXCLUDED_KEYWORD_PREFIX + i;
			String kw = Properties.get_properties_manager().get(key);
			if (kw != null) {
				String lower = kw.toLowerCase();
				returned.add(lower);
				logger.log("excluded key word: ->" + lower + "<-");
			}
		}
		return returned;
	}



	//**********************************************************
	public static void remove_keywords(List<String> to_be_removeds, Logger logger)
	//**********************************************************
	{
		for (int i = 0; i < Properties.get_excluded_keyword_list_max_size(logger); i++) {
			String key = Properties.EXCLUDED_KEYWORD_PREFIX + i;
			String kw = Properties.get_properties_manager().get(key);
			if (kw == null) continue;
			for (String to_be_removed : to_be_removeds) {
				if (to_be_removed.equals(kw)) {
					Properties.get_properties_manager().remove(key, kw);
					continue;
				}

			}
		}
	}

	//**********************************************************
	public static boolean add_keywords(Properties_manager pm, List<String> to_be_addeds)
	//**********************************************************
	{
		boolean some_saved = false;
		for (String new_excluded : to_be_addeds) {
			if (pm.save_unico(Properties.EXCLUDED_KEYWORD_PREFIX, new_excluded)) {
				some_saved = true;
			}
		}
		return some_saved;

	}
}
