package com.eggbeatstudios.cedric.whospayin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class LobbyInstance extends AppCompatActivity implements ListFrag.listFragListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_instance);
    }

    public void setReady(View view) {
        TextView readyButton = (TextView)findViewById(R.id.readyButton);
        if (readyButton.getText() == "Ready") {
            readyButton.setText(R.string.not_ready);
            //TODO: add code to send to server that this user is ready to play
        } else {
            readyButton.setText(R.string.ready);
            //TODO: add code to send to server that this user is not ready to play
        }
    }

    @Override
    public boolean selectItem() {
        return false;
    }
}
