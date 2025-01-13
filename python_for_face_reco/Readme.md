there are 2 different image ML services
- face recognition
- image similarity

both use open-source code and models python-based tensorflow

IMPORTANT WARNING: without hardware acceleration, this is a no-go.
In other words it will not 'work' if you do not have a machine with a graphic card 
and a lot of RAM for it, so typically a unified memory machine like an ARM-based MacBook

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


Caveats:
the python image reader is not tolerant to truncated images 
(unlike a lot of jpeg decoders including ImageIO used by klik) 
and a single bad image can cause the whole scheme to hiccup seriously
because the feature vector extraction will fail and every thing is based 
on comparing images by their feature vectors. 
The good thing id: you will get error messages on the console:
identify the wrong images and fix them (edit them or remove them!)