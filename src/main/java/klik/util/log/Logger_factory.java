//SOURCES ./Simple_logger.java

package klik.util.log;

import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;

//*******************************************************
public class Logger_factory
//*******************************************************
{
	public static Logger get(String tag)
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
