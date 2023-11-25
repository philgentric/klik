package klik.deduplicate;

//**********************************************************
public class File_pair
//**********************************************************
{
	public final My_File_and_status f1;
	public final My_File_and_status f2;
	public final boolean is_image;
	//**********************************************************
	public File_pair(My_File_and_status f1_, My_File_and_status f2_, boolean is_image_)
	//**********************************************************
	{
		f1 = f1_;
		f2 = f2_;
		is_image = is_image_;
	}
}
