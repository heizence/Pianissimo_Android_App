package com.example.pianissimo.TestFiles;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.databinding.ViewDataBinding;

import com.example.pianissimo.Modules.checkIsEmulator;
import com.example.pianissimo.Modules.httpRequestAPIs;
import com.example.pianissimo.R;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
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
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class test_each_room extends AppCompatActivity {
    private Context context = this;

    private Socket mSocket;
    private Handler mHandler;

    private View rootLayout;    // onCreate 바깥에 선언
    private TextView liveRoomNameView;
    private org.webrtc.SurfaceViewRenderer localSurfaceView;    // local stream 표시할 view
    //private org.webrtc.SurfaceViewRenderer remoteSurfaceView;   // remote peer stream 표시할 view
    private ScrollView chatScrollView;  // do not delete!
    private LinearLayout chatContentsLayout;
    private TextInputEditText chatInput;
    private Button sendChatBtn;

    private MediaConstraints audioConstraints;
    private VideoCapturer videoCapturer;
    private VideoTrack remoteVideoTrack;
    private AudioTrack remoteAudioTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;

    private ViewDataBinding binding; // for surfaceView binding
    //private PeerConnection peerConnection;
    private PeerConnectionList peerConnectionList;
    private EglBase rootEglBase;
    private PeerConnectionFactory factory;
    private VideoTrack videoTrackFromCamera;

    private String userName;
    private String roomName;
    private String roomId;
    private Boolean isStreamer;
    private String chatMessage;

    private ArrayList<JSONObject> iceCandidateArrayList;   // setLocalDescription 할 때 발생하는 iceCandidate 저장하는 arrayList

    private String signalingServer_URL_NORMAL = httpRequestAPIs.webRTC_Signaling_Server_URL;   // signaling server URL
    private String signalingServer_URL_EMULATOR = "http://10.0.2.2:8080";   // 에뮬레이터 전용 signaling server URL.

    private String tag_check = "[test_each_room CHECK]";
    private String tag_execute = "[test_each_room EXECUTE]";
    private String tag_socket_event = "[test_each_room SOCKET EVENT]";
    private String tag_event = "[test_each_room EVENT]";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_each_room);
        liveRoomNameView = findViewById(R.id.liveRoomName);
        localSurfaceView = findViewById(R.id.localSurfaceView);
        //remoteSurfaceView = findViewById(R.id.remoteSurfaceView); // webRTC 연결 테스트용으로만 사용

        chatScrollView = findViewById(R.id.chatScrollView);
        chatContentsLayout = findViewById(R.id.chatContentsLayout);
        chatInput = findViewById(R.id.chatInput);
        sendChatBtn = findViewById(R.id.sendChatBtn);

        // 바깥 영역 터치 시 EditText 키패드 내리기
        rootLayout = findViewById(R.id.test_each_room);
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });

        chatInput.addTextChangedListener(textWatcher);   // onCreate 내부에 설정해 주기

        mHandler = new Handler();

        Intent intent = getIntent();
        userName = intent.getStringExtra(getString(R.string.userName));
        roomName = intent.getStringExtra(getString(R.string.roomName));
        roomId = intent.getStringExtra(getString(R.string.roomId));
        isStreamer = intent.getBooleanExtra(getString(R.string.isStreamer), false);
        liveRoomNameView.setText("방송방 이름 : " + roomName);

        Log.d(tag_check, "onCreate: " + userName + ", " + roomName + ", " + roomId + ", " + isStreamer);

        initializeSurfaceViews();
        initializePeerConnectionFactory();

        peerConnectionList = new PeerConnectionList();   // 여러 개의 peerConnection 객체를 관리할 저장 공간 생성.

        // 스트리머의 경우 화면 및 음성을 송출하므로 카메라, 오디오 권한 확인 후 socket 연결 진행하기. 시청자는 해당 없음.
        if (isStreamer) {
            checkPermissions();
        } else {
            setSocket();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(tag_execute, "onDestroy");
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

    // 라이브 방송 시작 전 카메라 및 마이크 권한 허용 여부 체크
    public void checkPermissions() {
        Log.d(tag_execute, "checkPermissions");
        int PERMISSION_REQUEST_CODE = 100;

        // 카메라 및 오디오 권한 허용 상태(true 이면 허용이 되지 않은 상태)
        Boolean isCameraNotPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
        Boolean isAudioNotPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;

        if (isCameraNotPermitted || isAudioNotPermitted) {
            // Permission not granted
            // Request for camera and audio permissions
            Log.d(tag_check, "Camera and audio permission not granted. request permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            Log.d(tag_check, "Camera and audio permission has already granted!");
            setSocket();    // 스트리머 socket 설정 시작.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(tag_execute, "onRequestPermissionResult");
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.d(tag_check, "Camera and audio permission now granted!");
                setSocket();
            } else {
                Log.d(tag_check, "Camera and audio permission denied");
                // Permissions are denied, show an error message or disable the functionality
            }
        }
    }

    // 영상 화면 표시할 surfaceView 설정
    private void initializeSurfaceViews() {
        Log.d(tag_execute, "initializeSurfaceViews");
        rootEglBase = EglBase.create();
        localSurfaceView.init(rootEglBase.getEglBaseContext(), null);
        localSurfaceView.setEnableHardwareScaler(true);
        localSurfaceView.setMirror(true);
        // for test
//        remoteSurfaceView.init(rootEglBase.getEglBaseContext(), null);
//        remoteSurfaceView.setEnableHardwareScaler(true);
//        remoteSurfaceView.setMirror(true);
    }

    // PeerConnectionFactory 초기 설정. 영상 불러오기 전에, PeerConnection 생성하기 전에 먼저 실행해야 됨.(스트리머, 시청자 모두 해당)
    private void initializePeerConnectionFactory() {
        Log.d(tag_execute, "initializePeerConnectionFactory");
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        factory = new PeerConnectionFactory(null);
        factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
        Log.d(tag_check, "peerConnectionFactory : " + factory);
    }

    // PeerConnection 객체 생성
    private void createMyPeerConnection(String peerConnectionID, String peerSocketId) {
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
                    iceCandidateObj.put(getString(R.string.candidate), iceCandidate.sdp);

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
        iceCandidateArrayList = new ArrayList<>();  // iceCandidate 를 저장할 arrayList 생성
        Log.d(tag_check, "createPeerConnection finished!");

        // for test. 추후엔 스트리머만 화면 보이게 수정하기
        if (isStreamer) {
            // add stream
            MediaStream mediaStream = factory.createLocalMediaStream("ARDAMS");
            mediaStream.addTrack(videoTrackFromCamera);
            mediaStream.addTrack(localAudioTrack);
            peerConnection.addStream(mediaStream);
            Log.d(tag_check, "peerConnection.addStream. mediaStream : " + mediaStream);
        }
        peerConnectionList.add(peerConnectionID, peerConnection);
    }

    // 여러 개의 PeerConnection 을 저장하는 리스트 생성 class
    private class PeerConnectionList {
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

    // 기기 카메라에서 영상 데이터 불러와서 표시해 주기
    private void createVideoTrackFromCameraAndShowIt() {
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
            final int VIDEO_RESOLUTION_WIDTH = 500;
            final int VIDEO_RESOLUTION_HEIGHT = 500;
            final int FPS = 30;
            final String VIDEO_TRACK_ID = "ARDAMSv0";

            videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
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
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        Log.d(tag_execute, "createCameraCapturer");
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            // 전면 카메라 이용
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(tag_check, "isFrontFacing true");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    Log.d(tag_check, "has videoCapturer : " + videoCapturer);
                    return videoCapturer;
                }
            }
            // 후면 카메라 이용
            else {
                Log.d(tag_check, "isFrontFacing false");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    Log.d(tag_check, "has videoCapturer : " + videoCapturer);
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(tag_check, "isFrontFacing false");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    Log.d(tag_check, "has videoCapturer : " + videoCapturer);
                    return videoCapturer;
                }
            }
        }
        return null;
    }

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

    // 소켓 초기 설정
    public void setSocket() {
        try {
            // socket 생성 시 적용해 줄 option
            IO.Options mOptions = new IO.Options();

            mOptions.query = "userName=" + userName;
            mOptions.query += "&roomName=" + roomName;
            mOptions.query += "&roomId=" + roomId;
            mOptions.query += "&isStreamer=" + isStreamer;
            //System.out.println("## setSocket mOptions : " + mOptions.query);
            Log.d(tag_check, "setSocket mOptions query : " + mOptions.query);

            Boolean isEmulator = checkIsEmulator.check();
            Log.d(tag_check, "isEmulator : " + isEmulator);
            String signalingServer_URL = isEmulator ? signalingServer_URL_EMULATOR : signalingServer_URL_NORMAL;
            Log.d(tag_check, "check signalingServer_URL : " + signalingServer_URL);

            mSocket = IO.socket(signalingServer_URL, mOptions);

            /****************************** emit 이벤트 리스너 등록 *********************************/

            // 연결 시(스트리머, 시청자 모두 해당)
            mSocket.on(getString(R.string.connect), args -> {
                Log.d(tag_socket_event, "connect");
                Thread thread = new Thread(() -> {
                    try {
                        // 스트리머일 경우 영상 송출해 주기
                        if (isStreamer) {
                            createVideoTrackFromCameraAndShowIt();  // 해당 매서드 내에 emit("ready") 가 포함되어 있음.
                        } else {
                            mSocket.emit(getString(R.string.ready));
                            Log.d(tag_execute, "watcher ready emitted");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            });

            // 스트리머가 방송 시작
            mSocket.on(getString(R.string.streamer_start_live), args -> {
                Log.d(tag_socket_event, "streamer start live");
                Thread thread = new Thread(() -> {
                    try {
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

                                Log.d(tag_check, "check JSONObject data : " + data);
                                Log.d(tag_check, "streamerSocketId : " + streamerSocketId);
                                Log.d(tag_check, "watcherName : " + watcherName);
                                Log.d(tag_check, "watcherSocketId : " + watcherSocketId);
                                //Log.d(tag_check, "peerConnectionID : " + peerConnectionID);

                                TextView textView = new TextView(context);
                                textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                textView.setGravity(Gravity.CENTER);

                                String message = watcherName + "이(가) 입장하였습니다.(for test)";

                                // 스트리머, 신규 사용자일 경우에만 peerConnection 객체 생성해 주기

                                // 스트리머일 경우
                                if (isStreamer) {
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
                                                        offer.put(getString(R.string.peerConnectionID), peerConnectionID);
                                                        offer.put(getString(R.string.to), watcherSocketId);
                                                        offer.put(getString(R.string.sdp), sessionDescription.description);
                                                        //offer.put(getString(R.string.peerConnectionID), peerConnectionID);
                                                        //Log.d(tag_check, "sdp : " + sessionDescription.description);
                                                        mSocket.emit(getString(R.string.offer), offer);
                                                        Log.d(tag_execute, "offer event emitted");
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }, sessionDescription);
                                        }
                                    }, sdpMediaConstraints);
                                }
                                // 시청자일 경우
                                else {
                                    Log.d(tag_check, "mySocketID : " + mSocket.id());
                                    Boolean check1 = mSocket.id().equals(watcherSocketId);
                                    Boolean check2 = mSocket.id() == watcherSocketId;
                                    Log.d(tag_check, "compare ids check 1 : " + check1);
                                    Log.d(tag_check, "compare ids check 2 : " + check2);

                                    Log.d(tag_check, "client is 신규 시청자");
                                    if (mSocket.id().equals(watcherSocketId)) {
                                        Log.d(tag_check, "내가 채팅방에 입장");
                                        message = roomName + "에 입장하였습니다.(for test)";
                                    }

                                }
                                textView.setText(message);
                                chatContentsLayout.addView(textView);
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
                                Log.d(tag_check, "check JSONObject data : " + data);
                                Log.d(tag_check, "watcherName : " + watcherName);

                                TextView textView = new TextView(context);
                                textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                textView.setGravity(Gravity.CENTER);
                                textView.setText(watcherName + "이(가) 나갔습니다.(for test)");
                                chatContentsLayout.addView(textView);
                                peerConnectionList.remove(peerConnectionID);  // 해당 시청자와 연결되어 있었던 peerConnection 제거
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
                                String chatMessage = data.getString(getString(R.string.chatMessage));
                                Log.d(tag_check, "check JSONObject data : " + data);
                                Log.d(tag_check, "chatMessage : " + chatMessage);

                                TextView textView = new TextView(context);
                                textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                textView.setGravity(Gravity.CENTER);
                                textView.setText(chatMessage);
                                chatContentsLayout.addView(textView);
                                Log.d(tag_check, "textView added : " + textView);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });
                thread.start();
            });

            // 스트리머가 방송 종료
            mSocket.on(getString(R.string.end_live), args -> {
                System.out.println("## [SOCKET EVENT] end live");
                Log.d(tag_socket_event, "end live");
                Thread thread = new Thread(() -> {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context, "방송이 종료되었습니다!", Toast.LENGTH_SHORT).show();
                                finish();
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
                    String id = data.getString("id");
                    int label = data.getInt("label");
                    String candidateStr = data.getString("candidate");

                    PeerConnection peerConnection = peerConnectionList.get(peerConnectionID);

                    if (peerConnection != null) {
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                IceCandidate candidate = new IceCandidate(id, label, candidateStr);
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
        if (chatMessage.length() != 0) {
            Log.d(tag_execute, "sendMessage");

            Thread thread = new Thread(() -> {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mSocket.emit(getString(R.string.chat_message), chatMessage);
                            chatMessage = "";
                            chatInput.setText("");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            });
            thread.start();
        }
    }

    // 라이브 종료(스트리머) 또는 방송방 나가기(시청자)
    public void exit() {
        Log.d(tag_execute, "exit. isStreamer? : " + isStreamer);
        Thread thread = new Thread(() -> {
            try {
                remoteVideoTrack.setEnabled(false);
                remoteAudioTrack.setEnabled(false);
                if (isStreamer) {
                    mSocket.emit(getString(R.string.end_live));
                    Log.d(tag_execute, "end live emitted");
                } else {
                    mSocket.emit(getString(R.string.watcher_left));
                    Log.d(tag_execute, "watcher left emitted");
                }

                mSocket.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        Intent intent;

        if (isStreamer) {
            intent = new Intent(this, test_main.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
