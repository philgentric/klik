package klik.browser.items;

import klik.files_and_paths.Guess_file_type;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public enum Iconifiable_item_type
{
    folder,
    symbolic_link_on_folder,
    image_not_gif,
    image_gif,
    video,
    pdf,
    other;

    public static Iconifiable_item_type from_extension(Path path)
    {
        if ( path == null ) return other;
        if ( path.getFileName() == null ) return other;
        // special macos
        if (path.getFileName().toString().startsWith("._")) return other;

        if ( path.toFile().isDirectory())
        {
            if (Files.isSymbolicLink(path)) return symbolic_link_on_folder;
            return folder;
        }

        String extension = FilenameUtils.getExtension(path.getFileName().toString());
        if (Guess_file_type.is_this_extension_a_video(extension)) return video;
        if (Guess_file_type.is_this_extension_a_pdf(extension)) return pdf;
        if (Guess_file_type.is_this_extension_a_gif(extension)) return image_gif;
        if (Guess_file_type.is_this_extension_an_image_not_gif(extension)) return image_not_gif;

        return other;
    }
}
