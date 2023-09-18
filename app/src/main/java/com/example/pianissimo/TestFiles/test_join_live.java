package com.example.pianissimo.TestFiles;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import android.content.Intent;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.pianissimo.Modules.checkIsEmulator;
import com.example.pianissimo.Modules.httpRequestAPIs;
import com.example.pianissimo.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class test_join_live extends AppCompatActivity {
    private Context context;
    private View rootLayout;    // onCreate 바깥에 선언
    private LinearLayout roomListBtnsLayoutView;
    private String userName;
    private Boolean isStreamer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_join_live);

        context = this;

        // 바깥 영역 터치 시 EditText 키패드 내리기
        rootLayout = findViewById(R.id.test_join_live);
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });

        roomListBtnsLayoutView = findViewById(R.id.roomListBtnsLayout);
        System.out.println("## check roomListBtnLayout : " + roomListBtnsLayoutView);

        Intent intent = getIntent();
        userName = intent.getStringExtra("userName");
        isStreamer = intent.getBooleanExtra("isStreamer", false);

        renderRoomListBtns();
    }

    // 방송방 목록 랜더링
    public void renderRoomListBtns() {
        String signalingServer_URL_NORMAL = httpRequestAPIs.webRTC_Signaling_Server_URL + "/roomLists";   // signaling server URL
        String signalingServer_URL_EMULATOR = "http://10.0.2.2:8080/roomLists";   // 에뮬레이터 전용 signaling server URL.

        Boolean isEmulator = checkIsEmulator.check();
        System.out.println("## isEmulator : " + isEmulator);
        String signalingServer_URL = isEmulator ? signalingServer_URL_EMULATOR : signalingServer_URL_NORMAL;

        // 생성된 방송방 데이터 불러오기(for test)
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, signalingServer_URL,
                response -> {
                    // Display the response string.
                    System.out.println("## [Get room list response] : " + response);

                    if (response.length() != 0) {
                        System.out.println("## has data");
                        try {
                            JSONObject responseObj = new JSONObject(response);
                            System.out.println("## response obj : " + responseObj);
                            JSONArray roomNames = responseObj.names();

                            for (int i = 0; i < roomNames.length(); i++) {
                                // 방송방 정보
                                String roomName = roomNames.getString(i);
                                System.out.println("## check roomName : " + roomName);

                                JSONObject roomInfo = (JSONObject) responseObj.get(roomName);
                                System.out.println("## check roomInfo : " + roomInfo);

                                String roomId = roomInfo.getString(getString(R.string.roomId));
                                System.out.println("## check roomId : " + roomId);

                                // 버튼 생성
                                Button eachRoomBtn = new Button(context);
                                eachRoomBtn.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                                eachRoomBtn.setText(roomName);

                                eachRoomBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent intent = new Intent(context, test_each_room.class);
                                        intent.putExtra(getString(R.string.userName), userName);
                                        intent.putExtra(getString(R.string.isStreamer), isStreamer);
                                        intent.putExtra(getString(R.string.roomName), roomName);
                                        intent.putExtra(getString(R.string.roomId), roomId);
                                        startActivity(intent);
                                    }
                                });
                                roomListBtnsLayoutView.addView(eachRoomBtn);
                            }
                        } catch (Exception e) {

                        }
                    }
                }, error -> {
            // Handle error response.
            System.out.println("## [Get room list error] : " + error);
        });

        queue.add(stringRequest);

    }
}
