
package com.hapi.hapicommander;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class FileCacheDB
{
    private static final String LOG_TAG              =       "FileCacheDB";

    private static final String DATABASE_FILE       =       "filecache.db";
    private static final String TABLE_NAME          =       "file_tb";

    // column id strings for "_id" which can be used by any table
    //private static final String ID_COL            =       "_id";

    private static final String FILE_NAME_COL               =       "name";
    public static final int  FILE_NAME_COL_INDEX         =       0;

    private static final String TYPE_COL                    =       "type";
    public static final int  TYPE_COL_INDEX              =       1;

    public static final int    FILE_TYPE                   =       1;
    public static final int    DIRECTORY_TYPE              =       0;

    private static final String SIZE_COL                    =       "size";
    public static final int  SIZE_COL_INDEX              =       2;

    private static final String LAST_MODIFY_COL             =       "last_modify";
    public static final int  LAST_MODIFY_COL_INDEX       =       3;

    private static final String MARK_COL                    =       "mark";
    public static final int  MARK_COL_INDEX              =       4;

    //order by
    public static final String ORDER_BY_NAME       =       TYPE_COL + "," + FILE_NAME_COL;

    public static final String ORDER_BY_SIZE       =       TYPE_COL + "," + SIZE_COL;

    private FileCacheDB(Context context)
    {
      
        try
        {
            mDatabase = context.openOrCreateDatabase(DATABASE_FILE, Context.MODE_PRIVATE, null);
        }
        catch (SQLiteException e)
        {
                // try again by deleting the old db and create a new one
            if (context.deleteDatabase(DATABASE_FILE))
            {
                 mDatabase = context.openOrCreateDatabase(DATABASE_FILE, Context.MODE_PRIVATE, null);
            }
        }

        createTable();
        createPreCompiledStatement();

        // use read_uncommitted to speed up READ
        mDatabase.execSQL("PRAGMA read_uncommitted = true;");

        //defalut a like A is true
        //mDatabase.execSQL("PRAGMA case_sensitive_like=ON;");

        // use per table Mutex lock, turn off database lock, this
        // improves performance as database's ReentrantLock is expansive
        mDatabase.setLockingEnabled(false);

        //GET each col index
        mTypeColIndex = mInserter.getColumnIndex(TYPE_COL);
        mFileNameColIndex = mInserter.getColumnIndex(FILE_NAME_COL);
        mLastmodifyColIndex = mInserter.getColumnIndex(LAST_MODIFY_COL);
        mSizeColIndex = mInserter.getColumnIndex(SIZE_COL);
        mMarkColIndex = mInserter.getColumnIndex(MARK_COL);

    }

    public static synchronized FileCacheDB getInstance(Context context)
    {
        if (mInstance == null)
        {
            mInstance = new FileCacheDB(context);
        }
        return mInstance;
    }
    public void close()
    {
        clear();        //clear table

        clearPreCompiledStatement();

        mDatabase.close();
        mDatabase = null;

        mInstance = null;
    }

    private void createInserter()
    {
        mInserter = new DatabaseUtils.InsertHelper(mDatabase, TABLE_NAME);
    }
    private void closeInserter()
    {
        mInserter.close();
        mInserter = null;
    }
    private void createPreCompiledStatement()
    {

        mGetCountStatement = mDatabase.compileStatement("SELECT COUNT(*)" + " FROM " + TABLE_NAME + ";");

        mDeleteStatement = mDatabase.compileStatement("DELETE FROM " + TABLE_NAME + " WHERE " + FILE_NAME_COL + "=?;");

        mDeleteAllStatement = mDatabase.compileStatement("DELETE FROM " + TABLE_NAME + ";");

        mMarkStatement = mDatabase.compileStatement(
                             "UPDATE " + TABLE_NAME +
                             " SET " + MARK_COL + "=?" +
                             " WHERE " + FILE_NAME_COL + "=?;");

        mMarkAllStatement = mDatabase.compileStatement(
                                "UPDATE " + TABLE_NAME +
                                " SET " + MARK_COL + "=?" +
                                " WHERE " + FILE_NAME_COL + "!='..';");

        mUpdateNameStatement = mDatabase.compileStatement(
                                   "UPDATE " + TABLE_NAME +
                                   " SET " + FILE_NAME_COL + "=?" +
                                   " WHERE " + FILE_NAME_COL + "=?;");

        createInserter();

    }
    private void clearPreCompiledStatement()
    {

        mGetCountStatement = null;
        mDeleteStatement = null;
        mDeleteAllStatement = null;
        mMarkStatement = null;
        mMarkAllStatement = null;
        mUpdateNameStatement = null;

        closeInserter();

    }

    public void beginTransaction()
    {
        mDatabase.beginTransaction();
    }

    public void endTransaction()
    {
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    public void createTable()
    {
        mDatabase.beginTransaction();
        try
        {
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                              //+ " (" + ID_COL + " INTEGER PRIMARY KEY, "
                              + " (" + FILE_NAME_COL + " TEXT PRIMARY KEY, "
                              + TYPE_COL + " INTEGER DEFAULT 0, "
                              + SIZE_COL + " INTEGER DEFAULT 0, "
                              + LAST_MODIFY_COL + " INTEGER DEFAULT 0, "
                              + MARK_COL + " INTEGER DEFAULT 0"
                              + ");");
            
            //   clear();
            mDatabase.setTransactionSuccessful();
        }
        catch (SQLiteException e)
        {
            Log.e(LOG_TAG, "createTable", e);
        }
        finally
        {
            mDatabase.endTransaction();
        }
    }
    //return lastChangeCount
    public boolean clear()
    {
        if (mDatabase == null)
        {
            return false;
        }
        try
        {
            synchronized (mLock)
            {
                mDatabase.beginTransaction();
                mDeleteAllStatement.clearBindings();
                mDeleteAllStatement.execute();
                mDatabase.setTransactionSuccessful();
                mDatabase.endTransaction();
                return true;
            }
        }
        catch (IllegalStateException e)
        {
            Log.e(LOG_TAG, "clear", e);
        }
        return false;
    }

    //return lastInsertRow or -1
    public  long add(File c)
    {
        if (mDatabase == null)
        {
            return -1;
        }
        mInserter.prepareForInsert();
        if (c != null)
        {
            int file_type = c.isFile() ? FILE_TYPE : DIRECTORY_TYPE;

            mInserter.bind(mTypeColIndex, file_type);
            mInserter.bind(mFileNameColIndex, c.getName());
            mInserter.bind(mLastmodifyColIndex, c.lastModified());
            mInserter.bind(mSizeColIndex, c.length());
        }
        else
        {
            mInserter.bind(mTypeColIndex, DIRECTORY_TYPE);
            mInserter.bind(mFileNameColIndex, "..");
        }

        return mInserter.execute();
    }


    public boolean delete(String name)
    {
        if (name == null || mDatabase == null)
        {
            return false;
        }
        try
        {
            synchronized (mLock)
            {
                mDeleteStatement.clearBindings();
                mDeleteStatement.bindString(1, name);
                mDeleteStatement.execute();
                //return mDatabase.lastChangeCount();
                return true;
            }
        }
        catch (IllegalStateException e)
        {
            Log.e(LOG_TAG, "query", e);
        }
        return false;
    }

    public Cursor query(String order_by, int offset,int limit )
    {
        Log.d(LOG_TAG, "query order=" +order_by + ",limit=" + limit+ ",offset="+offset);
        if (mDatabase == null)
        {
            return null;
        }

        try
        {
            Cursor cursor = null;
            String limit_str = "" + offset  + "," +  limit;
            cursor = mDatabase.query(false, TABLE_NAME, null, null, null, null, null, order_by, limit_str);
            return cursor;
        }
        catch (IllegalStateException e)
        {
            Log.e(LOG_TAG, "query", e);
        }
		return null;

    }

    //fuzzy query
    public Cursor query(String name)
    {
        Log.d(LOG_TAG, "query:"+name);
        if (mDatabase == null)
        {
            return null;
        }
        try
        {
            Cursor cursor = null;
            String[] selectionArgs = {name,};
            cursor = mDatabase.rawQuery("SELECT * FROM " + TABLE_NAME + "WHERE "
                                             + FILE_NAME_COL + " LIKE ?%;", selectionArgs);
            return cursor;
        }
        catch (IllegalStateException e)
        {
            Log.e(LOG_TAG, "query", e);
        }
        return null;
    }

    public long getCount()
    {
        Log.d(LOG_TAG, "getCount");
        if (mDatabase == null)
        {
            return -1;
        }
        try
        {

            mGetCountStatement.clearBindings();
            return mGetCountStatement.simpleQueryForLong();

        }
        catch (IllegalStateException e)
        {
            Log.e(LOG_TAG, "getCount", e);
        }
        return -1;
    }

    public boolean mark(String filename, boolean is_mark)
    {

        if (mDatabase == null)
        {
            return false;
        }
        try
        {
            synchronized (mLock)
            {
                mMarkStatement.clearBindings();
                mMarkStatement.bindLong(1, (long)(is_mark?1:0));
                mMarkStatement.bindString(2, filename);
                mMarkStatement.execute();
                //return mDatabase.lastChangeCount();
                return true;
            }
        }
        catch (IllegalStateException e)
        {
            Log.e(LOG_TAG, "makr", e);
        }
        return false;
    }

    public boolean markAll(boolean is_mark)
    {

        if (mDatabase == null)
        {
            return false;
        }
        try
        {
            synchronized (mLock)
            {
                mMarkAllStatement.clearBindings();
                mMarkAllStatement.bindLong(1, (long)(is_mark?1:0));
                mMarkAllStatement.execute();
                //return lastChangeCount();
                return true;
            }
        }
        catch (IllegalStateException e)
        {
            Log.e(LOG_TAG, "markAll", e);
        }
        return false;
    }
    public boolean updateName(String old_name, String new_name)
    {

        if (mDatabase == null)
        {
            return false;
        }
        try
        {
            synchronized (mLock)
            {
                mUpdateNameStatement.clearBindings();
                mUpdateNameStatement.bindString(1, old_name);
                mUpdateNameStatement.bindString(2, new_name);
                mUpdateNameStatement.execute();
                //return lastChangeCount();
                return true;
            }
        }
        catch (IllegalStateException e)
        {
            Log.e(LOG_TAG, "makr", e);
        }
        return false;
    }


    private static FileCacheDB              mInstance = null;

    private SQLiteDatabase                  mDatabase = null;


    //private SQLiteStatement                 mQueryer;

    private static int                      mFileNameColIndex;
    private static int                      mSizeColIndex;
    private static int                      mLastmodifyColIndex;
    private static int                      mTypeColIndex;
    private static int                      mMarkColIndex;


    //pre compiled statement
    private SQLiteStatement                 mGetCountStatement;
    private SQLiteStatement                 mMarkStatement;
    private SQLiteStatement                 mMarkAllStatement;

    private SQLiteStatement                 mUpdateNameStatement;
    private SQLiteStatement                 mDeleteStatement;
    private SQLiteStatement                 mDeleteAllStatement;

    private DatabaseUtils.InsertHelper      mInserter;

    private final Object                    mLock     = new Object();
}
