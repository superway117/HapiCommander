package com.hapi.hapicommander;

import java.io.IOException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;




public class HapiCommander extends Activity 
{
	/**
	 * Logging tag.
	 */
	private final static String LOG_TAG = "HapiCommander";

    private static final int DIALOG_MULTI_DELETE = 4;
    private static final int DIALOG_FILTER = 5;
    private static final int DIALOG_DETAILS = 6;
    
    private static final int DIALOG_BOOKMARKS = 7;
    private static final int DIALOG_COMPRESSING = 8;
    private static final int DIALOG_WARNING_EXISTS = 9;
    private static final int DIALOG_CHANGE_FILE_EXTENSION = 10;
    private static final int DIALOG_MULTI_COMPRESS_ZIP = 11;

    private static final int DIALOG_DISTRIBUTION_START = 100; // MUST BE LAST

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
      //cust title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.cust_title);
        
 //list view
        mListAdapter=new FileListAdapter(this,R.layout.row_list,new FileDataProvider(this));
        mListView = (ListView) findViewById(R.id.list_filelist);
        registerForContextMenu(mListView);
        mListView.setAdapter(mListAdapter); 
        mListView.setOnItemClickListener(mOnClickListener);
        mListView.setFocusable(true);
        


//path view
        
        mPathView = (TextView) findViewById(R.id.text_path);
        mPathView.setText(mListAdapter.getCurrentDirectory().getAbsolutePath());

        
    }

    private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id)
        {
            onListItemClick((ListView)parent, v, position, id);
        }
    };
    public void onListItemClick(ListView listview, View v, int position, long id)
    {
        FileListEntry item = mListAdapter.getItem(position);
  
        if(item.getFile() == null)
        {
            Log.d(LOG_TAG,"onListItemClick getFile() == null");
            mListAdapter.up();
            mPathView.setText(mListAdapter.getCurrentDirectory().getAbsolutePath());
            mListView.setSelection(0);
            mListView.invalidateViews();
        }
        else if(item.getFile().isDirectory())
        {
            Log.d(LOG_TAG,"onListItemClick getFile() "+ item.getFile().getName());
            mListAdapter.navigateTo(position);
            mPathView.setText(mListAdapter.getCurrentDirectory().getAbsolutePath());
            mListView.setSelection(0);
            mListView.invalidateViews();

        }
        else
        {
                try 
                {

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(item.getFile()));
                    String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(item.getFile().getCanonicalPath()));
                    if(type!=null)
                    {
                        intent.setType(type);
                    }
                    
                    startActivity(intent);
                } 
                catch (IOException e) 
                {
                    Log.e(LOG_TAG, "open file IOException",e);
                } 
                catch (ActivityNotFoundException e) 
                {
                    Log.e(LOG_TAG, "open file ActivityNotFoundException",e);
                }
                catch (Exception e)
                {
                    Log.e(LOG_TAG, "open file Exception",e);
                }
   
            
        }
    }
    public boolean onOptionsItemSelected(MenuItem item)
    {

    	if (item.getItemId() == R.id.menu_createdir)
    	{
    		
    		return true;
    	}
    	else if (item.getItemId() == R.id.menu_paste)
    	{
    		
    		return true;
    	}
        else if (item.getItemId() == R.id.menu_deleteAll)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_markAll)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_unmarkAll)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_copyMarks)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_cutMarks)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_deleteMarks)
        {
            
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public boolean onContextItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.menu_copy)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_cut)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_mark)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_unmark)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_delete)
        {
            
            return true;
        }
        else if (item.getItemId() == R.id.menu_details)
        {
            
            return true;
        }
        return super.onContextItemSelected(item);
    }
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater(); //from activity
        inflater.inflate(R.menu.normal_menus, menu);
        return true;
    }
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater(); //from activity
        inflater.inflate(R.menu.file_menus, menu);
    }

    protected Dialog onCreateDialog(int id) 
    {
        switch (id)
        {
            case DIALOG_DETAILS:
            {
                LayoutInflater inflater = LayoutInflater.from(this);
                View view =  inflater.inflate(R.layout.dialog_details, null);
                        
                return new AlertDialog.Builder(this).setTitle(mContextText).
                        setIcon(mContextIcon).setView(view).create();
            }
        }
    }
    //view list
    private ListView            mListView= null;
    private TextView            mPathView= null;

    //list adapter
    private FileListAdapter     mListAdapter = null;

}

