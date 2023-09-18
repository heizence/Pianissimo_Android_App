package com.example.pianissimo.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.Manifest;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.pianissimo.Fragments.fragment_broadcast;
import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.R;
import com.google.android.material.textfield.TextInputEditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

public class start_livestream extends AppCompatActivity {
    private Context context;
    private ActivityResultLauncher<Intent> launcher;

    private View rootLayout;
    private PreviewView cameraPreviewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private Camera camera;
    private ImageCapture imageCapture;
    private Boolean isUsingFrontCamera = true;
    private int screenWidth;
    private int screenHeight;

    private TextInputEditText liveTitleInput;
    private String roomName = "";

    private String activityName = "[ACTIVITY start_livestream]:";
    private String tag_check = activityName + "[CHECK]";
    private String tag_execute = activityName + "[EXECUTE]";
    private String tag_event = activityName + "[EVENT]";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_start_livestream);
        context = this;

        // 바깥 영역 터치 시 EditText 키패드 내리기
        rootLayout = findViewById(R.id.screen_start_livestream_FrameLayout);
        rootLayout.setOnTouchListener((v, event) -> {
            if (this.getCurrentFocus() != null) {
                InputMethodManager inputManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return false;
        });

        cameraPreviewView = findViewById(R.id.screen_start_livestream_cameraView);

        liveTitleInput = findViewById(R.id.screen_start_livestream_input);
        liveTitleInput.addTextChangedListener(textWatcher);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        checkPermissions();
    }

    @Override
    protected void onResume() {
        Log.d(tag_execute, "LifeCycle onResume");
        super.onResume();
        if (cameraProvider != null) {
            cameraViewInit();
        }
    }

    @Override
    protected void onStop() {
        Log.d(tag_execute, "LifeCycle onStop");
        super.onStop();

        if (cameraProvider != null) {
            cameraSelector = null;
            cameraProvider.unbindAll();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(tag_execute, "LifeCycle onDestroy");
        super.onDestroy();
    }

    // 뒤로 가기(화면 종료)
    public void onClickGoBack_(View view) {
        Log.d(tag_check, "onClickGoBack");
        finish();
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
            cameraViewInit();
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
                cameraViewInit();
            } else {
                Log.d(tag_check, "Camera and audio permission denied");
                // Permissions are denied, show an error message or disable the functionality
                finish();   // 뒤로 가기 처리
            }
        }
    }

    // CameraProviderFuture 초기 설정
    // ProcessCameraProvider 는 앱 프로세스 내부에서 카메라의 lifecycle 을 LifecycleOwner 에 bind 할 수 있게 해주는 요소이다.
    public void cameraViewInit() {
        Log.d(tag_execute, "cameraViewInit");

        // cameraProviderFuture 의 instance 를 불러오기.
        // ListenableFuture<ProcessCameraProvider> type 은 camera provider 를 얻기 위한 비동기 작업을 나타내는 객체이다.
        cameraSelector = new CameraSelector.Builder()   // 카메라 종류(렌즈 종류) 선택
                .requireLensFacing(cameraSelector.LENS_FACING_FRONT)
                .build();   // selector 빌드

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // camera provider 가 사용 가능해질 때 실행되는 리스너
        cameraProviderFuture.addListener(() -> {
            try {
                Log.d(tag_event, "cameraProviderFuture event occurred.");
                cameraProvider = cameraProviderFuture.get();    // camera provider 가 사용 가능해지면 해당 camera provider 불러오기
                bindPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // CameraX bindPreview
    public void bindPreview() {
        Log.d(tag_execute, "bindPreview");
        Preview preview = new Preview.Builder().build();    // preview instance 빌드

        // preview 에 surfaceProvider 등록.
        // 카메라 미리보기를 표시해 줄 cameraPreviewView 로부터 surfaceProvider 를 불러옴. SurfaceProvider instance 를 반환해 주는데 그 instance 를 preview 의 surfaceProvider 로 등록
        // 카메라가 찍는 영상을 미리보기가 가능하게 해 줌.
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()   // imageCapture instance 빌드. preview 를 통해 표시되는 미리보기 영상을 캡쳐하기 위해 필요함.
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // capture mode 설정. CAPTURE_MODE_MINIMIZE_LATENCY 는 지연 시간에 맞게 이미지 캡처를 최적화한다.
                // 캡처할 이미지 rotation 설정.
                // getWindowManager().getDefaultDisplay().getRotation() 는 rotation value 를 불러옴.
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .setTargetResolution(new Size(screenWidth, screenHeight))   // 캡처할 이미지의 해상도 설정. 기기 화면 전체 가로, 세로 크기를 해상도로 설정.
                .build();

        // camera 를 사용하는 경우(cameraSelector, imageCapture)들을 cameraProvider 에 bind 해 줌.
        // (LifecycleOwner) this 는 lifecycle 을 소유하고 있는 activity 또는 fragment 를 의미
        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview);
    }


    // 카메라 종류 전환(전,후면)
    public void switchCamera(View view) {
        Log.d(tag_execute, "switchCamera : ");
        isUsingFrontCamera = !isUsingFrontCamera;

        CameraSelector newCameraSelector;
        int cameraType;
        if (isUsingFrontCamera) {
            cameraType = CameraSelector.LENS_FACING_FRONT;
        } else {
            cameraType = CameraSelector.LENS_FACING_BACK;
        }

        newCameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraType)
                .build();

        cameraSelector = newCameraSelector;
        cameraProvider.unbindAll();
        bindPreview();
    }

    // 라이브 방송 thumbnail image 캡쳐
    public void captureImage() {
        Log.d(tag_execute, "captureImage");
        File outputFile = new File(getFilesDir(), "thumbnailImg.jpg");
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Image saved successfully
                        // You can access the captured image using outputFile path
                        Log.d(tag_check, "captureImage success : " + outputFileResults);
                        Log.d(tag_check, "imageFile URI : " + outputFileResults.getSavedUri());
                        //File savedFile = outputFileResults.getSavedUri() != null ? new File(outputFileResults.getSavedUri().getPath()) : outputFile;
                        Uri savedUri = outputFileResults.getSavedUri();

                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

                        try {
                            InputStream inputStream = getContentResolver().openInputStream(savedUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);    // This code reads the input stream from the saved URI and decodes it into a bitmap.

                            // Rotate the captured image by 90 degrees clockwise
                            Matrix matrix = new Matrix();   // Create a Matrix object to perform rotation.
                            matrix.postRotate(90);  // Rotate the original bitmap by 90 degrees clockwise using matrix.postRotate(90).

                            // Then, we create a new bitmap (rotatedBitmap) using Bitmap.createBitmap() by passing the original bitmap, matrix, and other parameters.
                            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                            // Flip the rotated image vertically
                            Matrix flipMatrix = new Matrix();   // Create another Matrix object (flipMatrix) to perform vertical flipping.
                            flipMatrix.postScale(1, -1);    // Using flipMatrix.postScale(1, -1), we flip the rotated bitmap vertically.

                            // Create a new bitmap (flippedBitmap) by passing the rotated bitmap, matrix, and other parameters to Bitmap.createBitmap().
                            Bitmap flippedBitmap = Bitmap.createBitmap(rotatedBitmap, 0, 0, rotatedBitmap.getWidth(), rotatedBitmap.getHeight(), flipMatrix, true);

                            Log.d(tag_check, "check screenWidth and height : " + screenWidth + ", " + screenHeight);

                            int newWidth = (int) (screenWidth * 0.4);
                            int newHeight = (int) (screenHeight * 0.4);

                            // Create a new bitmap (scaledBitmap) by scaling the flipped bitmap to the new dimensions using Bitmap.createScaledBitmap().
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(flippedBitmap, newWidth, newHeight, true);

                            // Compress the scaled bitmap to reduce file size
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();   // Create a ByteArrayOutputStream to hold the compressed image data.
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);   // Compress the scaled bitmap to reduce the file size.
                            byte[] imageBytes = outputStream.toByteArray(); // The compressed image data is then converted to a byte array (imageBytes).

                            String thumbNailImgString = Base64.getEncoder().encodeToString(imageBytes); // Encode the byte array as a Base64 string.

                            if (thumbNailImgString.isEmpty()) {
                                dialog_confirm.show(context, "에러가 발생했습니다.");
                            } else {
                                Intent intent = new Intent(context, each_live_room_streamer.class);
                                intent.putExtra(getString(R.string.thumbnailImage), thumbNailImgString);
                                intent.putExtra(getString(R.string.roomName), roomName);
                                intent.putExtra(getString(R.string.isUsingFrontCamera), isUsingFrontCamera);

                                String roomId = UUID.randomUUID().toString();// roomId 로 사용할 uuid
                                intent.putExtra(getString(R.string.roomId), roomId);
                                startActivity(intent);

                                finish();   // 나중에 방송 종료하고 메인 화면으로 바로 돌아가기 위해서 현재 activity 제거해 주기.
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            dialog_confirm.show(context, "에러가 발생했습니다.");
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        // Error occurred while capturing image
                        Log.d(tag_check, "captureImage failed : " + exception);
                        dialog_confirm.show(context, "에러가 발생했습니다.");
                    }
                });
    }

    // 방송 시작
    public void onClickStartLive(View view) {
        if (roomName.length() == 0) {
            dialog_confirm.show(this, "방송 타이틀을 입력해 주세요");
        } else {
            // captureImage 내에서 이미지 캡쳐 성공 시 startActivity 처리.
            captureImage();
        }
    }

    // TextWatcher
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
                liveTitleInput.setText(roomName);
                liveTitleInput.setSelection(roomName.length());
                Toast toast = Toast.makeText(getBaseContext(), "특수문자는 사용할 수 없습니다", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                roomName = s.toString();
            }
        }
    };
}

