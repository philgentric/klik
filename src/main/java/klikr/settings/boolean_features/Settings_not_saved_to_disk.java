package klikr.settings.boolean_features;

import klikr.settings.Sort_files_by;

public class Settings_not_saved_to_disk
{
    // these feature can be toggled TRUE during one usage of klikr
    // but they are not saved as TRUE i.e.
    // they are reset to FALSE when klikr (re-)starts
    public static final Feature[] never_saved_to_disk_as_true ={
            Feature.Show_single_column_with_details,
            Feature.Fusk_is_on};

    // these ways to sort files are not saved to cache or disk because
    // they are CPU consuming, so the user would wait
    // every time the folder is visited again
    public static final Sort_files_by[] never_saved_to_disk ={
            Sort_files_by.FILE_AND_FOLDER_SIZE};
}
