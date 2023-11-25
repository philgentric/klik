package klik.files_and_paths;

//**********************************************************
public class Sizes
//**********************************************************
{
    public final long bytes;
    public final long files;
    public final int folders;
    public final long images;

    //**********************************************************
    public Sizes(long bytes, int folders, long files, long images)
    //**********************************************************
    {
        this.bytes = bytes;
        this.folders = folders;
        this.files = files;
        this.images = images;
    }
}