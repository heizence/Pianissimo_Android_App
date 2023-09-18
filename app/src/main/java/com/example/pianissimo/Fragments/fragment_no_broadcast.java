package com.example.pianissimo.Fragments;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.pianissimo.Activities.start_livestream;
import com.example.pianissimo.Adapters.Part_each_broadcast;
import com.example.pianissimo.R;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.List;

public class fragment_no_broadcast extends Fragment {
    private String screenName = "[FRAGMENT fragment_no_broadcasts]:";  //  Fragment 일 경우 FRAGMENT 로 할 수도 있음
    private String tag_check = screenName + "[CHECK]";  // 특정 값을 확인할 때 사용
    private String tag_execute = screenName + "[EXECUTE]";  // 매서드나 다른 실행 가능한 코드를 실행할 때 사용
    private String tag_event = screenName + "[EVENT]";  // 특정 이벤트 발생을 확인할 때 사용

    private View fragmentView;
    private ImageView startBroadcastBtn;

    private Context context = getContext();
    private ActivityResultLauncher<Intent> launcher;
    private ActivityResultLauncher<Intent> localLauncher;

    // Create a static factory method
    public static fragment_no_broadcast newInstance(ActivityResultLauncher<Intent> param_launcher) {
        fragment_no_broadcast fragment = new fragment_no_broadcast();
        fragment.setActivityResultLauncher(param_launcher);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(tag_execute, "LifeCycle onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(tag_execute, "LifeCycle onCreateView");

        fragmentView = inflater.inflate(R.layout.fragment_no_broadcasts, container, false);

        localLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(tag_check, "got result. resultCode : " + result.getResultCode());
                    Log.d(tag_check, "replace fragment to fragment_broadcast");
                    // result 의 경우의 수가 1가지 뿐이므로 resultCode 를 구분하는 조건문이 필요없음. 기본 resultCode 설정값은 Activity.RESULT_OK
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    fragment_broadcast fragment = new fragment_broadcast();
                    transaction.replace(R.id.mainFragmentView, fragment);
                    transaction.commit();
                }
        );

        // 방송 시작 버튼 이벤트 등록
        startBroadcastBtn = fragmentView.findViewById(R.id.fragmentNoBroadcast_startBtn);
        startBroadcastBtn.setOnClickListener(view -> {
            startLive();
        });

        return fragmentView;
    }

    // setActivityResultLauncher 등록. newInstance 매서드 내에서만 실행하기.
    public void setActivityResultLauncher(ActivityResultLauncher<Intent> param_launcher){
        Log.d(tag_execute, "setActivityResultLauncher");
        this.launcher = param_launcher;
    }

    public void startLive() {
        Log.d(tag_execute, "startLive2");
        Intent intent = new Intent(getActivity(), start_livestream.class);
        localLauncher.launch(intent);
    }
}
