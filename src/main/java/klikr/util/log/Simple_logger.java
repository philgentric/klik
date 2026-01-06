// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.log;

//*******************************************************
public class Simple_logger implements Logger
//*******************************************************
{

	//*******************************************************
	@Override
	public void log( String s)
	//*******************************************************
	{
		System.out.println(s);
	}

}
