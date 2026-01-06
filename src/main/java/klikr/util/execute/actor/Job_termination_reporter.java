// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.execute.actor;

// this enables to have a method called after a given job
// instead of adding it in the actor code when the job is finished
// which may involve having to pass additional stuff in the Message

//**********************************************************
public interface Job_termination_reporter
//**********************************************************
{
    void has_ended(String message, Job job);
}
