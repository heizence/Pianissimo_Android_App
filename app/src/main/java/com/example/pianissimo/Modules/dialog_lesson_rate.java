package com.example.pianissimo.Modules;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.pianissimo.Adapters.Adapter_each_my_lesson_history;
import com.example.pianissimo.R;

public class dialog_lesson_rate {
    // activity 에서 dialog 열 때 호출하는 매서드
    // adapter 에 해당 viewHolder 를 파라미터로 받아서 별 아이콘에 접근 가능하도록 해 줌.
    public static void show(Context context,
                            Adapter_each_my_lesson_history adapter,
                            Adapter_each_my_lesson_history.ViewHolder viewHolder
    ) {

        Dialog_Lesson_Rate dialog = new Dialog_Lesson_Rate(context, adapter, viewHolder);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // 확인 버튼 dialog 생성하는 class
    static class Dialog_Lesson_Rate extends Dialog {
        Adapter_each_my_lesson_history Adapter;
        Adapter_each_my_lesson_history.ViewHolder adapterViewHolder;
        private ImageView localStar1;
        private ImageView localStar2;
        private ImageView localStar3;
        private ImageView localStar4;
        private ImageView localStar5;

        private TextView cancelBtn;
        private TextView confirmBtn;

        private String lessonId;
        private int rateState;

        public Dialog_Lesson_Rate(@NonNull
                                          Context context,
                                  Adapter_each_my_lesson_history adapter,
                                  Adapter_each_my_lesson_history.ViewHolder viewHolder
        ) {
            super(context);
            Adapter = adapter;
            adapterViewHolder = viewHolder;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_lesson_rate);

            int selectedItemIndex = Adapter.selectedItemIndex;
            lessonId = Adapter.data.get(selectedItemIndex).getId();
            rateState = Adapter.data.get(selectedItemIndex).getRate();

            System.out.println("## lessonId : " + lessonId);
            System.out.println("## rateState : " + rateState);

            cancelBtn = findViewById(R.id.dialogLessonRateCancelBtn);
            confirmBtn = findViewById(R.id.dialogLessonRateConfirmBtn);

            localStar1 = findViewById(R.id.dialogLessonRateStar1);
            localStar2 = findViewById(R.id.dialogLessonRateStar2);
            localStar3 = findViewById(R.id.dialogLessonRateStar3);
            localStar4 = findViewById(R.id.dialogLessonRateStar4);
            localStar5 = findViewById(R.id.dialogLessonRateStar5);

            localStar1.setOnClickListener(view -> rateAction(1));
            localStar2.setOnClickListener(view -> rateAction(2));
            localStar3.setOnClickListener(view -> rateAction(3));
            localStar4.setOnClickListener(view -> rateAction(4));
            localStar5.setOnClickListener(view -> rateAction(5));

            rateAction(rateState);

            cancelBtn.setOnClickListener(view -> dismiss());
            confirmBtn.setOnClickListener(view -> rateLessonRequest());
        }

        // 평점 매기기. 평점에 따라 별 색칠해 주기. 왼쪽에서 오른쪽 순서로 색칠해 주기
        public void rateAction(int rateNumber) {
            // 평점 매길 때 마다 기존에 적용된 평점을 지우고 다시 적용
            localStar1.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));
            localStar2.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));
            localStar3.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));
            localStar4.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));
            localStar5.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));

            if (rateNumber >= 1) {
                localStar1.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                rateState = 1;
                if (rateNumber >= 2) {
                    localStar2.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                    rateState = 2;
                    if (rateNumber >= 3) {
                        localStar3.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                        rateState = 3;
                        if (rateNumber >= 4) {
                            localStar4.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                            rateState = 4;
                            if (rateNumber == 5) {
                                localStar5.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                                rateState = 5;
                            }
                        }
                    }
                }
            }
        }

        // recyclerView 에서 랜더링 되고 있는 레슨 데이터에 있는 평점 변경해 주기
        public void editLessonRate2(int rateNumber) {
            // 평점 변경할 때 기존에 적용된 평점을 지우고 다시 적용
            adapterViewHolder.star1.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));
            adapterViewHolder.star2.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));
            adapterViewHolder.star3.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));
            adapterViewHolder.star4.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));
            adapterViewHolder.star5.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_unrated));

            if (rateNumber >= 1) {
                adapterViewHolder.star1.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                rateState = 1;
                if (rateNumber >= 2) {
                    adapterViewHolder.star2.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                    rateState = 2;
                    if (rateNumber >= 3) {
                        adapterViewHolder.star3.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                        rateState = 3;
                        if (rateNumber >= 4) {
                            adapterViewHolder.star4.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                            rateState = 4;
                            if (rateNumber == 5) {
                                adapterViewHolder.star5.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_star_rated));
                                rateState = 5;
                            }
                        }
                    }
                }
            }
        }

        // 레슨 평가하기
        public void rateLessonRequest() {
            if (rateState == 0) {
                Toast toast = Toast.makeText(getContext(), "평점을 선택해 주세요.", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                // 요청 성공 시 실행할 callback

                class Callee_success extends httpRequestAPIs.Callee_success {
                    public void call(httpRequestAPIs.ResponseObject responseObj) {
                        System.out.println("## data : " + responseObj);
                        Adapter.editLessonRate(adapterViewHolder, rateState);
                        dismiss();
                    }
                }
                // 요청 실패 시 실행할 callback
                class Callee_failed extends httpRequestAPIs.Callee_failed {
                    public void call(int statusCode) {

                        dialog_confirm.show(getContext(), "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
                    }
                }
                httpRequestAPIs.rateLesson(lessonId, rateState, new Callee_success(), new Callee_failed());
            }
        }
    }
}



