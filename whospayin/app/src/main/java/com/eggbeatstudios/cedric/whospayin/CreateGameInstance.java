package com.eggbeatstudios.cedric.whospayin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Message;
import android.renderscript.RenderScript;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Vector;


/*TODO: Add logic to separate the game host from other players
    1) if the player is not host, then they came from the EnterGameInstance activity
    and we have to pass information from that activity to this one about the player's name
    and make sure that the player only stays connected to this instance of the game and no other

    2) if the player is host, give them access to the administrative tabs in the overflow
    menu such as kicking other players and setting the options for the game
 */
/* TODO: learn how to pass information between different instances of the activity
    need to pass client info to server
    need to pass server info to client
 */

public class CreateGameInstance extends AppCompatActivity implements ListFrag.listFragListener{

    //references for registering a service
    private ServerSocket mServerSocket;
    private NsdManager mNsdManager;
    private NsdServiceInfo serviceInfo;
    private NsdManager.RegistrationListener mRegistrationListener; //interface
    private String mServiceName;

    //boolean for testing if service is already registered
    private boolean isRegistered;

    //Player Information
    private String hostName = null;
    private List<String> playerNames = new Vector<>(5,5);

    //handler stuff
    private ListView playerList;
    //handle for player info
    Handler addPlayer = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ListFrag playerListFrag = (ListFrag)getFragmentManager().findFragmentById(R.id.playerNamesList);
            playerList = playerListFrag.lv;
            ListAdapter playerAdapt = new ArrayAdapter<>(CreateGameInstance.this, android.R.layout.simple_list_item_1,
                    playerNames);
            playerList.setAdapter(playerAdapt);
        }
    };

    //Tag for log info in debugging
    private static final String TAG = CreateGameInstance.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game_instance);

        //get player info from the main activity
        final Bundle playerInfo = getIntent().getExtras();
        if (!playerInfo.getBoolean("isClient")) {
            //then this player is the host

            hostName = playerInfo.getString("hostName");
            //attempt to register service on activity creation
            Log.d(TAG, "Creating a server");
            try {
                initializeServerSocket();
                Log.d(TAG, "Created Socket");
                initializeRegistrationListener();
                Log.d(TAG, "Initialized Registration Listener");
                registerService(mServerSocket.getLocalPort());
            } catch (IOException e) {
                //notify user that connection was not established
                TextView errorMsg = (TextView)findViewById(R.id.errorMessage);
                errorMsg.setText(getString(R.string.error_msg));
                errorMsg.setTextColor(Color.RED);

                //notify log that exception was thrown during service registration
                e.printStackTrace();
                Log.e(TAG, "IO Exception thrown, Failed to register service.", e);
                return;
            }

            Log.i(TAG, "Successfully registered service");

            //TODO: add logic so that the right host will be displayed as host
            TextView displayHost = (TextView)findViewById(R.id.hostName);
            String hostNameSet = displayHost.getText() + " " + hostName;
            displayHost.setText(hostNameSet);
        } else {
            //then this player is a client
            Runnable addPlayerName = new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        try {
                            playerNames.add(playerNames.size(), playerInfo.getString("clientName"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "cannot add player name", e);
                        }
                    }
                    addPlayer.sendEmptyMessage(0);
                }
            };
            Thread aPN = new Thread(addPlayerName);
            aPN.start();
        }

    }

    //Overflow menu stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        return true;
    }

    //Service control
    @Override
    protected void onPause() {
        if (hostName != null && isRegistered) {
            mNsdManager.unregisterService(mRegistrationListener);
            isRegistered = false;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hostName != null && !isRegistered) {
            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
            isRegistered = true;
        }
    }

    //service registration
    //registers a server on the local network so clients can pick it up
    public void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it.
        serviceInfo = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName("WhosPayin: " + hostName + "'s game");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);

        //NOTE: NSD_SERVICE is a static method, and therefore we use the class name
        //itself rather than the class instance to reference it
        //this gets the system's service by name and type casts it to a type "NsdManager"
        mNsdManager = (NsdManager)getSystemService(NSD_SERVICE);

        //register the service so that other services may find it
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        isRegistered = true;
    }

    //initialize a socket for connection
    public void initializeServerSocket() throws IOException {
        // Initialize a server socket on the next available port.
        // the port is chosen by the os
        mServerSocket = new ServerSocket(0);
        Log.d(TAG, "Socket created on port: " + mServerSocket.getLocalPort());

    }

    //a callback for service registration
    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = serviceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                Log.e(TAG, "Registration failed on attempt to initialize registration listener");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                Log.d(TAG, "Registration of this service will now end");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
                Log.e(TAG, "Failed in attempting to end registration of this service");
            }
        };

    }

    //getters
    public NsdManager getNsdManager() {
        return mNsdManager;
    }

    public ServerSocket getServerSocket() {
        return mServerSocket;
    }

    public NsdServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public String getServiceName() {
        return mServiceName;
    }

    //onClick's
    public void goMainMenu(View view) {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    public void startGame(View view) {
        TextView sGButton = (TextView)findViewById(R.id.startGameButton);
        if (sGButton.getText() == "Start Game") {
            //TODO: add code here to start game once every player is ready
        }
        if (sGButton.getText() == "Ready") {
            sGButton.setText(R.string.not_ready);
            //TODO: add code to send to server that this user is ready to play
        } else {
            sGButton.setText(R.string.ready);
            //TODO: add code to send to server that this user is not ready to play
        }
    }
    public void kickPlayer(MenuItem mItem) {
    }
    public void selectOptions(MenuItem mItem) {
    }
    public void aboutGame(MenuItem mItem) {
    }

    //ListFrag stuff

    @Override
    public void selectItem() {

    }
}
