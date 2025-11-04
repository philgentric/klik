// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.execute.actor;

// abstraction for passing parameters to an Actor
// 1 Message implies 1 Job
//**********************************************************
public interface Message
//**********************************************************
{
    String to_string();
    Aborter get_aborter(); // MUST NOT be null as it is needed to cancel the job
}
