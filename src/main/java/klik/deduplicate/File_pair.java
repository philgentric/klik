package klik.deduplicate;

import java.io.File;

//**********************************************************
public class File_pair
//**********************************************************
{
	File f1;
	File f2;
	boolean is_images;
	public File_pair(File f1_, File f2_, boolean is_images_)
	{
		f1 = f1_;
		f2 = f2_;
		is_images = is_images_;
	}
}
