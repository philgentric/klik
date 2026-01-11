// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.similarity;

import java.nio.file.Path;
import java.nio.file.Paths;

// DO NOT use the constructor !
//**********************************************************
public record Path_pair(Path i, Path j)
//**********************************************************
{


    public static Path_pair build(Path i, Path j)
    {
        // trying to make sure pairs are in the same order
        // i.e. pair(i,j) == pair(j,i)
        if ( i.hashCode() < j.hashCode()) return new Path_pair(i,j);
        else return new Path_pair(j,i);
    }

    public String to_string_key()
    {
        // The null character is not allowed in file paths on any operating system.
        return i.toAbsolutePath().normalize().toString()+"\0"+j.toAbsolutePath().normalize().toString();
    }

    public static Path_pair from_string_key(String s)
    {
        String[] parts = s.split("\0", 2); // Split into exactly 2 parts
        Path i = Paths.get(parts[0]);
        Path j = Paths.get(parts[1]);
        return Path_pair.build(i,j);
    }

}
