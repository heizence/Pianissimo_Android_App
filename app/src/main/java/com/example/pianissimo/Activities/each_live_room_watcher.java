package com.example.pianissimo.Activities;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.SharedPreferences;

import com.example.pianissimo.Adapters.Adapter_each_live_chat_message;
import com.example.pianissimo.R;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pianissimo.Modules.httpRequestAPIs;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class each_live_room_watcher extends AppCompatActivity {
    private String screenName = "[ACTIVITY each_live_room_watcher]";  //  Fragment 일 경우 FRAGMENT 로 할 수도 있음
    private String tag_check = screenName + "[CHECK]";  // 특정 값을 확인할 때 사용
    private String tag_execute = screenName + "[EXECUTE]";  // 매서드나 다른 실행 가능한 코드를 실행할 때 사용
    private String tag_event = screenName + "[EVENT]";  // 특정 이벤트 발생을 확인할 때 사용
    private String tag_socket_event = screenName + "[SOCKET EVENT]";

    private Context context = this;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;
    private DisplayMetrics displayMetrics;

    private Socket mSocket;
    private Handler mHandler;

    private View rootLayout;
    private androidx.constraintlayout.widget.ConstraintLayout surfaceViewWrapper; // localSurfaceView 의 부모 layout
    private SurfaceViewRenderer localSurfaceView;    // local stream 표시할 view
    private ImageView switchToFullScreenBtn;    // 전체화면 전환 버튼
    private int screenHeight;
    private int screenWidth;

    private ImageView streamerProfileImgView;
    private TextView roomTitleView;
    private TextView streamerNameTextView;
    private TextView numberOfWatchersTextView;
    private TextView timeLapsedTextView;

    private RecyclerView chatMessageRecyclerView;
    private Adapter_each_live_chat_message chatMessageAdapter;

    private LinearLayout chatRootLayout;    // when converting to full screen
    private EditText chatInput;
    private ImageView sendChatBtn;  // do not delete!

    private VideoRenderer videoRenderer;
    private VideoTrack remoteVideoTrack;
    private AudioTrack remoteAudioTrack;
    private float videoRatioFromStreamer;

    private PeerConnectionList peerConnectionList;
    private EglBase rootEglBase;
    private PeerConnectionFactory factory;

    private String userName;
    private String roomName;
    private String roomId;
    private LocalDateTime liveStartedDateAndTime;
    private Timer liveRoomTimer;
    private int numberOfWatchers;

    private String chatMessage = "";    // has to be an empty string for check. do not change it to null or undefined!

    protected void onCreate(Bundle savedInstanceState) {
        Log.d(tag_execute, "LifeCycle onCreate");
        super.onCreate(savedInstanceState);

        sharedPref = getSharedPreferences(getString(R.string.sharedPreferenceMain), MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();
        mHandler = new Handler();

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;

        normalScreenInit(); // 방송방 일반 화면 설정
    }

    @Override
    protected void onDestroy() {
        Log.d(tag_execute, "onDestroy");
        super.onDestroy();

        if (remoteVideoTrack != null) {
            remoteVideoTrack.setEnabled(false);
            remoteVideoTrack.removeRenderer(videoRenderer);
        }
        if (remoteAudioTrack != null) remoteAudioTrack.setEnabled(false);
        if (localSurfaceView != null) localSurfaceView.release();

        if (liveRoomTimer != null) {
            liveRoomTimer.cancel();
        }
    }

    // Executed when screen orientation or screen size changes
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(tag_execute, "onConfigurationChanged");

        // Check for specific configuration changes
        // ORIENTATION_LANDSCAPE(가로 전환) : 2
        // ORIENTATION_PORTRAIT(세로 전환) : 1

        // When screen orientation has changed to ORIENTATION_LANDSCAPE(Horizontal)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(tag_check, "Orientation has changed to LANDSCAPE(horizontal)");
            convertToHorizontalFullScreen();
        }
        // When screen orientation has changed to ORIENTATION_PORTRAIT(Vertical)
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(tag_check, "Orientation has changed to PORTRAIT(vertical)");
            convertToVerticalFullScreen();
        } else {
            Log.d(tag_check, "case 3");
            // Handle other configuration changes, if needed
        }
    }

    // 방송방 첫 입장 시 레이아웃 및 변수 설정. 또는 전체화면(가로, 세로 모두 포함)에서 일반 화면으로 다시 전환할 때 실행
    public void normalScreenInit() {
        Log.d(tag_execute, "normalScreenInit");

        // 전체화면에서 다시 일반 화면으로 전환할 때 실행. 처음 방송방 입장할 때에는 실행하지 않음.
        // 추후 remoteVideoTrack, localSurfaceView, rootEglBase 다시 설정해 주도록 되어 있음.
        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeRenderer(videoRenderer);
        }
        if (localSurfaceView != null) {
            localSurfaceView.release();
        }
        if (rootEglBase != null) {
            rootEglBase.release();
        }

        setContentView(R.layout.screen_each_live_room_watcher);
        surfaceViewWrapper = findViewById(R.id.screen_each_live_room_watcher_surfaceViewWrapper);
        rootLayout = findViewById(R.id.screen_each_live_room_watcher_layout);
        localSurfaceView = findViewById(R.id.screen_each_live_room_watcher_surfaceView);
        switchToFullScreenBtn = findViewById(R.id.screen_each_live_room_watcher_fullscreen_btn);

        // 전체 화면으로 전환 기능 추가
        switchToFullScreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(tag_execute, "switchToFullScreenBtn onclick");
                convertToVerticalFullScreen();
            }
        });

        // 영상 화면 영역 터치 시 키패드 내리기
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                chatInput.clearFocus();
            }
            return false;
        });

        streamerProfileImgView = findViewById(R.id.screen_each_live_room_watcher_HostProfileImg);
        roomTitleView = findViewById(R.id.screen_each_live_room_watcher_eachBroadCastTitle);
        streamerNameTextView = findViewById(R.id.screen_each_live_room_watcher_HostName);
        numberOfWatchersTextView = findViewById(R.id.screen_each_live_room_watcher_numberOfWatchersOrDate);

        // 시청자 수 랜더링. 전체화면에서 다시 일반 화면으로 전환할 때 실행. 처음 방송방 입장할 때에는 해당없음. socket 이벤트를 통해 랜더링함.
        if (numberOfWatchers > 0) {
            numberOfWatchersTextView.setText(numberOfWatchers + "명 시청중");
        }
        timeLapsedTextView = findViewById(R.id.screen_each_live_room_watcher_timeLapsed);

        // 실시간 채팅 Recyclerview 설정
        chatMessageRecyclerView = findViewById(R.id.chatMessageRecyclerView_watcher);
        chatMessageRecyclerView.setHasFixedSize(true);
        chatMessageRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        // 실시간 채팅 메시지 adapter 선언. 처음 방송방 입장 시에는 adapter 가 없으므로 새로 선언해 줌. 전체화면에서 다시 일반 화면으로 전환할 때는 해당없음.
        if (chatMessageAdapter == null) {
            chatMessageAdapter = new Adapter_each_live_chat_message(context);
        }
        chatMessageRecyclerView.setAdapter(chatMessageAdapter);
        chatMessageRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);

        chatInput = findViewById(R.id.screen_each_live_room_watcher_chatInput);
        sendChatBtn = findViewById(R.id.screen_each_live_room_watcher_sendChatBtn);

        // 실시간 채팅 영역 기기 화면 높이의 1/4 로 설정.
        chatMessageRecyclerView.getLayoutParams().height = screenHeight / 4;
        chatMessageRecyclerView.requestLayout();

        chatInput.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                chatInput.getWindowVisibleDisplayFrame(r);
                int chatInputRootViewHeight = chatInput.getRootView().getHeight();
                int keypadHeight = chatInputRootViewHeight - r.bottom;
                if (keypadHeight > chatInputRootViewHeight * 0.15) {
                    // 가장 아래 채팅 내용으로 scroll
                    chatMessageRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
                }
            }
        });
        chatInput.addTextChangedListener(textWatcher);

        userName = sharedPref.getString(getString(R.string.store_AU_Name), "");

        Intent intent = getIntent();
        roomId = intent.getStringExtra(getString(R.string.roomId));
        roomName = intent.getStringExtra(getString(R.string.roomName));
        String hostName = intent.getStringExtra(getString(R.string.hostName));
        String hostProfileImg = intent.getStringExtra(getString(R.string.hostProfileImg));
        String liveStartedAt = intent.getStringExtra(getString(R.string.liveStartedAt));
        Log.d(tag_check, "liveStartedAt : " + liveStartedAt);
        streamerNameTextView.setText(hostName);

        // 스트리머 프로필 사진 랜더링
        if (hostProfileImg != null) {
            byte[] decodedString = Base64.decode(hostProfileImg, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            streamerProfileImgView.setImageBitmap(decodedBitmap);
        }

        roomTitleView.setText(roomName);

        // 방송 시작 후 경과 시간 표시(n분(시간) 전 시작)
        // 처음 방송방 입장 시에는 경과 시간값이 없으므로 설정해 줌. 전체화면에서 다시 일반 화면으로 전환할 때는 해당없음.
        if (liveStartedDateAndTime == null) {
            liveStartedDateAndTime = LocalDateTime.parse(liveStartedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        calculateTimeLapsed();
        runLiveRoomTimer(); // 경과 시간 계산하는 타이머 실행

        initializeSurfaceViews();
        initializePeerConnectionFactory();

        // 처음 방송방 입장 시에는 소켓 설정이 없으므로 소켓 설정해 줌.
        // socket 설정 내부에 webRTC 연결 과정이 포함되어 있고 videoRenderer, remoteVideoTrack 설정이 포함되어 있음.
        if (mSocket == null) {
            setSocket();
        }
        // 전체화면에서 다시 일반 화면으로 전환할 때는 videoRenderer, remoteVideoTrack 다시 설정해 주기. 설정이 빠지만 화면이 보이지 않으므로 반드시 해 줄 것!
        else {
            Log.d(tag_check, "socket already set");
            Log.d(tag_check, "check videoRenderer : " + videoRenderer);
            Log.d(tag_check, "check remoteVideoTrack : " + remoteVideoTrack);
            videoRenderer = new VideoRenderer(localSurfaceView);
            remoteVideoTrack.addRenderer(videoRenderer);
        }
    }

    // 세로 전체 화면으로 전환
    public void convertToVerticalFullScreen() {
        Log.d(tag_execute, "convertToVerticalFullScreen");

        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeRenderer(videoRenderer);
            //remoteVideoTrack.dispose();
        }
        if (localSurfaceView != null) {
            localSurfaceView.release();
        }
        if (rootEglBase != null) {
            rootEglBase.release();
        }

        setContentView(R.layout.screen_each_live_room_watcher_fullscreen_vertical); // set a vertical fullscreen layout

        rootLayout = null;
        localSurfaceView = findViewById(R.id.screen_each_live_room_watcher_fullscreen_vertical_surfaceView);

        // 화면 터치 시 키패드 off 처리
        localSurfaceView.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                chatInput.clearFocus();
            }
            return false;
        });

        initializeSurfaceViews();

        // set a new videoRenderer with the new localSurfaceView
        videoRenderer = new VideoRenderer(localSurfaceView);
        remoteVideoTrack.addRenderer(videoRenderer);

        chatRootLayout = findViewById(R.id.screen_each_live_room_watcher_fullscreen_vertical_chatRootLayout);

        // 실시간 채팅 recyclerView 재설정
        chatMessageRecyclerView = findViewById(R.id.chatMessageRecyclerView_watcher_fullscreen_vertical);
        chatMessageRecyclerView.setHasFixedSize(true);
        chatMessageRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        chatMessageRecyclerView.setAdapter(chatMessageAdapter);
        chatMessageRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);

        // 실시간 채팅 영역 전체 화면에 맞게 재설정
        chatRootLayout.getLayoutParams().height = screenHeight / 2; // 기기 화면 높이의 1/2 로 설정.
        chatRootLayout.requestLayout();
        chatInput = findViewById(R.id.screen_each_live_room_watcher_fullscreen_vertical_chatInput);
        chatInput.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                chatInput.getWindowVisibleDisplayFrame(r);
                int chatInputRootViewHeight = chatInput.getRootView().getHeight();
                int keypadHeight = chatInputRootViewHeight - r.bottom;
                if (keypadHeight > chatInputRootViewHeight * 0.15) {
                    // 키보드 올라오면 채팅 영역 크기 기기 화면 높이의 1/3 로 줄이기
                    chatRootLayout.getLayoutParams().height = screenHeight / 3;
                    chatRootLayout.requestLayout();

                    // 가장 아래 채팅 내용으로 scroll
                    chatMessageRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);

                } else {
                    // 키보드 내려가면 원래 크기로 설정해 주기.
                    chatRootLayout.getLayoutParams().height = screenHeight / 2;
                    chatRootLayout.requestLayout();
                }
            }
        });
        chatInput.addTextChangedListener(textWatcher);

        // 전체화면 종료 버튼 설정
        ImageView exitFullScreenBtn = findViewById(R.id.screen_each_live_room_watcher_fullscreen_vertical_exitFullScreenBtn);
        exitFullScreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(tag_execute, "exitFullscreenBtn clicked");
                normalScreenInit(); // 다시 일반 화면으로 전환하기
            }
        });
    }

    // 가로 전체 화면으로 전환
    public void convertToHorizontalFullScreen() {
        Log.d(tag_execute, "convertToHorizontalFullScreen");

        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeRenderer(videoRenderer);
            //remoteVideoTrack.dispose();
        }
        if (localSurfaceView != null) {
            localSurfaceView.release();
        }
        if (rootEglBase != null) {
            rootEglBase.release();
        }

        setContentView(R.layout.screen_each_live_room_watcher_fullscreen_horizontal); // set a horizontal fullscreen layout

        rootLayout = null;
        localSurfaceView = findViewById(R.id.screen_each_live_room_watcher_fullscreen_horizontal_land_surfaceView);

        // 화면 터치 시 키패드 off 처리
        localSurfaceView.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                chatInput.clearFocus();
            }
            return false;
        });

        initializeSurfaceViews();

        // set a new videoRenderer with the new localSurfaceView
        videoRenderer = new VideoRenderer(localSurfaceView);
        remoteVideoTrack.addRenderer(videoRenderer);

        chatRootLayout = findViewById(R.id.screen_each_live_room_watcher_fullscreen_horizontal_land_chatRootLayout);

        // 실시간 채팅 recyclerView 재설정
        chatMessageRecyclerView = findViewById(R.id.chatMessageRecyclerView_watcher_fullscreen_horizontal_land);
        chatMessageRecyclerView.setHasFixedSize(true);
        chatMessageRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        chatMessageRecyclerView.setAdapter(chatMessageAdapter);
        chatMessageRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);

        // 실시간 채팅 영역 전체 화면에 맞게 재설정
        chatRootLayout.getLayoutParams().height = screenHeight / 2; // 기기 화면 높이의 1/2 로 설정.
        chatRootLayout.requestLayout();
        chatInput = findViewById(R.id.screen_each_live_room_watcher_fullscreen_horizontal_land_chatInput);
        chatInput.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                chatInput.getWindowVisibleDisplayFrame(r);
                int chatInputRootViewHeight = chatInput.getRootView().getHeight();
                int keypadHeight = chatInputRootViewHeight - r.bottom;
                if (keypadHeight > chatInputRootViewHeight * 0.15) {
                    // 키보드 올라오면 채팅 영역 크기 기기 화면 높이의 1/3 로 줄이기
                    chatRootLayout.getLayoutParams().height = screenHeight / 3;
                    chatRootLayout.requestLayout();

                    // 가장 아래 채팅 내용으로 scroll
                    chatMessageRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);

                } else {
                    // 키보드 내려가면 원래 크기로 설정해 주기.
                    chatRootLayout.getLayoutParams().height = screenHeight / 2;
                    chatRootLayout.requestLayout();
                }
            }
        });
        chatInput.addTextChangedListener(textWatcher);

        // 전체화면 종료 버튼 설정
        ImageView exitFullScreenBtn = findViewById(R.id.screen_each_live_room_watcher_fullscreen_horizontal_land_exitFullScreenBtn);
        exitFullScreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(tag_execute, "exitFullscreenBtn clicked");
                normalScreenInit(); // 다시 일반 화면으로 전환하기
            }
        });
    }

    // 영상 화면 표시할 surfaceView 설정
    public void initializeSurfaceViews() {
        Log.d(tag_execute, "initializeSurfaceViews");
        rootEglBase = EglBase.create();
        localSurfaceView.init(rootEglBase.getEglBaseContext(), null);
        localSurfaceView.setEnableHardwareScaler(true);
        localSurfaceView.setMirror(true);
    }

    // PeerConnectionFactory 초기 설정. 영상 불러오기 전에, PeerConnection 생성하기 전에 먼저 실행해야 됨.(스트리머, 시청자 모두 해당)
    public void initializePeerConnectionFactory() {
        Log.d(tag_execute, "initializePeerConnectionFactory");

        if (factory == null) {
            PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
            factory = new PeerConnectionFactory(null);
            factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());

            peerConnectionList = new PeerConnectionList();   // 여러 개의 peerConnection 객체를 관리할 저장 공간 생성.
        }
        Log.d(tag_check, "peerConnectionFactory : " + factory);
    }

    // PeerConnection 객체 생성
    public void createMyPeerConnection(String peerConnectionID, String peerSocketId) {
        Log.d(tag_execute, "createMyPeerConnection. peerConnectionID : " + peerConnectionID);
        //Log.d(tag_check, "check PeerConnection : " + peerConnection);
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        String URL = "stun:stun.l.google.com:19305";
        iceServers.add(new PeerConnection.IceServer(URL));
        Log.d(tag_check, "iceServers : " + iceServers);

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        MediaConstraints pcConstraints = new MediaConstraints();

        Log.d(tag_check, "rtcConfig : " + rtcConfig);
        Log.d(tag_check, "pcConstraints : " + pcConstraints);

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(tag_event, "peerConnection.onIceCandidate. iceCandidate : " + iceCandidate);
                JSONObject iceCandidateObj = new JSONObject();

                try {
                    iceCandidateObj.put(getString(R.string.type), getString(R.string.candidate));
                    iceCandidateObj.put(getString(R.string.peerConnectionID), peerConnectionID);
                    iceCandidateObj.put(getString(R.string.to), peerSocketId);
                    iceCandidateObj.put(getString(R.string.id), iceCandidate.sdpMid);
                    iceCandidateObj.put(getString(R.string.label), iceCandidate.sdpMLineIndex);
                    iceCandidateObj.put(getString(R.string.sdp), iceCandidate.sdp);

                    mSocket.emit(getString(R.string.iceCandidate), iceCandidateObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(tag_event, "peerConnection.onRenegotiationNeeded");
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(tag_event, "peerConnection.onSignalingChange. signalingState : " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(tag_event, "peerConnection.onIceConnectionChange. iceConnectionState : " + iceConnectionState);
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(tag_event, "peerConnection.onIceConnectionReceivingChange. boolean : " + b);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(tag_event, "peerConnection.onIceGatheringChange. iceGatheringState : " + iceGatheringState);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(tag_event, "peerConnection.onIceCandidatesRemoved. iceCandidates : " + iceCandidates);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(tag_event, "peerConnection.onAddStream. mediaStream : " + mediaStream);
                remoteVideoTrack = mediaStream.videoTracks.get(0);
                // Disable for silence. Remove caption when testing
                //remoteAudioTrack = mediaStream.audioTracks.get(0);
                //remoteAudioTrack.setEnabled(true);
                remoteVideoTrack.setEnabled(true);
                videoRenderer = new VideoRenderer(localSurfaceView);
                remoteVideoTrack.addRenderer(videoRenderer);
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(tag_event, "peerConnection.onRemoveStream. mediaStream : " + mediaStream);
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                // Disable for silence. Remove caption when testing
                //AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                //remoteAudioTrack.setEnabled(false);
                remoteVideoTrack.setEnabled(false);
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(tag_event, "peerConnection.onDataChannel. dataChannel : " + dataChannel);
            }
        };

        PeerConnection peerConnection = factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
        peerConnectionList.add(peerConnectionID, peerConnection);
        Log.d(tag_check, "createPeerConnection finished!");
    }

    // 여러 개의 PeerConnection 을 저장하는 리스트 생성 class
    public class PeerConnectionList {
        private Map<String, PeerConnection> list;

        // 리스트 생성
        public PeerConnectionList() {
            list = new HashMap<>();
        }

        // 새로운 시청자가 접속했을 때 peerConnection 생성하여 추가. key 값은 별도의 peerConnectionID 로 설정.
        public void add(String key, PeerConnection peerConnection) {
            // Add the PeerConnection to the container
            list.put(key, peerConnection);
        }

        // 리스트에 있는 peerConnection 불러오기
        public PeerConnection get(String key) {
            return list.get(key);
        }

        // 리스트에 있는 peerConnection 삭제하기
        public void remove(String key) {
            PeerConnection peerConnection = list.remove(key);
            if (peerConnection != null) {
                // Clean up and close the PeerConnection if needed
                peerConnection.close();
            }
        }
    }

    // SdpObserver 에서 꼭 필요한 기능만 남긴 새로운 SdpObserver
    public class SimpleSdpObserver implements SdpObserver {

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(String s) {
        }

        @Override
        public void onSetFailure(String s) {
        }

    }

    // 방송 시작 후 경과한 시간 갱신해 주는 타이머
    public void runLiveRoomTimer() {
        // Set Timer
        if (liveRoomTimer == null) {
            liveRoomTimer = new Timer();
        }

        TimerTask timerTask = new TimerTask() {
            public void run() {
                calculateTimeLapsed();
            }
        };

        // Start Timer
        liveRoomTimer.schedule(timerTask, 0, 1000 * 60);    // 1분 단위로 갱신
    }

    // 방송 시작 후 경과한 시간 계산하여 표시
    public void calculateTimeLapsed() {
        Log.d(tag_execute, "calculateTimeLapsed");
        LocalDateTime currentDateTime = LocalDateTime.now();    // android emulator 에서의 시간은 실제 시간과 다를 수 있음. 테스트 시 확인하기
        Log.d(tag_check, "currentDateTime : " + currentDateTime);
        Duration duration = Duration.between(liveStartedDateAndTime, currentDateTime);

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;  // only for check if the timer is running normally.

        Log.d(tag_check, "time lapsed : " + hours + " hours, " + minutes + " minutes " + seconds + " seconds");

        // 방송 시작 후 경과한 시간 계산해서 표시
        String timeLapsedText = "시작 : ";
        if (hours > 0) {
            timeLapsedText += hours + "시간 ";
            if (minutes > 0) timeLapsedText += minutes + "분 ";
        } else timeLapsedText += minutes + "분 ";
        timeLapsedText += "전";

        String finalTimeLapsedText = timeLapsedText;    // do not delete!
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                timeLapsedTextView.setText(finalTimeLapsedText);
            }
        });
    }

    // 소켓 초기 설정
    public void setSocket() {
        try {
            // socket 생성 시 적용해 줄 option
            IO.Options mOptions = new IO.Options();

            mOptions.query = "userName=" + userName;
            mOptions.query += "&roomName=" + roomName;
            mOptions.query += "&roomId=" + roomId;
            mOptions.query += "&isStreamer=false";
            Log.d(tag_check, "setSocket mOptions query : " + mOptions.query);

            String signalingServer_URL = httpRequestAPIs.webRTC_Signaling_Server_URL;
            Log.d(tag_check, "check signalingServer_URL : " + signalingServer_URL);

            mSocket = IO.socket(signalingServer_URL, mOptions);

            /****************************** emit 이벤트 리스너 등록 *********************************/

            // 연결 시(스트리머, 시청자 모두 해당)
            mSocket.on(getString(R.string.connect), args -> {
                Log.d(tag_socket_event, "connect");
                Thread thread = new Thread(() -> {
                    try {
                        mSocket.emit(getString(R.string.ready));
                        Log.d(tag_execute, "watcher ready emitted");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            });

            // 새로운 시청자가 방송 입장
            mSocket.on(getString(R.string.watcher_join), args -> {
                Log.d(tag_socket_event, "watcher join");
                Thread thread = new Thread(() -> {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject data = (JSONObject) args[0];
                                String streamerSocketId = data.getString(getString(R.string.streamerSocketId));
                                String watcherName = data.getString(getString(R.string.watcherName));
                                String watcherSocketId = data.getString(getString(R.string.watcherSocketId));
                                numberOfWatchers = data.getInt(getString(R.string.numberOfWatchers));
                                String peerConnectionID = data.getString(getString(R.string.peerConnectionID));

                                Log.d(tag_check, "check JSONObject data : " + data);
                                Log.d(tag_check, "streamerSocketId : " + streamerSocketId);
                                Log.d(tag_check, "watcherName : " + watcherName);
                                Log.d(tag_check, "watcherSocketId : " + watcherSocketId);
                                Log.d(tag_check, "numberOfWatchers : " + numberOfWatchers);

                                Log.d(tag_check, "mySocketID : " + mSocket.id());
                                Boolean check1 = mSocket.id().equals(watcherSocketId);
                                Boolean check2 = mSocket.id() == watcherSocketId;
                                Log.d(tag_check, "compare ids check 1 : " + check1);
                                Log.d(tag_check, "compare ids check 2 : " + check2);

                                Log.d(tag_check, "client is 신규 시청자");
                                if (mSocket.id().equals(watcherSocketId)) {
                                    Log.d(tag_check, "내가 채팅방에 입장");
                                }
                                numberOfWatchersTextView.setText(numberOfWatchers + "명 시청중");
                                // DB 에 저장된 방송 시청자 수 업데이트
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                });
                thread.start();
            });

            // 시청자 퇴장
            mSocket.on(getString(R.string.watcher_left), args -> {
                Log.d(tag_socket_event, "watcher left");
                Thread thread = new Thread(() -> {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject data = (JSONObject) args[0];
                                String peerConnectionID = data.getString(getString(R.string.peerConnectionID));
                                String watcherName = data.getString(getString(R.string.watcherName));
                                numberOfWatchers = data.getInt(getString(R.string.numberOfWatchers));

                                Log.d(tag_check, "check JSONObject data : " + data);
                                Log.d(tag_check, "watcherName : " + watcherName);
                                Log.d(tag_check, "numberOfWatchers : " + numberOfWatchers);

                                peerConnectionList.remove(peerConnectionID);  // 해당 시청자와 연결되어 있었던 peerConnection 제거
                                numberOfWatchersTextView.setText(numberOfWatchers + "명 시청중");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });
                thread.start();
            });

            // 채팅 수신
            mSocket.on(getString(R.string.chat_message), args -> {
                Log.d(tag_socket_event, "chat message");
                Thread thread = new Thread(() -> {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //System.out.println("## get args : " + args);
                                JSONObject data = (JSONObject) args[0];
                                Log.d(tag_check, "check JSONObject data : " + data);

                                String chatMessageContents = data.getString(getString(R.string.chatMessageContents));
                                String senderProfileImg = data.getString(getString(R.string.senderProfileImg));
                                String senderName = data.getString(getString(R.string.senderName));

                                // chat message 추가
                                chatMessageAdapter.add(senderName, senderProfileImg, chatMessageContents);
                                chatMessageRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });
                thread.start();
            });

            // 시청자가 offer 수신
            mSocket.on(getString(R.string.offer), args -> {
                Log.d(tag_socket_event, "offer");
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //System.out.println("## get args : " + args);
                            JSONObject data = (JSONObject) args[0];
                            Log.d(tag_check, "check JSONObject data : " + data);

                            String streamerSocketId = data.getString(getString(R.string.from)); // offer 보낸 사람(스트리머) socket ID
                            String watcherSocketId = data.getString(getString(R.string.to)); // offer 받는 사람(시청자) socket ID
                            String peerConnectionID = data.getString(getString(R.string.peerConnectionID));
                            String videoRatio = data.getString(getString(R.string.videoRatio));
                            videoRatioFromStreamer = Float.valueOf(videoRatio);

                            Log.d(tag_check, "videoRatio float : " + videoRatioFromStreamer);

                            createMyPeerConnection(peerConnectionID, streamerSocketId);
                            PeerConnection peerConnection = peerConnectionList.get(peerConnectionID);

                            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                                @Override
                                public void onSetSuccess() {
                                    Log.d(tag_check, "offer setRemoteDescription onSetSuccess");
                                }
                            }, new SessionDescription(OFFER, data.getString(getString(R.string.sdp))));

                            peerConnection.createAnswer(new SimpleSdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription sessionDescription) {
                                    peerConnection.setLocalDescription(new SimpleSdpObserver() {
                                        @Override
                                        public void onSetSuccess() {
                                            Log.d(tag_check, "answer setLocalDescription onSetSuccess");
                                            JSONObject answer = new JSONObject();
                                            try {
                                                answer.put(getString(R.string.type), getString(R.string.answer));
                                                answer.put(getString(R.string.from), watcherSocketId);
                                                answer.put(getString(R.string.to), streamerSocketId);
                                                answer.put(getString(R.string.sdp), sessionDescription.description);
                                                mSocket.emit(getString(R.string.answer), answer);
                                                Log.d(tag_execute, "answer emitted");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, sessionDescription);
                                }
                            }, new MediaConstraints());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            });

            // 스트리머가 answer 수신
            mSocket.on(getString(R.string.answer), args -> {
                Log.d(tag_socket_event, "answer");
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            Log.d(tag_check, "check JSONObject data : " + data);
                            String peerConnectionID = data.getString(getString(R.string.peerConnectionID));

                            PeerConnection peerConnection = peerConnectionList.get(peerConnectionID);
                            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                                @Override
                                public void onSetSuccess() {
                                    Log.d(tag_check, "answer setRemoteDescription onSetSuccess");
                                }
                            }, new SessionDescription(ANSWER, data.getString(getString(R.string.sdp))));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            });

            // 스트리머, 시청자가 iceCandidate 수신
            mSocket.on(getString(R.string.iceCandidate), args -> {
                Log.d(tag_socket_event, "iceCandidate");
                try {
                    JSONObject data = (JSONObject) args[0];
                    String peerConnectionID = data.getString(getString(R.string.peerConnectionID));
                    String id = data.getString(getString(R.string.id));
                    int label = data.getInt(getString(R.string.label));
                    String sdp = data.getString(getString(R.string.sdp));

                    PeerConnection peerConnection = peerConnectionList.get(peerConnectionID);

                    if (peerConnection != null) {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                IceCandidate candidate = new IceCandidate(id, label, sdp);
                                Log.d(tag_check, "check candidate : " + candidate);
                                peerConnection.addIceCandidate(candidate);
                                Log.d(tag_execute, "peerConnection.addIceCandidate done.");
                            }
                        });
                        thread.start();
                    } else {
                        Log.d(tag_check, "peerConnection is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // 스트리머가 방송 종료
            mSocket.on(getString(R.string.end_live), args -> {
                Log.d(tag_socket_event, "end live");
                exit();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "방송이 종료되었습니다!", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            // 소켓 연결하기
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // 채팅 메시지 보내기
    public void sendChatMessage(View view) {
        Log.d(tag_execute, "sendChatMessage");
        if (chatMessage.length() != 0) {
            Thread thread = new Thread(() -> {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 보내줘야 할 데이터 : 채팅 전송자 이름, 프로필 이미지 string, 채팅 내용)
                            JSONObject chatMessageObj = new JSONObject();
                            String senderProfileImg = sharedPref.getString(getString(R.string.store_AU_ProfileImg), "");
                            String senderUserId = sharedPref.getString(getString(R.string.store_AU_Id), "");  // 사용자 앱 회원 고유 id

                            chatMessageObj.put(getString(R.string.senderUserId), senderUserId); // for saving chat history
                            chatMessageObj.put(getString(R.string.senderName), userName);
                            chatMessageObj.put(getString(R.string.senderProfileImg), senderProfileImg);
                            chatMessageObj.put(getString(R.string.chatMessageContents), chatMessage);

                            mSocket.emit(getString(R.string.chat_message), chatMessageObj);
                            chatMessage = "";
                            chatInput.setText("");
                            chatInput.clearFocus();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            });
            thread.start();
        }
    }

    // 방송방 나가기
    public void exit() {
        Log.d(tag_execute, "exit.");
        Thread thread = new Thread(() -> {
            try {
                mSocket.emit(getString(R.string.watcher_left));
                Log.d(tag_execute, "watcher left emitted");
                mSocket.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

        setResult(Activity.RESULT_OK);  // fragment_broadcast 에서 받을 result 설정
        finish();
    }

    @Override
    public void onBackPressed() {
        Log.d(tag_execute, "onBackPressed");
        exit();
    }

    public final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            chatMessage = s.toString();
        }
    };


}
