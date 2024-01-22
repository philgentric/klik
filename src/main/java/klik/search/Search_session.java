package klik.search;

import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Browser;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.*;

//import javafx.scene.web.WebView;


//**********************************************************
public class Search_session implements Callback_for_file_found_publish
//**********************************************************
{
	private static final boolean dbg = false;
	private HashMap<String, List<Path>> search_results = new HashMap<>(); // the key is a string composed of the concatenated keywords
	private final List<String> keywords;
	Logger logger;
	Search_status status = Search_status.undefined;
	//private Job the_job;
	private final boolean look_only_for_images;
	private final Aborter aborter;
	private final Search_receiver search_receiver;
	private final Browser browser;
	private final Path target_path;
	private final String extension;
	//private Map<String, List<String>> key_to_keywords = new HashMap<>();


	//**********************************************************
	public Search_session(Path target_path, List<String> keywords, boolean look_only_for_images, String local_extension, Browser browser, Search_receiver search_receiver, Logger logger_)
	//**********************************************************
	{
		logger = logger_;
		aborter = new Aborter();
		status = Search_status.ready;
		this.keywords = keywords;
		this.browser = browser;
		this.target_path = target_path;
		this.search_receiver = search_receiver;
		this.look_only_for_images = look_only_for_images;
		this.extension = local_extension;
	}

	//**********************************************************
	void start_search()
	//**********************************************************
	{
		status = Search_status.searching;
		if ( dbg) logger.log("launching search actor on path:"+target_path);
		Actor_engine.run(new Finder_actor(logger),new Finder_message(target_path, keywords, look_only_for_images, extension,this,aborter,browser),null,logger);
	}

	//**********************************************************
	void stop_search()
	//**********************************************************
	{
		if ( dbg) logger.log("stop_search()");
		aborter.abort();
		status = Search_status.stopping;
	}



	//**********************************************************
	private static Comparator<? super String> keyword_comparator = new Comparator<String>()
	{
		@Override
		public int compare(String o1, String o2) {
			return o1.toLowerCase().compareTo(o2.toLowerCase());
		}
	};

	//**********************************************************
	public String get_max_key()
	//**********************************************************
	{
		return list_of_keywords_to_key(keywords);
	}
	//**********************************************************
	private static String list_of_keywords_to_key(List<String> keywords)
	//**********************************************************
	{
		keywords.sort(keyword_comparator);
		StringBuilder sb = new StringBuilder();
		for ( String s : keywords)
		{
			sb.append(s);
			sb.append(" ");// for human readibility
		}

		String key = sb.toString();
		//key_to_keywords.put(key,keywords);
		return key;
	}
/*
	//**********************************************************
	public List<String> key_to_keywords(String key)
	//**********************************************************
	{
		return key_to_keywords.get(key);
	}

 */
	//**********************************************************
	@Override // Callback_for_file_found_publish
	public void on_the_fly_stats(Search_result sr, Search_statistics st)
	//**********************************************************
	{
		if ( ! sr.matched_keywords().isEmpty())
		{
			if (dbg)
				logger.log("Search_session add_one_Search_result matched keyword: " + sr.matched_keywords() + " =>" + sr.path());
			String key = list_of_keywords_to_key(sr.matched_keywords());
			List<Path> list = search_results.get(key);
			if ( list == null) list = new ArrayList<>();
			if (!list.contains(sr.path())) list.add(sr.path());
			search_results.put(key,list);
		}
		search_receiver.receive_intermediary(st);

	}


	//**********************************************************
	@Override // Callback_for_file_found_publish
	public void has_ended(Search_status search_status, String message)
	//**********************************************************
	{
		if ( dbg) logger.log("Search_session has_ended() called: "+search_status+" "+message );
		search_receiver.has_ended(search_status, message);
	}

	private void ready()
	{
		status = Search_status.ready;
	}


	public HashMap<String, List<Path>> get_search_results() {
		return search_results;
	}
}
