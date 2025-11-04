// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.machine_learning.similarity;

import java.nio.file.Path;

// dont use the constructor
//**********************************************************
public record Path_pair(Path i, Path j)
//**********************************************************
{


    public static Path_pair get(Path i, Path j)
    {
        // trying to make sure pairs are in the same order
        // i.e. pair(i,j) == pair(j,i)
        if ( i.hashCode() < j.hashCode()) return new Path_pair(i,j);
        else return new Path_pair(j,i);
    }

}
