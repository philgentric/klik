package klik.change.active_list_stage;

import java.time.LocalDateTime;
import java.util.Map;

public interface Datetime_to_signature_source
{
    Map<LocalDateTime,String> get_map_of_date_to_signature();
}
