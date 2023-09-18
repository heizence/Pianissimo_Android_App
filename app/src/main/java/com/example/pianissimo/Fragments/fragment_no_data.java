package com.example.pianissimo.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.pianissimo.R;
import androidx.fragment.app.Fragment;

public class fragment_no_data extends Fragment {
    private View fragmentView;
    private TextView noDataTitle;
    private TextView noDataText;

    public void noData() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        System.out.println("## NO DATA fragment");
        fragmentView = inflater.inflate(R.layout.fragment_no_data, container, false);
        noDataTitle = fragmentView.findViewById(R.id.noDataTitle);
        noDataText = fragmentView.findViewById(R.id.noDataText);

        if (getArguments() != null) {
            String title = getArguments().getString(getString(R.string.noDataTitle));
            String text = getArguments().getString(getString(R.string.noDataText));

            //System.out.println("## noDataTitle : " + noDataTitle);
            //System.out.println("## noDataText : " + noDataText);

            noDataTitle.setText(title);
            noDataText.setText(text);
        }

        return fragmentView;
    }
}
