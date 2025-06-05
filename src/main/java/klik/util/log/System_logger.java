//SOURCES ./Simple_logger.java

package klik.util.log;

import klik.properties.Booleans;
import klik.properties.features.Debugging_feature;

//*******************************************************
public class System_logger
//*******************************************************
{
	public static Logger get_system_logger(String tag)
	{
		if ( Booleans.get_boolean(Debugging_feature.Use_file_logging.name()))
		{
			return new File_logger(tag);
		}
		else
		{
			return new Simple_logger();
		}
	}


}
