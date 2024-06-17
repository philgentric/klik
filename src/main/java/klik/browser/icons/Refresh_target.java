package klik.browser.icons;

import klik.browser.Change_type;
import klik.util.Hourglass;

import java.util.concurrent.CountDownLatch;

public interface Refresh_target {
    void refresh_UI_after_scan_dir_5(Change_type change_type, String from, Hourglass running_man);
}
