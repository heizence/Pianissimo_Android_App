package com.example.pianissimo.Modules;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.pianissimo.R;

public class dialog_confirm_or_cancel {
    // activity 에서 dialog 열 때 호출하는 매서드
    public static void show(Context context,
                            String contents,    // alert dialog 메시지 내용
                            Callee_Cancel param_calleeCancel,   // 취소 버튼 클릭 시 실행할 콜백 함수
                            Callee_Confirm param_calleeConfirm  // 확인 버튼 클릭 시 실행할 콜백 함수
    ) {
        dialog_confirm_or_cancel.dialogInstance dialog = new dialog_confirm_or_cancel.dialogInstance(context, contents, param_calleeCancel, param_calleeConfirm);
        dialog.show();
    }

    // 확인 버튼 dialog 생성하는 class
    static class dialogInstance extends Dialog {
        private TextView dialogContentsTextView;
        private TextView selectCancelBtn;
        private TextView selectConfirmBtn;

        private String dialogContentsTxt;

        // 콜백 함수 사용을 위한 변수 정의
        private Caller_Cancel callerSelectCancel;
        private Callee_Cancel calleeSelectCancel;

        private Caller_Confirm callerSelectConfirm;
        private Callee_Confirm calleeSelectConfirm;

        public dialogInstance(@NonNull Context context, String dialogContentsTxt, Callee_Cancel param_calleeCancel, Callee_Confirm param_calleeConfirm) {
            super(context);
            this.dialogContentsTxt = dialogContentsTxt;

            this.callerSelectCancel = new Caller_Cancel();
            this.calleeSelectCancel = param_calleeCancel;

            this.callerSelectConfirm = new Caller_Confirm();
            this.calleeSelectConfirm = param_calleeConfirm;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_confirm_or_cancel);
            dialogContentsTextView = findViewById(R.id.dialogConfirmOrCancel_Contents);
            dialogContentsTextView.setText(dialogContentsTxt);

            selectCancelBtn = findViewById(R.id.dialogConfirmOrCancel_CancelBtn);
            selectConfirmBtn = findViewById(R.id.dialogConfirmOrCancel_ConfirmBtn);

            // 취소 버튼 선택 시 콜백 함수 실행
            selectCancelBtn.setOnClickListener(view -> {
                callerSelectCancel.register(calleeSelectCancel);
                dismiss();
            });

            // 확인 버튼 선택 시 콜백 함수 실행
            selectConfirmBtn.setOnClickListener(view -> {
                callerSelectConfirm.register(calleeSelectConfirm);
                dismiss();
            });
        }
    }

    // 콜백 함수 전달을 위한 interface(취소 버튼 선택)
    interface Callback_Cancel{
        void call();
    }

    // 콜백 함수 전달을 위한 interface(확인 버튼 선택)
    interface Callback_Confirm {
        void call();
    }

    // 호출을 하는 주체(취소 버튼 선택)
    static class Caller_Cancel{
        public void register(Callback_Cancel callback) {
            callback.call();
        }
    }

    // 호출을 하는 주체(확인 버튼 선택)
    static class Caller_Confirm{
        public void register(Callback_Confirm callback) {
            callback.call();
        }
    }

    // 요청 후 콜백 함수를 실행시키는 주체(취소 버튼 선택)
    public static class Callee_Cancel implements Callback_Cancel {
        public void call() {}
    }

    // 요청 후 콜백 함수를 실행시키는 주체(확인 버튼 선택)
    public static class Callee_Confirm implements Callback_Confirm {
        public void call() {}
    }
}
