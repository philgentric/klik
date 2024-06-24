package klik.browser;

public enum Change_type
{
    files_or_folders_changed, // a new file or folder, or a gone file or folder: requires a scan_dir: all_items_map AND pane are CLEARED
    layout_changed, // e.g. window size changed : requires to recompute all icon/buttons (x,y) positions
    visibility_changed, // when scrolling: check visibility, change Y of visible items
}
