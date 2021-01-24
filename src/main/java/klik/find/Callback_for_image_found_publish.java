package klik.find;


public interface Callback_for_image_found_publish
{
	/*
	 * this is implemented by Finder_for_FX
	 * in order to record ONE result of the search
	 */
	void add_one_Search_result(Search_result sr);
	
	
	void update_display_in_FX_thread();//HashMap<String, Set<File>> similars);

}
