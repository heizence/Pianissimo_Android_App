package com.example.pianissimo.Adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pianissimo.Fragments.fragment_each_notice;
import com.example.pianissimo.R;

import java.util.ArrayList;
import java.util.List;

public class Adapter_each_notice extends RecyclerView.Adapter<Adapter_each_notice.ViewHolder>{
    private List<Part_each_notice> data = new ArrayList<>();
    private FragmentManager fragmentManager;

    // adapter 생성. 부모 activity 또는 이전 fragment 에서 FragmentManager 객체 받아오기
    public Adapter_each_notice(FragmentManager fragmentManagerInput) {
        fragmentManager = fragmentManagerInput;
    }

    // 데이터 추가
    public void add(Part_each_notice object) {
        data.add(object);
        this.notifyItemInserted(data.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView noticeTitleTextView;
        private final TextView noticeWrittenDateTextView;

        public ViewHolder(View view) {
            super(view);
            noticeTitleTextView = view.findViewById(R.id.part_eachNoticeTitle);
            noticeWrittenDateTextView = view.findViewById(R.id.part_eachNoticeWrittenDate);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();

                    // 각 공지사항 fragment 에 전달해 줄 데이터 생성
                    Bundle bundle = new Bundle();
                    bundle.putString(context.getString(R.string.noticeTitle), data.get(getAdapterPosition()).getTitle());
                    bundle.putString(context.getString(R.string.noticeWrittenDate), data.get(getAdapterPosition()).getWrittenDate());
                    bundle.putString(context.getString(R.string.noticeContents), data.get(getAdapterPosition()).getContents());

                    // 각 공지사항 fragment 선언
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    fragment_each_notice fragment = new fragment_each_notice();
                    fragment.setArguments(bundle);  // bundle 데이터 넣어주기

                    // 각 공지사항 fragment 로 넘어가기
                    transaction.replace(R.id.mainFragmentView, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.part_each_notice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.noticeTitleTextView.setText(data.get(position).getTitle());
        viewHolder.noticeWrittenDateTextView.setText(data.get(position).getWrittenDate());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
