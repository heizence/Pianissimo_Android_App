package com.example.pianissimo.Fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.pianissimo.R;
import androidx.fragment.app.Fragment;

public class fragment_each_notice extends Fragment {
    private View fragmentView;

    private TextView titleTextView;
    private TextView writtenDateTextView;
    private TextView contentsTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_each_notice, container, false);

        titleTextView = fragmentView.findViewById(R.id.eachNoticeTitle);
        writtenDateTextView = fragmentView.findViewById(R.id.eachNoticeWrittenDate);
        contentsTextView = fragmentView.findViewById(R.id.eachNoticeContents);

        if (getArguments() != null) {
            String title = getArguments().getString(getString(R.string.noticeTitle));
            String writtenDate = getArguments().getString(getString(R.string.noticeWrittenDate));
            String contents = getArguments().getString(getString(R.string.noticeContents));

//            System.out.println("## title : " + title);
//            System.out.println("## writtenDate : " + writtenDate);
//            System.out.println("## contents : " + contents);

            titleTextView.setText(title);
            writtenDateTextView.setText(writtenDate);
            contentsTextView.setText(contents);
        }

        return fragmentView;
    }
}
