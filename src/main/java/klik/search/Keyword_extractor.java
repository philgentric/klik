package klik.search;

import klik.util.files_and_paths.Extensions;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;

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
    private static final int MIN_KEYWORD_LENGTH = 3;
    boolean dbg = false;
    Logger logger;
    List<String> exclusion_list;

    //**********************************************************
    public Keyword_extractor(Logger l, List<String> exclusion_list_)
    //**********************************************************
    {
        logger = l;
        if (logger == null) dbg = false;
        exclusion_list = exclusion_list_;
    }

    //**********************************************************
    public Set<String> extract_keywords_from_file_and_dir_names(Path from)
    //**********************************************************
    {
        Set<String> local_keywords = new TreeSet<>();
        String regex = "_|\\.";
        once(from, local_keywords, regex);
        // going UP ONCE
        from = from.getParent();
        once(from, local_keywords, regex);
        return local_keywords;
    }

    //**********************************************************
    private void once(Path from, Set<String> local_keywords, String regex)
    //**********************************************************
    {
        String clean_string = sanitize_file_name(from);
        logger.log("clean string to extract keywords from=" + clean_string);
        extract_keywords(clean_string, regex, local_keywords);
    }

    // extracts keywords from "name"
    // using "reg" as a split
    // puts the keywords in local_keywords
    //**********************************************************
    private void extract_keywords(String name, String regex, Set<String> local_keywords)
    //**********************************************************
    {
        name = Extensions.get_base_name(name);
        if ( name == null) return;
        String[] res = name.split(regex);
        logger.log("found" + res.length + " pieces in " + name);

        for (String re : res) {
            if (re.isEmpty()) continue;
            logger.log("piece->" + re + "<-");
            // in order to get rid of number
            // we try to convert each piece
            // into a number

            boolean is_a_string_of_only_numbers = false;
            for (char c : re.toCharArray()) {
                is_a_string_of_only_numbers = Character.isDigit(c);
                if (!is_a_string_of_only_numbers) break;
            }
            if (is_a_string_of_only_numbers) continue;


            if (re.length() < MIN_KEYWORD_LENGTH) {
                logger.log("piece->" + re + "<- is too short e.g. length < " + MIN_KEYWORD_LENGTH);
                continue;
            }
            if (is_excluded(re)) {
                logger.log("piece->" + re + "<- is excluded");
                continue;
            }
            local_keywords.add(re.toLowerCase());
        }
    }

    //**********************************************************
    private boolean is_excluded(String string)
    //**********************************************************
    {
        if (exclusion_list.contains(string.toLowerCase())) return true;

        for (String s : exclusion_list) {
            if (string.contains(s)) return true;
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
        if (dbg) logger.log("sanitized name ->" + g + "<-");
        g = g.replaceAll("-", "_");
        g = g.replaceAll("\\(", "_");
        g = g.replaceAll("\\)", "_");
        if (dbg) logger.log("sanitized name ->" + g + "<-");
        //g = g.replaceAll(".","_");
        //logger.log("sanitized name ->"+g+"<-");
        // replace all digits with underscore
        for (int k = 0; k <= 9; k++) {
            String kk = String.valueOf(k);
            g = g.replaceAll(kk, "_");
            if (dbg) logger.log("sanitized name ->" + g + "<-");
        }
        if (dbg) logger.log("sanitized name ->" + g + "<-");
        g = g.replaceAll(" ", "_").trim();
        if (dbg) logger.log("sanitized name ->" + g + "<-");
        return g;
    }


}
