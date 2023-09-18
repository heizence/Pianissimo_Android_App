package com.example.pianissimo.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pianissimo.Adapters.Adapter_each_notice;
import com.example.pianissimo.Adapters.Part_each_notice;
import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.Modules.httpRequestAPIs;
import com.example.pianissimo.R;
import com.google.gson.Gson;

import java.util.ArrayList;

public class fragment_home extends Fragment {
    private Context context = getContext();
    private View fragmentView;
    private RecyclerView recyclerView;
    private Adapter_each_notice adapter;
    private int pageIndex;  // 데이터를 불러올 시작 범위 index(0부터 시작)
    private boolean loadMoreData;  // recyclerView 에서 가장 아래로 scroll down 되었을 때 추가로 데이터 로드 여부

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        pageIndex = 0;
        loadMoreData = true;

        fragmentView = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = fragmentView.findViewById(R.id.noticeRecyclerView);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(fragmentView.getContext()));

        adapter = new Adapter_each_notice(getActivity().getSupportFragmentManager());
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
                        System.out.println("## scrolled down to the bottom! : " + lastVisibleItemPosition);
                        getData();
                    }
                    loadMoreData = false;
                }
            }
        });

        getData();

        return fragmentView;
    }

    // 각 공지사항 데이터
    public class Notices {
        private ArrayList<Part_each_notice> notices;    // do not delete!
        private boolean isLastPage; // do not delete!

        public ArrayList<Part_each_notice> getNotices() {
            return notices;
        }
        public boolean isLastPage() {
            return isLastPage;
        }
    }

    // 공지사항 데이터 불러오기
    public void getData() {
        // 요청 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                System.out.println("## data : " + responseObj.data);

                Gson gson = new Gson();
                Notices notices = gson.fromJson(responseObj.data, Notices.class);
                ArrayList<Part_each_notice> noticeList = notices.getNotices();
                int numberOfData = noticeList.size();

                System.out.println("## isLastPage : " + notices.isLastPage());

                if (numberOfData > 0) {
                    for (int i = 0; i < numberOfData; i++) {
                        Part_each_notice noticeObj = noticeList.get(i);
                        adapter.add(noticeObj);
                    }
                    // 서버에서 보내준 마지막 페이지 여부에 따라 데이터 추가 로드 여부 설정
                    if (!notices.isLastPage()) {
                        loadMoreData = true;
                        pageIndex += 1;
                    } else loadMoreData = false;
                }
                // 데이터가 없으면 데이터 없음 화면 표시해 주기
                else {
                    loadMoreData = false;
                    System.out.println("## no data!!!!!!");

                    // 데이터 없음 화면에 표시해 줄 제목 및 내용 텍스트 추가
                    Bundle bundle = new Bundle();
                    bundle.putString(getString(R.string.noDataTitle), "공지사항");
                    bundle.putString(getString(R.string.noDataText), "공지사항이 없습니다.");
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    fragment_no_data fragment = new fragment_no_data();

                    fragment.setArguments(bundle);
                    transaction.replace(R.id.fragmentHome, fragment);
                    transaction.commit();
                }
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                System.out.println("## failed statusCode : " + statusCode);
                dialog_confirm.show(context, "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
            }
        }

        httpRequestAPIs.getNotices(pageIndex, new Callee_success(), new Callee_failed());
    }
}
