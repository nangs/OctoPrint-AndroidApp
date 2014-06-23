package android.app.printerapp.devices;

import android.app.Activity;
import android.app.printerapp.R;
import android.app.printerapp.devices.discovery.JmdnsServiceListener;
import android.app.printerapp.model.ModelPrinter;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

/**
 * This is the fragment that will contain the Device VIEW logic
 * @author alberto-baeza
 *
 */
public class DevicesFragment extends Fragment{
	
	
	//Controllers and adapters
	private DevicesListController mListController;
	private DevicesGridAdapter mGridAdapter;
	private static DevicesListAdapter mListAdapter;
	
	//private DevicesLayoutAdapter mLayoutAdapter;
	
	//Empty constructor
	public DevicesFragment(){}
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		/**
		 * Since API level 11, thread policy has changed and now does not allow network operation to
		 * be executed on UI thread (NetworkOnMainThreadException), so we have to add these lines to 
		 * permit it.
		 */
		if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
		
		
		
		super.onCreate(savedInstanceState);
		
		//Retain instance to keep the Fragment from destroying itself
		setRetainInstance(true);
		
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		//Reference to View
		View rootView = null;
		
		//If is not new
		if (savedInstanceState==null){
			
			//Show custom option menu
			setHasOptionsMenu(true);
			
			//Inflate the fragment
			rootView = inflater.inflate(R.layout.devices_layout,
					container, false);
			
			/**
			 * CUSTOM VIEW METHODS
			 */
			
			//Set tab host for the view
			setTabHost(rootView);
			
			//List controller
			mListController = new DevicesListController();
			
			
			//------------------------------- View references -----------------//
									
			GridView g = (GridView) rootView.findViewById(R.id.devices_grid);

			mGridAdapter = new DevicesGridAdapter(getActivity(),
					R.layout.grid_element, mListController.getList());
			
			g.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {

					 arg1.startActionMode(mActionModeCallback);
					
				}
			});
	 
			g.setAdapter(mGridAdapter);
			
			
			
			
			
			/*******************************************************************/
			
			//Reference to the first tab
			/*ViewGroup mViewGroup = (ViewGroup) rootView.findViewById(R.id.devices_grid);
			mLayoutAdapter = new DevicesLayoutAdapter(getActivity(), mViewGroup);*/
			
			
			
			//Reference to the second tab, handled by an adapter
			mListAdapter = new DevicesListAdapter(getActivity(), 
					R.layout.list_element, mListController.getList());
			
			ListView l = (ListView) rootView.findViewById(R.id.devices_list);
			l.setAdapter(mListAdapter);
			
			
			/*******************************************************************/
			SlidingUpPanelLayout s = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_panel);
			TextView t = (TextView) rootView.findViewById(R.id.drag_text);
			s.setDragView(t);
			
			
			LinearLayout ll = (LinearLayout) rootView.findViewById(R.id.linearlayout_storage);
			
			new ControlBarStorage(ll,getActivity());
			
			//Custom service listener
			new JmdnsServiceListener(this);
		
		}
		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.devices_menu, menu);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	/**
	 * Constructor for the tab host
	 * TODO: Should be moved to a View class since it only handles ui.
	 */
	public void setTabHost(View v){
				 
		TabHost tabs=(TabHost) v.findViewById(android.R.id.tabhost);
		tabs.setup();
		 
		TabHost.TabSpec spec=tabs.newTabSpec("Status");
		spec.setIndicator("Status");
		spec.setContent(R.id.tab1);
		tabs.addTab(spec);
		 
		spec=tabs.newTabSpec("Videowall");
		spec.setIndicator("Videowall");
		spec.setContent(R.id.tab2);
		tabs.addTab(spec);
		
		 
		tabs.setCurrentTab(0);
		
		tabs.setOnTabChangedListener(new OnTabChangeListener() {
		    @Override
		    public void onTabChanged(String tabId) {
		        Log.i("CONTROLLER", "Tab pressed: " + tabId);
		    }
		});
		
	}
	
	/**
	 * LIST HANDLER
	 * TODO: Eventually this will add elements to a Database
	 */
	
	public void listHandler(final ModelPrinter m){
		
		getActivity().runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				
				mListController.addToList(m);
				
				/*************************************************************
				 * VIEW HANDLER
				 *************************************************************/

				mGridAdapter.notifyDataSetChanged();
				//mLayoutAdapter.addToLayout(m);
				mListAdapter.notifyDataSetChanged();
				
			}
		});

		
	}
	
	public static void notifyAdapter(){
		mListAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Callback for the  contextual menu as described @ Android Developers
	 */
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		
	    // Called when the action mode is created; startActionMode() was called
	    @Override
	    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
	        // Inflate a menu resource providing context menu items
	        MenuInflater inflater = mode.getMenuInflater();
	        inflater.inflate(R.menu.devices_cab_menu, menu);
	        return true;
	    }

	    // Called each time the action mode is shown. Always called after onCreateActionMode, but
	    // may be called multiple times if the mode is invalidated.
	    @Override
	    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
	        return false; // Return false if nothing is done
	    }

	    // Called when the user selects a contextual menu item
	    @Override
	    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
	        switch (item.getItemId()) {
	            case R.id.menu_cab_delete:
	                mode.finish(); // Action picked, so close the CAB
	                return true;
	                
	            case R.id.menu_cab_settings:
	            	mode.finish(); // Action picked, so close the CAB
	            	return true;
	            default:
	                return false;
	        }
	    }

	    // Called when the user exits the action mode
	    @Override
	    public void onDestroyActionMode(ActionMode mode) {
	       //mActionMode = false;
	    }
	};
	
	
	
}
