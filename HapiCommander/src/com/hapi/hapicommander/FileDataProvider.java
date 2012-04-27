package com.hapi.hapicommander;

import java.io.File;
import java.io.FileFilter;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;


public class FileDataProvider 
{

	private final static String LOG_TAG = "FileDataProvider";

	private static final int CACHED_NUMBER = 100;

	public static final int ORDER_BY_NAME = 0;
	public static final int ORDER_BY_SIZE = 1;


	public FileDataProvider(Context context) 
	{
		init(context,Environment.getExternalStorageDirectory().getAbsolutePath(),ORDER_BY_NAME,null);
	}
	public FileDataProvider(Context context,String path) 
	{
		init(context,path,ORDER_BY_NAME,null);
	}
	public FileDataProvider(Context context,String path,int order_by) 
	{
		init(context,path,order_by,null);
	}
	
	public FileDataProvider(Context context,String path,int order_by,FileFilter filter) 
	{
		init(context,path,order_by,filter);
	}
	
	private void init(Context context,String path,int order_by,FileFilter filter) 
	{
		this.mContext = context;
		
		mCurrentDirectory = new File(path);
		if(mCurrentDirectory==null || !mCurrentDirectory.isDirectory())
			mCurrentDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
		
		mDB = FileCacheDB.getInstance(context);

		if(order_by==ORDER_BY_SIZE)
			mOrderBy = FileCacheDB.ORDER_BY_SIZE;
		else
			mOrderBy = FileCacheDB.ORDER_BY_NAME;
		
		mFilter = filter;
		
	}

	
	public File getCurrentDirectory()
	{
		return mCurrentDirectory;
	}
	
	public long getCount()
	{
		if(mCacheCount<0)
			mCacheCount =  mDB.getCount();
		return mCacheCount;
	}
	
	public boolean root() 
	{
		return navigateTo(new File("/"));
	}

	public boolean up() 
	{
		if (mCurrentDirectory.getParentFile() != null) 
		{
			return navigateTo(mCurrentDirectory.getParentFile());
		}
		return false;
	}

	public boolean refresh() 
	{
		Log.d(LOG_TAG,"refresh ");
		return navigateTo(mCurrentDirectory);
	}

	public boolean mark(int position,boolean is_mark) 
	{
		Cursor cursor = query(position);
		if(cursor==null)
			return false;
		return mDB.mark(cursor.getString(FileCacheDB.FILE_NAME_COL_INDEX),is_mark);
	}

	public void markAll() 
	{
		markAllImpl(true);
	}

	public void unMarkAll() 
	{
		markAllImpl(false);
	}

	private void markAllImpl(boolean marked) 
	{
		mDB.markAll(marked);
		
	}

	public boolean navigateTo(int position) 
	{

		Cursor cursor = query(position);
		
		if(cursor==null)
			return false;
		String name = cursor.getString(FileCacheDB.FILE_NAME_COL_INDEX);
		int type = cursor.getInt(FileCacheDB.TYPE_COL_INDEX);
		if(type==FileCacheDB.FILE_TYPE)
			return false;
		Log.d(LOG_TAG,"navigateTo position ="+ position + ",name="+name);
		if(!name.equals(".."))
		{
			File sub_dir=new File(mCurrentDirectory,name);
			return navigateTo(sub_dir);
		}
		else
			return up();
	}
	
	private void clearCache()
	{
		mDB.clear();
		clearCursor();
		mCacheCount = -1;
	}
	private void clearCursor()
	{
		mCursorOffset = -1;
		mCursorLimit = -1;
		mCursor = null;
	}
	private boolean isCursorValid(int position)
	{
		if(mCursorOffset == -1 || mCursorLimit == -1 || mCursor == null)
			return false;
		if(position>=mCursorOffset && position<mCursorOffset+mCursorLimit)
			return true;
		return false;

	}

	private boolean navigateTo(File f) 
	{
		if(f.isDirectory())
		{
			Log.d(LOG_TAG,"navigateTo dir "+ f.getName());
			clearCache();
			mCurrentDirectory = f;
			mDB.beginTransaction();
			if (!f.getAbsolutePath().equals("/")) 
			{
				mDB.add(null);
			}
			File[] files = f.listFiles(mFilter);
			if(files != null)
			{
				for (File file : files) 
				{
					mDB.add(file);
				}
			}
			mDB.endTransaction();
			return true;

		} 
		return false;
	}

	private int queryFromDB(int position)
	{
		mCursorOffset = position- CACHED_NUMBER/2;
		if(mCursorOffset<0)
			mCursorOffset=0;
		try
		{
			mCursor = mDB.query(mOrderBy,mCursorOffset,CACHED_NUMBER);
			mCursorLimit = mCursor.getCount();
		}
		catch (Exception e) 
		{
			clearCache();
			Log.e(LOG_TAG, "queryFromDB", e);
		}
		return mCursorLimit;

	}

	public Cursor query(int position)
	{
		if(!isCursorValid(position))
		{
			queryFromDB(position);
		}
		if(mCursor!=null)
			mCursor.moveToPosition(position - mCursorOffset);
		return mCursor;
	}

	private boolean deleteRecursive(File fileOrDirectory) 
    {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
            	deleteRecursive(child);
        return fileOrDirectory.delete();
    }

	public boolean delete(int position)
	{
		if(!isCursorValid(position))
		{
			query(position);
		}
		if(mCursor!=null)
		{
			String old_name = mCursor.getString(FileCacheDB.FILE_NAME_COL_INDEX);
			File old_file = new File(mCurrentDirectory,old_name);
			if(true == deleteRecursive(old_file))
			{
				mDB.delete(old_name);
				if(mCacheCount>0)
					mCacheCount--;
				return true;
			}
		}
		return false;
	}
	public boolean rename(int position,String new_name)
	{
		if(!isCursorValid(position))
		{
			query(position);
		}
		if(mCursor!=null)
		{
			String old_name = mCursor.getString(FileCacheDB.FILE_NAME_COL_INDEX);
			File new_file = new File(mCurrentDirectory,new_name);
			File old_file = new File(mCurrentDirectory,old_name);
			try
			{
				if(old_file.renameTo(new_file)==true)
				{
					mDB.updateName(old_name,new_name);
					clearCursor();
					return true;
				}
				else
				{
					return false;
				}
			}
			catch (SecurityException e)
			{
				return false;
			}
		}
		return false;
	}
	public boolean mkdir(String name)
	{
		try
		{
			File folder = new File(getCurrentDirectory(),name);
			boolean err =  folder.mkdir();
			if(err)
				clearCursor();
			return err;
		}
		catch (SecurityException e)
		{
			return false;
		}
	}
	private Context 					mContext				=		null;
	private File 						mCurrentDirectory		=		null;

	private FileCacheDB 				mDB						=		null;
	private String                      mOrderBy				=       FileCacheDB.ORDER_BY_NAME;  

	private Cursor						mCursor					=		null;
	private int 						mCursorOffset			=		-1;
	private int 						mCursorLimit			=		-1;


	private long						mCacheCount				=		0;

	private FileFilter 					mFilter 				= 		null;
	
	

}
