package com.eggbeatstudios.cedric.whospayin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Cedric on 7/22/2016.
 */
public class PlayerDBHandler extends SQLiteOpenHelper {

    /*database handling variables
     */
    private static final int DATABASE_VERSION = 1;
    //the database name
    private static final String DATABASE_NAME = "WPplayers.db";
    //the table name
    public static final String TABLE_PLAYERS = "players";
    //the column names
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PLAYER_NAME = "player_name";
    public static final String COLUMN_IS_HOST = "is_host";

    /*base handler functions
     */
    //constructor
    //passes information about the database to the superclass "SQLiteOpenHelper"
    public PlayerDBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //this is literally sql at this point
        String query = "CREATE TABLE " + TABLE_PLAYERS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PLAYER_NAME + " TEXT, " +
                COLUMN_IS_HOST + " BOOLEAN " +
                ");";

        //how we execute queries on android (db is passed in as parameter)
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        //recreate the table
        db.execSQL("DROP_TABLE_IF_EXISTS " + TABLE_PLAYERS + ";");
        onCreate(db);
    }

    /*functions to add and remove player info
     */
    //returns the ID of the player to track players with the same name
    public int addPlayer(Player player) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_PLAYER_NAME, player.getPlayerName());
        values.put(COLUMN_IS_HOST, player.getIsHost());

        SQLiteDatabase db = getWritableDatabase();

        int playerID = (int)db.insert(TABLE_PLAYERS, null, values);
        db.close();
        return playerID;
    }

    public Player deletePlayer(int id, String pName) {
        SQLiteDatabase db = getWritableDatabase();
        Player deletedPlayer = new Player();
        String query = "SELECT * FROM " + TABLE_PLAYERS + " WHERE " +
                COLUMN_PLAYER_NAME + " =\"" + pName + "\";";

        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();

        //search for player
        //this also checks in case of players that have the same name,
        //in which case the id of the player in the db is accounted for
        while (!c.isAfterLast()) {
            if (c.getInt(c.getColumnIndex(COLUMN_ID)) == id &&
                    c.getString(c.getColumnIndex(COLUMN_PLAYER_NAME)).equals(pName)) {
                //retrieve player properties
                deletedPlayer.setID(id);
                deletedPlayer.setPlayerName(pName);
                deletedPlayer.setIsHost(c.getInt(c.getColumnIndex(COLUMN_IS_HOST)) > 0);
            }
            c.moveToNext();
        }

        c.close();

        //deletes only the player with corresponding id and player name
        db.execSQL("DELETE FROM " + TABLE_PLAYERS + " WHERE " + COLUMN_ID + " =\"" + id + "\" AND " +
                COLUMN_PLAYER_NAME + " =\"" + pName + "\";" );

        db.close();

        //TODO: with isHost property, we may be able to assign a new host if the old one backs out
        return deletedPlayer;
    }

    //removes all players from database
    public void clearPlayers() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_PLAYERS + ";");
    }
}
