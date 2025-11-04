// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.deduplicate;

import klik.util.execute.actor.Aborter;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.File_with_a_few_bytes;
import klik.util.files_and_paths.Name_cleaner;
import klik.util.log.Logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;


//**********************************************************
public class Runnable_for_finding_duplicate_file_pairs implements Runnable
//**********************************************************
{
	private static final boolean dbg = false;
	private static final boolean ultra_dbg = false;
	Logger logger;
	private final List<File_with_a_few_bytes> all_files;
	private final int i_min;
	private final int i_max;
	BlockingQueue<File_pair_deduplication> output_queue_of_same_in_pairs;
	Deduplication_engine deduplication_engine;
	private final Aborter private_aborter;
	//**********************************************************
	public Runnable_for_finding_duplicate_file_pairs(
			Deduplication_engine deduplication_,
			List<File_with_a_few_bytes> all_files_,
			int i_min_,
			int i_max_,
			BlockingQueue<File_pair_deduplication> output_queue,
			Aborter private_aborter_,
			Logger logger_)
	//**********************************************************
	{
		all_files = all_files_;
		i_min = i_min_;
		i_max = i_max_;
		logger = logger_;
		private_aborter = private_aborter_;
		output_queue_of_same_in_pairs = output_queue;
		deduplication_engine = deduplication_;
	}

	//**********************************************************
	@Override
	public void run()
	//**********************************************************
	{
		deduplication_engine.threads_in_flight.incrementAndGet();
		int duplicates_found_by_this_thread = 0;

		//boolean[] stop = new boolean[1];
		if ( dbg) logger.log("Runnable_for_finding_duplicate_file_pairs RUN starts");

		int ignored = 0;
		for (int i = i_min; i < i_max; i++ )
		{
			for ( int j = i+1; j < all_files.size(); j++)
			{
				if (private_aborter.should_abort()) {
					if (dbg) logger.log("Runnable_for_finding_duplicate_file_pairs abort");
					deduplication_engine.threads_in_flight.decrementAndGet();
					return;
				}
				deduplication_engine.console_window.count_pairs_examined.incrementAndGet ();

				//File_pair2 pair = new File_pair2(i, j);
				if (!File_with_a_few_bytes.files_have_same_content(all_files.get(i), all_files.get(j), private_aborter, logger)) {
					if (ultra_dbg)
						logger.log(" not same CONTENT:" + all_files.get(i).file.getAbsolutePath() + " - " + all_files.get(j).file.getAbsolutePath());
				}
				else
				{
					deduplication_engine.duplicates_found.incrementAndGet();
					//if (dbg) logger.log("duplicate fond:\n     " + all_files.get(i).file.getAbsolutePath() + "\n    " + all_files.get(j).file.getAbsolutePath());

					File_pair_deduplication pair_after = decide_which_to_delete(i,j);
					deduplication_engine.console_window.count_duplicates.incrementAndGet();
					//logger.log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>finder 1 more TO_BE_DELETED = "+duplicates_found_by_this_thread);

					if (dbg) logger.log(" DUPLICATES:\n" +
							pair_after.f1.my_file.file.getAbsolutePath() + " to be deleted: " + pair_after.f1.to_be_deleted + "\n" +
							pair_after.f2.my_file.file.getAbsolutePath() + " to be deleted: " + pair_after.f2.to_be_deleted);
					output_queue_of_same_in_pairs.add(pair_after);
				}
			}
		}


		logger.log("found duplicates:  "+deduplication_engine.duplicates_found.get());
		int remaining = deduplication_engine.threads_in_flight.decrementAndGet();
		if ( remaining != 0)
		{
			deduplication_engine.console_window.set_status_text("Thread found "+duplicates_found_by_this_thread+" duplicated pairs ... Search continues on "+ remaining +" threads!");
		}
		else
		{
			deduplication_engine.console_window.set_status_text("Total = "+ deduplication_engine.duplicates_found.get()+" duplicated pairs found, "+ignored+" ignored pairs (e.g. hidden files)");
		}

		//threads_in_flight.decrementAndGet();
	}


	//**********************************************************
	private File_pair_deduplication decide_which_to_delete(int i, int j)
	//**********************************************************
	{
		boolean is_image = Guess_file_type.is_this_path_an_image(all_files.get(i).file.toPath());

		// first we check if the one of the file names has been cleaned
		if(Name_cleaner.clean(all_files.get(i).file.getName(),true,logger).equals(all_files.get(j).file.getName()))
		{
			// i name is clean
			if(! Name_cleaner.clean(all_files.get(j).file.getName(),true,logger).equals(all_files.get(i).file.getName()))
			{
				// f1 name is clean, and f2 is not ..
				return set_f2_to_be_deleted(i,j, is_image);
			}
		}
		else
		{
			if(Name_cleaner.clean(all_files.get(j).file.getName(),true,logger).equals(all_files.get(i).file.getName()))
			{
				// f2 name is clean, and f1 is not ..
				return set_f1_to_be_deleted(i,j, is_image);
			}
		}

		// in order to decide which file to delete we compare the path length
		int lenght_of_path_for_i = all_files.get(i).file.getAbsolutePath().length();
		int lenght_of_path_for_j = all_files.get(j).file.getAbsolutePath().length();

		if ( lenght_of_path_for_i == lenght_of_path_for_j)
		{
			// same length
			if (all_files.get(i).file.getName().contains("_") )
			{
				if ( all_files.get(j).file.getName().contains("_"))
				{
					// both have underscore(s): delete f1
					//logger.log("f1 to be deleted as both names have underscores");
					return set_f1_to_be_deleted(i,j, is_image);
				}
				else
				{
					//logger.log("f2 to be deleted as name has no underscores, and f1 has");
					return set_f2_to_be_deleted(i,j, is_image);
				}
			}
			else
			{
				// no underscore in i's name:
				if ( all_files.get(j).file.getName().contains("_"))
				{
					// f2 has underscore(s): delete f1
					//logger.log("f1 to be deleted as name has no underscores, and f2 has");
					return set_f1_to_be_deleted(i,j, is_image);
				}
				else
				{
					// none have underscores  ... delete j
					//logger.log("f2 to be deleted as none have underscores");
					return set_f2_to_be_deleted(i,j, is_image);
				}
			}
		}
		else if ( lenght_of_path_for_i > lenght_of_path_for_j)
		{
			//logger.log("f1 to be deleted as its path is longer");
			return set_f1_to_be_deleted(i,j, is_image);
		}
		else
		{
			//logger.log("f2 to be deleted as its path is longer");
			return set_f2_to_be_deleted(i,j, is_image);
		}
	}

	private File_pair_deduplication make_pair(int i, int j, boolean is_image) {
		My_File_and_status mf1 = new My_File_and_status(all_files.get(i));
		My_File_and_status mf2 = new My_File_and_status(all_files.get(j));
		return  new File_pair_deduplication(mf1, mf2, is_image);
	}

	private File_pair_deduplication set_f1_to_be_deleted(int i, int j, boolean is_image) {
		File_pair_deduplication out = make_pair(i,j,is_image);
		out.f1.to_be_deleted = true;
		return out;
	}
	private File_pair_deduplication set_f2_to_be_deleted(int i, int j, boolean is_image) {
		File_pair_deduplication out = make_pair(i,j,is_image);
		out.f2.to_be_deleted = true;
		return out;
	}

}
