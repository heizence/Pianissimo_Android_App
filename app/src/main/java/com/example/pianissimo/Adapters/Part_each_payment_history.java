package com.example.pianissimo.Adapters;

public class Part_each_payment_history {
    // 변수명은 서버에서 보내주는 데이터의 변수명과 통일하야 함. 변경 시 서버 쪽과 반드시 같이 변경하기
    private String id;  // 결제 내역 데이터 고유 id 값
    private Boolean isActive;   // 이용권 이용중 여부. true 이면 이용중, false 이면 만료
    private String ticketName;   // 이용권 이름(개월 수 + 종류)
    private String startDate; // 이용권 결제 날짜
    private String endDate; // 이용권 만료 날짜

    public Part_each_payment_history(String id, Boolean isActive, String ticketName, String startDate, String endDate) {
        this.isActive = isActive;
        this.id = id;
        this.ticketName = ticketName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // 각 데이터 불러오기
    public String getId() {return id;}
    public Boolean getIsActive() {return isActive;}
    public String getTicketName() {return ticketName;}
    public String getStartDate() {return startDate;}
    public String getEndDate() {return endDate;}

}
