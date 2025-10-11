package klik.util.files_and_paths;

public class Extensions
{
    //**********************************************************
    public static String get_base_name(String file_name)
    //**********************************************************
    {
        int index = file_name.lastIndexOf(".");
        if ( index == -1) return file_name;

        String extension = file_name.substring(0,index);
        return extension;
    }
    //**********************************************************
    public static String get_extension(String file_name)
    //**********************************************************
    {
        int index = file_name.lastIndexOf(".");
        if ( index == -1) return "";

        String extension = file_name.substring(index+1);
        return extension;
    }

    //**********************************************************
    public static String add(String base, String extension)
    //**********************************************************
    {
        return base+"."+extension;
    }
}
