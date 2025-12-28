#!/usr/bin/env bash

get_face_embeddings_server_pids_for_port() {
  local port="$1"
  ps -e -o pid=,cmd= 2>/dev/null || \
    ps -axo pid=,command= 2>/dev/null | tail -n +2 |
    grep -E -i "[p]ython[^ ]*_*_face_embeddings_server\.run_server[^ ]*${port}" |
    awk '{print $1}'
}


FACENET_EMBEDDINGS_PORTS=(8020 8021)

for PORT in "${FACENET_EMBEDDINGS_PORTS[@]}"; do
  pids=$(get_face_embeddings_server_pids_for_port "$PORT")
  if [[ -n $pids ]]; then
        echo "FaceNet, killing process(es) ${pids} for port ${PORT}"
        kill -9 $pids
    else
        echo "FaceNet, No process(es) found for port ${PORT}"
    fi
done


get_face_detection_server_pids_for_port() {
  local port="$1"
  ps -e -o pid=,cmd= 2>/dev/null || \
    ps -axo pid=,command= 2>/dev/null | tail -n +2 |
    grep -E -i "[p]ython[^ ]*_*_face_detection_server\.run_server[^ ]*${port}" |
    awk '{print $1}'
}


FACE_DETECTION_PORTS=(
8040 8041 8042 8043 8044 8045 8046 8047 8048 8049
8080 8081
8090 8091
8100 8101
8110 8111)

for PORT in "${FACE_DETECTION_PORTS[@]}"; do
  pids=$(get_face_detection_server_pids_for_port "$PORT")
  if [[ -n $pids ]]; then
        echo "Face extraction, killing process(es) ${pids} for port ${PORT}"
        kill -9 $pids
    else
        echo "Face extraction, No process(es) found for port ${PORT}"
    fi
done