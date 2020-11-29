package klik.find;

import java.nio.file.Path;

public class Search_result 
{
	public Search_result(Path f, String k)
	{
		file = f;
		keyword = k;
	}
	Path file;
	String keyword;
}
