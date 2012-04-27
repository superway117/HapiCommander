package com.hapi.hapicommander;

import java.io.File;

public class FileListEntry
{

	public FileListEntry(File dir,String name,boolean is_mark)
	{
		if(name.equals(".."))
		{
			mFile = null;
			mIsMarked=false;
		}
		else
		{
			mFile = new File(dir,name);
			mIsMarked=is_mark;
		}
	}
	public File getFile()
	{
		return mFile;
	}
	public boolean isMark()
	{
		return mIsMarked;
	}
	
	private File mFile;
	private boolean mIsMarked;
}
