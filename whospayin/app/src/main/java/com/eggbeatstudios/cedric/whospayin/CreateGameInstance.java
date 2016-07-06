package com.eggbeatstudios.cedric.whospayin;

import android.content.Context;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.renderscript.RenderScript;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;

public class CreateGameInstance extends AppCompatActivity {

    //references for registering a service
    private static ServerSocket mServerSocket;
    private static NsdManager mNsdManager;
    private static NsdServiceInfo serviceInfo;
    private static NsdManager.RegistrationListener mRegistrationListener; //interface
    private static String mServiceName;

    //boolean for testing if service is already registered
    private static boolean isRegistered;

    //Tag for log info in debugging
    private static final String TAG = CreateGameInstance.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game_instance);

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
    }

    @Override
    protected void onPause() {
        if (isRegistered) {
            mNsdManager.unregisterService(mRegistrationListener);
            isRegistered = false;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isRegistered) {
            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
            isRegistered = true;
        }
    }

    //registers a server on the local network so clients can pick it up
    public void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it.
        serviceInfo = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName("WhosPayinGameInstance");
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
    public static NsdManager getNsdManager() {
        return mNsdManager;
    }

    public static ServerSocket getServerSocket() {
        return mServerSocket;
    }

    public static NsdServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public static String getServiceName() {
        return mServiceName;
    }
}
