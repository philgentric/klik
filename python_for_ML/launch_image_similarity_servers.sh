#!/bin/bash

source ~/.klik/venv-metal/bin/activate

python3 --version
r
# First argument is the UDP monitoring port
UDP_PORT=$1
shift  # Remove first argument, leaving remaining args as HTTP ports
EMBEDDINGS_PORTS=("$@")

for PORT in "${EMBEDDINGS_PORTS[@]}"; do
    python3 -c "import MobileNet_embeddings_server;
MobileNet_embeddings_server.run_server(${PORT}, ${UDP_PORT})" &
done
