package com.example.pianissimo.TestFiles;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.R;
import com.google.android.material.textfield.TextInputEditText;

public class test_main extends AppCompatActivity {
    private Context context = this;
    private String userName = "";

    private View rootLayout;
    private EditText userNameInput;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_main);
        sharedPref = getSharedPreferences(getString(R.string.sharedPreferenceMain), MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        userNameInput = findViewById(R.id.userNameInput);

        // 바깥 영역 터치 시 EditText 키패드 내리기
        rootLayout = findViewById(R.id.test_main);
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });

        userNameInput.addTextChangedListener(textWatcher);   // onCreate 내부에 설정해 주기
    }

    // 라이브 시작
    public void startLive(View view) {
        if (userName.length() == 0) {
            dialog_confirm.show(this, "사용자 이름을 입력해 주세요.");
        }
        else {
            Intent intent = new Intent(context, test_start_live.class);
            intent.putExtra("userName", userName);
            intent.putExtra("isStreamer", true);
            startActivity(intent);
        }
    }

    // 라이브 참석(방송방 입장)
    public void joinLive(View view) {
        if (userName.length() == 0) {
            dialog_confirm.show(this, "사용자 이름을 입력해 주세요.");
        }
        else {
            Intent intent = new Intent(context, test_join_live.class);
            intent.putExtra("userName", userName);
            intent.putExtra("isStreamer", false);
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
            userName = s.toString();
        }
    };
}
