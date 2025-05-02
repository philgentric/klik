package klik.util.log;

import klik.properties.Booleans;

//*******************************************************
public class System_logger
//*******************************************************
{
	public static Logger get_system_logger(String tag)
	{
		if ( Booleans.get_boolean(Booleans.USE_FILE_LOGGING,new Simple_logger()))
		{
			return new File_logger(tag);
		}
		else
		{
			return new Simple_logger();
		}
	}


}
