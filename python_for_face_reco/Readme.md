to make face recognition work :
- run the face detection server
- run the face embeddings server

1) need to install python + pip

on macos:
brew install python

2) open a shell/terminal
preferably: create a venv environement
python3 -m venv venv1
activate it:
source .venv/bin/activate
3) go to the ja
pip install -r requirements.txt


to run the 2 servers:

python3 face_detection_server.py

python3 embeddings_server.py


