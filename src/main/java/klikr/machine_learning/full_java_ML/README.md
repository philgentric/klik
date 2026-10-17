# full_java_ML — pure-JVM ML, no Python

Replaces the four Python HTTP/ML servers with in-process JVM inference:

| Old Python server | Replacement here |
|---|---|
| `MobileNet_embeddings_server.py` | `Onnx_feature_vector_source` (MOBILENET) via ONNX Runtime |
| `FaceNet_embeddings_server.py` | `Onnx_feature_vector_source` (FACENET) via ONNX Runtime |
| `Haar_face_detection_server.py` (4 variants) | `Opencv_face_detector` (YuNet) |
| `MTCNN_face_detection_server.py` | `Opencv_face_detector` (YuNet) |

Face detection is now a **single** path: OpenCV's **YuNet** NN detector. It
outperforms both the Haar cascades and MTCNN, so the Haar XMLs and MTCNN are no
longer used by this package. This removes the multi-server HTTP load-balancing,
the registry files, the UDP monitor, and the venv/pip install flow.

## Classes

- `ML_model_paths` — extracts the `.onnx` models from the classpath
  (`/ml_models/`) to a temp dir so the native runtimes can open them.
- `Onnx_embedding_engine` — shared `OrtEnvironment`; one thread-safe
  `OrtSession` per model `Kind` (MOBILENET / FACENET). `embed(float[]) -> double[]`.
- `Image_preprocessor` — OpenCV load + resize + BGR→RGB, producing the NHWC
  (MobileNet) or NCHW (FaceNet) float tensor with the right normalization.
- `Onnx_feature_vector_source` — implements the existing
  `Feature_vector_source` interface. Drop-in for `Feature_vector_source_server`.
- `Opencv_face_detector` — YuNet detection, returns a cropped JavaFX `Image`
  plus a `Face_recognition_in_image_status`, mirroring the existing
  `Face_detector.Face_detection_result` contract.

## Model contract (what `export_models.py` must produce)

Place these on the classpath under `src/main/resources/ml_models/`:

| File | Source model | Input | Layout | Normalization | Output |
|---|---|---|---|---|---|
| `mobilenet_v3_large.onnx` | Keras MobileNetV3Large, no top, avg pool, imagenet | 224×224×3 | NHWC | Keras `mobilenet_v3.preprocess_input` (identity on [0,255]) | ~960-d vector |
| `facenet_vggface2.onnx` | facenet-pytorch InceptionResnetV1('vggface2') | 160×160×3 | NCHW | `/255` | 512-d embedding |
| `face_detection_yunet.onnx` | OpenCV Zoo YuNet 2023mar | dynamic | — | — | rows `[x,y,w,h, 10 landmarks, score]` |

The Java code looks up the model's **first input name** automatically, so the
exact input-tensor name does not matter as long as there is one input.

## One-time export

```bash
cd src/main/resources/scripts
python3 -m venv /tmp/exportvenv && source /tmp/exportvenv/bin/activate
pip install tensorflow tf2onnx onnx torch facenet-pytorch
python3 export_models.py
```

This writes the three `.onnx` files into `../ml_models/`. They are
platform-independent — commit them so they ship in every installer
(dmg/msi/deb/rpm/AppImage). After this, Python is no longer needed at runtime.

## Cache parity warning

The embedding caches store raw `double[]`. ONNX-produced vectors may differ
very slightly from the old Python ones (preprocessing / op ordering). Treat the
JVM path as a **new cache version** (rebuild) rather than compare ONNX vectors
against cached Python vectors. Validate a few images produce near-identical
vectors (Python vs JVM) before trusting a reuse of old caches.

## Wiring (not done yet — standalone by design)

This package is self-contained; nothing existing was modified or deleted. To
switch the app over:

- image similarity: use
  `Onnx_feature_vector_source.create(Kind.MOBILENET, ctx)` where
  `Feature_vector_source_server` (MobileNet) is used today.
- face embeddings: `Onnx_feature_vector_source.create(Kind.FACENET, ctx)`.
- face detection: call `Opencv_face_detector.detect_face(path, ctx)` instead of
  `Face_detector.detect_face(path, type, ctx)`; the result record has the same
  `(Image image, Face_recognition_in_image_status status)` shape.

Create the two `Onnx_feature_vector_source` instances once and share them (the
`OrtSession` is thread-safe), replacing the N-HTTP-server load balancer with a
plain worker thread pool.
