package com.example.pianissimo.Adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pianissimo.Fragments.fragment_no_data;
import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.Modules.httpRequestAPIs;
import com.example.pianissimo.R;

import java.util.ArrayList;
import java.util.List;

public class Adapter_each_my_practice_room_status extends RecyclerView.Adapter<Adapter_each_my_practice_room_status.ViewHolder>{
    private List<Part_each_my_practice_room_status> data = new ArrayList<>();
    private Context context;
    private FragmentManager fragmentManager;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;

    // adapter 생성. 부모 activity 또는 이전 fragment 에서 FragmentManager 객체 받아오기
    public Adapter_each_my_practice_room_status(Context context, FragmentManager fragmentManagerInput) {
        this.context = context;
        fragmentManager = fragmentManagerInput;
        sharedPref = context.getSharedPreferences(context.getString(R.string.sharedPreferenceMain), context.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();
    }

    // 데이터 추가
    public void add(Part_each_my_practice_room_status object) {
        data.add(object);
        this.notifyItemInserted(data.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView practiceRoomNameView;
        private final TextView dateView;
        private final TextView timeView;
        private final TextView cancelBtn;

        public ViewHolder(View view) {
            super(view);
            practiceRoomNameView = view.findViewById(R.id.eachMyPracticeRoomStatusRoomName);
            dateView = view.findViewById(R.id.eachMyPracticeRoomStatusLessonDate);
            timeView = view.findViewById(R.id.eachMyPracticeRoomStatusTime);
            cancelBtn = view.findViewById(R.id.eachMyPracticeRoomStatusCancel);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.part_each_my_practice_room_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.practiceRoomNameView.setText(data.get(position).getRoomName());
        viewHolder.dateView.setText(data.get(position).getDate());
        viewHolder.timeView.setText(data.get(position).getRoomUsageTime());

        String bookDataId = data.get(position).getId();

        viewHolder.cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                cancelBook(bookDataId, position);
            }
        });
    }

    // 예약된 연습실 취소하기
    public void cancelBook(String bookStatusId, int position) {
        // 요청 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                System.out.println("## data : " + responseObj.data);
                dialog_confirm.show(context, "연습실 예약이 취소하였습니다.");

                // recyclerView 에서 해당 데이터 지워주기
                data.remove(position);
                Adapter_each_my_practice_room_status.this.notifyItemRemoved(position);

                // 예약한 연습실이 없을 때 데이터 없음 화면 띄워주기
                if (data.size() == 0) {
                    Bundle bundle = new Bundle();
                    bundle.putString(context.getString(R.string.noDataTitle), "내 연습실 예약 현황");
                    bundle.putString(context.getString(R.string.noDataText), "예약한 연습실이 없습니다.");

                    fragment_no_data fragment = new fragment_no_data();
                    fragment.setArguments(bundle);

                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.fragmentMyPracticeRoomStatus, fragment);
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
        httpRequestAPIs.cancelPracticeRoom(bookStatusId, new Callee_success(), new Callee_failed());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
