package com.eggbeatstudios.cedric.whospayin;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

public class LobbyInstance extends AppCompatActivity implements ListFrag.listFragListener{

    //connection stuff
    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mServiceInfo;
    private String mServiceName;

    //Player Information for displaying
    private Player clientPlayer;
    private List<String> playerNames = new Vector<>(5,5);

    //handler stuff
    private ListView playerList;

    //TODO: create custom list item layout to display when players are ready
    Handler addPlayer = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ListFrag playerListFrag = (ListFrag)getFragmentManager().findFragmentById(R.id.lobbyNamesList);
            playerList = playerListFrag.lv;
            ListAdapter playerAdapt = new ArrayAdapter<>(LobbyInstance.this, android.R.layout.simple_list_item_1,
                    playerNames);
            playerList.setAdapter(playerAdapt);
        }
    };

    private static final String TAG = LobbyInstance.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_instance);

        SharedPreferences sharePref = getSharedPreferences("user_names", Context.MODE_PRIVATE);
        final Bundle playerInfo = getIntent().getExtras();

        //initialize client player
        String clientName = sharePref.getString("username","");
        Log.d(TAG, "client name: " + clientName);
        clientPlayer = new Player(clientName, false);

        final NsdServiceInfo selectedService = playerInfo.getParcelable("selectedService");
        mServiceInfo = selectedService;
        assert mServiceInfo != null; //assume to be true
        mServiceName = mServiceInfo.getServiceName();

        initializeResolveListener();
        mNsdManager = (NsdManager)getSystemService(NSD_SERVICE);
        mNsdManager.resolveService(selectedService, mResolveListener);

//        //send player info to server
//        sendPlayerName(mServiceInfo);

        //TODO: what needs to happen is that the client get all players from the server's database
        // after which the client updates their list with that information
        Runnable addPlayerName = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        playerNames.add(playerNames.size(), clientPlayer.getPlayerName());
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

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            //TODO: not sure how this will behave, it may always pass the same service info that we already have
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

    //ListFrag interface items
    public void selectItem() {

    }

    //server communication
    public void sendPlayerName(NsdServiceInfo sInfo) {
        try {
            Socket mSocket = new Socket(sInfo.getServiceName(), sInfo.getPort());
            DataOutputStream dos = new DataOutputStream(mSocket.getOutputStream());
            dos.writeUTF(clientPlayer.getPlayerName());
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Could not create client socket", e);
        }

    }
}
