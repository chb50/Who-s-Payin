package com.eggbeatstudios.cedric.whospayin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //EditText is for text fields editable by the user
    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //display current name if saved
        SharedPreferences sharePref = getSharedPreferences("user_name", Context.MODE_PRIVATE);
        if (!sharePref.getString("username", "").equals("")) {

            String savedName = getString(R.string.current_name) + sharePref.getString("username", "");
            TextView currentNameDisplay = (TextView)findViewById(R.id.displayCurrentName);
            currentNameDisplay.setText(savedName);
        }
    }

    //save the user name on "Save Name" button click
    public void saveName(View view) {
        EditText usernameInput = (EditText) findViewById(R.id.usernameInput);

        //if the user has not typed anything into the username field
        if (usernameInput.getText().toString().matches("")) {
            //NOTE: can still press space and it be a valid input
            Toast.makeText(MainActivity.this, "There is no name to save!", Toast.LENGTH_LONG).show();
            return;
        }

        //make a shared preference to save user's name for future games
        SharedPreferences sharePref = getSharedPreferences("user_name", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharePref.edit();
        editor.putString("username", usernameInput.getText().toString());
        editor.apply();

        //notify user that their name has been saved
        Toast.makeText(MainActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
        TextView currentName = (TextView)findViewById(R.id.displayCurrentName);
        String newName = getString(R.string.current_name) + usernameInput.getText().toString();
        //NOTE: Android was crying about concatenation within the setText field, so had to store in seperate variable first
        currentName.setText(newName);
    }

    //enter game button takes user to "EnterGameInstance" activity
    public void enterGame(View view) {
        Intent i = new Intent(this, EnterGameInstance.class);
        startActivity(i);
    }

    //create game button takes user to "CreateGameInstance" activity
    public void createGame(View view) {
        Intent i = new Intent(this, CreateGameInstance.class);
        startActivity(i);
    }
}
