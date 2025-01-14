package klik.browser.comparators;

public record Path_pair_int(Integer i, Integer j){

    public static Path_pair_int get(int i, int j){
        if ( i < j) return new Path_pair_int(i, j);
        else return new Path_pair_int(j, i);
    }

};
