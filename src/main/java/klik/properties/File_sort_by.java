package klik.properties;

import klik.browser.comparators.*;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.util.log.Logger;
import klik.actor.Aborter;

import java.nio.file.Path;
import java.util.Comparator;

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
  public static Comparator<Path> get_comparator(Path displayed_folder_path, Image_properties_RAM_cache image_properties_cache, Aborter aborter, Logger logger)
  //**********************************************************
  {
    switch(File_sort_by.get_sort_files_by(logger))
    {
      case SIMILARITY_BY_PURSUIT:
        return new Similarity_comparator_by_pursuit(displayed_folder_path, image_properties_cache, aborter, logger);
      case SIMILARITY_BY_PAIRS:
        return new Similarity_comparator_pairs_of_closests(displayed_folder_path, aborter, logger);
      case NAME, ASPECT_RATIO, RANDOM_ASPECT_RATIO, IMAGE_HEIGHT, IMAGE_WIDTH:
        return new Alphabetical_file_name_comparator();
      case RANDOM:
        return new Random_comparator();
      case DATE:
        return new Date_comparator(logger);
      case SIZE:
        return new Decreasing_file_size_comparator();
      case NAME_GIFS_FIRST:
        return new Alphabetical_file_name_comparator_gif_first();
    }
    return null;
  }
  //**********************************************************
  public static File_sort_by get_sort_files_by(Logger logger)
  //**********************************************************
  {
    String s = Static_application_properties.get_main_properties_manager(logger).get(SORT_FILES_BY);
    if (s == null) {
      Static_application_properties.get_main_properties_manager(logger).add_and_save(SORT_FILES_BY, File_sort_by.NAME.name());
      return File_sort_by.NAME;
    }
    else
    {
      try {
        return File_sort_by.valueOf(s);
      }
      catch ( IllegalArgumentException e)
      {
        return File_sort_by.NAME;
      }
    }
  }

  //**********************************************************
  public static void set_sort_files_by(File_sort_by b, Logger logger)
  //**********************************************************
  {
    Static_application_properties.get_main_properties_manager(logger).add_and_save(SORT_FILES_BY, b.name());
  }

}
