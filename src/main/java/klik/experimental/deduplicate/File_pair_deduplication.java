// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.deduplicate;

//**********************************************************
public class File_pair_deduplication
//**********************************************************
{
	public final My_File_and_status f1;
	public final My_File_and_status f2;
	public final boolean is_image;
	//**********************************************************
	public File_pair_deduplication(My_File_and_status f1_, My_File_and_status f2_, boolean is_image_)
	//**********************************************************
	{
		f1 = f1_;
		f2 = f2_;
		is_image = is_image_;
	}
}
