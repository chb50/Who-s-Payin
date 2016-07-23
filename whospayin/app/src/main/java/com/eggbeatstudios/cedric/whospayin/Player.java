package com.eggbeatstudios.cedric.whospayin;

/**
 * Created by Cedric on 7/22/2016.
 */

/* this class is used to represent the row of the "players" table
    that holds the information of a single player
 */
public class Player {

    private int _id;
    private String _player_name;
    private boolean _is_host; //while this is a boolean value, the boolean is represented as a bit in SQL

    public Player() {
    }

    public Player(String pName) {
        this._player_name = pName;
    }

    public Player(String pName, boolean isHost) {
        this._player_name = pName;
        this._is_host = isHost;
    }

    //setters
    public void setID(int id) {
        this._id = id;
    }

    public void setPlayerName(String pName) {
        this._player_name = pName;
    }

    public void setIsHost(boolean isHost) {
        this._is_host = isHost;
    }

    //getters

    public int getID() {
        return this._id;
    }

    public String getPlayerName() {
        return this._player_name;
    }

    public boolean getIsHost() {
        return this._is_host;
    }
}
