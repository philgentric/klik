package klik.search;


public interface Callback_for_file_found_publish
{
	void on_the_fly_stats(Search_result search_results, Search_statistics search_statistics);
	void has_ended(Search_status status);
}
