package klikr.machine_learning.python_via_html_ML;

import klikr.machine_learning.python_via_html_ML.monitoring.ML_server_launch_status;

import java.util.List;

public record ML_servers_status(List<Integer> available_ports, ML_server_launch_status launch_status) {
}
