to make face recognition work :
- run the face detection servers
- run the face embeddings servers



for these servers to work, you need to install a few things:

1) need to install python + pip

on macos:
brew install python
or
brew install python@3.10 to get a specific version
(in case pip install -r requirements.txt ./la   fails with a python version error)

2) open a shell/terminal
preferably: create a venv environment
python3.10 -m venv venv1
activate it:
source ./venv1/bin/activate
3) install python dependencies
   (in this folder)
pip install -r requirements.txt

4) run the servers:

invoke the script "launch_servers"
you may need to execute first:
chmod +x launch_servers

to stop the servers, invoke "kill_servers"
you may need to execute first:
chmod +x kill_servers