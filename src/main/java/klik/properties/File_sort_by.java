package klik.properties;

import klik.browser.virtual_landscape.Browsing_caches;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.comparators.*;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.util.log.Logger;
import klik.actor.Aborter;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

// warning: these names are used as-is in the resource bundles !!!
public enum File_sort_by {
  NAME,
  DATE,
  SIZE,
  SIMILARITY_BY_PAIRS,
  SIMILARITY_BY_PURSUIT,
  IMAGE_WIDTH,
  IMAGE_HEIGHT,
  ASPECT_RATIO,
  RANDOM,
  RANDOM_ASPECT_RATIO,
  NAME_GIFS_FIRST;

  public static final String SORT_FILES_BY = "sort_files_by";


  //**********************************************************
  public static Comparator<Path> get_preliminary_comparator(
          Path_list_provider path_list_provider,
          Path_comparator_source path_comparator_source,
          Image_properties_RAM_cache image_properties_cache,
          double x, double y,
          Aborter aborter, Logger logger)
  //**********************************************************
  {
    switch(File_sort_by.get_sort_files_by(path_list_provider.get_folder_path()))
    {
      case SIMILARITY_BY_PURSUIT:
        return get_similarity_comparator_by_pursuit(path_list_provider, path_comparator_source, image_properties_cache, x, y, aborter, logger);
      case SIMILARITY_BY_PAIRS:
        return get_similarity_comparator_pairs_of_closests(path_list_provider, x, y, aborter, logger);
      case NAME, ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT, IMAGE_WIDTH:
        return new Alphabetical_file_name_comparator();
      case RANDOM:
        return new Random_comparator();
      case DATE:
        return new Date_comparator(logger);
      case SIZE:
        return new Decreasing_disk_footprint_comparator();
      case NAME_GIFS_FIRST:
        return new Alphabetical_file_name_comparator_gif_first();
    }
    return null;
  }



  //**********************************************************
  public static Comparator<Path> get_true_comparator(
          Path_list_provider path_list_provider,
          Path_comparator_source path_comparator_source,
          Image_properties_RAM_cache image_properties_cache,
          double x, double y, Aborter aborter, Logger logger)
  //**********************************************************
  {
    switch(File_sort_by.get_sort_files_by(path_list_provider.get_folder_path()))
    {
      case SIMILARITY_BY_PURSUIT:
        return get_similarity_comparator_by_pursuit(path_list_provider, path_comparator_source, image_properties_cache, x, y, aborter, logger);
      case SIMILARITY_BY_PAIRS:
        return get_similarity_comparator_pairs_of_closests(path_list_provider, x, y, aborter, logger);
      case NAME:
        return new Alphabetical_file_name_comparator();
      case ASPECT_RATIO:
        return new Aspect_ratio_comparator(image_properties_cache);
      case RANDOM_ASPECT_RATIO:
        return new Aspect_ratio_comparator_random(image_properties_cache);
      case IMAGE_HEIGHT:
        return new Image_height_comparator(image_properties_cache,logger);
      case IMAGE_WIDTH:
        return new Image_width_comparator(image_properties_cache);
      case RANDOM:
        return new Random_comparator();
      case DATE:
        return new Date_comparator(logger);
      case SIZE:
        return new Decreasing_disk_footprint_comparator();
      case NAME_GIFS_FIRST:
        return new Alphabetical_file_name_comparator_gif_first();
    }
    return null;
  }



  //**********************************************************
  private static Similarity_comparator_pairs_of_closests get_similarity_comparator_pairs_of_closests(Path_list_provider path_list_provider, double x, double y, Aborter aborter, Logger logger)
  //**********************************************************
  {
    Similarity_cache similarity_cache = get_similarity_cache(path_list_provider, x, y, aborter, logger);
    return new Similarity_comparator_pairs_of_closests(similarity_cache, path_list_provider, x, y, aborter, logger);
  }

  //**********************************************************
  private static Similarity_comparator_by_pursuit get_similarity_comparator_by_pursuit(
          Path_list_provider path_list_provider, Path_comparator_source path_comparator_source, Image_properties_RAM_cache image_properties_cache, double x, double y, Aborter aborter, Logger logger)
  //**********************************************************
  {
    Similarity_cache similarity_cache = get_similarity_cache(path_list_provider, x, y, aborter, logger);
    return new Similarity_comparator_by_pursuit(
            similarity_cache,
            path_list_provider,
            path_comparator_source,
            image_properties_cache,
            x, y,
            aborter, logger);
  }

  //**********************************************************
  private static Similarity_cache get_similarity_cache(Path_list_provider path_list_provider, double x, double y, Aborter aborter, Logger logger)
  //**********************************************************
  {
    Similarity_cache similarity_cache = Browsing_caches.similarity_cache_of_caches.get(path_list_provider.get_folder_path().toAbsolutePath().toString());
    if (similarity_cache == null)
    {
      similarity_cache = new Similarity_cache(path_list_provider, x, y, aborter, logger);
      Browsing_caches.similarity_cache_of_caches.put(path_list_provider.get_folder_path().toAbsolutePath().toString(), similarity_cache);
    }
    return similarity_cache;
  }


  private static Map<Path,File_sort_by> cached = new HashMap<>();
  //**********************************************************
  public static File_sort_by get_sort_files_by(Path folder_path)
  //**********************************************************
  {
    File_sort_by from_cache = cached.get(folder_path);
    if ( from_cache != null) return from_cache;

    String s = Non_zooleans.get_main_properties_manager().get(SORT_FILES_BY);
    if (s == null)
    {
      Non_zooleans.get_main_properties_manager().set(SORT_FILES_BY, File_sort_by.NAME.name());
      cached.put(folder_path, File_sort_by.NAME);
      return File_sort_by.NAME;
    }


    try {
      File_sort_by returned = File_sort_by.valueOf(s);
      //logger.log(Stack_trace_getter.get_stack_trace("sort files by: "+returned));
      cached.put(folder_path, returned);
      return returned;
    }
    catch ( IllegalArgumentException e)
    {
      return File_sort_by.NAME;
    }

  }

  //**********************************************************
  public static void set_sort_files_by(Path folder_path, File_sort_by b, Logger logger)
  //**********************************************************
  {
    cached.put(folder_path, b);

    if ( b == File_sort_by.SIMILARITY_BY_PAIRS)
    {
        logger.log("warning: SIMILARITY_BY_PAIRS not saved to properties");
        return;
    }
    if ( b == File_sort_by.SIMILARITY_BY_PURSUIT)
    {
      logger.log("warning: SIMILARITY_BY_PURSUIT not saved to properties");
      return;
    }
    Non_zooleans.get_main_properties_manager().set(SORT_FILES_BY, b.name());
  }

}
