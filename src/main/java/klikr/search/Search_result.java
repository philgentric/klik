// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.search;

import java.nio.file.Path;
import java.util.List;

public record Search_result(Path path, List<String> matched_keywords){}
