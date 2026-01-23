// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.deduplication;

import javafx.stage.Window;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Most_similar;
import klikr.machine_learning.similarity.Similarity_engine;
import klikr.machine_learning.similarity.Similarity_file_pair;
import klikr.util.files_and_paths.File_pair;
import klikr.util.files_and_paths.File_with_a_few_bytes;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;


//**********************************************************
public class Runnable_for_finding_duplicate_file_pairs_similarity implements Runnable
//**********************************************************
{
	private static final boolean dbg = false;
	Logger logger;
	private final List<File_with_a_few_bytes> all_files;
	BlockingQueue<Similarity_file_pair> output_queue_of_same_in_pairs;
	Deduplication_by_similarity_engine deduplication_by_similarity_engine;
	private final Aborter private_aborter;
	private final Similarity_engine similarity_engine;
	private final Klikr_cache<Path, Image_properties> image_properties_cache;// may be null
	private final Supplier<Feature_vector_cache> fv_cache_supplier;
	private final Window owner;
    private final double too_far_away;
	//**********************************************************
	public Runnable_for_finding_duplicate_file_pairs_similarity(
            List<Path> paths,
            double too_far_away,
            Klikr_cache<Path, Image_properties> image_properties_cache,// maybe null
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Path_comparator_source path_comparator_source,
            Deduplication_by_similarity_engine deduplication_by_similarity_engine_,
            List<File_with_a_few_bytes> all_files_,
            BlockingQueue<Similarity_file_pair> output_queue,
            Window owner,
            Aborter private_aborter_,
            Logger logger_)
	//**********************************************************
	{
        this.too_far_away = too_far_away;
		this.owner = owner;
		this.image_properties_cache = image_properties_cache;
		this.fv_cache_supplier = fv_cache_supplier;
		all_files = all_files_;
		logger = logger_;
		private_aborter = private_aborter_;
		output_queue_of_same_in_pairs = output_queue;
		deduplication_by_similarity_engine = deduplication_by_similarity_engine_;
		Path_list_provider path_list_provider = new Path_list_provider_for_file_system(deduplication_by_similarity_engine.target_dir.toPath(),owner,logger);
		similarity_engine = new Similarity_engine(
                paths,
                path_list_provider,
				path_comparator_source,
				owner,
				deduplication_by_similarity_engine.private_aborter,
				logger);

	}

	//**********************************************************
	@Override
	public void run()
	//**********************************************************
	{
		deduplication_by_similarity_engine.threads_in_flight.increment();
		int duplicates_found_by_this_thread = 0;
		if ( dbg) logger.log("Runnable_for_finding_duplicate_file_pair_similarity RUN starts");
		int ignored = 0;
		List<Path> already_done = new ArrayList<>();
		Feature_vector_cache fv_cache = fv_cache_supplier.get();
		if ( fv_cache == null)
		{
			logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL: fv_cache is null"));
			return;
		}

		for (File_with_a_few_bytes f : all_files)
		{
			if ( private_aborter.should_abort()) return;
			double x = deduplication_by_similarity_engine.owner.getX()+100;
			double y = deduplication_by_similarity_engine.owner.getY()+100;
			List<Most_similar> similars = similarity_engine.find_similars_special(
                    false,
                    image_properties_cache, // may be null
					f.file.toPath(),
					already_done,
					1,
					false,
					too_far_away,
					()->fv_cache,
					owner,
					x,
					y,
					deduplication_by_similarity_engine.console_window.count_pairs_examined,
					private_aborter
					);
			already_done.add(f.file.toPath());
			if ( similars.isEmpty()) continue;
			deduplication_by_similarity_engine.duplicates_found.increment();
			Most_similar most_similar = similars.get(0);
			if (dbg) logger.log("similars fond:\n     " + f.file.getAbsolutePath() + "\n    " + most_similar.path());

			File_pair pair= new File_pair(f.file, most_similar.path().toFile());
			Similarity_file_pair similarity_file_pair = new Similarity_file_pair(most_similar.similarity(), pair);
			deduplication_by_similarity_engine.console_window.count_duplicates.increment();
			if (dbg) logger.log(" DUPLICATES:\n" +
					pair.f1().getAbsolutePath()  +"\n" +
					pair.f2().getAbsolutePath() );
			output_queue_of_same_in_pairs.add(similarity_file_pair);

		}


		logger.log("found duplicates:  "+deduplication_by_similarity_engine.duplicates_found.doubleValue());
        deduplication_by_similarity_engine.threads_in_flight.decrement();
        double remaining = deduplication_by_similarity_engine.threads_in_flight.doubleValue();
		if ( remaining != 0)
		{
			deduplication_by_similarity_engine.console_window.set_status_text("Thread found "+duplicates_found_by_this_thread+" duplicated pairs ... Search continues on "+ remaining +" threads!");
		}
		else
		{
			deduplication_by_similarity_engine.console_window.set_status_text("Total = "+ deduplication_by_similarity_engine.duplicates_found.doubleValue()+" duplicated pairs found, "+ignored+" ignored pairs (e.g. hidden files)");
		}

		//threads_in_flight.decrementAndGet();
	}



}
