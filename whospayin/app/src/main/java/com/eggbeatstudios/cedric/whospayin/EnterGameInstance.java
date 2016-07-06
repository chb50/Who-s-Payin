package com.eggbeatstudios.cedric.whospayin;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.net.InetAddress;
import java.util.List;
import java.util.Vector;

public class EnterGameInstance extends AppCompatActivity {

    /* Begin server Management Variables */
    private static NsdManager mNsdManager;
    private static NsdManager.DiscoveryListener mDiscoveryListener;
    private static NsdManager.ResolveListener mResolveListener;
    //server variables
    private static NsdServiceInfo mServiceInfo;
    private static String mServiceName;

    /*End server management variables */

    /* variables for displaying games */
    private List<String> gameInstances = new Vector<String>(); //where games are stored to be displayed to the user
    Handler updateUI = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            ListView gamesList = (ListView)findViewById(R.id.availibleGames);
            //specify context as "EnterGameInstance"
            ListAdapter gameListAdapter = new ArrayAdapter<>(EnterGameInstance.this, android.R.layout.simple_list_item_1, gameInstances);
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

        //initialize parameters
        mNsdManager = (NsdManager)getSystemService(NSD_SERVICE);

        //request service info of the server activity
        setServiceInfo(CreateGameInstance.getServiceInfo());
        setServiceName(CreateGameInstance.getServiceName());

        //initialize listeners
        initializeDiscoveryListener();
        initializeResolveListener();
        Log.d(TAG, "Discovery listener activated");

        //begin searching for services
        mNsdManager.discoverServices(
                mServiceInfo.getServiceType(), NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
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
                    mServiceInfo.getServiceType(), NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            isDiscovered = true;
        }
    }

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
                if (!service.getServiceType().equals(mServiceInfo.getServiceType())) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains("WhosPayinGameInstance")){ //finds a game instance
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    mNsdManager.resolveService(service, mResolveListener);

                    //add code to update user interface about the game that was found

                    //thread for updating gameInstances list
                    //what is inserted into "gameInstance" will change eventually
                    //for now we give it the name of the Service to display
                    Runnable updateList = new Runnable() {
                        @Override
                        public void run() {
                            synchronized (this) {
                                try {
                                    gameInstances.add(gameInstances.size(), mServiceInfo.getServiceName());
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

    //setters
    public void setServiceInfo(NsdServiceInfo si) {
        mServiceInfo = si;
    }

    public void setServiceName(String sn) {
        mServiceName = sn;
    }
}
