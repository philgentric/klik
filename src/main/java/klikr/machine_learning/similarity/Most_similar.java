// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.similarity;

import klikr.machine_learning.feature_vector.Feature_vector;

import java.nio.file.Path;

public record Most_similar(Path path, Feature_vector fv1, Feature_vector fv2, Double similarity){}
