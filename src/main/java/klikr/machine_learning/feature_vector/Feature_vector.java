// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

//**********************************************************
public interface Feature_vector
//**********************************************************
{
    String to_string();
    double distance(Feature_vector feature_vector);
}
