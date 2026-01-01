#!/usr/bin/env bash

set -euo pipefail      # safer bash defaults

# kill every “MTCNN_face_detection_server” process
pids=$(pgrep -f MTCNN_face_detection_server)
if [[ -z $pids ]]; then
    echo "No MTCNN_face_detection_server processes found."
    exit 0
fi
printf "Killing process(es):\n $pids \nfor MTCNN_face_detection_server\n"
kill -9 $pids

# kill every “haars_face_detection_server” process
pids=$(pgrep -f haars_face_detection_server)
if [[ -z $pids ]]; then
    echo "No haars_face_detection_server processes found."
    exit 0
fi
printf "Killing process(es):\n $pids \nfor haars_face_detection_server\n"
kill -9 $pids

# kill every “FaceNet_embeddings_server” process
pids=$(pgrep -f FaceNet_embeddings_server)
if [[ -z $pids ]]; then
    echo "No FaceNet_embeddings_server processes found."
    exit 0
fi
# shellcheck disable=SC2028
printf "Killing process(es):\n $pids \nfor FaceNet_embeddings_server\n"
kill -9 $pids
