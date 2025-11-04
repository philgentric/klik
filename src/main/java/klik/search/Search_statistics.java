// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.search;

import java.util.Map;

public record Search_statistics(int visited_folders, int visited_files, Map<String, Integer> matched_keyword_counts){}
