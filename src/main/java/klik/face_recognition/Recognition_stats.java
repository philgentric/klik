package klik.face_recognition;

import java.util.concurrent.atomic.AtomicInteger;

public class Recognition_stats
{
    AtomicInteger done = new AtomicInteger(0);
    AtomicInteger no_face_detected = new AtomicInteger(0);
    AtomicInteger face_not_recognized = new AtomicInteger(0);
    AtomicInteger face_recognized = new AtomicInteger(0);
    AtomicInteger error = new AtomicInteger(0);

    public String to_string() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n   no_face_detected: ");
        sb.append(no_face_detected.get());

        sb.append("\n   face_not_recognized: ");
        sb.append(face_not_recognized.get());

        sb.append("\n   face_recognized: ");
        sb.append(face_recognized.get());

        sb.append("\n   error: ");
        sb.append(error.get());
        sb.append("\n   done: ");
        sb.append(done.get());
        return sb.toString();
    }
}
