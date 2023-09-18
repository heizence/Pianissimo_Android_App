package com.example.pianissimo.Modules;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.pianissimo.R;

public class dialog_edit_profile_img {
    // activity 에서 dialog 열 때 호출하는 매서드
    public static void show(Context context,
                            Callee_Select_Album param_calleeSelectAlbum,    // 앨범에서 사진 선택 시 실행할 콜백 함수
                            Callee_Select_Camera param_calleeSelectCamera   // 카메라로 사진 촬영 시 실행할 콜백 함수
    ) {
        dialog_edit_profile_img.dialogInstance dialog = new dialog_edit_profile_img.dialogInstance(context, param_calleeSelectAlbum, param_calleeSelectCamera);
        dialog.show();
    }

    // 확인 버튼 dialog 생성하는 class
    static class dialogInstance extends Dialog {
        private TextView selectFromAlbumBtn;
        private TextView selectFromCameraBtn;

        // 콜백 함수 사용을 위한 변수 정의
        private Caller_Select_Album callerSelectAlbum;
        private Callee_Select_Album calleeSelectAlbum;

        private Caller_Select_Camera callerSelectCamera;
        private Callee_Select_Camera calleeSelectCamera;

        public dialogInstance(@NonNull Context context, Callee_Select_Album param_calleeSelectAlbum, Callee_Select_Camera param_calleeSelectCamera) {
            super(context);
            this.callerSelectAlbum = new Caller_Select_Album();
            this.calleeSelectAlbum = param_calleeSelectAlbum;

            this.callerSelectCamera = new Caller_Select_Camera();
            this.calleeSelectCamera = param_calleeSelectCamera;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_edit_profile_img);

            selectFromAlbumBtn = findViewById(R.id.dialogEditProfileSelectFromAlbumBtn);
            selectFromCameraBtn = findViewById(R.id.dialogEditProfileSelectFromCameraBtn);

            // 앨범에서 사진 선택 시 콜백 함수 실행
            selectFromAlbumBtn.setOnClickListener(view -> {
                callerSelectAlbum.register(calleeSelectAlbum);
                dismiss();
            });

            // 카메라로 사진 촬영 시 콜백 함수 실행
            selectFromCameraBtn.setOnClickListener(view -> {
                callerSelectCamera.register(calleeSelectCamera);
                dismiss();
            });
        }
    }

    // 콜백 함수 전달을 위한 interface(앨범에서 사진 선택)
    interface Callback_Select_Album {
        void call();
    }

    // 콜백 함수 전달을 위한 interface(카메라로 사진 촬영)
    interface Callback_Select_Camera {
        void call();
    }

    // 호출을 하는 주체(앨범에서 사진 선택)
    static class Caller_Select_Album {
        public void register(Callback_Select_Album callback) {
            callback.call();
        }
    }

    // 호출을 하는 주체(카메라로 사진 촬영)
    static class Caller_Select_Camera {
        public void register(Callback_Select_Camera callback) {
            callback.call();
        }
    }

    // 요청 후 콜백 함수를 실행시키는 주체(앨범에서 사진 선택)
    public static class Callee_Select_Album implements Callback_Select_Album {
        public void call() {
        }
    }

    // 요청 후 콜백 함수를 실행시키는 주체(카메라로 사진 촬영)
    public static class Callee_Select_Camera implements Callback_Select_Camera {
        public void call() {
        }
    }
}
