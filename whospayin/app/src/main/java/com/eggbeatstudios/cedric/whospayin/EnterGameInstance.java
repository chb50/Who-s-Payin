package com.eggbeatstudios.cedric.whospayin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

//TODO: add the ability to filter a search for game instances
public class EnterGameInstance extends AppCompatActivity implements ListFrag.listFragListener{

    /* Begin server Management Variables */
    private static NsdManager mNsdManager;
    private static NsdManager.DiscoveryListener mDiscoveryListener;
    private static NsdManager.ResolveListener mResolveListener;
    //server variables
    private static Socket mSocket;
    private NsdServiceInfo mServiceInfo;
    private String mServiceName;
    private List<NsdServiceInfo> listOfServices = new Vector<NsdServiceInfo>(5,5); //used to find and connect to the service the user requests
    NsdServiceInfo selectedService;

    /*End server management variables */


    /* variables for displaying games */
    private List<String> gameInstances = new Vector<String>(5,5); //where games are stored to be displayed to the user
    private ListFrag serverListFrag;
    private ListView gamesList;

    //client info stuff
    private static boolean isClient;
    private String clientName;

    Handler updateUI = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            serverListFrag = (ListFrag)getFragmentManager().findFragmentById(R.id.availibleGames);
            gamesList = serverListFrag.lv;
            //specify context as "EnterGameInstance"
            ListAdapter gameListAdapter = new ArrayAdapter<>(EnterGameInstance.this, android.R.layout.simple_list_item_1,
                    gameInstances);
            gamesList.setAdapter(gameListAdapter);
        }

    };

    //boolean to test if discovery has been carried out
    private static boolean isDiscovered;


    private static final String TAG = EnterGameInstance.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_game_instance);

        //get client name
        Bundle playerInfo = getIntent().getExtras();
        clientName = playerInfo.getString("clientName");

        //initialize parameters
        mNsdManager = (NsdManager)getSystemService(NSD_SERVICE);


        //initialize listeners
        initializeDiscoveryListener();
        initializeResolveListener();
        Log.d(TAG, "Discovery listener activated");

        //begin searching for services
        mNsdManager.discoverServices(
                "_http._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        isDiscovered = true;




    }

    @Override
    protected void onPause() {
        if (isDiscovered) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            isDiscovered = false;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isDiscovered) {
            mNsdManager.discoverServices(
                    "_http._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            isDiscovered = true;
        }
    }



//    public void initializeSocket() throws IOException {
//        mSocket = new Socket();
//    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                /*TODO: Error: Attempt to invoke virtual method 'java.lang.String android.net.nsd.NsdServiceInfo.getServiceName()' on a null object reference here
                there was a bug causing the app to crash if the user did not
                attempt to create a game instance first.
                tried to hard code the service type, but caused a null pointer
                error with the vector container (NOTE: calling mServiceInfo.getServiceName() in thread)
                 */

                final NsdServiceInfo s = service;
                if (!service.getServiceType().equals("_http._tcp.")) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains("WhosPayin:")){ //finds a game instance

                    //add code to update user interface about the game that was found

                    //thread for updating gameInstances list
                    //what is inserted into "gameInstance" will change eventually
                    //for now we give it the name of the Service to display
                    Runnable updateList = new Runnable() {
                        @Override
                        public void run() {
                            synchronized (this) {
                                try {
                                    gameInstances.add(gameInstances.size(), s.getServiceName());
                                    listOfServices.add(listOfServices.size(), s); //add service info to list of services
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "Could not access gameInstances vector", e);
                                }
                            }
                            updateUI.sendEmptyMessage(0);
                        }
                    };

                    Thread updateListThread = new Thread(updateList);
                    updateListThread.start();
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
                final NsdServiceInfo s = service;

                //update list to remove the services lost
                Runnable updateList = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            try {
                                gameInstances.remove(s.getServiceName());
                                listOfServices.remove(s); //remover service from list
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, "Could not remove from gameInstances vector", e);
                            }
                        }
                        updateUI.sendEmptyMessage(0);
                    }
                };

                Thread updateListThread = new Thread(updateList);
                updateListThread.start();
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                //mNsdManager.stopServiceDiscovery(this); //may not want to call this
                Log.d(TAG, "Service discovery halted");
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                //mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mServiceInfo = serviceInfo;
                int port = mServiceInfo.getPort();
                InetAddress host = mServiceInfo.getHost();
            }
        };
    }


    //ran with the joinGame method to move player to a lobby activity
    @Override
    public void selectItem() {
        serverListFrag = (ListFrag)getFragmentManager().findFragmentById(R.id.availibleGames);
        String selectedServiceName;
        gamesList = serverListFrag.lv;

        //TODO: this code works, but is probably not the optimal way of doing things
        //finds the highlighted server entry
        //position = id + 'number of header views'
        for (int iter = 0; iter < gamesList.getCount(); ++iter) {
            View v = gamesList.getChildAt(iter);
            Drawable vBack = v.getBackground();
            //check for which item is highlighted
            if (vBack instanceof ColorDrawable && Color.YELLOW == ((ColorDrawable)vBack).getColor()) {
                selectedServiceName = gamesList.getItemAtPosition(iter).toString();
                Log.d(TAG, "this service is highlighted: " + selectedServiceName);
                //try implementing for loop in a classic "index" fashion
                for (int iterTwo = 0; iterTwo < listOfServices.size(); ++iterTwo) {
                    if (listOfServices.get(iterTwo).getServiceName().equals(selectedServiceName)) {
                        selectedService = listOfServices.get(iterTwo);
                        return;
                    }
                }
            }
        }
    }

    public void joinGame(View view) {
        selectItem(); //sets selectedService
        if (selectedService != null) {
            //TODO: need to pass the server information to the lobby activity (CreateGameInstance)
            //clear the gameInstance list and list of services
            Runnable clearList = new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        try {
                            gameInstances.clear();
                            listOfServices.clear();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "Cannot clear list", e);
                        }
                    }
                }
            };
            Thread cl = new Thread(clearList);
            cl.start();

            //resolve the service (dont know if this will carry to the next activity
            mNsdManager.resolveService(selectedService, mResolveListener);
            isClient = true;
            //set up intent
            Intent i = new Intent(this, CreateGameInstance.class);
            i.putExtra("isClient", isClient);
            i.putExtra("clientName", clientName);
            startActivity(i);
        } else {
            Log.d(TAG, "selectedService is NULL");
        }
    }

    //onClick's
    public void goMainMenu(View view) {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}
