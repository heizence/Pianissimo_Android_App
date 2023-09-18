package com.example.pianissimo.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.Modules.httpRequestAPIs;
import com.example.pianissimo.R;
import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class fragment_book_lesson extends Fragment {
    private View fragmentView;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;

    private TextView lessonsLeftNumView;
    private LinearLayout getPreviousWeekDataBtn;
    private LinearLayout getNextWeekDataBtn;
    private TextView dateRangeTxt;
    private FlexboxLayout instructorBtnLayout;
    private Button previouslySelectedInstructorBtn; // 강사 버튼 터치할 때 이전에 선택 상태였던 버튼
    private GridLayout lessonStatusTable;

    private String lessonsLeft;    // 남은 레슨권 수. 연산이 요구되는 경우는 없으므로 String 으로 저장.
    private LocalDate startDate;   // 레슨 데이터 조회 범위 시작 날짜
    private LocalDate endDate; // 레슨 데이터 조회 범위 끝 날짜
    DateTimeFormatter dateFormatDot;   // 날짜를 string 으로 변환해 주는 format. 20xx.xx.xx
    DateTimeFormatter dateFormatDash;   // 날짜를 string 으로 변환해 주는 format. 20xx-xx-xx

    private String selectedInstructorName;  // 현재 선택된 강사 버튼에 해당하는 강사 이름. 레슨 예약 현황 조회 시 필터링에 사용됨
    private AppUserLessonStatus userLessonStatus;   // 앱 사용자가 예약한 레슨 날짜 및 시간 저장 공간. 테이블 랜더링 때 사용됨

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentView = inflater.inflate(R.layout.fragment_book_lesson, container, false);
        sharedPref = getContext().getSharedPreferences(getString(R.string.sharedPreferenceMain), Context.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        lessonsLeft = sharedPref.getString(getString(R.string.store_AU_LessonsLeft), "0");

        lessonsLeftNumView = fragmentView.findViewById(R.id.fragmentBookLessonLessonsLeftNum);
        getPreviousWeekDataBtn = fragmentView.findViewById(R.id.fragmentBookLessonGetPreviousWeekDataBtn);
        getNextWeekDataBtn = fragmentView.findViewById(R.id.fragmentBookLessonGetNextWeekDataBtn);
        dateRangeTxt = fragmentView.findViewById(R.id.fragmentBookLessonDateRange);
        instructorBtnLayout = fragmentView.findViewById(R.id.fragmentBookLessonInstructorBtns);
        lessonStatusTable = fragmentView.findViewById(R.id.fragmentBookLessonLessonStatusTable);

        lessonsLeftNumView.setText(lessonsLeft);
        userLessonStatus = new AppUserLessonStatus();

        setInitDateRangeAndButtonEvents();
        getInstructorNames();

        return fragmentView;
    }

    // 레슨 데이터 조회 날짜 설정 및 버튼 이벤트 등록
    public void setInitDateRangeAndButtonEvents() {
        // 레슨 예약 현황 조회 범위 날짜 초기 설정
        dateFormatDot = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        dateFormatDash = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        startDate = today.with(java.time.DayOfWeek.MONDAY);
        endDate = today.with(java.time.DayOfWeek.SUNDAY);
        String dateRangeString = startDate.format(dateFormatDot) + " ~ " + endDate.format(dateFormatDot);
        dateRangeTxt.setText(dateRangeString); // 날짜 범위 표시

        // 레슨 예약 현황 조회 범위 날짜 선택 버튼(이전 주, 다음 주) 이벤트 등록
        getPreviousWeekDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDate = startDate.minusWeeks(1);
                endDate = endDate.minusWeeks(1);

                String dateRangeString = startDate.format(dateFormatDot) + " ~ " + endDate.format(dateFormatDot);
                dateRangeTxt.setText(dateRangeString); // 날짜 범위 표시
                getData();
            }
        });
        getNextWeekDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDate = startDate.plusWeeks(1);
                endDate = endDate.plusWeeks(1);

                String dateRangeString = startDate.format(dateFormatDot) + " ~ " + endDate.format(dateFormatDot);
                dateRangeTxt.setText(dateRangeString); // 날짜 범위 표시
                getData();
            }
        });
    }

    // 강사 이름 불러오기
    public void getInstructorNames() {
        // 요청 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                //System.out.println("## data : " + responseObj.data);

                // 데이터 JSON 에서 배열 형식으로 변환
                Gson gson = new Gson();
                String[] InstructorsName = gson.fromJson(responseObj.data, String[].class);

                // 강사 이름으로 버튼 생성
                createInstructorBtns(InstructorsName);
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                System.out.println("## failed statusCode : " + statusCode);
                dialog_confirm.show(getContext(), "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
            }
        }
        httpRequestAPIs.getInstructorsName(new Callee_success(), new Callee_failed());
    }

    // 강사 버튼 랜더링
    public void createInstructorBtns(String[] InstructorsName) {
        // 선택된 강사 이름, 각 강사 버튼 선택 상태 배열 초기 설정
        selectedInstructorName = InstructorsName[0];

        // 불러온 강사 이름 갯수만큼 버튼 생성
        for (int i = 0; i < InstructorsName.length; i++) {
            // 버튼 생성
            Button eachInstructorBtn = new Button(getContext());
            eachInstructorBtn.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);

            /***************************** 버튼 디자인적인 부분 생성 및 적용 **********************************/
            // 적용해줄 margin, width, height 수치 생성
            int margin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    10f,
                    getResources().getDisplayMetrics()
            ); // convert 16dp to pixels

            int minWidth = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    60,
                    getResources().getDisplayMetrics()
            ); // convert 16dp to pixels

            int height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    30,
                    getResources().getDisplayMetrics()
            ); // convert 16dp to pixels

            // padding 설정
            eachInstructorBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, height
                    //ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            eachInstructorBtn.setPadding(4, 4, 4, 4);

            // margin 설정
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) eachInstructorBtn.getLayoutParams();
            layoutParams.setMargins(0, 0, margin, margin);

            // layout, width 설정
            eachInstructorBtn.setLayoutParams(layoutParams);
            eachInstructorBtn.setMinimumWidth(minWidth);

            // background style 설정
            eachInstructorBtn.setBackgroundResource(R.drawable.style_book_item_btn);

            /********************************************************************************************/

            eachInstructorBtn.setText(InstructorsName[i]);  // 강사 이름 표시

            // 화면 진입 시 첫 번째 버튼(가장 왼쪽)은 터치 상태로 설정해 주기
            if (i == 0) {
                eachInstructorBtn.setSelected(true);
                selectedInstructorName = InstructorsName[i];
                //instructorBtnsIsSelectedArr.set(0, true);
                eachInstructorBtn.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.white)
                );
                previouslySelectedInstructorBtn = eachInstructorBtn;
            }

            /* 강사 버튼 터치 이벤트 등록
            각 버튼 선택, 미선택 여부는 eachInstructorBtnfinal.isSelected 를 통해 판별함.
            버튼은 한 번에 1개만 선택된 상태로 있을 수 있음.
            최소 1개의 버튼은 선택된 상태로 있어야 함.
            */

            // Variable 'i' is accessed from within inner class, needs to be final or effectively final
            int finalI = i;
            eachInstructorBtn.setOnClickListener(new View.OnClickListener() {
                final Button eachInstructorBtnfinal = eachInstructorBtn;    // do not delete!

                @Override
                public void onClick(View view) {
                    // 버튼이 미선택 상태일 때
                    if (!eachInstructorBtnfinal.isSelected()) {
                        eachInstructorBtnfinal.setSelected(true);
                        eachInstructorBtn.setTextColor(
                                ContextCompat.getColor(getContext(), R.color.white)
                        );

                        selectedInstructorName = InstructorsName[finalI];
                        previouslySelectedInstructorBtn.setSelected(false);
                        previouslySelectedInstructorBtn.setTextColor(
                                ContextCompat.getColor(getContext(), R.color.black)
                        );
                        previouslySelectedInstructorBtn = eachInstructorBtnfinal;
                        getData();
                        //System.out.println("## button pressed! selectedInstructorName : " + selectedInstructorName);
                    }
                    // 버튼이 선택 상태일 때
                    else {
                        return;
                    }
                }
            });
            // 강사 버튼들을 담고있는 LinearLayout 에 포함시켜 주기
            instructorBtnLayout.addView(eachInstructorBtn);
        }
        getData();  // 버튼 랜더링 끝난 후 데이터 불러오기
    }

    // 레슨 데이터 불환오기
    public void getData() {
        // 요청 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                 System.out.println("## getData : " + responseObj.data);

                Gson gson = new Gson();
                JsonObject lessonData = gson.fromJson(responseObj.data, JsonObject.class);
                generateLessonTable(lessonData);
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                System.out.println("## failed statusCode : " + statusCode);
                dialog_confirm.show(getContext(), "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
            }
        }
        httpRequestAPIs.getLessons(startDate.format(dateFormatDash), endDate.format(dateFormatDash),
                selectedInstructorName, new Callee_success(), new Callee_failed());
    }

    // 레슨 예약하기
    public void bookLesson(String lessonId) {
        if (lessonsLeft.equals("0")) {
            dialog_confirm.show(getContext(), "남은 레슨권이 없습니다.");
        } else {
            // 요청 성공 시 실행할 callback
            class Callee_success extends httpRequestAPIs.Callee_success {
                public void call(httpRequestAPIs.ResponseObject responseObj) {
                    System.out.println("## data : " + responseObj);
                    dialog_confirm.show(getContext(), "예약이 완료되었습니다.");

                    // 남은 레슨 수 1 차감
                    int lessonsLeftUpdated = Integer.valueOf(lessonsLeft) - 1;
                    sharedPrefEditor.putString(getString(R.string.store_AU_LessonsLeft), String.valueOf(lessonsLeftUpdated));
                    sharedPrefEditor.apply();
                    lessonsLeftNumView.setText(String.valueOf(lessonsLeftUpdated));
                    getData();
                }
            }

            // 요청 실패 시 실행할 callback
            class Callee_failed extends httpRequestAPIs.Callee_failed {
                public void call(int statusCode) {
                    System.out.println("## failed statusCode : " + statusCode);
                    dialog_confirm.show(getContext(), "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
                }
            }

            String studentId = sharedPref.getString(getString(R.string.store_AU_Id), "");

            httpRequestAPIs.bookLesson(studentId, lessonId, new Callee_success(), new Callee_failed());
        }
    }

    /* 앱 사용자가 예약한 레슨 날짜 및 시간 저장 공간
    레슨 날짜 및 시간: true 형식으로 저장. 예) 2023-03-22&12 : true
     */
    class AppUserLessonStatus {
        private Map<String, Object> data = new HashMap<>();

        public void add(String key) {
            data.put(key, true);
        }

        public String get(String key) {
            if (data.get(key) == null) return null;
            else return data.get(key).toString();
        }
    }

    // 레슨 예약 현황 테이블 생성
    public void generateLessonTable(JsonObject lessonData) {
        Context context = getContext();
        String dayOfWeek[] = {"", "월", "화", "수", "목", "금", "토", "일"};   // 첫 번째 원소 "" 지우지 말 것!
        int startTimeArr[] = {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};   // 레슨 시작 시간

        TextView textView;
        GridLayout.LayoutParams params;
        lessonStatusTable.setPadding(16, 16, 16, 16);

        for (int i = 0; i < lessonStatusTable.getRowCount(); i++) {
            for (int j = 0; j < lessonStatusTable.getColumnCount(); j++) {
                textView = new TextView(context);
                textView.setBackground(context.getDrawable(R.drawable.style_book_each_block));

                // 이전에 클릭처리 및 등록된 이벤트 없애주기. 지우지 말 것!
                textView.setClickable(false);
                textView.setOnClickListener(null);

                params = new GridLayout.LayoutParams();
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.rowSpec = GridLayout.spec(i, 1, 1f); // span 1 row starting at row i
                params.columnSpec = GridLayout.spec(j, 1, 1f); // span 1 column starting at column j

                // 첫 번째 row 는 요일 랜더링
                if (i == 0) {
                    String day = Array.get(dayOfWeek, j).toString();
                    textView.setText(day);
                    textView.setPadding(5, 5, 5, 5);
                    textView.setTextSize(18);
                    textView.setGravity(Gravity.CENTER);
                    lessonStatusTable.addView(textView, params);
                }
                // 두 번째 row 부터 선택 칸 랜더링
                else {
                    // 첫 번째 column 은 레슨 시간 랜더링
                    if (j == 0) {
                        LinearLayout layout = new LinearLayout(context);
                        TextView startTimeTxt = new TextView((context));
                        TextView endTimeTxt = new TextView((context));

                        int startTime = (Integer) Array.get(startTimeArr, i - 1);
                        int endTime = startTime + 1;

                        startTimeTxt.setText(startTime + ":00 ~");
                        endTimeTxt.setText(endTime + ":00");
                        startTimeTxt.setTextSize(17);
                        endTimeTxt.setTextSize(17);

                        layout.addView(startTimeTxt);
                        layout.addView(endTimeTxt);
                        layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setPadding(5, 5, 0, 5);
                        layout.setBackground(context.getDrawable(R.drawable.style_book_each_block));
                        lessonStatusTable.addView(layout, params);
                    }
                    /*
                    레슨 상태를 표시하고 터치해서 예약할 수 있는 칸 랜더링
                    예약이 비어 있는 칸은 푸른색, 예약이 차 있는 칸은 회색, 날짜와 시간이 지나서 아예 접근이 불가능한 경우에는 흰색으로 랜더링
                    내가 예약한 날짜와 시간에 해당하는 칸에는 체크 이미지 넣어주기
                    */
                    else {
                        //textView.setText((i + 1) + ", " + (j + 1)); // for test

                        // 각 cell TextView 에 레슨 데이터 적용해 주기
                        LocalDate lessonDate = startDate.plusDays(j - 1);   // j 가 1일 때부터 랜더링이 시작되므로 1 빼주기. 처음부터 시작 날짜에 1을 더하면 안 됨.

                        int lessonStartTime = 12 + i - 1;   // i 가 1일 때부터 랜더링이 시작되므로 1 빼주기

                        // 레슨 예약 현황 데이터 객체의 key 값. 서버랑 형식이 일치해야 함.
                        String eachLessonDataKey = lessonDate.format(dateFormatDash) + "&" + lessonStartTime;
                        // key 값에 매칭되는 value 값. 데이터 고유 id + 예약한 원생 고유 id(예약 없으면 null 로 표시)
                        JsonElement eachLessonDataValue = lessonData.get(eachLessonDataKey);

                        // 데이터가 있을 때 경우에 따라 랜더링 및 클릭 이벤트 적용해 주기
                        if (eachLessonDataValue != null) {
                            String[] valueStr = eachLessonDataValue.toString().replace("\"", "").split("&");
                            String lessonId = valueStr[0];  // 데이터 고유 id
                            String studentId = valueStr[1]; // 예약한 원생 고유 id. 예약 없으면 null 로 표시

                            // 예약이 비어 있고 사용자가 예약한 다른 강사의 날짜와 시간과 겹치지 않을 때
                            if (studentId.equals("null") && userLessonStatus.get(eachLessonDataKey) == null) {
                                System.out.println("## lesson empty! " + "key : " + eachLessonDataKey + ", value : " + eachLessonDataValue);
                                textView.setBackground(context.getDrawable(R.drawable.style_book_each_block_available));
                                textView.setClickable(true);

                                // 칸 클릭 시 예약 요청하기
                                TextView finalTextView = textView;
                                textView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        bookLesson(lessonId);
                                        finalTextView.setOnClickListener(null); // 레슨 예약 후 해당 칸에 있는 클릭 이벤트 listener 제거해 주기. 안 하면 중복 클릭됨
                                    }
                                });
                            } else {
                                String appUserId = sharedPref.getString(getString(R.string.store_AU_Id), "");   // 앱 사용자 회원 고유 id

                                // 예약한 원생이 앱 사용자일 경우(자신이 예약한 레슨일 경우)
                                if (studentId.equals(appUserId)) {
                                    System.out.println("## my lesson! " + "key : " + eachLessonDataKey + ", value : " + eachLessonDataValue);

                                    ImageView checkImageView = new ImageView(context);
                                    checkImageView.setImageResource(R.drawable.ic_check_01);
                                    checkImageView.setBackground(context.getDrawable(R.drawable.style_book_each_block)); // 칸 색상 흰색으로 돌려주기. 지우지 말 것!

                                    userLessonStatus.add(eachLessonDataKey);    // 사용자가 예약한 날짜 및 시간대 추가
                                    lessonStatusTable.addView(checkImageView, params);
                                    continue;
                                } else {
                                    System.out.println("## lesson occupied! " + "key : " + eachLessonDataKey + ", value : " + eachLessonDataValue);
                                    textView.setBackground(context.getDrawable(R.drawable.style_book_each_block_occupied));
                                }
                            }
                        }
                        lessonStatusTable.addView(textView, params);
                    }
                }
            }
        }
    }
}
