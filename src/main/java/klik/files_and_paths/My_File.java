package klik.files_and_paths;

import klik.actor.Aborter;
import klik.deduplicate.Runnable_for_finding_duplicate_file_pairs;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

//**********************************************************
public class My_File
//**********************************************************
{
	public final File file;
	final long size; // if size differ ... different files
	byte[] first_bytes = null; // if we looked once, we keep the first few bytes, because if the first N bytes differ = different files
	private static final int BUFFER_SIZE = 8192;//4096;//1024;

	private static int warnings = 0;

	//**********************************************************
	public My_File(File f_, Logger logger)
	//**********************************************************
	{
		file = f_;
		long tmp_size = file.length();
		if ( tmp_size == 0)
		{
			try {
				BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				tmp_size = attr.size();
			} catch (IOException e) {
				logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
			}
		}
		size = tmp_size;
		if ( size == 0)
		{
			warnings++;
			//if ( warnings < 100)
			logger.log("WARNING (My_File): empty file found:"+ file.getAbsolutePath());
		}

	}

	//**********************************************************
	public static boolean files_have_same_content(My_File mf1, My_File mf2, Aborter aborter, Logger logger)
	//**********************************************************
	{
		// note we absolutely do NOT look at the file name: we look at the content
		if ( mf1.size == 0)
		{
			warnings++;
			if ( warnings< 100) logger.log("WARNING: empty file1 NOT COMPARED:"+mf1.file.getAbsolutePath());
			return false;
		}
		if ( mf2.size == 0)
		{
			warnings++;
			if ( warnings< 100) logger.log("WARNING: empty file2 NOT COMPARED:"+mf2.file.getAbsolutePath());
			return false;
		}

		if ( mf1.size != mf2.size )
		{
			if ( Runnable_for_finding_duplicate_file_pairs.ultra_dbg ) logger.log("sizes differ "+ mf1.file.getName()+" "+mf1.size+" v.s. "+ mf2.file.getName()+" "+mf2.size);
			return false;
		}

		if (( mf1.first_bytes != null) && (mf2.first_bytes != null))
		{
			//if ( !Arrays.equals(mf1.first_bytes,mf2.first_bytes) )
			if ( Arrays.mismatch(mf1.first_bytes,mf2.first_bytes) != -1)
			{
				if ( Runnable_for_finding_duplicate_file_pairs.ultra_dbg ) logger.log("BYTES differ "+ mf1.file.getName()+" "+mf1.size+" v.s. "+ mf2.file.getName()+" "+mf2.size);
				return false;
			}
		}

		if ( Runnable_for_finding_duplicate_file_pairs.ultra_dbg ) logger.log("Starting TOTAL CHECK for: "+ mf1.file.getName()+" "+mf1.size+" v.s. "+ mf2.file.getName()+" "+mf2.size);

		// let us check, block per block
		// at the first sign of a difference, return false
		boolean returned = true;
		try
		{
			BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(mf1.file));
			BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(mf2.file));
			// TODO optimize : allocate once
			byte[] hashsrc = new byte[BUFFER_SIZE];
			byte[] hashdest = new byte[BUFFER_SIZE];
			//Arrays.fill(hashsrc,(byte)0); // dont need to do that
			boolean quit = false;
			boolean first_loop = true;
			long start =  System.currentTimeMillis();
			for(;;)
			{
				if ( aborter.should_abort()) return false;
				int available_src = bis1.available();
				int available_dest = bis2.available();
				if ( available_src != available_dest )
				{
					//if ( dbg) logger.log("read sizes differ");
					returned = false;
					break;
				}
				if ( available_src < BUFFER_SIZE)
				{
					if ( bis1.read(hashsrc, 0, available_src) != available_src)
					{
						logger.log("WARNING file reading failed for: "+mf1.file.getAbsolutePath());
						return false;
					}
					if (bis2.read(hashdest, 0, available_dest) != available_dest)
					{
						logger.log("WARNING file reading failed for: "+mf2.file.getAbsolutePath());
						return false;
					}
					quit = true;
				}
				else
				{
					if ( bis1.read(hashsrc, 0, BUFFER_SIZE) != BUFFER_SIZE)
					{
						logger.log("WARNING file reading failed for: "+mf1.file.getAbsolutePath());
						return false;
					}
					if ( bis2.read(hashdest, 0, BUFFER_SIZE) != BUFFER_SIZE)
					{
						logger.log("WARNING file reading failed for: "+mf2.file.getAbsolutePath());
						return false;
					}
				}

				if ( first_loop)
				{
					first_loop = false;
					mf1.first_bytes = hashsrc;
					mf2.first_bytes = hashdest;
				}
				//if ( !Arrays.equals(hashsrc,hashdest) )
				if ( Arrays.mismatch(hashsrc,hashdest) != -1)
				{
					//if ( dbg ) logger.log("content differ");
					returned = false;
					break;
				}
				if (quit) break;
				long now = System.currentTimeMillis();
				if ( (now-start) > 3000 )
				{
					logger.log("... still doing TOTAL CHECK for: "+ mf1.file.getName()+" "+mf1.size+" v.s. "+ mf2.file.getName()+" "+mf2.size);
					start = now;
				}
			}
			bis1.close();
			bis2.close();
			return returned;

		}
		catch(Exception ioe)
		{
			logger.log(ioe.toString());
			return false;
		}

	}



}

