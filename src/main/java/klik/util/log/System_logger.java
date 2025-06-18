//SOURCES ./Simple_logger.java

package klik.util.log;

import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;

//*******************************************************
public class System_logger
//*******************************************************
{
	public static Logger get_system_logger(String tag)
	{
		if ( Booleans.get_boolean(Feature.Log_to_file.name(),null))
		{
			return new File_logger(tag);
		}
		else
		{
			return new Simple_logger();
		}
	}


}
