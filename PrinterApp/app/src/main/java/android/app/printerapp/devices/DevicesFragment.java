package android.app.printerapp.devices;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.printerapp.ItemListActivity;
import android.app.printerapp.R;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.devices.discovery.JmdnsServiceListener;
import android.app.printerapp.devices.discovery.PrintNetworkManager;
import android.app.printerapp.model.ModelPrinter;
import android.app.printerapp.octoprint.OctoprintFiles;
import android.app.printerapp.octoprint.StateUtils;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.DragShadowBuilder;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

import it.sephiroth.android.library.widget.HListView;

/**
 * This is the fragment that will contain the Device Grid and functionality
 *
 * @author alberto-baeza
 */
    public class DevicesFragment extends Fragment {


    //Controllers and adapters
    private DevicesGridAdapter mGridAdapter;
    private DevicesListAdapter mListAdapter;
    private DevicesCameraAdapter mCameraAdapter;

    //MUSIC!!!11!!1
    private static SoundPool mSoundPool;
    private static int mSoundMusic;
    private static boolean mSoundIsLoaded = false;

    //Network manager contoller
    private PrintNetworkManager mNetworkManager;
    private JmdnsServiceListener mServiceListener;

    /**
     * Additional variables
     */

    //Save current filter
    private int mFilter;


    //Empty constructor
    public DevicesFragment() {
    }


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
        if (savedInstanceState == null) {

            //Show custom option menu
            setHasOptionsMenu(true);

            //Inflate the fragment
            rootView = inflater.inflate(R.layout.devices_layout,container, false);

            /**
             * CUSTOM VIEW METHODS
             */

            //Set tab host for the view
            setTabHost(rootView);


            //------------------------------- View references -----------------//

            //Grid

            mGridAdapter = new DevicesGridAdapter(getActivity(),
                    R.layout.grid_item_printer, DevicesListController.getList());
            GridView gridView = (GridView) rootView.findViewById(R.id.devices_grid);
            gridView.setSelector(new ColorDrawable(getResources().getColor(R.color.transparent)));
            gridView.setOnItemClickListener(gridClickListener());
            gridView.setOnItemLongClickListener(gridLongClickListener());

            gridView.setAdapter(mGridAdapter);


            /*******************************************************************/

            //List

            mListAdapter = new DevicesListAdapter(getActivity(),
                    R.layout.list_item_drawer, DevicesListController.getList());

            ListView listView = (ListView) rootView.findViewById(R.id.devices_list);
//            listView.addHeaderView(inflater.inflate(R.layout.devices_list_header, null));
            listView.setOnItemClickListener(listClickListener());
            listView.setAdapter(mListAdapter);


            /*************** VIDEO HANDLER ****************************/

            mCameraAdapter = new DevicesCameraAdapter(getActivity(), R.layout.video_view, DevicesListController.getList());

            GridView cameraView = (GridView) rootView.findViewById(R.id.devices_camera);
            cameraView.setAdapter(mCameraAdapter);


            /***************** SLIDE PANEL ************************************/

            //Slide panel setup

            SlidingUpPanelLayout slidePanel = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_panel);
            slidePanel.setOverlayed(false);
            CheckBox imageButton = (CheckBox) rootView.findViewById(R.id.expand_button_checkbox);
            slidePanel.setDragView(imageButton);

            /******************* QUICKPRINT PANEL ************************************/

            HListView quickprintHorizontalListView = (HListView) rootView.findViewById(R.id.quickprint_horizontal_list_view);
            new DevicesQuickprint(getActivity(), quickprintHorizontalListView);

            /***************************************************************/

            //Custom service listener
            mServiceListener = new JmdnsServiceListener(this);
            mNetworkManager = new PrintNetworkManager(this);

            //Default filter
            mFilter = R.id.dv_radio0;

            /**
             * MUSIC!!!
             */

            loadMusic();

        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.devices_menu, menu);
    }

    //Option menu
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_filter: //Filter grid / list

                optionFilter();
                return true;

            case R.id.menu_reload: //Reload service discovery

                optionReload();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
    public void setTabHost(View v) {

        final TabHost tabs = (TabHost) v.findViewById(android.R.id.tabhost);
        tabs.setup();

        TabHost.TabSpec spec = tabs.newTabSpec("Map");
        spec.setIndicator(getString(R.string.devices_tabhost_tab_map));
        spec.setContent(R.id.tab1);
        tabs.addTab(spec);

        spec = tabs.newTabSpec("List");
        spec.setIndicator(getString(R.string.devices_tabhost_tab_list));
        spec.setContent(R.id.tab2);
        tabs.addTab(spec);

        spec = tabs.newTabSpec("Videowall");
        spec.setIndicator(getString(R.string.devices_tabhost_tab_video));
        spec.setContent(R.id.tab3);
        tabs.addTab(spec);


        tabs.setCurrentTab(0);

        //Set style for the tab widget
        for (int i = 0; i < tabs.getTabWidget().getChildCount(); i++) {
            final View tab = tabs.getTabWidget().getChildTabViewAt(i);
            tab.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_indicator_ab_green));
            TextView tv = (TextView) tabs.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTextColor(getResources().getColor(R.color.body_text_2));
        }

        tabs.setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {

            }
        });

    }

    /**
     * Add a new element to the list and notify the adapter
     * It's handled on this Fragment to allow dynamic addition
     *
     * @param m Printer to add
     */
    public void addElement(final ModelPrinter m) {

        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (!DevicesListController.checkExisting(m.getAddress())) {

                    DevicesListController.addToList(m);

                    //TODO Change linked switch
                    m.setNotLinked();
                    notifyAdapter();

                }

            }
        });

    }


    //Notify all adapters
    public void notifyAdapter() {

        try {
            mListAdapter.notifyDataSetChanged();
            mGridAdapter.notifyDataSetChanged();
            mCameraAdapter.notifyDataSetChanged();
        } catch (NullPointerException e) {
            //Random adapter crash
            e.printStackTrace();
        }

    }


    /**
     * Filter options
     */
    //TODO check if one adapter is possible
    public void optionFilter() {

        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.devices_filter_dialog_title);


        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.menu_filter_dialog, null, false);

        final RadioGroup rg = (RadioGroup) v.findViewById(R.id.radioGroup_devices);

        rg.check(mFilter);

        adb.setView(v);

        adb.setPositiveButton(R.string.filter, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (rg.getCheckedRadioButtonId()) {

                    case R.id.dv_radio0: {
                        mGridAdapter.getFilter().filter(null);//Show all devices
                        mListAdapter.getFilter().filter(null);//Show all devices

                    }
                    break;
                    case R.id.dv_radio1: {
                        mGridAdapter.getFilter().filter(String.valueOf(StateUtils.STATE_PRINTING));//Active printers
                        mListAdapter.getFilter().filter(String.valueOf(StateUtils.STATE_PRINTING));//Active printers

                    }
                    break;
                    case R.id.dv_radio2: {
                        mGridAdapter.getFilter().filter(String.valueOf(StateUtils.STATE_OPERATIONAL));    //Inactive printers
                        mListAdapter.getFilter().filter(String.valueOf(StateUtils.STATE_OPERATIONAL));    //Inactive printers

                    }
                    break;
                    case R.id.dv_radio3: {
                        mGridAdapter.getFilter().filter(null);    //TODO Groups
                    }
                    break;
                    case R.id.dv_radio4: {
                        mGridAdapter.getFilter().filter(String.valueOf(StateUtils.STATE_NEW)); //Linked
                        mListAdapter.getFilter().filter(String.valueOf(StateUtils.STATE_NEW)); //Linked
                    }
                    break;

                }

                mFilter = rg.getCheckedRadioButtonId();


            }
        });
        adb.setNegativeButton(R.string.cancel, null);

        adb.show();

    }

    /**
     * Method to reload the service discovery and reload the devices
     */
    public void optionReload(){

        mServiceListener.reloadListening();

    }

    /**
     * Dialog for the QR code insertion and sending
     *
     * @param m
     */
    public void codeDialog(final ModelPrinter m) {

        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.devices_setup_title);

        //Inflate the view
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.setup_dialog, null, false);


        //On insertion write the printer onto the database and start updating the socket
        adb.setPositiveButton(R.string.add, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                m.setId(DatabaseController.writeDb(m.getName(), m.getAddress(), String.valueOf(m.getPosition())));
                m.setLinked(getActivity());
                notifyAdapter();
            }
        });

        adb.setNegativeButton(R.string.cancel, null);

        adb.setView(v);

        adb.show();

    }

    /**
     * ***************************** click listeners ********************************
     */


    //TODO: Click listeners should be moved to another class
    public OnItemClickListener listClickListener() {

        return new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                //avoid header
                if (arg2 > 0) {
                    ModelPrinter m = DevicesListController.getList().get(arg2 - 1);
                    if (m.getStatus() == StateUtils.STATE_NEW) {
                        codeDialog(m);
                    } else if (m.getStatus() == StateUtils.STATE_ADHOC) {
                        mNetworkManager.setupNetwork(DevicesFragment.this, m.getName(), arg2);
                    }
                }


            }
        };

    }

    //onclick listener will open the action mode
    public OnItemClickListener gridClickListener() {

        return new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                ModelPrinter m = null;

                //search printer by position
                for (ModelPrinter mp : DevicesListController.getList()) {
                    if (mp.getPosition() == arg2) m = mp;
                }

                if (m != null) {

                    if (m.getStatus() == StateUtils.STATE_NEW) {
                        codeDialog(m);
                    } else if (m.getStatus() == StateUtils.STATE_ADHOC) {
                        mNetworkManager.setupNetwork(DevicesFragment.this, m.getName(), arg2);
                    } else {
                        //show custom dialog
                        if (m.getStatus() == StateUtils.STATE_ERROR) {
                            Toast toast = new Toast(getActivity());
                            LayoutInflater inflater = getActivity().getLayoutInflater();
                            View toastView = inflater.inflate(R.layout.toast_layout, null);
                            TextView tv = (TextView) toastView.findViewById(R.id.toast_text);
                            tv.setText(m.getMessage());
                            toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 50);
                            toast.setDuration(Toast.LENGTH_SHORT);
                            toast.setView(toastView);
                            toast.show();
                        }

                        //Check if the Job has finished, and create a dialog to remove the file / send a new one
                        if (m.getStatus() > 0) {

                            //if job finished, create dialog
                            if (m.getJob().getFinished()) {
                                createFinishDialog(m);

                                //if not finished, normal behavior
                            } else {
                                ItemListActivity.showExtraFragment(1, m.getName());
                            }
                        }
                    }
                }
            }
        };

    }

    //onlongclick will start the draggable printer handler
    public OnItemLongClickListener gridLongClickListener() {
        return new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int arg2, long arg3) {

                ModelPrinter m = null;

                for (ModelPrinter mp : DevicesListController.getList()) {
                    if (mp.getPosition() == arg2) m = mp;
                }

                if (m != null) {

                    ClipData data = null;
                    data = ClipData.newPlainText("printer", m.getName());
                    DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(arg1);
                    arg1.startDrag(data, shadowBuilder, arg1, 0);
                }


                return false;
            }
        };
    }

    @Override
    public void onDestroyView() {
        //mNetworkManager.destroy();
        super.onDestroyView();
    }

    /**
     * ***************************************
     * PLAY MUSIC!
     * ****************************************
     */

    public void loadMusic() {
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mSoundMusic = mSoundPool.load(getActivity(), R.raw.finish, 1);
        mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mSoundIsLoaded = true;
            }
        });
    }

    public static void playMusic() {
        if (mSoundIsLoaded) {
            Log.i("out", "PLAYING MUSIC");
            mSoundPool.play(mSoundMusic, 1, 1, 1, 0, 1);
        }
    }

    /**
     * *****************************************
     * FINISH DIALOG
     * ******************************************
     */

    //TODO OUT OF HERE!
    public void createFinishDialog(final ModelPrinter m) {

        //Constructor
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(getActivity().getString(R.string.finish_dialog_title) + m.getJob().getFilename());
        adb.setMessage(R.string.finish_dialog_text);

        adb.setPositiveButton(R.string.finish_dialog_remove, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                m.setJobPath(null);

                //Remove file from server
                OctoprintFiles.fileCommand(getActivity(), m.getAddress(), m.getJob().getFilename(), "/local/", true);
            }
        });

        adb.setNegativeButton(R.string.finish_dialog_notremove, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Select the same file again to reset progress
                OctoprintFiles.fileCommand(getActivity(), m.getAddress(), m.getJob().getFilename(), "/local/", false);

            }
        });

        adb.show();
    }
}
