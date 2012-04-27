package com.hapi.hapicommander;

import java.io.File;

import java.util.Comparator;

public class FileComparator implements Comparator<File> 
{
	public static enum Types
	{
		NAME, 
		SIZE;
	}
	
	public FileComparator()
	{
		this(Types.NAME);
	}
	public FileComparator(Types type)
	{
		this.m_type = type;
	}
	public int compare(File file, File compared) 
	{
		if(file==null)
			return -1;
		if(compared==null)
			return 1;
		switch(m_type)
		{
		case SIZE:
			return compare_size(file,compared);
		default:
			return compare_name(file,compared);
		}
	}

	private int compare_type(File file, File compared) 
	{
		if(file.isDirectory() && !compared.isDirectory())
		{ 
			return -1;
		}  
		else if(!file.isDirectory() && compared.isDirectory())
		{ 
			return 1;
		} 
		return 0;
	}

	private int compare_size(File file, File compared) 
	{
		int ret = compare_type(file,compared);
		if(ret != 0)
			return ret;

		if(file.isFile() && compared.isFile())
		{
			long len1= file.length();
			long len2 = compared.length();
			//return new Long(file.length()).compareTo(new Long(compared.length();));
			return len1 < len2 ? -1 : (len1 == len2 ? 0 : 1);
		} 
		else  
		{
			return file.getName().toLowerCase().compareTo(compared.getName().toLowerCase());
		}
	}

	private int compare_name(File file, File compared) 
	{
		int ret = compare_type(file,compared);
		if(ret != 0)
			return ret;

		return file.getName().toLowerCase().compareTo(compared.getName().toLowerCase());
	}
	private Types m_type;
	
}