package com.example.pianissimo.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.pianissimo.Modules.Sha256_hash;
import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.R;
import com.google.android.material.textfield.TextInputEditText;
import com.example.pianissimo.Modules.httpRequestAPIs;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;
import java.util.regex.Pattern;

public class edit_password extends AppCompatActivity {
    private static final String TAG = "edit_password";
    private Context context = this;
    private View rootLayout;
    private TextInputEditText newPasswordInput;
    private TextInputEditText newPwConfirmInput;

    private String newPassword = "";
    private String newPasswordConfirm = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_edit_password);

        // 바깥 영역 터치 시 EditText 키패드 내리기
        rootLayout = findViewById(R.id.editPasswordLayout);
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });

        newPasswordInput = findViewById(R.id.newPasswordInput);
        newPwConfirmInput = findViewById(R.id.newPwConfirmInput);

        // Textwatcher 등록
        newPasswordInput.addTextChangedListener(newPasswordTextWatcher);
        newPwConfirmInput.addTextChangedListener(newPasswordConfirmTextWatcher);
    }

    // 비밀번호 변경 요청
    public void editPassword(View view) {
        if (newPassword.length() == 0) {
            dialog_confirm.show(this, "새 비밀번호를 입력해 주세요.");
        } else if (newPasswordConfirm.length() == 0) {
            dialog_confirm.show(this, "새 비밀번호 확인을 입력해 주세요.");
        } else if (!Objects.equals(newPassword, newPasswordConfirm)) {
            dialog_confirm.show(this, "비밀번호가 일치하지 않습니다.");
        } else {
            String hashedPassword = Sha256_hash.hexString(newPassword);

            // 요청 성공 시 실행할 callback
            class Callee_success extends httpRequestAPIs.Callee_success {
                public void call(httpRequestAPIs.ResponseObject responseObj) {
                    // 성공 시 response 는 jwt token
                    dialog_confirm.show(context, "비밀번호가 변경되었습니다.");
                }
            }

            // 요청 실패 시 실행할 callback
            class Callee_failed extends httpRequestAPIs.Callee_failed {
                public void call(int statusCode) {
                    System.out.println("## failed statusCode : " + statusCode);
                    dialog_confirm.show(context, "요청 중 에러가 발생하였습니다. 잠시 후 다시 시도해 주세요.");
                }
            }
            httpRequestAPIs.editPassword(hashedPassword, new Callee_success(), new Callee_failed());
        }
    }

    // 새 비밀번호 입력 TextWatcher
    public final TextWatcher newPasswordTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            newPassword = s.toString();
        }
    };

    // 새 비밀번호 확인 입력 TextWatcher
    public final TextWatcher newPasswordConfirmTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            newPasswordConfirm = s.toString();
        }
    };
}
