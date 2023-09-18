package com.example.pianissimo.Adapters;

public class Part_each_broadcast {
    // 변수명은 서버에서 보내주는 데이터의 변수명과 통일하야 함. 변경 시 서버 쪽과 반드시 같이 변경하기
    private String id;  // 방송 데이터 고유 id 값
    private String thumbnailImage; // 섬네일 이미지
    private boolean isLive; // 라이브 여부
    private String roomName;   // 방송 제목
    private String hostName; // 스트리머 이름
    private String hostProfileImage; // 스트리머 프로필 이미지
    private int numberOfWatchers; // 방송 시청자 수. 라이브 방송일 경우에만 표시.
    private String liveStartedAt; // 방송이 시작된 날짜 및 시간. 예) "2023-03-21 18:30:12"

    public Part_each_broadcast(String id, String thumbnailImage, boolean isLive, String roomName, String hostName, String hostProfileImage, int numberOfWatchers, String liveStartedAt) {
        this.id = id;
        this.thumbnailImage = thumbnailImage;
        this.isLive = isLive;
        this.roomName = roomName;
        this.hostName = hostName;
        this.hostProfileImage = hostProfileImage;
        this.numberOfWatchers = numberOfWatchers;
        this.liveStartedAt = liveStartedAt;
    }

    public String getId() {return id;}
    public String getThumbnailImage() {return thumbnailImage;}
    public boolean getIsLive() {return isLive;}
    public String getRoomName() {return roomName;}
    public String getHostName() {return hostName;}
    public String getHostProfileImage() {return hostProfileImage;}
    public int getNumberOfWatchers() {return numberOfWatchers;}
    public String getLiveStartedAt() {return liveStartedAt;}
}
