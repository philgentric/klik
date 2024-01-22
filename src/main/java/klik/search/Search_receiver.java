package klik.search;

public interface Search_receiver {
    void receive_intermediary(Search_statistics st);
    void has_ended(Search_status search_status, String message);
}
