package com.example.pianissimo.Adapters;

public class Part_each_my_lesson_history {
    // 변수명은 서버에서 보내주는 데이터의 변수명과 통일하야 함. 변경 시 서버 쪽과 반드시 같이 변경하기
    private String id;  // 레슨 내역 데이터 고유 id 값
    private String instructorName;   // 강사 이름
    private String date; // 레슨 날짜
    private int startTime; // 레슨 시간
    private int rate; // 평점

    public Part_each_my_lesson_history(String id, String instructorName, String date, int startTime, int rate) {
        this.id = id;
        this.instructorName = instructorName;
        this.date = date;
        this.startTime = startTime;
        this.rate = rate;
    }

    // 각 데이터 불러오기
    public String getId() {return id;}
    public String getInstructorName() {return instructorName;}
    public String getDate() {return date;}
    public int getStartTime() {return startTime;}
    public String getLessonTime() {
        int endTime = startTime + 1;
        String timeRange = startTime + ":00 ~ " + endTime + ":00";
        return timeRange;
    }
    public int getRate() {return rate;}
}
