#!/usr/bin/env bash
set -euo pipefail      # safer bash defaults

# kill every “MobileNet_embeddings_server” process
pids=$(pgrep -f MobileNet_embeddings_server)
if [[ -z $pids ]]; then
    echo "Image similarity: No MobileNet_embeddings_server processes found."
    exit 0
fi
printf "Killing process(es):\n $pids \nfor MobileNet_embeddings_server\n"
kill -9 $pids