#!/usr/bin/env bash

# -----------------------------------------------
# kill every “MobileNet_embeddings_server” process
# -----------------------------------------------
set -euo pipefail      # safer bash defaults

pids=$(pgrep -f MobileNet_embeddings_server)

if [[ -z $pids ]]; then
    echo "Image similarity: No MobileNet_embeddings_server processes found."
    exit 0
fi

echo "Image similarity: Killing process(es) $pids for MobileNet_embeddings_server"
kill -9 $pids