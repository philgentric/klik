package klik.in3D;

public interface Image_source
{
    Image_and_path get_next();
    Image_and_path get(int i);

    int how_many_images();
}
