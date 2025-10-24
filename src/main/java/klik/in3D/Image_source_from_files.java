package klik.in3D;

import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//*******************************************************
public class Image_source_from_files implements Image_source
//*******************************************************
{
    private final int icon_size;
    private int i = 0;
    private List<Path> paths = new ArrayList<>();
    private Map<Path,Image_and_path> cache = new HashMap<>();

    //*******************************************************
    public Image_source_from_files(Path folder, int icon_size)
    //*******************************************************
    {
        this.icon_size = icon_size;
        File[] files_ = folder.toFile().listFiles();
        if ( files_ == null)
        {
            System.out.println("❌ FATAL");
            return;
        }
        for (File f : files_)
        {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))
            {
                paths.add(f.toPath());
            }
        }
    }


    //*******************************************************
    @Override
    public Image_and_path get_next()
    //*******************************************************
    {
        return get(i++);
    }

    //*******************************************************
    @Override
    public Image_and_path get(int i)
    //*******************************************************
    {
        if ( i >= paths.size() ) return null;
        Path p = paths.get(i);
        Image_and_path returned = cache.get(p);
        if ( returned != null) return returned;
        returned = make_one(p);
        cache.put(p,returned);
        return  returned;
    }

    //*******************************************************
    private Image_and_path make_one(Path path)
    //*******************************************************
    {
        try (InputStream is = new FileInputStream(path.toFile())) {
            return new Image_and_path(
                    new Image(is, icon_size, icon_size, true, true),
                    path);
        } catch (Exception e) {
            System.out.println("❌ fatal " + e);
            return null;
        }
    }

    //*******************************************************
    @Override
    public int how_many_images()
    //*******************************************************
    {
        return paths.size();
    }


}
