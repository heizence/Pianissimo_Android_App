package com.example.pianissimo.TestFiles;

import android.text.Editable;
import android.text.TextWatcher;

import android.view.inputmethod.InputMethodManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.R;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class test_start_live extends AppCompatActivity {
    private View rootLayout;    // onCreate 바깥에 선언
    private String roomName = "";
    private String userName;
    private Boolean isStreamer;
    private EditText roomNameInput;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_start_live);

        // 바깥 영역 터치 시 EditText 키패드 내리기
        rootLayout = findViewById(R.id.test_start_live);
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });
        roomNameInput = findViewById(R.id.roomNameInput);
        roomNameInput.addTextChangedListener(textWatcher);   // onCreate 내부에 설정해 주기

        Intent intent = getIntent();
        userName = intent.getStringExtra("userName");
        isStreamer = intent.getBooleanExtra("isStreamer", false);
    }

    // 방송방 생성 후 방송 시작
    public void createRoomAndStartLive(View view) {
        if (roomName.length() == 0) {
            dialog_confirm.show(this, "방송방 이름을 입력해 주세요.");
        } else {
            UUID uuid = UUID.randomUUID();  // roomId 로 사용할 uuid

            System.out.println("## check uuid : " + uuid);

            Intent intent = new Intent(this, test_each_room.class);
            intent.putExtra("userName", userName);
            intent.putExtra("roomName", roomName);
            intent.putExtra("isStreamer", isStreamer);
            intent.putExtra("roomId", uuid.toString());
            startActivity(intent);
        }
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
            roomName = s.toString();
        }
    };
}
