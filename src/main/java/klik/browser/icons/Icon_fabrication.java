package klik.browser.icons;

/*
state machine for an item to avoid multiple request to make the icon
 */
public enum Icon_fabrication {
    no_icon,
    default_icon,
    true_icon_requested,
    true_icon_in_the_making,
    true_icon,
    dont_make_icon
}
