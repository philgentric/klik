package klik.browser.comparators;

import java.nio.file.Path;

public record Path_pair(Integer i, Integer j){

    public static Path_pair get(int i, int j){
        if ( i < j) return new Path_pair(i,j);
        else return new Path_pair(j,i);
    }

};
