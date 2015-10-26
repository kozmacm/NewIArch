package com.github.newiarch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.LruCache;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

public class GalleryFragment extends Fragment {

	private GridView gridView;
	private GridViewAdapter customGridAdapter;
	public static File fileName = null;
	String folderName;
	static LruCache<String, Bitmap> mMemoryCache;
	static DiskLruImageCache mDiskLruCache;
	final static Object mDiskCacheLock = new Object();
	static boolean mDiskCacheStarting = true;
	private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
	private static final String DISK_CACHE_SUBDIR = "iarch_thumbs";
	List<CharSequence> list = new ArrayList<CharSequence>();
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		Bundle bundle = this.getArguments();
		if (bundle != null) {
			folderName = bundle.getString("EXTRAS_FOLDERNAME");
		}
		setHasOptionsMenu(true);
		View galleryView = inflater.inflate(R.layout.fragment_gallery, container, false);
		String[] shortFolderName = folderName.split("/"); 
		//getActionBar().setTitle(shortFolderName[6]);
		
		gridView = (GridView) galleryView.findViewById(R.id.gridView);
		customGridAdapter = new GridViewAdapter(getActivity(), R.layout.row_grid, getData());
		gridView.setAdapter(customGridAdapter);
		gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		gridView.setMultiChoiceModeListener(new MultiChoiceModeListener());
		
		//handle item click
		gridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				//get files in images directory
				String[] shortFileName = folderName.split("/");
				File path = new File(Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES) + "/iArch/" + shortFileName[6]);
			    File[] imageFiles = path.listFiles();
			    //use it like imageFiles[position]
			    fileName = imageFiles[position];
			    System.out.println("image selected : " + imageFiles[position]);
				
			    // Create new fragment and transaction
				Fragment newFragment = new ImageDetailsFragment();
				Bundle bundle = new Bundle();
				bundle.putString("EXTRAS_FILENAME", fileName.toString());
				newFragment.setArguments(bundle);
				FragmentTransaction transaction = getFragmentManager().beginTransaction();

				// Replace whatever is in the fragment_container view with this fragment,
				// and add the transaction to the back stack
				transaction.replace(R.id.container, newFragment);
				transaction.addToBackStack(null);

				// Commit the transaction
				transaction.commit();
			    
			}

		});
		/*
		//sync datastores so that no fields will be empty when picture clicked the first 
		//time or after user disconnects and reconnects dropbox
		DbxDatastore datastore;
		try {
			String[] splitFile = folderName.split("/");
			if (MainActivity.mAccountManager.hasLinkedAccount()) {	
				datastore = MainActivity.mDatastoreManager.openDatastore(splitFile[6]);
				datastore.sync();
				datastore.close();
			}
		} catch (DbxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		// Get max available VM memory, exceeding this amount will throw an
	    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
	    // int in its constructor.
	    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

	    // Use 1/8th of the available memory for this memory cache.
	    final int cacheSize = maxMemory / 8;

	    //RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(getFragmentManager());
	    //mMemoryCache = retainFragment.mRetainedCache;
	    //if (mMemoryCache == null) {
	    	mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
	    		@Override
	    		protected int sizeOf(String key, Bitmap bitmap) {
	    			// The cache size will be measured in kilobytes rather than
	    			// number of items.
	    			return bitmap.getByteCount() / 1024;
	    		}
	    	};
	    	//retainFragment.mRetainedCache = mMemoryCache;
	    //}
	    
	    
	    // Initialize disk cache on background thread
	    File cacheDir = getDiskCacheDir(getActivity(), DISK_CACHE_SUBDIR);
	    new InitDiskCacheTask().execute(cacheDir);

		return galleryView;
	}
	
	private ActionBar getActionBar() {
	    return getActivity().getActionBar();
	}
	
	private ArrayList<ImageItem> getData() {
		final ArrayList<ImageItem> imageItems = new ArrayList<ImageItem>();
		
		File path = new File(folderName);
	    File[] imageFiles = path.listFiles();
	    
	    for (int i = 0; i < imageFiles.length; i++) {
	    	String folderName = imageFiles[i].toString();
	    	String[] shortFolderName = folderName.split("/");
	    	imageItems.add(new ImageItem(imageFiles[i], shortFolderName[7], folderName));
	    }
	    
		return imageItems;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//inflater.inflate(R.menu.gallery_fragment, menu);
	}
	
	public class MultiChoiceModeListener implements GridView.MultiChoiceModeListener {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.setTitle("Select Items");
            mode.setSubtitle("One item selected");
            MenuInflater inflater = getActivity().getMenuInflater();
            //inflater.inflate(R.menu.contextual_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.action_delete_multiple:
				System.out.println("LIST: " + list);
				for (int i=0; i<list.size(); i++) {
					String fileName = list.get(i).toString();
					File file = new File(folderName + "/" + fileName);
					file.delete();			
				}
				list.clear();
				
				mode.finish();
			}
			GalleryFragment.mDiskLruCache.clearCache();
			
			//reload the adapter, including reloading memory and disk caches
			customGridAdapter = new GridViewAdapter(getActivity(), R.layout.row_grid, getData());
			gridView.invalidateViews();
			gridView.setAdapter(customGridAdapter);
			
			final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		    final int cacheSize = maxMemory / 8;

		    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
		    	@Override
		    	protected int sizeOf(String key, Bitmap bitmap) {
		    		// The cache size will be measured in kilobytes rather than
		    		// number of items.
		    		return bitmap.getByteCount() / 1024;
		    	}
		    };
		    		    
		    File cacheDir = getDiskCacheDir(getActivity(), DISK_CACHE_SUBDIR);
		    new InitDiskCacheTask().execute(cacheDir);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			customGridAdapter = new GridViewAdapter(getActivity(), R.layout.row_grid, getData());
			gridView.invalidateViews();
			gridView.setAdapter(customGridAdapter);
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			View singleView = gridView.getChildAt(position);
			TextView text = (TextView) singleView.findViewById(R.id.text);
			if (checked == true) {
				//item checked
				singleView.setBackgroundColor(getResources().getColor(android.R.color.background_light));
				text.setTextColor(Color.parseColor("#ff000000"));
				System.out.println("ID: " + text.getText());
				list.add(text.getText());
			} else {
				//item unchecked
				singleView.setBackgroundColor(Color.parseColor("#ff000000"));
				singleView.setBackgroundResource(R.drawable.gallery_object);
				text.setTextColor(Color.parseColor("#ffffff"));
				list.remove(text.getText());
			}
			
			//count number of items selected; display it at top of screen
			int selectCount = gridView.getCheckedItemCount();
            switch (selectCount) {
            case 1:
                mode.setSubtitle("One item selected");
                break;
            default:
                mode.setSubtitle("" + selectCount + " items selected");
                break;
            }
		}

	}
	
	class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
	    @Override
	    protected Void doInBackground(File... params) {
	        synchronized (mDiskCacheLock) {
	            //File cacheDir = params[0];
	            mDiskLruCache = new DiskLruImageCache(getActivity(), folderName, DISK_CACHE_SIZE, Bitmap.CompressFormat.PNG, 100);
	            mDiskCacheStarting = false; // Finished initialization
	            mDiskCacheLock.notifyAll(); // Wake any waiting threads
	        }
	        return null;
	    }
	}

	// Creates a unique subdirectory of the designated app cache directory.
	public static File getDiskCacheDir(Context context, String uniqueName) {
	    final String cachePath = context.getCacheDir().getPath();

	    return new File(cachePath + File.separator + uniqueName);
	}
			
	static class RetainFragment extends Fragment {
	    private static final String TAG = "RetainFragment";
	    public LruCache<String, Bitmap> mRetainedCache;

	    public RetainFragment() {}

	    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
	        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
	        if (fragment == null) {
	            fragment = new RetainFragment();
	            fm.beginTransaction().add(fragment, TAG).commit();
	        }
	        return fragment;
	    }

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setRetainInstance(true);
	    }
	}
	
	
}
