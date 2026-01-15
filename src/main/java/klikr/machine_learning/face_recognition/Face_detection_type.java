// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.face_recognition;

public enum Face_detection_type
{
    alt_default,
    alt_tree,
    alt1,
    alt2,
    MTCNN;

    String get_xml_file_name()
    {
        switch(this)
        {
            case alt_default:
                return "haarscascade_frontalface_alt_default.xml";
            case alt_tree:
                return "haarscascade_frontalface_alt_tree.xml";
            case alt1:
                return "haarscascade_frontalface_alt_1.xml";
            case alt2:
                return "haarscascade_frontalface_alt_2.xml";
        }
        return "";
    }
}

