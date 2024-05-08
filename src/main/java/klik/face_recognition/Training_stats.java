package klik.face_recognition;

import java.util.concurrent.atomic.AtomicInteger;

public class Training_stats
{
    AtomicInteger done = new AtomicInteger(0);
    AtomicInteger no_face_detected = new AtomicInteger(0);
    AtomicInteger face_wrongly_recognized_recorded = new AtomicInteger(0);
    AtomicInteger face_exact_recognized_not_recorded = new AtomicInteger(0);
    AtomicInteger face_correctly_recognized_not_recorded = new AtomicInteger(0);
    AtomicInteger face_recognized_recorded = new AtomicInteger(0);
    AtomicInteger error = new AtomicInteger(0);

    public String to_string() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n   no_face_detected: ");
        sb.append(no_face_detected.get());

        sb.append("\n   face_correctly_recognized_not_recorded: ");
        sb.append(face_correctly_recognized_not_recorded.get());

        sb.append("\n   face_exact_recognized_not_recorded: ");
        sb.append(face_exact_recognized_not_recorded.get());

        sb.append("\n   face_wrongly_recognized_recorded: ");
        sb.append(face_wrongly_recognized_recorded.get());

        sb.append("\n   face_recognized_recorded: ");
        sb.append(face_recognized_recorded.get());

        sb.append("\n   error: ");
        sb.append(error.get());

        sb.append("\n   done: ");
        sb.append(done.get());
        return sb.toString();
    }
}
