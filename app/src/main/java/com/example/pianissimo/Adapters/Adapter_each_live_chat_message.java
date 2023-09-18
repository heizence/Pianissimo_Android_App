package com.example.pianissimo.Adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pianissimo.R;

import java.util.ArrayList;
import java.util.List;

public class Adapter_each_live_chat_message extends RecyclerView.Adapter<Adapter_each_live_chat_message.ViewHolder> {
    private String screenName = "[RECYCLER_VIEW_ADAPTER_each_live_chat_broadcast]:";  //  Fragment 일 경우 FRAGMENT 로 할 수도 있음
    private String tag_check = screenName + "[CHECK]";  // 특정 값을 확인할 때 사용
    private String tag_execute = screenName + "[EXECUTE]";  // 매서드나 다른 실행 가능한 코드를 실행할 때 사용
    private String tag_event = screenName + "[EVENT]";  // 특정 이벤트 발생을 확인할 때 사용
    public Context context;

    // sharedPreference 를 다룰 필요가 있을 때 사용
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;

    // 실시간 채팅 내역 데이터
    private List<Part_each_live_chat_message> data = new ArrayList<>();

    // adapter 생성
    public Adapter_each_live_chat_message(Context context) {
        this.context = context;
        sharedPref = context.getSharedPreferences(context.getString(R.string.sharedPreferenceMain), context.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();
    }

    // 기기 화면 전환 시 기존의 채팅 데이터 다시 불러와서 랜더링 해 주기
    public void loadChatData(List<Part_each_live_chat_message> chatMessageData) {
        data = chatMessageData;

    }

    // recyclerView 데이터 갱신할 때 사용. ArrayList 비워주고 다시 add 해서 채워넣기.
    public void emptyData() {
        this.data = new ArrayList<>();
    }

    // 데이터 추가
    public void add(String senderName, String senderProfileImg, String messageContents) {
        Part_each_live_chat_message chatMessageObj = new Part_each_live_chat_message(senderName, senderProfileImg, messageContents);
        data.add(chatMessageObj);
        this.notifyItemInserted(data.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView senderProfileImg;
        private final TextView senderName;
        private final TextView chatMessageContents;

        public ViewHolder(View view) {
            super(view);
            senderProfileImg = view.findViewById(R.id.part_each_live_chat_message_senderProfileImg);
            senderName = view.findViewById(R.id.part_each_live_chat_message_senderName);
            chatMessageContents = view.findViewById(R.id.part_each_live_chat_message_chatContents);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.part_each_live_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        // 스트리머 프로필 이미지 표시
        String senderProfileImage = data.get(position).getSenderProfileImg();

        // 프로필 사진이 있는 경우에만 표시해 주기
        if (senderProfileImage != null && senderProfileImage.length() != 0) {
            byte[] senderProfile_decodedString = Base64.decode(senderProfileImage, Base64.DEFAULT);
            Bitmap senderProfile_decodedBitmap = BitmapFactory.decodeByteArray(senderProfile_decodedString, 0, senderProfile_decodedString.length);
            viewHolder.senderProfileImg.setImageBitmap(senderProfile_decodedBitmap);
        }

        viewHolder.senderName.setText(data.get(position).getSenderName());
        viewHolder.chatMessageContents.setText(data.get(position).getMessageContents());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
