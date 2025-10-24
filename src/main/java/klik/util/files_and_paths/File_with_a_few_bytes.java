package klik.util.files_and_paths;

import klik.util.execute.actor.Aborter;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// for fast file processing we keep in RAM the file length and the first few bytes
//**********************************************************
public class File_with_a_few_bytes
//**********************************************************
{
	private static final boolean ultra_dbg = false;
	public final File file;
	final long size; // if size differ ... different files
	byte[] first_bytes = null; // if we looked once, we keep the first few bytes, because if the first N bytes differ = different files
	private static final int BUFFER_SIZE = 8192;//4096;//1024;

	private static int warnings = 0;

	//**********************************************************
	public File_with_a_few_bytes(File f_, Logger logger)
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
			logger.log("WARNING (File_with_a_few_bytes): empty file found:"+ file.getAbsolutePath());
		}

	}

	//**********************************************************
	public static boolean files_have_same_content(File_with_a_few_bytes mf1, File_with_a_few_bytes mf2, Aborter aborter, Logger logger)
	//**********************************************************
	{

		// note we absolutely do NOT look at the file name: we look at the content
		if ( mf1.size == 0)
		{
			warnings++;
			if ( warnings< 100) logger.log("❗ WARNING: empty file1 NOT COMPARED:"+mf1.file.getAbsolutePath());
			return false;
		}
		if ( mf2.size == 0)
		{
			warnings++;
			if ( warnings< 100) logger.log("❗ WARNING: empty file2 NOT COMPARED:"+mf2.file.getAbsolutePath());
			return false;
		}

		if ( mf1.size != mf2.size )
		{
			if ( ultra_dbg ) logger.log("sizes differ "+ mf1.file.getName()+" "+mf1.size+" v.s. "+ mf2.file.getName()+" "+mf2.size);
			return false;
		}


		if (( mf1.first_bytes != null) && (mf2.first_bytes != null))
		{
			if ( mf1.first_bytes.length != mf2.first_bytes.length )
			{
				if (ultra_dbg) logger.log("first bytes length differ "+ mf1.first_bytes.length+" v.s. "+ mf2.first_bytes.length);
				return false;
			}
			if ( Arrays.mismatch(mf1.first_bytes,mf2.first_bytes) != -1)
			{
				if (ultra_dbg) logger.log("BYTES differ "+ mf1.file.getName()+" "+mf1.size+" v.s. "+ mf2.file.getName()+" "+mf2.size);
				return false;
			}
		}

		if ( ultra_dbg ) logger.log("Starting TOTAL CHECK for: "+ mf1.file.getName()+" "+mf1.size+" v.s. "+ mf2.file.getName()+" "+mf2.size);

		// let us check, block per block
		// at the first sign of a difference, return false
		boolean returned = true;
		try(BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(mf1.file));
			BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(mf2.file));
		)
		{
			// TODO optimize : allocate once?
			byte[] buffer1 = new byte[BUFFER_SIZE];
			byte[] buffer2 = new byte[BUFFER_SIZE];
			boolean first_loop = true;
			long start =  System.currentTimeMillis();
			for(;;)
			{
				if ( aborter.should_abort()) return false;
				int bytes1 = bis1.read(buffer1, 0, BUFFER_SIZE);
				int bytes2 = bis2.read(buffer2, 0, BUFFER_SIZE);

				if (( bytes1==-1)&&(bytes2 ==-1))
				{
					// both files ended
					returned = true;
					break;
				}
				if (( bytes1==-1)||(bytes2 ==-1))
				{
					// one file ended before the other
					returned = false;
					break;
				}
				if ( first_loop)
				{
					first_loop = false;
					if ( bytes1 < BUFFER_SIZE)
					{
						mf1.first_bytes = new byte[bytes1];
						System.arraycopy(buffer1,0,mf1.first_bytes,0,bytes1);
					}
					else
					{
						mf1.first_bytes = buffer1;
					}
					if ( bytes2 < BUFFER_SIZE)
					{
						mf2.first_bytes = new byte[bytes2];
						System.arraycopy(buffer2,0,mf2.first_bytes,0,bytes2);
					}
					else
					{
						mf2.first_bytes = buffer2;
					}
				}
				if ( bytes1 != bytes2 )
				{
					logger.log("read sizes differ");
					returned = false;
					break;
				}

				if ( Arrays.mismatch(buffer1,buffer2) != -1)
				{
					//if ( dbg ) logger.log("content differ");
					returned = false;
					break;
				}
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


    //**********************************************************
    public static List<Path> convert_to_paths(List<File_with_a_few_bytes> files)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( File_with_a_few_bytes fff : files)
        {
            Path p = fff.file.toPath();
            returned.add(p);
        }
        return returned;
    }
}

