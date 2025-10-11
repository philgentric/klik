package klik.browser.items;

import klik.util.files_and_paths.Extensions;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;

//**********************************************************
public enum Iconifiable_item_type
//**********************************************************
{
    folder,
    symbolic_link_on_folder,
    image_not_gif_not_png,
    image_png,
    image_gif,
    video,
    pdf,
    no_path,
    other;

    //**********************************************************
    public static Iconifiable_item_type from_extension(Path path)
    //**********************************************************
    {
        if ( path == null )
        {
            System.out.println(Stack_trace_getter.get_stack_trace("path is null"));
            return no_path;
        }
        if ( path.getFileName() == null )
        {
            System.out.println(Stack_trace_getter.get_stack_trace("path.getFileName() is null"));
            return no_path;
        }

        // special macos
        if (path.getFileName().toString().startsWith("._"))
        {
            //System.out.println(Stack_trace_getter.get_stack_trace("path.getFileName() starts with ._"));
            return no_path;
        }

        if ( path.toFile().isDirectory())
        {
            if (Files.isSymbolicLink(path)) return symbolic_link_on_folder;
            return folder;
        }

        String extension = Extensions.get_extension(path.getFileName().toString());
        if (Guess_file_type.is_this_extension_a_video(extension)) return video;
        if (Guess_file_type.is_this_extension_a_pdf(extension)) return pdf;
        if (Guess_file_type.is_this_extension_a_gif(extension)) return image_gif;
        if (Guess_file_type.is_this_extension_a_png(extension)) return image_png;
        if (Guess_file_type.is_this_extension_an_image_not_gif_not_png(extension)) return image_not_gif_not_png;

        //System.out.println(("WARNING: from_extension returns DEFAULT 'other'' for path: " + path));

        return other;
    }
}
