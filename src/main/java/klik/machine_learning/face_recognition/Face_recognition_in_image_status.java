// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.face_recognition;

public enum Face_recognition_in_image_status {
    server_not_reacheable,
    error,
    face_detected,
    no_face_detected,
    no_feature_vector,
    no_face_recognized,
    feature_vector_ready,
    face_recognized,
    exact_match // special case of face recognized

}
