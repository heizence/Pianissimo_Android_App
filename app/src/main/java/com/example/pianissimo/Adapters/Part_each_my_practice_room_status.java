package com.example.pianissimo.Adapters;

public class Part_each_my_practice_room_status {
    // 변수명은 서버에서 보내주는 테이터의 변수명과 통일하야 함. 변경 시 서버 쪽과 반드시 같이 변경하기
    private String id;  // 연습실 예약 상태 데이터 고유 id 값
    private String roomName;   // 연습실 이름
    private String date; // 연습실 이용 날짜
    private int startTime; // 연습실 이용 시작 시간

    public Part_each_my_practice_room_status(String id, String roomName, String date, int startTime) {
        this.id = id;
        this.roomName = roomName;
        this.date = date;
        this.startTime = startTime;
    }

    // 각 데이터 불러오기
    public String getId() {return id;}
    public String getRoomName() {return roomName;}
    public String getDate() {return date;}
    public String getRoomUsageTime() {
        int endTime = startTime + 1;
        String timeRange = startTime + ":00 ~ " + endTime + ":00";
        return timeRange;
    }
}
