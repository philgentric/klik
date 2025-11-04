// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES icons/Error_type.java
package klik.browser;

import klik.browser.icons.Error_type;

public interface Error_receiver {
    void receive_error(Error_type error);
}
