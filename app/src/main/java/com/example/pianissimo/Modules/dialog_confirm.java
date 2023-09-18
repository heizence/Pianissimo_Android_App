package com.example.pianissimo.Modules;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.example.pianissimo.R;

public class dialog_confirm {
    // activity 에서 dialog 열 때 호출하는 매서드
    public static void show(Context context, String contents) {
        AlertDialog_confirm dialog = new AlertDialog_confirm(context, contents);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // 확인 버튼 dialog 생성하는 class
    static class AlertDialog_confirm extends Dialog {
        private String dialogContentsTxt;
        private TextView dialogContentsTextView;
        private TextView confirmBtn;

        public AlertDialog_confirm(@NonNull Context context, String dialogContentsTxt) {
            super(context);
            this.dialogContentsTxt = dialogContentsTxt;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_confirm);

            dialogContentsTextView = findViewById(R.id.dialogConfirmContents);
            confirmBtn = findViewById(R.id.dialogConfirmOkBtn);
            dialogContentsTextView.setText(dialogContentsTxt);

            confirmBtn.setOnClickListener(view -> dismiss());
        }
    }
}



