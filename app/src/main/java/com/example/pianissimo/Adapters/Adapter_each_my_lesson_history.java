package com.example.pianissimo.Adapters;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pianissimo.Modules.dialog_lesson_rate;
import com.example.pianissimo.R;

import java.util.ArrayList;
import java.util.List;

public class Adapter_each_my_lesson_history extends RecyclerView.Adapter<Adapter_each_my_lesson_history.ViewHolder> {
    private Adapter_each_my_lesson_history Adapter;
    public Context context;
    public int selectedItemIndex;

    // 레슨 내역 데이터. 평가하기 모달에서 접근할 필요가 있으므로 private 이 아닌 public 으로 선언. 변경하지 말 것!
    public List<Part_each_my_lesson_history> data = new ArrayList<>();

    // adapter 생성. 부모 activity 또는 이전 fragment 에서 FragmentManager 객체 받아오기
    public Adapter_each_my_lesson_history(Context context) {
        this.Adapter = this;
        this.context = context;
    }

    // 데이터 추가
    public void add(Part_each_my_lesson_history object) {
        data.add(object);
        this.notifyItemInserted(data.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView instructorNameView;
        private final TextView dateView;
        private final TextView timeView;
        private final Button rateBtn;

        // 평가하기 모달에서 접근할 필요가 있으므로 private 이 아닌 public 으로 선언. 변경하지 말 것!
        public final ImageView star1;
        public final ImageView star2;
        public final ImageView star3;
        public final ImageView star4;
        public final ImageView star5;

        public ViewHolder(View view) {
            super(view);
            instructorNameView = view.findViewById(R.id.eachMyLessonHistoryInstructorName);
            dateView = view.findViewById(R.id.eachMyLessonHistoryLessonDate);
            timeView = view.findViewById(R.id.eachMyLessonHistoryTime);
            rateBtn = view.findViewById(R.id.eachMyLessonHistoryRateBtn);

            star1 = view.findViewById(R.id.eachMyLessonHistoryStar1);
            star2 = view.findViewById(R.id.eachMyLessonHistoryStar2);
            star3 = view.findViewById(R.id.eachMyLessonHistoryStar3);
            star4 = view.findViewById(R.id.eachMyLessonHistoryStar4);
            star5 = view.findViewById(R.id.eachMyLessonHistoryStar5);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.part_each_my_lesson_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.instructorNameView.setText(data.get(position).getInstructorName());
        viewHolder.dateView.setText(data.get(position).getDate());
        viewHolder.timeView.setText(data.get(position).getLessonTime());

        int rate = data.get(position).getRate();

        // 평점에 따라 별 색칠해 주기. 오른쪽에서 왼쪽 순서로 색칠해 주기
        if (rate >= 1) {
            viewHolder.star1.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
            if (rate >= 2) {
                viewHolder.star2.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
                if (rate >= 3) {
                    viewHolder.star3.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
                    if (rate >= 4) {
                        viewHolder.star4.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
                        if (rate == 5) {
                            viewHolder.star5.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
                        }
                    }
                }
            }
            viewHolder.rateBtn.setText("수정");   // 평점이 1 이상인 경우엔 첫 평가가 아닌 재평가이므로 수정.
        }

        int adapterPosition = viewHolder.getAdapterPosition();

        viewHolder.rateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedItemIndex = adapterPosition;
                dialog_lesson_rate.show(context, Adapter, viewHolder);
            }
        });
    }

    // recyclerView 에서 랜더링 되고 있는 레슨 데이터에 있는 평점 변경해 주기
    public void editLessonRate(ViewHolder viewHolder, int rateNumber) {
        // 평점 변경할 때 기존에 적용된 평점을 지우고 다시 적용
        viewHolder.star1.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_unrated));
        viewHolder.star2.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_unrated));
        viewHolder.star3.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_unrated));
        viewHolder.star4.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_unrated));
        viewHolder.star5.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_unrated));

        // 별 아이콘 색칠 다시 해 주기
        int updatedRate = 0;
        if (rateNumber >= 1) {
            viewHolder.star1.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
            updatedRate = 1;
            if (rateNumber >= 2) {
                viewHolder.star2.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
                updatedRate = 2;
                if (rateNumber >= 3) {
                    viewHolder.star3.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
                    updatedRate = 3;
                    if (rateNumber >= 4) {
                        viewHolder.star4.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
                        updatedRate = 4;
                        if (rateNumber == 5) {
                            viewHolder.star5.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_star_rated));
                            updatedRate = 5;
                        }
                    }
                }
            }
        }

        // 해당 레슨 데이터를 새로운 데이터로 변경해 주기. 평점 수정해 주기
        Part_each_my_lesson_history newData = new Part_each_my_lesson_history(
                data.get(selectedItemIndex).getId(),
                data.get(selectedItemIndex).getInstructorName(),
                data.get(selectedItemIndex).getDate(),
                data.get(selectedItemIndex).getStartTime(),
                updatedRate
        );
        data.set(selectedItemIndex, newData);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
