package com.example.pianissimo.Activities;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.Modules.httpRequestAPIs;
import com.example.pianissimo.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class reissue_password extends AppCompatActivity {
    private Context context = this;
    private View rootLayout;
    private TextInputEditText userIdInput;

    private String userId = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_reissue_password);

        // 바깥 영역 터치 시 EditText 키패드 내리기
        rootLayout = findViewById(R.id.reissuePasswordLayout);
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });

        userIdInput = findViewById(R.id.userIdInput);

        // Textwatcher 등록
        userIdInput.addTextChangedListener(textWatcher);   // onCreate 내부에 설정해 주기
    }

    // 비밀번호 재발급 요청
    public void reissuePassword(View view) {
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(userId);

        if (userId.length() == 0 || !matcher.matches()) {
            dialog_confirm.show(this, "올바른 아이디(이메일) 형식을 입력해 주세요.");
        } else {
            // 요청 성공 시 실행할 callback
            class Callee_success extends httpRequestAPIs.Callee_success {
                public void call(httpRequestAPIs.ResponseObject responseObj) {
                    dialog_confirm.show(context, "해당 이메일로 임시 비밀번호가 전송되었습니다.");
                }
            }

            // 요청 실패 시 실행할 callback
            class Callee_failed extends httpRequestAPIs.Callee_failed {
                public void call(int statusCode) {
                    System.out.println("## failed statusCode : " + statusCode);

                    if (statusCode == 404) {
                        dialog_confirm.show(context, "해당 아이디로 가입된 계정이 없습니다.");
                    } else {
                        dialog_confirm.show(context, "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
                    }
                }
            }
            httpRequestAPIs.reissuePassword(userId, new Callee_success(), new Callee_failed());
        }
    }

    // 아이디 입력 TextWatcher
    public final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            Pattern regex = Pattern.compile("[$&+,:;=\\\\?#|/'<>^*()%!-]");

            if (regex.matcher(s.toString()).find()) {
                userIdInput.setText(userId);
                userIdInput.setSelection(userId.length());
                Toast toast = Toast.makeText(getBaseContext(), "특수문자는 사용할 수 없습니다", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                userId = s.toString();
            }
        }
    };
}
