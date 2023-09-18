package com.example.pianissimo.TestFiles;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.pianissimo.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;

public class test_capture extends AppCompatActivity {
    private Context context;

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

    private String activityName = "[ACTIVITY test_capture]:";
    private String tag_check = activityName + "[CHECK]";
    private String tag_execute = activityName + "[EXECUTE]";
    private String tag_event = activityName + "[EVENT]";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        setContentView(R.layout.test_capture);
        cameraPreviewView = findViewById(R.id.test_capture_cameraView); // 카메라 미리보기를 표시해 줄 view 를 layout 에 추가

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        setCameraProvider();
    }

    /* ProcessCameraProvider 요청하기.
    ProcessCameraProvider 는 앱 프로세스 내부에서 카메라의 lifecycle 을 LifecycleOwner 에 bind 할 수 있게 해주는 요소이다.
    */
    public void setCameraProvider() {
        Log.d(tag_execute, "setCameraProvider");

        // cameraProviderFuture 의 instance 를 불러오기.
        // ListenableFuture<ProcessCameraProvider> type 은 camera provider 를 얻기 위한 비동기 작업을 나타내는 객체이다.
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // camera provider 가 사용 가능해질 때 실행되는 리스너
        cameraProviderFuture.addListener(() -> {
            try {
                Log.d(tag_event, "cameraProviderFuture event occurred.");
                cameraProvider = cameraProviderFuture.get();    // camera provider 가 사용 가능해지면 해당 camera provider 불러오기
                setCameraSelector();    // CameraSelector 생성 및 설정하기(별도로 작성된 매서드).
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // CameraSelector 생성 및 설정
    public void setCameraSelector() {
        Log.d(tag_execute, "setCameraSelectorAndBind");
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraSelector.LENS_FACING_FRONT)    // 카메라 종류(렌즈 종류) 선택
                .build();   // selector 빌드
        bindPreview();  // bindPreview 설정(별도로 작성된 매서드)
    }

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
        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview); // do not delete!
    }

    // 카메라 변경(전,후면)
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

    // 이미지 캡쳐
    public void captureImage(View view) {
        Log.d(tag_execute, "captureImage");
        File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "thumbnailImg.jpg");
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                        Log.d(tag_check, "captureImage success : " + outputFileResults);
                        Log.d(tag_check, "imageFile URI : " + outputFileResults.getSavedUri());
                        File savedFile = outputFileResults.getSavedUri() != null ? new File(outputFileResults.getSavedUri().getPath()) : outputFile;

                        try {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                            Bitmap capturedBitmap = BitmapFactory.decodeFile(savedFile.getAbsolutePath(), options);

                            // Resize the Bitmap to your desired dimensions
                            int desiredWidth = screenWidth / 2; // Set the desired width
                            int desiredHeight = screenHeight / 2; // Set the desired height

                            // Convert the resized Bitmap to a byte array
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(capturedBitmap, desiredWidth, desiredHeight, true);

                            // Convert the resized Bitmap to a byte array
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                            byte[] imageBytes = byteArrayOutputStream.toByteArray();

                            String thumbNailImgString = Base64.getEncoder().encodeToString(imageBytes);
                            Log.d(tag_check, "thumbNailImgString : " + thumbNailImgString);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ImageCaptureException error) {
                        Log.d(tag_check, "captureImage failed : " + error);
                    }
                }
        );
    }
}
