// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.face_recognition;

import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Recognition_stats
//**********************************************************
{
    long start = System.currentTimeMillis();
    AtomicInteger no_face_detected = new AtomicInteger(0);
    AtomicInteger face_not_recognized = new AtomicInteger(0);
    AtomicInteger face_recognized = new AtomicInteger(0);
    AtomicInteger error = new AtomicInteger(0);
    AtomicInteger skipped = new AtomicInteger(0);
    AtomicInteger done = new AtomicInteger(0);

    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        int local_error = error.get();
        int local_skipped = skipped.get();
        int local_no_face_detected = no_face_detected.get();
        int local_face_not_recognized = face_not_recognized.get();
        int local_face_recognized = face_recognized.get();
        int local_done = done.get();

        int tot = 0;
        StringBuilder sb = new StringBuilder();

        sb.append("\n   error: ");
        sb.append(error);
        tot += local_error;

        sb.append("\n   skipped: ");
        sb.append(local_skipped);
        tot += local_skipped;

        sb.append("\n   no_face_detected: ");
        sb.append(local_no_face_detected);
        tot += local_no_face_detected;

        sb.append("\n   face_not_recognized: ");
        sb.append(local_face_not_recognized);
        tot += local_face_not_recognized;

        sb.append("\n   face_recognized: ");
        sb.append(local_face_recognized);
        tot += local_face_recognized;

        sb.append("\n   done: ");
        sb.append(local_done);
        tot -= local_done;
        sb.append(" rate: ");
        long elapsed = System.currentTimeMillis()-start;
        sb.append(String.format("%.2f",1000.0*(double)local_done/(double) elapsed));
        sb.append(" faces per second");
        if ( tot != 0)
        {
            sb.append("\n   WARNING: sum does not match");
        }
        return sb.toString();
    }
}
