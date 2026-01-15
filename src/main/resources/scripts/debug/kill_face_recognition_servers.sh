#!/usr/bin/env bash

set -euo pipefail      # safer bash defaults

# kill every “MTCNN_face_detection_server” process
pids=$(pgrep -f MTCNN_face_detection_server || true)
if [[ -z $pids ]]; then
    printf "No MTCNN_face_detection_server processes found\n"
else
    printf "Killing process(es):\n $pids \nfor MTCNN_face_detection_server\n"
    kill -9 $pids
fi

# kill every “haars_face_detection_server” process
pids=$(pgrep -f haars_face_detection_server || true)
if [[ -z $pids ]]; then
    printf "No haars_face_detection_server processes found\n"
else
    printf "Killing process(es):\n $pids \nfor haars_face_detection_server\n"
    kill -9 $pids
fi

# kill every “FaceNet_embeddings_server” process
pids=$(pgrep -f FaceNet_embeddings_server || true)
if [[ -z $pids ]]; then
    printf "No FaceNet_embeddings_server processes found\n"
else
    printf "Killing process(es):\n $pids \nfor FaceNet_embeddings_server\n"
    kill -9 $pids
fi
