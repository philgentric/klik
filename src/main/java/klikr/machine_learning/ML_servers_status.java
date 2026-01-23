package klikr.machine_learning;

import java.util.List;

public record ML_servers_status(List<Integer> available_ports, ML_server_launch_status launch_status) {
}
