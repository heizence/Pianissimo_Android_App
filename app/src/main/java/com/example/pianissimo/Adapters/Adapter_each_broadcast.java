package com.example.pianissimo.Adapters;

import android.app.Activity;
import android.graphics.Matrix;
import android.util.Log;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pianissimo.Activities.each_live_room_watcher;
import com.example.pianissimo.R;

import java.util.ArrayList;
import java.util.List;

public class Adapter_each_broadcast extends RecyclerView.Adapter<Adapter_each_broadcast.ViewHolder> {
    private String screenName = "[RECYCLER_VIEW_ADAPTER_each_broadcast]:";  //  Fragment 일 경우 FRAGMENT 로 할 수도 있음
    private String tag_check = screenName + "[CHECK]";  // 특정 값을 확인할 때 사용
    private String tag_execute = screenName + "[EXECUTE]";  // 매서드나 다른 실행 가능한 코드를 실행할 때 사용
    private String tag_event = screenName + "[EVENT]";  // 특정 이벤트 발생을 확인할 때 사용

    public Context context;
    private FragmentManager fragmentManager;

    // sharedPreference 를 다룰 필요가 있을 때 사용
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;
    private ActivityResultLauncher<Intent> startForResult;
    public int selectedItemIndex;

    // 방송 목록 데이터
    private List<Part_each_broadcast> data = new ArrayList<>();

    // adapter 생성
    public Adapter_each_broadcast(Context context) {
        this.context = context;
        sharedPref = context.getSharedPreferences(context.getString(R.string.sharedPreferenceMain), context.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();
    }

    // fragmentManager 등록. fragmentManager 가 필요할 때만 사용
    public void setFragmentManager(FragmentManager fragmentManagerInput) {
        this.fragmentManager = fragmentManagerInput;
    }

    public void setActivityResultLauncher(ActivityResultLauncher<Intent> startForResult) {
        this.startForResult = startForResult;
        Log.d(tag_check, "setActivityResultLauncher : " + startForResult);
    }

    // recyclerView 데이터 갱신할 때 사용. ArrayList 비워주고 다시 add 해서 채워넣기.
    public void emptyData() {
        Log.d(tag_check, "emptyData");
        this.data = new ArrayList<>();
    }

    // 데이터 추가
    public void add(Part_each_broadcast object) {
        Log.d(tag_execute, "add");
        data.add(object);
        this.notifyItemInserted(data.size());
    }

    // 데이터 삭제
    private void deleteItem(int position) {
        // recyclerView 에서 해당 데이터 지워주기
        data.remove(position);
        Adapter_each_broadcast.this.notifyItemRemoved(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // 표시해 줄 데이터
        // 라이브의 경우 : streamer name/profileImg, roomName, isLive, numberOfWatchers, thumbnailImage
        // 다시보기일 경우 : streamer name/profileImg, roomName, isLive, liveStartedAt, thumbnailImage

        private final View rootLayout;
        private final ImageView thumbnail;
        private final ImageView hostProfileImg;
        private final TextView isLiveTag;
        private final TextView roomName;
        private final TextView hostName;
        private final TextView viewerOrDate;    // 시청자 수 또는 방송 종료 날짜

        public ViewHolder(View view) {
            super(view);
            rootLayout = view.findViewById(R.id.eachBroadCastLayout);
            thumbnail = view.findViewById(R.id.eachBroadCastThumbnail);
            hostProfileImg = view.findViewById(R.id.eachBroadCastHostProfileImg);
            hostName = view.findViewById(R.id.eachBroadCastHostName);
            isLiveTag = view.findViewById(R.id.eachBroadCastIsLiveTag);
            roomName = view.findViewById(R.id.eachBroadCastRoomName);
            viewerOrDate = view.findViewById(R.id.eachBroadCastViewerOrDate);

            // 각 방송방 항목 클릭 시 동작
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(tag_execute, "view onClick");
                    Context context = v.getContext();

                    boolean isLive = data.get(getAdapterPosition()).getIsLive();    // 라이브 또는 방송 다시보기 여부.
                    Intent intent = new Intent(context, each_live_room_watcher.class);

                    /*
                    1.라이브일 경우 : 시청자 방송방 액티비티 실행하여 방송방 입장
                    액티비티에 넘겨줘야 할 데이터 : roomId(방송 데이터 식별을 위한 고유 id), 방 이름(방송 타이틀), 스트리머 이름/프로필 이미지, 라이브 시작 시간(YYYY-MM-DD hh:mm)
                    */

                    if (isLive) {
                        Log.d(tag_check, "isLive");
                        intent.putExtra(context.getString(R.string.roomId), data.get(getAdapterPosition()).getId());    // roomId
                        intent.putExtra(context.getString(R.string.roomName), data.get(getAdapterPosition()).getRoomName());    // 방 이름(방송 타이틀)
                        intent.putExtra(context.getString(R.string.hostName), data.get(getAdapterPosition()).getHostName());    // 스트리머 이름
                        intent.putExtra(context.getString(R.string.hostProfileImg), data.get(getAdapterPosition()).getHostProfileImage());    // 스트리머 프로필 이미지
                        intent.putExtra(context.getString(R.string.liveStartedAt), data.get(getAdapterPosition()).getLiveStartedAt());    // 라이브 시작 시간
                        startForResult.launch(intent);
                    }
                    // 방송 다시보기 부분. 추후 개발
                    /*
                    2.다시보기일 경우 : 방송 다시보기 액티비티 실행.
                    액티비티에 넘겨줘야 할 데이터 : id(라이브가 아니므로 roomId 로 사용되지는 않음), streamer name/profileImg, roomName, liveStartedAt
                    */
                    else {

                    }

                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.part_each_broadcast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        // 표시해 줄 데이터
        // 라이브의 경우 : streamer name/profileImg, roomName, isLive, numberOfWatchers, thumbnailImage
        // 다시보기일 경우 : streamer name/profileImg, roomName, isLive, liveStartedAt, thumbnailImage

        Log.d(tag_check, "check roomName : " + data.get(position).getRoomName());
        Log.d(tag_check, "check HostName : " + data.get(position).getHostName());
        Log.d(tag_check, "check isLive : " + data.get(position).getIsLive());
        Log.d(tag_check, "check liveStartedAt : " + data.get(position).getLiveStartedAt());
        Log.d(tag_check, "check numberOfWatchers(live only) : " + data.get(position).getNumberOfWatchers());

        // 썸네일 이미지 표시
        String thumbnailImage = data.get(position).getThumbnailImage();
        byte[] thumbnail_decodedString = Base64.decode(thumbnailImage, Base64.DEFAULT);
        Bitmap thumbnail_decodedBitmap = BitmapFactory.decodeByteArray(thumbnail_decodedString, 0, thumbnail_decodedString.length);

        int width = thumbnail_decodedBitmap.getWidth();
        int height = thumbnail_decodedBitmap.getHeight();
        Log.d(tag_check, "thumbnailImage width : " + width);
        Log.d(tag_check, "thumbnailImage height : " + height);

        thumbnail_decodedBitmap = Bitmap.createBitmap(thumbnail_decodedBitmap, 0, 0, thumbnail_decodedBitmap.getWidth(), thumbnail_decodedBitmap.getHeight());
        viewHolder.thumbnail.setImageBitmap(thumbnail_decodedBitmap);

        // 스트리머 프로필 이미지 표시
        String hostProfileImage = data.get(position).getHostProfileImage();

        // 프로필 사진이 있는 경우에만 표시해 주기
        if (hostProfileImage != null) {
            byte[] hostProfile_decodedString = Base64.decode(hostProfileImage, Base64.DEFAULT);
            Bitmap hostProfile_decodedBitmap = BitmapFactory.decodeByteArray(hostProfile_decodedString, 0, hostProfile_decodedString.length);
            viewHolder.hostProfileImg.setImageBitmap(hostProfile_decodedBitmap);
        }

        viewHolder.roomName.setText(data.get(position).getRoomName());
        viewHolder.hostName.setText(data.get(position).getHostName());

        // 실시간, 다시보기에 따라 태그 표시 또는 숨기기
        boolean isLive = data.get(position).getIsLive();
        System.out.println();
        // 라이브일 경우
        if (isLive) {
            // 시청자 수 표시
            int numberOfWatchers = data.get(position).getNumberOfWatchers();
            viewHolder.viewerOrDate.setText(numberOfWatchers + "명 시청중");
        } else {
            viewHolder.isLiveTag.setVisibility(View.INVISIBLE);
            // 방송 시작한 날짜 및 시간 표시
            viewHolder.viewerOrDate.setText(data.get(position).getLiveStartedAt());
        }

        /*
        특정 요소 또는 버튼 클릭 시 이벤트 등록
        int adapterPosition = viewHolder.getAdapterPosition();

        viewHolder.someBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedItemIndex = adapterPosition;
                // do something!
            }
        });
        */

        /*
        특정 아이템 삭제하기
        viewHolder.someDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                deleteItem(position);
            }
        });
        */
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
