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
	private HashMap<String, List<Path>> search_results;// = new HashMap<>(); // the key is a string composed of the concatenated keywords
	Logger logger;
	Search_status status = Search_status.undefined;
	Search_config search_config;
	private final Aborter local_aborter;
	private final Search_receiver search_receiver;
	private final Browser the_browser;
	private final Results_frame find_result_frame;


	//**********************************************************
	public Search_session(Search_config search_config, Browser browser, Search_receiver search_receiver, Logger logger_)
	//**********************************************************
	{
		logger = logger_;
		local_aborter = new Aborter("Search_session",logger);
		status = Search_status.ready;
		this.search_config = search_config;
		this.search_receiver = search_receiver;
		this.the_browser = browser;
		this.find_result_frame = new Results_frame(browser, search_results, this, logger);
	}

	//**********************************************************
	void start_search()
	//**********************************************************
	{
		status = Search_status.searching;
		if ( dbg) logger.log("launching search actor on path:"+search_config.path());
		Actor_engine.run(new Finder_actor(logger),new Finder_message(search_config,this,local_aborter, the_browser),null,logger);
	}

	//**********************************************************
	void stop_search()
	//**********************************************************
	{
		if ( dbg) logger.log("stop_search()");
		local_aborter.abort();
		status = Search_status.interrupted;
	}

	//**********************************************************
	private static Comparator<? super String> keyword_comparator_no_case = new Comparator<String>()
	{
		@Override
		public int compare(String o1, String o2) {
			return o1.toLowerCase().compareTo(o2.toLowerCase());
		}
	};

	//**********************************************************
	private static Comparator<? super String> keyword_comparator_with_case = new Comparator<String>()
	{
		@Override
		public int compare(String o1, String o2) {
			return o1.compareTo(o2);
		}
	};

	//**********************************************************
	public String get_max_key()
	//**********************************************************
	{
		return list_of_keywords_to_key(search_config.keywords(),search_config.check_case());
	}

	//**********************************************************
	private static String list_of_keywords_to_key(List<String> keywords, boolean check_case)
	//**********************************************************
	{
		Comparator<? super String> comparator;
		if ( check_case)
		{
			comparator = keyword_comparator_with_case;
		}
		else
		{
			comparator = keyword_comparator_no_case;
		}
		keywords.sort(comparator);
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
		search_receiver.receive_intermediary_statistics(st);
		if (sr == null)  return;
		if ( sr.matched_keywords().isEmpty())
		{
			if( search_config.extension() != null)
			{
				// this is a search by extension
				List<String> ll = new ArrayList<>();
				ll.add(search_config.extension());
				String key = list_of_keywords_to_key(ll,false);
				if ( search_results != null)
				{
					List<Path> list = search_results.get(key);
					if ( list == null) list = new ArrayList<>();
					if (!list.contains(sr.path())) list.add(sr.path());
					search_results.put(key,list);
				}
			}
		}
		else
		{
			if (dbg)
				logger.log("Search_session add_one_Search_result matched keyword: " + sr.matched_keywords() + " =>" + sr.path());
			String keys = list_of_keywords_to_key(sr.matched_keywords(),search_config.check_case());
			if ( search_results != null)
			{
				List<Path> list = search_results.get(keys);
				if ( list == null) list = new ArrayList<>();
				if (!list.contains(sr.path())) list.add(sr.path());
				search_results.put(keys,list);
			}
			if ( find_result_frame != null)
			{
				boolean is_max = keys.equals(get_max_key());
				find_result_frame.inject_search_results(sr,keys, is_max, the_browser);
			}
		}


	}


	//**********************************************************
	@Override // Callback_for_file_found_publish
	public void has_ended(Search_status search_status)
	//**********************************************************
	{
		if ( dbg) logger.log("Search_session has_ended() called: "+search_status );
		search_receiver.has_ended(search_status);
		if ( find_result_frame != null)
		{
			find_result_frame.has_ended();
		}
	}

	private void ready()
	{
		status = Search_status.ready;
	}


	public HashMap<String, List<Path>> get_search_results() {
		return search_results;
	}
}
