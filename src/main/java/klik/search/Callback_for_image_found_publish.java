package klik.search;


import klik.browser.Browser;

public interface Callback_for_image_found_publish
{
	/*
	 * this is implemented by Finder_for_FX
	 * in order to record ONE result of the search
	 */
	void add_one_Search_result(Search_result sr, Search_statistics st);
	void update_display_in_FX_thread(Browser b, String reason_to_stop);
	void has_ended();
}
