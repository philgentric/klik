#!/usr/bin/env python3
# Copyright (c) 2025 Philippe Gentric
# SPDX-License-Identifier: MIT
#
# ONE-TIME model export for the pure-JVM ML (klikr.machine_learning.ONNX_java_ML).
#
# Produces the three .onnx files the Java code loads, into:
#     src/main/resources/ml_models/
#
#   mobilenet_v3_large.onnx    <- Keras MobileNetV3Large (imagenet, no top, avg pool)
#   facenet_vggface2.onnx      <- facenet-pytorch InceptionResnetV1(pretrained='vggface2')
#   face_detection_yunet.onnx  <- downloaded from OpenCV Zoo (YuNet)
#
# YuNet replaces BOTH the 4 Haar cascades AND MTCNN: it is more accurate and
# faster, so the Java detector has a single code path.
#
# The two embedding models are the SAME ones the old Python HTTP servers used,
# so embeddings stay comparable (modulo the preprocessing-parity note in
# README.md).
#
# Run it once on a machine that has the Python ML stack installed:
#     python3 export_models.py                 # all three
#     python3 export_models.py mobilenet       # subset
#
# Requirements (install into a venv):
#     pip install tensorflow tf2onnx onnx torch facenet-pytorch
#     # YuNet is just a file download (urllib), no extra deps.
# -----------------------------------------------------------------------------

import os
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
# repo-root/src/main/resources/ml_models  (this script lives in scripts/)
OUT_DIR = os.path.normpath(os.path.join(HERE, "..", "ml_models"))

MOBILENET_OUT = os.path.join(OUT_DIR, "mobilenet_v3_large.onnx")
FACENET_OUT   = os.path.join(OUT_DIR, "facenet_vggface2.onnx")
YUNET_OUT     = os.path.join(OUT_DIR, "face_detection_yunet.onnx")

# OpenCV Zoo YuNet (2023mar) — matches OpenCV FaceDetectorYN expectations.
YUNET_URL = ("https://github.com/opencv/opencv_zoo/raw/main/models/"
             "face_detection_yunet/face_detection_yunet_2023mar.onnx")


def ensure_out_dir():
    os.makedirs(OUT_DIR, exist_ok=True)
    print(f"Output dir: {OUT_DIR}")


def export_mobilenet():
    """MobileNetV3Large, include_top=False, pooling='avg', 224x224x3, imagenet.
    Matches MobileNet_embeddings_server.py. Keras preprocess_input for V3 is an
    identity on [0,255]; the Java side feeds raw [0,255] NHWC accordingly."""
    print("\n== Exporting MobileNetV3Large -> ONNX ==")
    import tensorflow as tf
    import tf2onnx
    from tensorflow.keras.applications import MobileNetV3Large
    from tensorflow.keras.models import Model

    base = MobileNetV3Large(include_top=False, pooling='avg',
                            input_shape=(224, 224, 3), weights='imagenet')
    model = Model(inputs=base.input, outputs=base.output)

    # NHWC input named 'input' to match the Java engine's first-input lookup.
    spec = (tf.TensorSpec((1, 224, 224, 3), tf.float32, name="input"),)
    tf2onnx.convert.from_keras(model, input_signature=spec, opset=13,
                               output_path=MOBILENET_OUT)
    print(f"  wrote {MOBILENET_OUT}")


def export_facenet():
    """InceptionResnetV1(pretrained='vggface2'), 160x160x3, NCHW, /255.
    Matches FaceNet_embeddings_server.py (512-d embedding)."""
    print("\n== Exporting FaceNet InceptionResnetV1(vggface2) -> ONNX ==")
    import torch
    from facenet_pytorch import InceptionResnetV1

    model = InceptionResnetV1(pretrained='vggface2').eval()
    dummy = torch.randn(1, 3, 160, 160)  # NCHW, batch=1
    torch.onnx.export(
        model, dummy, FACENET_OUT,
        input_names=["input"], output_names=["embedding"],
        opset_version=13, do_constant_folding=True,
        dynamic_axes=None,
    )
    print(f"  wrote {FACENET_OUT}")


def download_yunet():
    """YuNet replaces Haar + MTCNN. Fetch the pretrained ONNX from OpenCV Zoo."""
    print("\n== Downloading YuNet (replaces Haar + MTCNN) ==")
    if os.path.exists(YUNET_OUT) and os.path.getsize(YUNET_OUT) > 0:
        print(f"  already present: {YUNET_OUT}")
        return
    print(f"  {YUNET_URL}")
    urllib.request.urlretrieve(YUNET_URL, YUNET_OUT)
    print(f"  wrote {YUNET_OUT}")


def main():
    ensure_out_dir()
    targets = sys.argv[1:] or ["mobilenet", "facenet", "yunet"]
    errors = []
    if "mobilenet" in targets:
        try: export_mobilenet()
        except Exception as e: errors.append(f"mobilenet: {e}")
    if "facenet" in targets:
        try: export_facenet()
        except Exception as e: errors.append(f"facenet: {e}")
    if "yunet" in targets:
        try: download_yunet()
        except Exception as e: errors.append(f"yunet: {e}")

    print("\n==================== SUMMARY ====================")
    for f in (MOBILENET_OUT, FACENET_OUT, YUNET_OUT):
        state = "OK" if os.path.exists(f) and os.path.getsize(f) > 0 else "MISSING"
        print(f"  [{state}] {f}")
    if errors:
        print("\nERRORS:")
        for e in errors:
            print("  - " + e)
        sys.exit(1)
    print("\nDone. These .onnx files are platform-independent; commit them so")
    print("they ship inside every installer (dmg/msi/deb/rpm/AppImage).")


if __name__ == "__main__":
    main()
