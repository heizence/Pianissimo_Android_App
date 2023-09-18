package com.example.pianissimo.Activities;

import android.app.Activity;
import android.content.SharedPreferences;

import com.example.pianissimo.Adapters.Adapter_each_live_chat_message;
import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.Modules.dialog_confirm_or_cancel;
import com.example.pianissimo.R;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pianissimo.Modules.httpRequestAPIs;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class each_live_room_streamer extends AppCompatActivity {
    private String screenName = "[ACTIVITY each_live_room_streamer]";  //  Fragment 일 경우 FRAGMENT 로 할 수도 있음
    private String tag_check = screenName + "[CHECK]";  // 특정 값을 확인할 때 사용
    private String tag_execute = screenName + "[EXECUTE]";  // 매서드나 다른 실행 가능한 코드를 실행할 때 사용
    private String tag_event = screenName + "[EVENT]";  // 특정 이벤트 발생을 확인할 때 사용
    private String tag_socket_event = screenName + "[SOCKET EVENT]";

    private Context context = this;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;

    private Socket mSocket;
    private Handler mHandler;

    private org.webrtc.SurfaceViewRenderer localSurfaceView;    // local stream 표시할 view

    private RecyclerView chatMessageRecyclerView;
    private Adapter_each_live_chat_message chatMessageAdapter;

    private LinearLayout chatRootLayout;

    private EditText chatInput;

    private MediaConstraints audioConstraints;
    private VideoCapturer videoCapturer;
    private VideoTrack remoteVideoTrack;
    private AudioTrack remoteAudioTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;

    private PeerConnectionList peerConnectionList;
    private EglBase rootEglBase;
    private PeerConnectionFactory factory;
    private VideoTrack videoTrackFromCamera;

    private int videoResolutionWidth;
    private  int videoResolutionHeight;
    private float videoRatio;   // Calculated by screenHeight / screenWidth

    private String thumbnailImage;
    private String userName;
    private String roomName;
    private String roomId;
    private LocalDateTime liveStartedDateAndTime;

    private int localVar_NumberOfWatchers;
    private boolean isUsingFrontCamera;
    private String chatMessage = "";    // has to be an empty string for check. do not change it to null or undefined!

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_each_live_room_streamer);

        sharedPref = getSharedPreferences(getString(R.string.sharedPreferenceMain), MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();
        mHandler = new Handler();

        localSurfaceView = findViewById(R.id.screen_each_live_room_streamer_surfaceView);
        // 영상 화면 영역 터치 시 키패드 내리기
        localSurfaceView.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                chatInput.clearFocus();
            }
            return false;
        });

        chatRootLayout = findViewById(R.id.screen_each_live_room_streamer_chatRootLayout);

        // 실시간 채팅 recyclerView 설정
        chatMessageRecyclerView = findViewById(R.id.chatMessageRecyclerView_streamer);
        chatMessageRecyclerView.setHasFixedSize(true);
        chatMessageRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        chatMessageAdapter = new Adapter_each_live_chat_message(context);
        chatMessageRecyclerView.setAdapter(chatMessageAdapter);
        chatInput = findViewById(R.id.screen_each_live_room_streamer_chatInput);

        // 실시간 채팅 영역 기기 화면 높이의 1/2 로 설정.
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        chatRootLayout.getLayoutParams().height = screenHeight / 2;
        chatRootLayout.requestLayout();
        //

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

        Intent intent = getIntent();
        userName = sharedPref.getString(getString(R.string.store_AU_Name), "");
        roomName = intent.getStringExtra(getString(R.string.roomName));
        isUsingFrontCamera = intent.getBooleanExtra(getString(R.string.isUsingFrontCamera), true);
        thumbnailImage = intent.getStringExtra(getString(R.string.thumbnailImage));
        roomId = intent.getStringExtra(getString(R.string.roomId));
        //String liveStartedAt = intent.getStringExtra(getString(R.string.liveStartedAt));

        Log.d(tag_check, "roomName : " + roomName);
        Log.d(tag_check, "isUsingFrontCamera : " + isUsingFrontCamera);
        Log.d(tag_check, "thumbnailImage : " + thumbnailImage);
        Log.d(tag_check, "roomId : " + roomId);

        initializeSurfaceViews();
        initializePeerConnectionFactory();

        // 방송 시작 후 경과 시간 표시(n분(시간) 전 시작)
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);

        // Get the current date and time
        Date currentDate = new Date();
        String liveStartedAt = dateTimeFormat.format(currentDate);
        Log.d(tag_check, "liveStartedAt : " + liveStartedAt);
        liveStartedDateAndTime = LocalDateTime.parse(liveStartedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        peerConnectionList = new PeerConnectionList();   // 여러 개의 peerConnection 객체를 관리할 저장 공간 생성.
        setSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (videoCapturer != null) {
                videoCapturer.stopCapture();
                videoCapturer = null;
            }
            localSurfaceView.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 라이브 방송 데이터 생성하여 DB 에 저장
    public void createLiveRoomData() {
        Log.d(tag_execute, "createLiveRoomData");
        // 요청 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                Log.d(tag_check, "createLiveRoomData success. data : " + responseObj.data);
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                Log.d(tag_check, "createLiveRoomData failed. statusCode : " + statusCode);
                dialog_confirm.show(context, "에러가 발생하였습니다.");
            }
        }

        String hostId = sharedPref.getString(getString(R.string.store_AU_Id), "");  // 스트리머 앱 회원 Id
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);

        // Get the current date and time
        Date currentDate = new Date();
        String liveStartedDateAndTime = dateTimeFormat.format(currentDate);
        // Display the formatted date and time
        Log.d(tag_check, "current date and time : " + liveStartedDateAndTime);

        httpRequestAPIs.createLiveRoomData(roomId, hostId, roomName, liveStartedDateAndTime, thumbnailImage, 0,
                new Callee_success(), new Callee_failed());
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
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        factory = new PeerConnectionFactory(null);
        factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
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
                    //iceCandidateObj.put(getString(R.string.candidate), iceCandidate.sdp);
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

                // Does not work on android emulators yet.
                if (iceConnectionState.toString().equals("CONNECTED")) {
                    //updateNumberOfWatchers();
                }
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
                remoteAudioTrack = mediaStream.audioTracks.get(0);
                remoteAudioTrack.setEnabled(true);
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addRenderer(new VideoRenderer(localSurfaceView));
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(tag_event, "peerConnection.onRemoveStream. mediaStream : " + mediaStream);
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                remoteAudioTrack.setEnabled(false);
                remoteVideoTrack.setEnabled(false);
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(tag_event, "peerConnection.onDataChannel. dataChannel : " + dataChannel);
            }
        };

        PeerConnection peerConnection = factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
        Log.d(tag_check, "createPeerConnection finished!");

        MediaStream mediaStream = factory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(videoTrackFromCamera);
        //mediaStream.addTrack(localAudioTrack);    // Disable for silence. Remove caption when testing
        peerConnection.addStream(mediaStream);
        Log.d(tag_check, "peerConnection.addStream. mediaStream : " + mediaStream);

        peerConnectionList.add(peerConnectionID, peerConnection);
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

    // 기기 카메라에서 영상 데이터 불러와서 표시해 주기
    public void createVideoTrackFromCameraAndShowIt() {
        Log.d(tag_execute, "createVideoTrackFromCameraAndShowIt");
        audioConstraints = new MediaConstraints();
        Boolean useCamera2 = Camera2Enumerator.isSupported(this);

        Log.d(tag_check, "useCamera2 : " + useCamera2);

        if (useCamera2) {
            Log.d(tag_check, "useCamera2 supported");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Log.d(tag_check, "useCamera2 not supported");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }

        Log.d(tag_check, "videoCapturer : " + videoCapturer);
        VideoSource videoSource = factory.createVideoSource(videoCapturer);
        Log.d(tag_check, "videoSource : " + videoSource);
        //videoSource.adaptOutputFormat(720, 548, 30);   // adjust buffer

        if (videoSource != null) {        // 비디오 해상도, FPS, Track ID
            Log.d(tag_check, "has videoSource");
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            videoResolutionWidth = 500;
            videoResolutionHeight = 500;
            final int FPS = 30;
            final String VIDEO_TRACK_ID = "ARDAMSv0";

            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            videoRatio = screenHeight / screenWidth;

            videoCapturer.startCapture(videoResolutionWidth, videoResolutionHeight, FPS);
            videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
            videoTrackFromCamera.setEnabled(true);
            videoTrackFromCamera.addRenderer(new VideoRenderer(localSurfaceView));
            Log.d(tag_check, "videoTrack : " + videoTrackFromCamera);

            //create an AudioSource instance
            audioSource = factory.createAudioSource(audioConstraints);
            localAudioTrack = factory.createAudioTrack("101", audioSource);

            Log.d(tag_check, "audioSource : " + audioSource);
            Log.d(tag_check, "localAudioTrack : " + localAudioTrack);
            Log.d(tag_check, "video and audio set!");

            mSocket.emit(getString(R.string.ready));
            Log.d(tag_execute, "streamer ready emitted");
        } else {
            Log.d(tag_check, "no videoSource");
        }
    }

    // CameraCapturer 객체 생성
    public VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        Log.d(tag_execute, "createCameraCapturer");
        final String[] deviceNames = enumerator.getDeviceNames();

        VideoCapturer videoCapturer = null;

        Log.d(tag_check, "isUsingFrontCamera : " + isUsingFrontCamera);
        for (String deviceName : deviceNames) {
            // 전면 카메라 이용
            if (isUsingFrontCamera) {
                Log.d(tag_check, "use front camera");
                if (enumerator.isFrontFacing(deviceName)) {
                    Log.d(tag_check, "front camera exist!");
                    videoCapturer = enumerator.createCapturer(deviceName, null);
                    if (videoCapturer != null) {
                        return videoCapturer;
                    }
                }
            }
            // 후면 카메라 이용
            else {
                Log.d(tag_check, "use back camera");
                if (!enumerator.isFrontFacing(deviceName)) {
                    Log.d(tag_check, "back camera exist!");
                    videoCapturer = enumerator.createCapturer(deviceName, null);
                    if (videoCapturer != null) {
                        return videoCapturer;
                    }
                }
            }
        }
        Log.d(tag_check, "camera doesn't exist or other problem occurred!");
        return null;
    }

    // 소켓 초기 설정
    public void setSocket() {
        try {
            // socket 생성 시 적용해 줄 option
            IO.Options mOptions = new IO.Options();

            mOptions.query = "userName=" + userName;
            mOptions.query += "&roomName=" + roomName;
            mOptions.query += "&roomId=" + roomId;
            mOptions.query += "&isStreamer=true";
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
                        createVideoTrackFromCameraAndShowIt();  // 해당 매서드 내에 emit("ready") 가 포함되어 있음.
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            });

            // 스트리머가 방 생성 후 방송 시작. 스트리머만 해당.
            mSocket.on(getString(R.string.streamer_start_live), args -> {
                Log.d(tag_socket_event, "streamer start live");
                Thread thread = new Thread(() -> {
                    try {
                        createLiveRoomData();    // DB 에 방송 정보 저장.
                        // for test
                        mSocket.emit(getString(R.string.create_new_room));    // 방송 시작되면 새로운 방 생성해 주기
                        Log.d(tag_execute, "create new room emitted");
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
                                String peerConnectionID = data.getString(getString(R.string.peerConnectionID));
                                int numberOfWatchers = data.getInt(getString(R.string.numberOfWatchers));
                                localVar_NumberOfWatchers = numberOfWatchers;

                                Log.d(tag_check, "check JSONObject data : " + data);
                                Log.d(tag_check, "streamerSocketId : " + streamerSocketId);
                                Log.d(tag_check, "watcherName : " + watcherName);
                                Log.d(tag_check, "watcherSocketId : " + watcherSocketId);
                                //Log.d(tag_check, "peerConnectionID : " + peerConnectionID);

                                // 스트리머, 신규 사용자일 경우에만 peerConnection 객체 생성해 주기

                                // 스트리머일 경우
                                Log.d(tag_check, "client is 스트리머");
                                // peerConnection offer 생성을 위한 constraints
                                MediaConstraints sdpMediaConstraints = new MediaConstraints();
                                sdpMediaConstraints.mandatory.add(
                                        new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
                                sdpMediaConstraints.mandatory.add(
                                        new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
                                Log.d(tag_check, "sdpMediaConstraints : " + sdpMediaConstraints);
                                createMyPeerConnection(peerConnectionID, watcherSocketId);

                                // peerConnection offer 생성 후 전송
                                PeerConnection peerConnection = peerConnectionList.get(peerConnectionID);
                                Log.d(tag_check, "check peerConnection from list : " + peerConnection);
                                peerConnection.createOffer(new SimpleSdpObserver() {
                                    @Override
                                    public void onCreateSuccess(SessionDescription sessionDescription) {
                                        Log.d(tag_event, "peerConnection offer onCreateSuccess");
                                        peerConnection.setLocalDescription(new SimpleSdpObserver() {
                                            @Override
                                            public void onSetSuccess() {
                                                Log.d(tag_check, "offer setLocalDescription onSetSuccess");
                                                JSONObject offer = new JSONObject();
                                                try {
                                                    offer.put(getString(R.string.type), getString(R.string.offer));
                                                    offer.put(getString(R.string.from), mSocket.id());
                                                    offer.put(getString(R.string.to), watcherSocketId);
                                                    offer.put(getString(R.string.peerConnectionID), peerConnectionID);
                                                    offer.put(getString(R.string.sdp), sessionDescription.description);
                                                    offer.put(getString(R.string.videoRatio), String.valueOf(videoRatio));

                                                    Log.d(tag_check, "check videoRatio : " + videoRatio);
                                                    mSocket.emit(getString(R.string.offer), offer);
                                                    Log.d(tag_execute, "offer event emitted");
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }, sessionDescription);
                                    }
                                }, sdpMediaConstraints);

                                updateNumberOfWatchers();
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

                    try {
                        JSONObject data = (JSONObject) args[0];
                        String peerConnectionID = data.getString(getString(R.string.peerConnectionID));
                        String watcherName = data.getString(getString(R.string.watcherName));
                        int numberOfWatchers = data.getInt(getString(R.string.numberOfWatchers));
                        localVar_NumberOfWatchers = numberOfWatchers;

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Log.d(tag_check, "check JSONObject data : " + data);
                                    Log.d(tag_check, "watcherName : " + watcherName);
                                    peerConnectionList.remove(peerConnectionID);  // 해당 시청자와 연결되어 있었던 peerConnection 제거
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        updateNumberOfWatchers();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                                JSONObject data = (JSONObject) args[0];
                                Log.d(tag_check, "check JSONObject data : " + data);

                                String senderUserId = data.getString(getString(R.string.senderUserId));
                                String senderProfileImg = data.getString(getString(R.string.senderProfileImg));
                                String senderName = data.getString(getString(R.string.senderName));
                                String chatMessageContents = data.getString(getString(R.string.chatMessageContents));

                                // chat message 추가
                                chatMessageAdapter.add(senderName, senderProfileImg, chatMessageContents);
                                chatMessageRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);

                                // 채팅 보낼 때 DB 에 채팅 내용 저장하기
                                saveChatHistory(senderUserId, chatMessageContents);
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
                            JSONObject data = (JSONObject) args[0];
                            Log.d(tag_check, "check JSONObject data : " + data);

                            String streamerSocketId = data.getString(getString(R.string.from)); // offer 보낸 사람(스트리머) socket ID
                            String watcherSocketId = data.getString(getString(R.string.to)); // offer 받는 사람(시청자) socket ID
                            String peerConnectionID = data.getString(getString(R.string.peerConnectionID));

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
                    //String candidateStr = data.getString("candidate");
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
                            String senderUserId = sharedPref.getString(getString(R.string.store_AU_Id), ""); // 사용자 앱 회원 고유 id
                            String senderProfileImg = sharedPref.getString(getString(R.string.store_AU_ProfileImg), "");

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

    // 유저가 전송한 실시간 채팅 내용 DB 에 저장하기. 채팅 이벤트가 발생할 때 마다 실행
    public void saveChatHistory(String senderUserId, String chatMessage) {
        Log.d(tag_execute, "saveChatHistory. senderUserId : " + senderUserId + ", chatMessage : " + chatMessage);
        // 요청 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                Log.d(tag_check, "saveChatHistory success!");
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                Log.d(tag_check, "saveChatHistory failed. statusCode : " + statusCode);
                dialog_confirm.show(context, "에러가 발생하였습니다.");
            }
        }

        LocalDateTime currentDateTime = LocalDateTime.now();
        Duration duration = Duration.between(liveStartedDateAndTime, currentDateTime);
        int timeLapsedMillis = (int) duration.toMillis();  // 방송 시작 후 채팅이 입력된 시점까지 경과된 시간

        httpRequestAPIs.saveChatHistory(roomId, senderUserId, chatMessage, timeLapsedMillis, new Callee_success(), new Callee_failed());
    }

    // 실시간 시청자 수 업데이트
    public void updateNumberOfWatchers() {
        Log.d(tag_execute, "updateNumberOfViewers");
        Log.d(tag_check, "localVar_NumberOfWatchers : " + localVar_NumberOfWatchers);
        // DB 에서 방송 데이터 삭제 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                Log.d(tag_check, "updateNumberOfViewers success!");
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                Log.d(tag_check, "updateNumberOfViewers failed. statusCode : " + statusCode);
            }
        }
        httpRequestAPIs.updateNumberOfWatchers(roomId, localVar_NumberOfWatchers, new Callee_success(), new Callee_failed());
    }

    // 방송 종료 버튼 클릭 시 확인 모달 열기
    public void openExitDialog(View view) {
        Log.d(tag_execute, "openExitDialog");
        // 취소 버튼 클릭 시 실행할 콜백 함수
        class Callee_Cancel extends dialog_confirm_or_cancel.Callee_Cancel {
            public void call() {
                Log.d(tag_execute, "cancel clicked");
                return;
            }
        }
        // 확인 버튼 클릭 시 실행할 콜백 함수
        class Callee_Confirm extends dialog_confirm_or_cancel.Callee_Confirm {
            public void call() {
                Log.d(tag_execute, "confirm clicked");
                endLive();
            }
        }
        dialog_confirm_or_cancel.show(context, "방송을 종료하시겠습니까?", new Callee_Cancel(), new Callee_Confirm());
    }

    // 라이브 방송 종료하기
    public void endLive() {
        Log.d(tag_execute, "end live");
        // DB 에서 방송 데이터 삭제 성공 시 실행할 callback
        class Callee_success extends httpRequestAPIs.Callee_success {
            public void call(httpRequestAPIs.ResponseObject responseObj) {
                Log.d(tag_check, "deleteLiveRoomData success!");

                mSocket.emit(getString(R.string.end_live));
                Log.d(tag_execute, "end live emitted");
                mSocket.disconnect();

                setResult(Activity.RESULT_OK);
                finish();
            }
        }

        // 요청 실패 시 실행할 callback
        class Callee_failed extends httpRequestAPIs.Callee_failed {
            public void call(int statusCode) {
                Log.d(tag_check, "deleteLiveRoomData failed. statusCode : " + statusCode);
                dialog_confirm.show(context, "에러가 발생하였습니다.");
            }
        }

        httpRequestAPIs.deleteLiveRoomData(roomId, new Callee_success(), new Callee_failed());
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

    @Override
    public void onBackPressed() {
    }
}
