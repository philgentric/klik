// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.experimental.deduplicate.console;


public class Thing_to_do {
    public final Type_of_thing_to_do type;
    public final String text;

    public static Thing_to_do get_increment_examined_thing_to_do() {
        return new Thing_to_do(Type_of_thing_to_do.increment_examined, null);
    }

    public static Thing_to_do get_increment_duplicates_thing_to_do() {
        return new Thing_to_do(Type_of_thing_to_do.increment_duplicates, null);
    }

    public static Thing_to_do get_increment_deleted_thing_to_do() {
        return new Thing_to_do(Type_of_thing_to_do.increment_deleted, null);
    }

    public static Thing_to_do get_status_thing_to_do(String text) {
        return new Thing_to_do(Type_of_thing_to_do.add_to_status, text);
    }

    private Thing_to_do(Type_of_thing_to_do type, String text) {
        this.type = type;
        this.text = text;
    }

    public static Thing_to_do get_die_thing_to_do() {
        return new Thing_to_do(Type_of_thing_to_do.die, null);
    }

    public static Thing_to_do get_increment_directory_examined_thing_to_do() {
        return new Thing_to_do(Type_of_thing_to_do.increment_directory_examined, null);
    }

    public static Thing_to_do get_total_to_be_examined_thing_to_do(int i) {
        return new Thing_to_do(Type_of_thing_to_do.set_total_to_be_examined, String.valueOf(i));
    }
}
