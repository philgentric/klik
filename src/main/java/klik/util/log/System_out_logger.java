package klik.util.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//*******************************************************
public class System_out_logger implements Logger
//*******************************************************
{
	private final String tag;
	//*******************************************************
	public System_out_logger(String tag_)
	//*******************************************************
	{
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd:HHmmss");
		tag = tag_+now.format(dtf)+" ";

	}
	//*******************************************************
	@Override
	public void log(boolean show_date_tag, boolean also_System_out_println, String s)
	//*******************************************************
	{
		if ( show_date_tag)
		{
			System.out.println(tag+s);
		}
		else
		{
			System.out.println(s);
		}
	}
	//*******************************************************
	@Override
	public void close()
	//*******************************************************
	{
	}

	//*******************************************************
	@Override
	public void close(boolean slow_but_clean)
	//*******************************************************
	{
	
	}

	@Override
	public void flush() 
	{
	}
}
