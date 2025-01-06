there are 2 different image ML services
- face recognition
- image similarity

both use python-based tensorflow

0) install tensorflow

WARNING: tensorflow installation can be super tedious... YMMV
Only provided here is a recipe that leverages 
the hardware acceleration (metal) for ARM64 Macs:
(works on my MBP-M3)

open a shell/terminal, type:
brew install python@3.10
/opt/homebrew/bin/python3.10 -m venv ~/venv-metal
source ~/venv-metal/bin/activate
pip install -U pip
pip install tensorflow
pip install tensorflow-macos
pip install tensorflow-metal
pip install -r requirements.txt


1) vgg19 image similarity python service
to start the vgg19 embeddings servers:
open a shell/terminal, cd to this folder, type:
source ~/venv-metal/bin/activate
./launch_vgg19_servers

to kill the servers:
./kill_vgg19_servers

2) face recognition servers
to start the face detection and face embeddings servers:
open a shell/terminal, cd to this folder, type:
source ~/venv-metal/bin/activate
./launch_face_servers

to kill the servers:
./kill_face_servers


