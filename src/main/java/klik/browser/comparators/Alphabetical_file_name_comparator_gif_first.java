package klik.browser.comparators;

import klik.util.files_and_paths.Guess_file_type;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class Alphabetical_file_name_comparator_gif_first implements Comparator<Path>
//**********************************************************
{
    @Override
    public int compare(Path f1, Path f2)
    {
        Boolean is_gif1 = Guess_file_type.is_this_extension_a_gif(FilenameUtils.getExtension(f1.getFileName().toString()));
        Boolean is_gif2 = Guess_file_type.is_this_extension_a_gif(FilenameUtils.getExtension(f2.getFileName().toString()));
        if ( is_gif1 && is_gif2)
        {
            return f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString());
        }
        else if ( is_gif1 )
        {
            return -1;
        }
        else if ( is_gif2 )
        {
            return 1;
        }
        else
        {
            return f1.getFileName().toString().compareToIgnoreCase(f2.getFileName().toString());
        }
    }
};