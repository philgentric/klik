# ML services #

Klik has 4 different image ML services
- face recognition
- image similarity on demand: for one picture in a folder get the 5 closest pictures in the same folder
- image similarity for the whole folder: sort the folder by image similarity
- image de-duplication by similarity (in a folder): find quasi-duplicated images (with 2 variants, one uses distance=0 to detect images that differ by extremely small details such as metadata discrepancies)

All use python open-source code and models. 

Both face recognition and image similarity rely on launching MULTIPLE local HTTP servers that can be queried by the klikr app from java, which means that the servers survive the klikr app restarts. Note: there are multiple servers (on multiple TCP ports) because python does not support multithreading (well... not as well as Java!-) and the python HTTP server does not support queuing too well, so the klikr java code will 'load balance' the requests to the servers.

IMPORTANT WARNING: without proper hardware, this is probably not usable.
For ML speed, you need a machine with a graphic card that can access enough RAM...
(unified memory machines like an ARM-based MacBook work fine). Furthermore, these ML services use a lot of caches both in RAM and on disk (otherwise they would be horribly slow), so you need a machine with enough of both. In other words, if you use klikr on a small/old machine without a keras-supported GPU and/or without enough RAM (say less than 16GB) and disk space (many GBs), simply do not use these features as they will be slow and/or may crash the app (or even the machine in extreme cases!).

# Installation #

Klik provides buttons in the "more settings" menu:
- install the python-based ML libs
- start/stop the servers
- 
Otherwise here is the "manual" procedure:

WARNING: tensorflow installation can be super tedious... YMMV
Provided here is a recipe that leverages the hardware acceleration (metal) for ARM64 Macs

brew install python@3.10

/opt/homebrew/bin/python3.10 -m venv ~/.klikr/venv

source ~/.klikr/venv/bin/activate

pip install -U pip

for macOS:

pip install tensorflow-macos tensorflow-metal

otherwise:

pip install tensorflow tensorflow

(assuming your shell is in the Klik source code repo folder:)

cd ./src/main/resources/scripts

pip install -r requirements.txt

./launch_image_similarity_servers.sh

./kill_image_similarity_servers.sh

or windows ps1 version

./launch_face_recognition_servers.sh

./kill_face_recognition_servers.sh

or windows ps1 version

### Caveats ###

*The python image reader is not tolerant to truncated images* 
(unlike a lot of jpeg decoders including ImageIO used by Klik) 
which means that feature vector extraction will FAIL on such images.

When using the "sort by image similarity" folder preferences, a few bad images in a folder can cause the whole scheme to hiccup seriously because the feature vector extraction will fail and everything is based on comparing images by their feature vectors. The good thing is: you will get error messages in the console.

Identify the wrong files and fix them (edit them or remove them!)

## How it works ##

The idea is to create a feature vector for each image.
This vector can then be used to compute a distance between 2 images
(the metric used is cosine similarity)

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

The first time is quite slow for large folders but thanks to caching the process is super fast for subsequent runs. Caveat: caching has a cost in RAM and DISK space (caches may grow as large as they can, and they are saved to disk)... This is why in the 'Preferences' menu you have cache clearing options.

### Face recognition ###

The face recognition service is a bit more complex.

#### Training stage ####

One must first perform a training stage.

For this, one needs a data set where in a master folder you have one sub-folder for each 'person'.
For example, the Kaggle 'Celebrity Face Image Dataset' at: 
https://www.kaggle.com/datasets/vishesh1412/celebrity-face-image-dataset


Then, browsing this master folder, in the 'Files' menu :
1. select 'Start new face recognition training'; you will be prompted for a name (a folder with that name will be created in the .klikr folder)
2. select 'Start automated face recognition training'; you will be prompted to create a file named '.folder_name_is_recognition_label'
BEWARE: if you have many images, training will take a long time 

Training results (a 'model' in ML terms) are stored in the .klikr folder under the aforementioned name. 
Incremental training is possible: you can add new persons into a model or try new pictures to generate more prototypes for a given person.

#### Recognition stage ####

Once training is done, the face recognition service can be started for a given 'model'

You can store on disk multiple recognition models, and you can switch between them, but only one model can be active at a time.

Then, for any image you can click on the menu item 'perform face recognition' and the service will return the 5 closest images, each with the 'label' (the name of the person in the image)

#### Caveats ####

Face recognition starts with a face detection, and that can fail in 2 ways: no face is detected at all or what the system thinks is a face is not... This has multiple consequences:
- During training all images for which face detection fails will not produce a prototype
- During training, if you provide pictures with more than 1 face, the system will pick a face (no control over which) and this can result in a wrong prototype!
- During training, things that the face detection wrongly detected as a face will end up in the prototype set. There is a cure for this issue: the prototypes are stored WITH the corresponding image: if you browse that folder (in the .klikr folder) you can remove the wrong images and the corresponding prototype will be removed as well.
- During recognition, the system cannot recognize a face when face detection fails. If all face detection methods fail, you can try the 'direct' recognition i.e. the whole picture is taken a face; If that fails you can edit the picture to crop out the face and retry the 'direct' method.
- During recognition, face detection will first pick one face, and in pictures will multiple faces, it may not be the one you want. Solution: edit the picture and crop the face you want to train/recognize.
