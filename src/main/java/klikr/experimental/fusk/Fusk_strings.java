// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.experimental.fusk;

import klikr.util.log.Logger;


//**********************************************************
public class Fusk_strings
//**********************************************************
{

    //**********************************************************
    static String fusk_string(String input, Logger logger)
    //**********************************************************
    {
        char[] in = input.toCharArray();
        char[] out = fusk_string_internal(in);
        String returned = new String(out);
        if( returned.length() != input.length())
        {
            logger.log("warning string length changed"+input+"=>"+returned);
        }
        return returned;
    }

    //**********************************************************
    public static String defusk_string(String input, Logger logger)
    //**********************************************************
    {
        char[] in = input.toCharArray();
        char[] out = defusk_string_internal(in);
        String returned = new String(out);
        if( returned.length() != input.length())
        {
            logger.log("warning string length changed"+input+"=>"+returned);
        }
        return returned;
    }

    // numbers are NOT eligible, this makes sure numbered
    // items are displayed in the same order
    // "." is not eligible because of extensions

    static char[] eligible_for_change ={
            'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
            '_',' '};


    //**********************************************************
    private static char forward_legal_to_legal(char c)
    //**********************************************************
    {
        for (int i = 0; i < eligible_for_change.length; i++)
        {
            if ( c == eligible_for_change[i])
            {
                int j = i+7;
                if ( j>= eligible_for_change.length) j = j- eligible_for_change.length;
                return eligible_for_change[j];
            }
        }
        return c;
    }

    //**********************************************************
    private static char backward_legal_to_legal(char c)
    //**********************************************************
    {
        for (int i = 0; i < eligible_for_change.length; i++)
        {
            if ( c == eligible_for_change[i])
            {
                int j = i-7;
                if ( j< 0) j = j+ eligible_for_change.length;
                return eligible_for_change[j];
            }
        }
        return c;
    }

    //**********************************************************
    private static char[] fusk_string_internal(char[] in)
    //**********************************************************
    {
        char[] out = new char[in.length];
        for(int i = 0 ; i < in.length;i++)
        {
            out[i] = forward_legal_to_legal(in[i]);
        }
        return out;
    }

    //**********************************************************
    private static char[] defusk_string_internal(char[] in)
    //**********************************************************
    {
        char[] out = new char[in.length];
        for(int i = 0 ; i < in.length;i++)
        {
            out[i] = backward_legal_to_legal(in[i]);
        }
        return out;
    }
}
