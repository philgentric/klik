package klik.search;

public interface Search_receiver {
    void receive_intermediary_statistics(Search_statistics st);
    void has_ended(Search_status status);
}
