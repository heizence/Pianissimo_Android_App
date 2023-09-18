package com.example.pianissimo.Adapters;

public class Part_each_live_chat_message {
    // 변수명은 서버에서 보내주는 데이터의 변수명과 통일하야 함. 변경 시 서버 쪽과 반드시 같이 변경하기
    private String senderName; // 채팅 전송자 이름
    private String senderProfileImg; // 채팅 전송자 프로필 이미지
    private String messageContents; // 채팅 메시지 내용

    public Part_each_live_chat_message(String senderName, String senderProfileImg, String messageContents) {
        this.senderName = senderName;
        this.senderProfileImg = senderProfileImg;
        this.messageContents = messageContents;
    }

    public String getSenderName() {return senderName;}
    public String getSenderProfileImg() {return senderProfileImg;}
    public String getMessageContents() {return messageContents;}
}
