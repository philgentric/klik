package klik.browser.items;

import klik.util.files_and_paths.Extensions;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Files;
import java.nio.file.Path;

//**********************************************************
public enum Iconifiable_item_type
//**********************************************************
{
    folder,
    symbolic_link_on_folder,
    non_javafx_image, // webp, tiff, etc
    javafx_image_not_gif_not_png, // which includes jpeg
    image_png,
    image_gif,
    //image_fits, // this is use only with the nasa java fits library, but we support it via GraphicsMagick i.e. non_javafx_image
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
        if (Guess_file_type.is_this_extension_a_non_javafx_type(extension)) return non_javafx_image;
        //if (Guess_file_type.is_this_extension_a_fits(extension)) return image_fits;
        if (Guess_file_type.is_this_extension_an_image_not_gif_not_png(extension)) return javafx_image_not_gif_not_png;

        //System.out.println(("WARNING: from_extension returns DEFAULT 'other'' for path: " + path));

        return other;
    }
}
