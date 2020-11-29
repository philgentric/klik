package klik.util;

import klik.util.Logger;

import java.time.Clock;
import java.time.Instant;


/*
 * returns a long representing the time of NOW in nanoseconds in UTC CLOCK
 * if you do not need a CLOCK use System.nanoTime() it is faster
 */
//**********************************************************
public class Nano_time
{
	private static Clock utc = Clock.systemUTC();

	
	//**********************************************************
	public static String get_ISO8601_string_Zulu_of_now()
	//**********************************************************
	{
		Instant now = utc.instant();
		return now.toString();
	}

	//**********************************************************
	public static long get_utc_nano_time_of_now() 
	//**********************************************************
	{
		return get_nano_time_of_now(utc);
		
	}

	//**********************************************************
	public static long get_nano_time_of_now(Clock clock) 
	//**********************************************************
	{
		Instant instant = clock.instant();
		long now;
		now = instant.getEpochSecond();
		//System.out.println(now);
		now *= 1000000000L;
		//System.out.println(now);
		now += instant.getNano();
		//System.out.println(now);
		return now;
	}

	
	//**********************************************************
	public static long get_nano_time_of(Instant i) 
	//**********************************************************
	{
		long now;
		now = i.getEpochSecond();
		now *= 1000000000L;
		now += i.getNano();
		return now;
	}

	
	private static final long ABOUT_TEN_YEARS_IN_NANOSECONDS = 10*365*24*3600*1000000000L;

	//**********************************************************
	public static void log_time_stamp(long local, String descriptor, Logger logger)
	//**********************************************************
	{
		if ( local > ABOUT_TEN_YEARS_IN_NANOSECONDS)
		{
			// this looks (hum!) possibly like a 1970 UTC date in nanoseconds
			logger.log("             raw "+descriptor+" = "+local+ " possibly ns in UTC epoch?...");
			long epochMilli = local/1000000L;
			Instant t = Instant.ofEpochMilli(epochMilli);
			logger.log("...assuming ns in epoch, ISO8601 format for "+descriptor+" is "+t.toString());
		}
		else
		{
			// this is probably just a cheap sampled timer
			logger.log(descriptor+" (ns) = "+local +" (beware, this is not an UTC clock, rather looks like a timer output)");								
		}
	}


	//**********************************************************
	public static String long_time_stamp_to_ISO8601_string(long local) 
	//**********************************************************
	{
		if ( local > ABOUT_TEN_YEARS_IN_NANOSECONDS)
		{
			// this looks (hum!) possibly like a 1970 UTC date in nanoseconds
			// convert to milliseconds
			long epochMilli = local/1000000L;
			Instant t = Instant.ofEpochMilli(epochMilli);
			return t.toString();// + " (if "+local+" is ns in UTC epoch)";
		}
		else
		{
			// this is probably just a cheap sampled timer
			return local+" (ns) this does not look like an UTC clock, but a timer output?";
		}
	}

	//**********************************************************
	public static String get_entire_hour(long local) 
	//**********************************************************
	{
		String full = long_time_stamp_to_ISO8601_string(local);
		//2020-01-20T14:39:18.303Z
		//             << 13 characters to get the UTC hour
		return full.substring(0,13);
	}
	//**********************************************************
	public static String get_year(long local) 
	//**********************************************************
	{
		String full = long_time_stamp_to_ISO8601_string(local);
		//2020-01-20T14:39:18.303Z
		//             << 13 characters to get the UTC hour
		return full.substring(0,4);
	}
	//**********************************************************
	public static String get_month(long local) 
	//**********************************************************
	{
		String full = long_time_stamp_to_ISO8601_string(local);
		//2020-01-20T14:39:18.303Z
		//             << 13 characters to get the UTC hour
		return full.substring(5,7);
	}

}
