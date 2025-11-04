// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.log;

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
