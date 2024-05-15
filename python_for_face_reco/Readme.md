to make face recognition work :
- run the face detection servers
- run the face embeddings servers



for these servers to work, you need to install afew things:

1) need to install python + pip

on macos:
brew install python

2) open a shell/terminal
preferably: create a venv environement
python3 -m venv venv1
activate it:
source .venv/bin/activate
3) install python dependencies
pip install -r requirements.txt

4) run the servers:

invoke the script "launch_servers"
you may need to execute first:
chmod +x launch_servers

to stop the servers, invoke "kill_servers"
you may need to execute first:
chmod +x kill_servers