package klik.search;

import klik.actor.Actor;
import klik.actor.Message;
import klik.files_and_paths.Ding;
import klik.files_and_paths.Guess_file_type;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Finder_actor implements Actor
//**********************************************************
{

    static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    private int visited_folders;
    private int visited_files;
    Map<String,Integer> matched_keyword_counts = new HashMap<>();
    Logger logger;
    long start;

    //**********************************************************
    public Finder_actor(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }



    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Finder_message fm = (Finder_message) m;
        if ( fm.keywords.isEmpty())
        {
            logger.log("no keywords ? aborting search");
            fm.callback.has_ended(Search_status.invalid, "no keywords");

            Ding.play(logger);
            return "no keywords ? aborting search";
        }
        visited_files = 0;
        visited_folders =0;
        logger.log("Finder::search()");
        print_keywords(fm.keywords,fm.extension);

        String reason_to_stop = find_similar_files(fm);

        logger.log("find_similar_files returned");

        //fm.callback.update_display_in_FX_thread(fm.the_browser, reason_to_stop);
        fm.callback.has_ended(Search_status.done, "Done");
        return "search done";
    }



    //**********************************************************
    private String find_similar_files(Finder_message fm)
    //**********************************************************
    {
        logger.log("find_similar_files()");
        Path dir = fm.path; // for a folder
        if ( !Files.isDirectory(fm.path))
        {
            dir = fm.path.getParent();
        }
        start = System.currentTimeMillis();
        extract_dir( dir, fm);
        return "end of search";

    }


    //**********************************************************
    private void extract_dir(Path dir, Finder_message fm)
    //**********************************************************
    {
        if ( ultra_dbg) logger.log("finder extract_dir");

        /*
        if (dir.toAbsolutePath().toString().contains("Library"))
        {
            logger.log(Stack_trace_getter.get_stack_trace("ZOZO"));
            return;
        }

         */
        if ( fm.aborter.should_abort() )
        {
            logger.log("finder abort");
            return;
        }
        if ( !Files.isDirectory(dir) ) return;

        if ( dbg) logger.log("Now looking into dir:"+dir.toAbsolutePath());

        visited_folders++;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))
        {
            for (Path path : stream)
            {
                if ( fm.aborter.should_abort() )
                {
                    logger.log("finder abort");
                    return;
                }
                if ( Files.isDirectory(path))
                {
                    if (Files.isSymbolicLink(path))
                    {
                        if ( dbg) logger.log("NOT following symbolic link:"+path);
                    }
                    else
                    {
                        if ( dbg) logger.log("going down? trying folder:"+path);
                        extract_dir(path, fm);
                    }
                }
                else
                {
                    if ( fm.look_only_for_images)
                    {
                        if (Guess_file_type.is_this_path_an_image(path))
                        {
                            check_if_file_name_matches_keywords(path, fm);
                        }
                    }
                    else
                    {
                        check_if_file_name_matches_keywords(path, fm);
                    }
                }
                long now = System.currentTimeMillis();
                if ( (now-start) > 300)
                {
                    if ( fm.callback != null)
                    {
                        fm.callback.on_the_fly_stats(new Search_result(dir,new ArrayList<String>()),new Search_statistics(visited_folders, visited_files,matched_keyword_counts));
                        start = now;
                    }
                }

            }
        } catch (IOException e)
        {
            if (dbg) logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            else logger.log("Finder_actor:extract_dir "+e.toString());
            if ( fm.callback != null)
            {
                fm.callback.on_the_fly_stats(new Search_result(dir,new ArrayList<String>()),new Search_statistics(visited_folders, visited_files,matched_keyword_counts));
            }
        }
    }

    //**********************************************************
    private void check_if_file_name_matches_keywords(Path target_file_path, Finder_message fm)
    //**********************************************************
    {
        visited_files++;

        if ( fm.extension != null)
        {
            String ext = FilenameUtils.getExtension(target_file_path.getFileName().toString()).toLowerCase();
            if (! ext.equals(fm.extension))
            {
                if ( ultra_dbg) logger.log("extensions dont match"+ext+" vs "+fm.extension);
                return;
            }
        }
        String base_name = FilenameUtils.getBaseName(target_file_path.getFileName().toString());

        if ( ultra_dbg) logger.log("checking if all keywords are present for: "+base_name);
        // look for ALL of them
        List<String> all_matched_keywords = new ArrayList<>();
        for ( String keyword : fm.keywords)
        {
            if ( !base_name.toLowerCase().contains(keyword.toLowerCase()) )
            {
                // if one keyword is missing we give up
                break;
            }
            count_keyword(keyword);
            all_matched_keywords.add(keyword);
        }

        if ( fm.aborter.should_abort()) return;

        if ( all_matched_keywords.isEmpty())
        {
            if (ultra_dbg) logger.log("checking if a few keywords are present for: " + base_name);
            List<String> shorter_keyword_list = new ArrayList<>();
            for (String keyword : fm.keywords)
            {
                if (base_name.toLowerCase().contains(keyword.toLowerCase()))
                {
                    count_keyword(keyword);
                    shorter_keyword_list.add(keyword);
                }
            }
            if ( !shorter_keyword_list.isEmpty()) {
                process_name_selected(target_file_path, shorter_keyword_list, fm);
            }
        }
        else
        {
            if ( dbg) logger.log("all keywords "+all_matched_keywords+" found for "+target_file_path.getFileName());
            process_name_selected(target_file_path, all_matched_keywords, fm);
        }


    }

    //**********************************************************
    private void count_keyword(String keyword)
    //**********************************************************
    {
        Integer previous = matched_keyword_counts.get(keyword);
        if ( previous == null) previous = Integer.valueOf(0);
        matched_keyword_counts.put(keyword,previous+1);
    }

    //**********************************************************
    private void process_name_selected(Path path, List<String> matched_keywords,Finder_message fm)
    //**********************************************************
    {
        if ( Files.isDirectory(path))
        {
            if ( dbg)
                logger.log("keywords  ->"+matched_keywords+"<- matched for DIR "+path.toAbsolutePath());
            record_found_dir(path, matched_keywords,fm);
        }
        else
        {
            if(fm.look_only_for_images)
            {
                if ( !Guess_file_type.is_this_path_an_image (path))
                {
                    if ( dbg) logger.log("is not an image: "+path.toAbsolutePath());
                    return;
                }
            }
            if ( dbg) logger.log("keywords ->"+matched_keywords+"<- matched for file: "+path.toAbsolutePath());
            record_found_file(path, matched_keywords, fm);
        }
    }

    //**********************************************************
    private void record_found_dir(Path dir, List<String> matched_keywords, Finder_message fm)
    //**********************************************************
    {

        if ( ultra_dbg) logger.log("Matching folder found: "+dir.toAbsolutePath());
        if ( fm.callback != null)
        {
            fm.callback.on_the_fly_stats(new Search_result(dir,matched_keywords),new Search_statistics(visited_folders, visited_files,matched_keyword_counts));
        }

    }

    //**********************************************************
    private void record_found_file(Path file, List<String> matched_keywords, Finder_message fm)
    //**********************************************************
    {

        if ( ultra_dbg) logger.log("Matching file found: "+file.toAbsolutePath());
        if ( fm.callback != null)
        {
            fm.callback.on_the_fly_stats( new Search_result(file,matched_keywords), new Search_statistics(visited_folders,visited_files,matched_keyword_counts));
        }
    }

    //**********************************************************
    private static List<String> load_keyword_exclusion_list(Logger logger)
    //**********************************************************
    {

        List<String> returned = new ArrayList<>();
        int max = Static_application_properties.get_excluded_keyword_list_max_size(logger);
        for (int i = 0; i < max; i++) {
            String key = Static_application_properties.EXCLUDED_KEYWORD_PREFIX + i;
            String kw = Static_application_properties.get_properties_manager(logger).get(key);
            if (kw != null) {
                String lower = kw.toLowerCase();
                returned.add(lower);
                logger.log("excluded key word: ->" + lower + "<-");
            }
        }
        return returned;
    }
    //**********************************************************
    private void print_keywords(List<String> keywords, String extension)
    //**********************************************************
    {
        //if ( dbg == false) return;

        logger.log("---Finder keywords------");
        logger.log("Extension="+extension);
        for( String s: keywords)
        {
            logger.log("->"+s+"<-");
        }

        logger.log("------------------------");

    }

/*
    //**********************************************************
    public void find_files(Path target_path, Browser browser)
    //**********************************************************
    {
        List<String> exclusion_list = load_keyword_exclusion_list(logger);
        Keyword_extractor ke = new Keyword_extractor(logger, exclusion_list);
        Set<String> keywords_set = ke.extract_keywords_from_file_and_dir_names(target_path);
        if (keywords_set == null) {
            logger.log("FATAL null keywords ??? ");
            return;
        }
        if (keywords_set.isEmpty()) {
            logger.log("FATAL no keywords ??? ");
            return;
        }
        List<String> keywords = new ArrayList<>();
        for (String k : keywords_set) {
            keywords.add(k.toLowerCase());
        }

        logger.log("---- looking at keywords -------");
        for (String s : keywords) {
            logger.log("->" + s + "<-");
        }
        logger.log("--------------------------------");

        find_files_from_keywords( target_path,  browser,  keywords);

    }


 */

}
