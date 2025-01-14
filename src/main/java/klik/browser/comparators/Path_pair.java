package klik.browser.comparators;

import java.nio.file.Path;

public record Path_pair(Path i, Path j){

    public static Path_pair get(Path i, Path j){
        if ( i.hashCode() < j.hashCode()) return new Path_pair(i,j);
        else return new Path_pair(j,i);
    }

};
