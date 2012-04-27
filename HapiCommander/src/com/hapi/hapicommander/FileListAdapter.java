package com.hapi.hapicommander;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;





public class FileListAdapter extends BaseAdapter
{
	
	private final static String LOG_TAG = "HapiCommander";
	
	public interface OnGetRowView 
	{
        void onGetView(int position, View view, Cursor cusor);
    }
	
	
	
	public FileListAdapter(Context context,int resource,FileDataProvider provider)
	{

		init(context,resource,provider);
	}
	private void init(Context context,int resource,FileDataProvider provider)
	{
		mContext = context;
		mProvider = provider;
        if(provider==null)
            mProvider=new FileDataProvider(context);
		mResource = resource;
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		refresh();
		//defalut is list type
		mOnGetRowView = new OnListTypeGetRowView();
	}
	public int getCount()
	{
		return (int)mProvider.getCount();
	}
	
    public File getFile(int position)
    {
        Cursor cursor = mProvider.query(position);
        if(cursor == null)
            return null;
        
        String name = cursor.getString(FileCacheDB.FILE_NAME_COL_INDEX);
        return new File(getCurrentDirectory(),name);
    }
	public FileListEntry getItem(int position)
    {
		Cursor cursor = mProvider.query(position);
		if(cursor == null)
			return null;
		int mark = cursor.getInt(FileCacheDB.MARK_COL_INDEX);
        String name = cursor.getString(FileCacheDB.FILE_NAME_COL_INDEX);
		return new FileListEntry(getCurrentDirectory(),name,mark>0?true:false);
    }

	public long getItemId(int position)
    {
    	return position;
    }
    
    public boolean isEmpty()
    {
        return mProvider.getCount()==0;
    }
    public void refresh()
    {
        mProvider.refresh();
        
    }
    
    public void up()
    {
        mProvider.up();

    }
    
    public void root()
    {
        mProvider.root();
    }
    
    public void navigateTo(int position)
    {
        mProvider.navigateTo(position);
    }
    
    public File getCurrentDirectory()
	{
		return mProvider.getCurrentDirectory();
	}
    
    
    public boolean delete(int position)
    {
        return mProvider.delete(position);
    }
    
    public boolean rename(int position,String new_name)
    {
        return mProvider.rename(position,new_name);
    }
    public boolean mkdir(String name)
    {
        return mProvider.mkdir(name);
    }

    public boolean mark(int position,boolean is_mark)
    {
        return mProvider.mark(position,is_mark);
    }

    public void markAll() 
    {
        mProvider.markAll();
    }

    public void unMarkAll()
    {
        mProvider.unMarkAll();
    } 


    public void setOnGetRowView(OnGetRowView getview) 
    {
    	mOnGetRowView = getview;
    }
    public void setListTypeOnGetRowView() 
    {
    	mOnGetRowView = new OnListTypeGetRowView();
    }
    public void setDetailsTypeOnGetRowView() 
    {
    	mOnGetRowView = new OnDetailsTypeGetRowView();
    }

    public final OnGetRowView getOnGetRowView() 
    {
        return mOnGetRowView;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) 
    {
        return createViewFromResource(position, convertView, parent, mResource);
    }
    
    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) 
    {
    	View view;
    	
        if (convertView == null) 
        {
            view = mInflater.inflate(resource, parent, false);
        } 
        else 
        {
            view = convertView;
        }
        Cursor cusor = mProvider.query(position);

        if(mOnGetRowView!=null && cusor != null)
        	mOnGetRowView.onGetView(position,view,cusor);
        return view;
    }
    
  //implement list type
    private  class OnListTypeGetRowView implements OnGetRowView
    {
    	public void onGetView(int position, View view, Cursor cursor)
    	{
    		TextView textview_name;
    		TextView textview_size;
    		ImageView imageview_icon;
    		
    		try 
    		{
	    			textview_name = (TextView) view.findViewById(R.id.text_name);
	    			textview_size = (TextView) view.findViewById(R.id.text_size);
	    			imageview_icon = (ImageView) view.findViewById(R.id.image_fileicon);
    	
            } 
    		catch (ClassCastException e) 
    		{
                Log.e("OnListTypeGetRowView", "OnListTypeGetRowView requires a right row layout");
                throw new IllegalStateException("OnListTypeGetRowView requires a right row layout", e);
            }
    		
    		if(cursor!=null)
    		{
    			try 
    			{
	                int type = cursor.getInt(FileCacheDB.TYPE_COL_INDEX);
	                long size = cursor.getInt(FileCacheDB.SIZE_COL_INDEX);
                    String name = cursor.getString(FileCacheDB.FILE_NAME_COL_INDEX);
                    int icon = R.drawable.folder;
	                textview_name.setText(name);
		    		if(type == FileCacheDB.FILE_TYPE)
                    {
                        if(size>10*1024)
                        {
                            long kb = size/1024 + (size%1024)>512 ? 1 : 0;
		    			    textview_size.setText(""+kb+"kb");
                        }
                        else
                            textview_size.setText(""+size+"b");
                        icon  = MimeTypeMap.getSingleton().getDefaultIconFromExtension(MimeTypeMap.getFileExtensionFromName(name));
                        if(icon == -1)
                            icon = R.drawable.common_file;
                    }
		    		else if(!name.equals(".."))
		    			textview_size.setText("DIR");
                    else
                        textview_size.setText("");
                    imageview_icon.setImageResource(icon);
	                
	                //textview_size.setText("size");
	                //textview_name.setText("name");
    			}
    			catch (Exception e) 
    			{
    				Log.e(LOG_TAG, "OnListTypeGetRowView get data from curso",e);
                    textview_size.setText(e.getMessage());
                    textview_name.setText(e.toString());

    			}

    		}
    		else
    		{
    			textview_name.setText("..");
                textview_size.setText("");
    		}
    			
    	}
    }
    //implement details type
    private static class OnDetailsTypeGetRowView implements OnGetRowView
    {
    	public void onGetView(int position, View view, Cursor cusor)
    	{
    		
    	}
    }
    
    private Context                 mContext           =       null;
    private FileDataProvider        mProvider           =       null;
    private int                     mResource; 
    private LayoutInflater          mInflater;
    private OnGetRowView            mOnGetRowView      =       null;
}


