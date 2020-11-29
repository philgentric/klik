package klik.util;

public class Key_value implements Comparable<Key_value>
{
		public String key;
		public String value;
		//**********************************************************
		public Key_value(String k, String v) 
		//**********************************************************
		{
			key = k;
			value = v;
		}
		@Override
		public int compareTo(Key_value o) 
		{
			return value.compareTo(o.value);
		}
}
