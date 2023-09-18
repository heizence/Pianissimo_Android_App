package com.example.pianissimo.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.pianissimo.R;

public class fragment_lesson extends Fragment {
    private View fragmentView;
    private LinearLayout registerLessonMenu;
    private LinearLayout myLessonStatusMenu;
    private LinearLayout myLessonHistoryMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_lesson, container, false);

        registerLessonMenu = fragmentView.findViewById(R.id.registerLessonMenu);
        myLessonStatusMenu = fragmentView.findViewById(R.id.myLessonStatusMenu);
        myLessonHistoryMenu = fragmentView.findViewById(R.id.myLessonHistoryMenu);

        // 레슨 예약 버튼 클릭 이벤트
        registerLessonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 사용자가 결제한 이용권 종류 조회
                SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.sharedPreferenceMain), Context.MODE_PRIVATE);
                String ticketType = sharedPref.getString(getString(R.string.store_AU_TicketType), "");

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

                System.out.println("## check ticketType : " + ticketType);
                // 레슨 이용권을 결제했을 때 레슨 예약 화면 표시
                if (ticketType.equals("레슨 이용권")) {
                    fragment_book_lesson fragment = new fragment_book_lesson();
                    transaction.replace(R.id.mainFragmentView, fragment);
                }
                // 연습실 이용권을 결제했을 때 레슨 이용권 없음 화면 표시
                else {
                    // 데이터 없음 화면에 표시해 줄 제목 및 내용 텍스트 추가
                    Bundle bundle = new Bundle();
                    bundle.putString(getString(R.string.noDataTitle), "레슨 예약");
                    bundle.putString(getString(R.string.noDataText), "레슨 이용권이 없습니다.");

                    fragment_no_data fragment = new fragment_no_data();
                    fragment.setArguments(bundle);
                    transaction.replace(R.id.mainFragmentView, fragment);
                }
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        // 내 레슨 예약 현황 버튼 클릭 이벤트
        myLessonStatusMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                fragment_my_lesson_status fragment = new fragment_my_lesson_status();

                transaction.add(R.id.mainFragmentView, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        // 받은 레슨 내역 버튼 클릭 이벤트
        myLessonHistoryMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                fragment_my_lesson_history fragment = new fragment_my_lesson_history();

                transaction.add(R.id.mainFragmentView, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        return fragmentView;
    }
}
