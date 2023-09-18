package com.example.pianissimo.Adapters;

public class Part_each_notice {
    // 변수명은 서버에서 보내주는 테이터의 변수명과 통일하야 함. 변경 시 서버 쪽과 반드시 같이 변경하기
    private String id;  // 공지사항 데이터 고유 id 값
    private String title;   // 제목
    private String contents; // 내용
    private String writtenDate; // 작성날짜

    public Part_each_notice(String id, String title, String contents, String writtenDate) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.writtenDate = writtenDate;
    }

    // 각 데이터 불러오기
    public String getId() {return id;}
    public String getTitle() {return title;}
    public String getContents() {return contents;}
    public String getWrittenDate() {return writtenDate;}
}
