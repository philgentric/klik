package klik.machine_learning.face_recognition;

import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Training_stats
//**********************************************************
{

    AtomicInteger skipped = new AtomicInteger(0);
    AtomicInteger error = new AtomicInteger(0);
    AtomicInteger no_face_detected = new AtomicInteger(0);
    AtomicInteger face_wrongly_recognized_recorded = new AtomicInteger(0);
    AtomicInteger face_correctly_recognized_not_recorded = new AtomicInteger(0);

    // to check the total:
    AtomicInteger done = new AtomicInteger(0);

    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        int local_error = error.get();
        int local_skipped = skipped.get();
        int local_no_face_detected = no_face_detected.get();
        int local_face_correctly_recognized_not_recorded = face_correctly_recognized_not_recorded.get();
        int local_face_wrongly_recognized_recorded = face_wrongly_recognized_recorded.get();
        int local_done = done.get();

        StringBuilder sb = new StringBuilder();
        int tot = 0;
        sb.append("\n   error: ");
        sb.append(local_error);
        tot += local_error;

        sb.append("\n   skipped: ");
        sb.append(local_skipped);
        tot += local_skipped;

        sb.append("\n   no_face_detected: ");
        sb.append(local_no_face_detected);
        tot += local_no_face_detected;

        sb.append("\n   face_correctly_recognized_not_recorded: ");
        sb.append(local_face_correctly_recognized_not_recorded);
        tot += local_face_correctly_recognized_not_recorded;

        sb.append("\n   face_wrongly_recognized_recorded: ");
        sb.append(local_face_wrongly_recognized_recorded);
        tot += local_face_wrongly_recognized_recorded;

        sb.append("\n   recognition rate: ");
        double rate = 100.0*(double) local_face_correctly_recognized_not_recorded/(double) (local_face_wrongly_recognized_recorded+local_face_correctly_recognized_not_recorded);
        String rate_s = String.format("%.1f",rate);
        sb.append(rate_s);

        sb.append("\n   done: ");
        sb.append(local_done);

        if ( tot-local_done != 0)
        {
            sb.append("\n   WARNING: sum does not match");
        }
        return sb.toString();
    }
}
