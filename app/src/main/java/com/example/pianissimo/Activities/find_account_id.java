package com.example.pianissimo.Activities;

import android.text.Editable;
import android.text.TextWatcher;
import android.content.Intent;
import android.os.Bundle;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.Modules.httpRequestAPIs;
import com.example.pianissimo.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class find_account_id extends AppCompatActivity {
    private Context context = this;
    private View rootLayout;
    private TextInputEditText phoneNumInput;

    private String phoneNum = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_find_account_id);

        // 바깥 영역 터치 시 EditText 키패드 내리기
        rootLayout = findViewById(R.id.findAccountIdLayout);
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });

        phoneNumInput = findViewById(R.id.phoneNumInput);

        // Textwatcher 등록
        phoneNumInput.addTextChangedListener(textWatcher);   // onCreate 내부에 설정해 주기
    }

    // 아이디(이메일) 찾기 요청
    public void findAccountId(View view) {
        String regexPattern = "010([0-9]{4})[0-9]{4}";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(phoneNum);

        if (phoneNum.length() == 0 || !matcher.matches()) {
            dialog_confirm.show(this, "올바른 전화번호 형식을 입력해 주세요.");
        }
        else {
            // 요청 성공 시 실행할 callback
            class Callee_success extends httpRequestAPIs.Callee_success {
                public void call(httpRequestAPIs.ResponseObject responseObj) {
                    dialog_confirm.show(context, "해당 전화번호로 아이디를 전송하였습니다.");
                }
            }

            // 요청 실패 시 실행할 callback
            class Callee_failed extends httpRequestAPIs.Callee_failed {
                public void call(int statusCode) {
                    System.out.println("## failed statusCode : " + statusCode);

                    if (statusCode == 404) {
                        dialog_confirm.show(context, "해당 전화번호로 가입된 계정이 없습니다.");
                    } else {
                        dialog_confirm.show(context, "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
                    }
                }
            }
            httpRequestAPIs.findId(phoneNum, new Callee_success(), new Callee_failed());
        }
    }

    // 휴대전화 번호 입력 TextWatcher
    public final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            phoneNum = s.toString();
        }
    };
}
