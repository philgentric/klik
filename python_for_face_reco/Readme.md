# ML services #

Klik has 3 different image ML services
- face recognition
- image similarity on demand: for one picture in a folder get the 5 closest pictures in the same folder
- image similarity for the whole folder: sort the folder by image similarity

All use python open-source code and models (it is based on tensorflow/keras and opencv) 

Both face recognition and image similarity rely on launching MULTIPLE local HTTP servers that can be queried by the klik app from java, which means that the servers survive the klik app restarts. Note: there are multiple servers (on multiple TCP ports) because python does not support multithreading (well... not as well as Java!-) and the python HTTP server does not support queuing too well, so the klik java code will 'load balance' the requests to the servers.

IMPORTANT WARNING: without proper hardware, this is probably not usable.
For ML speed, you need a machine with a graphic card that can access enough RAM...
(unified memory machines like an ARM-based MacBook work fine). Furtheremore, these ML services use a lot of caches both in RAM and on disk (otherwise they would be horribly slow), so you need a machine with enough of both. In other words, if you use klik on a small/old machine without a keras-supported GPU and/or without enough RAM (say less than 16GB) and disk space (many GBs), simply do not use these features as they will be slow and/or may crash the app (or even the machine in extreme cases!).

# Installation #
1.ONCE: install tensorflow+keras enchilada

WARNING: tensorflow installation can be super tedious... YMMV 

Only provided here is a recipe that leverages the hardware acceleration (metal) for ARM64 Macs:
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

2.AFTER a reboot: activate the virtual environment
source ~/venv-metal/bin/activate

3. image embeddings python service.
to start the embeddings servers, use one of the launch commands:
./launch_MobileNetV2_servers
./launch_InceptionV3_servers
./launch_VGG19_servers

recommended: use MobileNetV2, it is the fastest because it is the smallest model and performance is still good.

to kill the embeddings servers:
./kill_embeddings_servers

4. face recognition servers
to start the face detection and face embeddings servers:
open a shell/terminal, cd to this folder, type:
source ~/venv-metal/bin/activate
./launch_face_servers

this will start several servers for respectively face detection (multiple flavors) based on Haars-Cascade and face embeddings (InceptionResNetV1 vggface2)

to kill the face recognition servers:
./kill_face_servers

### Caveats ###

The python image reader is not tolerant to truncated images 
(unlike a lot of jpeg decoders including ImageIO used by klik) which means that feature vector extraction will fail on such images.

When using the "sort by image similarity" folder preferences, a few bad images in a folder can cause the whole scheme to hiccup seriously because the feature vector extraction will fail and every thing is based on comparing images by their feature vectors. The good thing is: you will get error messages on the console:
identify the wrong images and fix them (edit them or remove them!)

## How it works ##

The idea is to create a feature vector for each image (e.g. in a folder).
This (large!) vector can then be used to compute a distance between 2 images
(the metric used is cosine similarity, which is a good metric for high-dimensional vectors)

### Image similarity ### 

For any image in a folder, you can click and chose 'show 5 similar images in this folder' : the whole folder will be scanned and the closest 5 images will be displayed.

This is also how the "sort by image similarity works": the folder is scanned and feature vectors are computed for each image. Then, for each image, the distance to all other images is computed and this is used to tweak the comparator by creating a 'dummy name' i.e. if 2 images named respectively Image1 and Image2 are detected as being 'close' the name 'Image2' is replaced with 'Image1_xxxx', which will cause this second image to be displayed next to the first one when isuing a simple alphabetical sort. You may wonder what is the logic of choosing 'Image1' rather than vice-versa: there is no logic, this is done at random, so if you browse several times a folder you may see a different 'order' each time... this is a caveat of the fact that image similarity  CANNOT be mapped into a strict/consistent order ( in the math sense) which is MANDATORY for the (Tim-sort) algorithm that is used to sort the files so that they appear in a given order.

Since on a per folder base, we need to compute:
- the feature vectors for all images
- the distance between all pairs of images
- the 'closest' images for each image
- the 'dummy names' for each image
- the 'sort order' for each image
...

The first time is quite slow for large folders (e.g. more 1000 images) but thanks to a multiple cache mechanism the process is much faster for subsequent runs. Caveat: caching has a cost in RAM and DISK space (caches may grow as large as they can, and they are saved to disk)... This is why in the 'Preferences' menu you have cache clearing options.

### Face recognition ###

The face recognition service is a bit more complex.

#### Training stage ####

One must first perform a training stage where for each 'person' a set of 'prototype' face embeddings is stored (in a folder on disk). To do this you name a folder with the name of the person (in classification this is called a 'label') and you place pictures of THAT person in that folder. Then you place all these 'person' folders in a 'people' folder where you create a hidden file named '.folder_name_is_recognition_label': Then you can select the automatic face recognition. BEWARE: if you have many images, training will take a long time because is involves computing the face embeddings for each image, and then perform a face recognition using the whole stored prototypes. If the recognition is correct (if the right label is predicted) the prototype is dropped, otherwise it is kept. This way, we keep the prototype set as small as possible otherwise the recognition would be slower.

Training results (a 'model' in ML terms) are stored with name that you will provide. Incremental training works fine: you can add new persons into a model or try new pictures to generate more prototypes for a given person.

#### Recognition stage ####

Once training is done, the face recognition service can be started for a given 'model'

You can store on disk multiple recognition models, and you can switch between them, but only one model can be active at a time.

Then, for any image you can click on the menu item 'perform face recognition' and the service will return the 5 closest images, each with the 'label' (the name of the person in the image)

#### Caveats ####

Face recognition starts with a face detection, and that can fail in 2 ways: no face is detected at all or what the system thinks is a face is not... This has multiple consequences:
- During training all images for which face detection fail will not produce a prototype
- During training, if you provide pictures with more than 1 person face visible, the system will pick one face (no control over which) and this can result in a wrong prototype!
- During training, things that the face detection wrongly detected as a face will end up in the prototype set. There is a cure for this issue: the prototypes are stored WITH the corresponding image: if you browse that folder (in the .klik folder) you can remove the wrong images and the corresponding prototype will be removed as well.
- During recognition, the system cannot recognize a face when face detection fails. 
- During recognition, face detection will first pick one face, and in pictures will multiple faces, it may not be the one you want. Solution: edit the picture with your favorite editor and crop the face you want to recognize.
