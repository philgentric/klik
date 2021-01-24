package klik.find;

import klik.util.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/*
 * extracts keywords from a file name
 * with an exclusion list
 */
//**********************************************************
public class Keyword_extractor 
//**********************************************************
{
	boolean dbg = false;
	Logger logger;
	List<String> exclusion_list;
	
	//**********************************************************
	public Keyword_extractor(Logger l, List<String> exclusion_list_)
	//**********************************************************
	{
		logger = l;
		if ( logger == null) dbg = false;
		exclusion_list = exclusion_list_;
	}
	//**********************************************************
	public Set<String> extract_keywords_from_file_and_dir_names(Path from)
	//**********************************************************
	{
		Set<String> local_keywords = new TreeSet<>();

		Path target = from;
		String regex = "_|\\."; // "_";
		while ( local_keywords.isEmpty())
		{
			String clean_string = sanitize_file_name(target);
			logger.log("clean_string="+clean_string);
			extract_keywords(clean_string, regex, local_keywords);
			if (local_keywords.size() > 0 ) break;

			if ( dbg) logger.log("keyword list empty, trying to go up once");
			// going UP ONCE
			target = target.getParent();
		}
		return local_keywords;

	}
	
	// extracts keywords from "name"
	// using "reg" as a split
	// puts the keywords in local_keywords
	//**********************************************************
	private void extract_keywords(String name, String regex, Set <String> local_keywords) 
	//**********************************************************
	{
		String res[] = name.split(regex);
		for(int i = 0; i< res.length; i++)
		{
			if ( res[i].isEmpty() ) continue;
			logger.log("piece->"+res[i]+"<-");
			// in order to get rid of number
			// we try to convert each piece 
			// into a number
			try
			{
				double testor = Double.valueOf(res[i]);
			}
			catch (NumberFormatException e)
			{
				if (res[i].length() <= 3 ) continue;
				if ( is_excluded(res[i]))
				{
					logger.log("piece->"+res[i]+"<- is excluded");
					continue;
				}
				local_keywords.add(res[i]);			
			}
		}


	}
	
	//**********************************************************
	private boolean is_excluded(String string) 
	//**********************************************************
	{
		if ( exclusion_list.contains(string.toLowerCase() )) return true;
		
		for ( String s : exclusion_list)
		{
			if ( string.contains(s)) return true;
		}
		return false;
	}

	
	// clean up the string of the filename
	// replaces -. and numbers by _
	//**********************************************************
	private String sanitize_file_name(Path from)
	//**********************************************************
	{
		String g = from.getFileName().toString();
		if ( dbg) logger.log("sanitized name ->"+g+"<-");
		g = g.replaceAll("-","_");
		g = g.replaceAll("\\(","_");
		g = g.replaceAll("\\)","_");
		if ( dbg) logger.log("sanitized name ->"+g+"<-");
		//g = g.replaceAll(".","_");
		//logger.log("sanitized name ->"+g+"<-");
		// replace all digits with underscore
		for ( int k = 0 ; k <= 9 ; k++)
		{
			String kk = ""+k;
			g = g.replaceAll(kk,"_");
			if ( dbg) logger.log("sanitized name ->"+g+"<-");
		}
		if ( dbg) logger.log("sanitized name ->"+g+"<-");
		g = g.replaceAll(" ","_").trim();
		if ( dbg) logger.log("sanitized name ->"+g+"<-");
		return g;
	}

	
}
