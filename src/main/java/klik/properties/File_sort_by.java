package klik.properties;

import klik.util.log.Logger;

// warning: these names are used as-is in the resource bundles !!!
public enum File_sort_by {
  NAME,
  DATE,
  SIZE,
  IMAGE_WIDTH,
  IMAGE_HEIGHT,
  ASPECT_RATIO,
  RANDOM,
  RANDOM_ASPECT_RATIO,
  NAME_GIFS_FIRST;

  public static final String SORT_FILES_BY = "sort_files_by";


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
