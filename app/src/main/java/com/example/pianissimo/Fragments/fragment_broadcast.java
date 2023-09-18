package com.example.pianissimo.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pianissimo.Activities.start_livestream;
import com.example.pianissimo.Adapters.Adapter_each_broadcast;
import com.example.pianissimo.Adapters.Part_each_broadcast;
import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.Modules.httpRequestAPIs;
import com.example.pianissimo.R;
import com.google.gson.Gson;

import java.util.ArrayList;

public class fragment_broadcast extends Fragment {
    private String screenName = "[FRAGMENT fragment_broadcast]:";  //  Fragment 일 경우 FRAGMENT 로 할 수도 있음
    private String tag_check = screenName + "[CHECK]";  // 특정 값을 확인할 때 사용
    private String tag_execute = screenName + "[EXECUTE]";  // 매서드나 다른 실행 가능한 코드를 실행할 때 사용
    private String tag_event = screenName + "[EVENT]";  // 특정 이벤트 발생을 확인할 때 사용

    private Context context = getContext();
    private ActivityResultLauncher<Intent> launcher;
    private View fragmentView;
    private ImageView broadcastStartBtn;
    private RecyclerView recyclerView;
    private Adapter_each_broadcast adapter;
    private int pageIndex;  // 데이터를 불러올 시작 범위 index(0부터 시작)
    private boolean loadMoreData;  // recyclerView 에서 가장 아래로 scroll down 되었을 때 추가로 데이터 로드 여부

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(tag_execute, "LifeCycle onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.d(tag_execute, "LifeCycle onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(tag_execute, "LifeCycle onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(tag_execute, "LifeCycle onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(tag_execute, "LifeCycle onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(tag_execute, "LifeCycle onDestroy");
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(tag_execute, "LifeCycle onCreateView");

        pageIndex = 0;
        loadMoreData = true;

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(tag_check, "got result. resultCode : " + result.getResultCode());
                    // result 의 경우의 수가 1가지 뿐이므로 resultCode 를 구분하는 조건문이 필요없음. 기본 resultCode 설정값은 Activity.RESULT_OK
                    getData();
                }
        );

        fragmentView = inflater.inflate(R.layout.fragment_broadcast, container, false);

        // 방송 시작 버튼 이벤트 등록
        broadcastStartBtn = fragmentView.findViewById(R.id.fragmentBroadcastLiveStartBtn);
        broadcastStartBtn.setOnClickListener(view -> {
            Log.d(tag_execute, "start broadcast");
            startLive();
        });

        recyclerView = fragmentView.findViewById(R.id.broadcastRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext()));

        adapter = new Adapter_each_broadcast(getContext());
        adapter.setFragmentManager(getActivity().getSupportFragmentManager());
        adapter.setActivityResultLauncher(launcher);
        recyclerView.setAdapter(adapter);

        // scroll 가장 밑쪽으로 내렸을 때 추가로 데이터 불러오는 이벤트 리스너 등록
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                int totalItemCount = layoutManager.getItemCount();

                // User has scrolled to the bottom
                if (lastVisibleItemPosition == totalItemCount - 1) {
                    if (loadMoreData == true) {
                        Log.d(tag_event, "scrolled down to the bottom : " + lastVisibleItemPosition);
                        getData();
                    }
                    loadMoreData = false;
                }
            }
        });
        getData();
        return fragmentView;
    }

    // 각 라이브 방송 데이터
    public class Broadcasts {
        private ArrayList<Part_each_broadcast> broadcasts;    // do not delete!
        private boolean isLastPage; // do not delete!

        public ArrayList<Part_each_broadcast> getBroadcasts() {
            return broadcasts;
        }

        public boolean isLastPage() {
            return isLastPage;
        }
    }

    // 방송 데이터 불러오기
    public void getData() {
        Log.d(tag_execute, "getData");
        // 요청 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                Log.d(tag_check, "getData success. check data : " + responseObj.data);

                Gson gson = new Gson();
                Broadcasts broadcasts = gson.fromJson(responseObj.data, Broadcasts.class);
                ArrayList<Part_each_broadcast> broadcastList = broadcasts.getBroadcasts();
                int numberOfData = broadcastList.size();

                Log.d(tag_check, "isLastPage : " + broadcasts.isLastPage());
                if (numberOfData > 0) {
                    adapter.emptyData();
                    for (int i = 0; i < numberOfData; i++) {
                        Part_each_broadcast broadcastObj = broadcastList.get(i);
                        adapter.add(broadcastObj);
                    }
                    // 서버에서 보내준 마지막 페이지 여부에 따라 데이터 추가 로드 여부 설정
                    if (!broadcasts.isLastPage()) {
                        loadMoreData = true;
                        pageIndex += 1;
                    } else loadMoreData = false;
                }
                // 데이터가 없으면 데이터 없음 화면 표시해 주기
                else {
                    loadMoreData = false;
                    Log.d(tag_check, "no data");

                    // 데이터 없음 화면에 표시해 줄 제목 및 내용 텍스트 추가
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    fragment_no_broadcast fragment = new fragment_no_broadcast();
                    transaction.replace(R.id.mainFragmentView, fragment);
                    transaction.commit();
                }
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                Log.d(tag_check, "getData failed. statusCode : " + statusCode);
                dialog_confirm.show(context, "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
            }
        }

        httpRequestAPIs.getBroadCastData(pageIndex, new Callee_success(), new Callee_failed());
    }

    public void startLive() {
        Log.d(tag_execute, "startLive");
        Intent intent = new Intent(getActivity(), start_livestream.class);
        //startActivity(intent);
        launcher.launch(intent);
    }
}
