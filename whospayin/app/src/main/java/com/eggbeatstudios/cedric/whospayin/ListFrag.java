package com.eggbeatstudios.cedric.whospayin;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by Cedric on 7/7/2016.
 */


public class ListFrag extends Fragment {

    public ListView lv;
    listFragListener activityCommander;

    //for debugging
    public static final String TAG = ListFrag.class.getName();

    public interface listFragListener {
        void selectItem();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            //define "activityCommander" by typecasting activity to a type "TopSectionListener"
            activityCommander = (listFragListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_frag, container, false);
        lv = (ListView)view.findViewById(R.id.listFrag);

        //create an item click listener
        lv.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                        highlightItem(parent, view, i, l);

                    }
                }
        );

        return view;
    }



    public void highlightItem(AdapterView<?> parent, View view, int i, long l) {
        //the parent is what contains the entries, the view is the entry that was clicked

        //reset all entries to white background to de-select
        for (int iter = 0; iter < parent.getCount(); ++iter) {
            View v = parent.getChildAt(iter);
            v.setBackgroundColor(Color.WHITE);
        }

        //set selected entry to have yellow background
        view.setBackgroundColor(Color.YELLOW);
    }
}
