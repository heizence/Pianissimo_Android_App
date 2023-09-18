package com.example.pianissimo.TestFiles;

import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.SurfaceViewRenderer;

import com.example.pianissimo.R;

public class test_chat_layout extends AppCompatActivity {
    private RelativeLayout buttonLayout;
    private ScrollView scrollView;
    private EditText editText;
    private Button sendButton;
    private LinearLayout messagesLayout;
    private androidx.constraintlayout.widget.ConstraintLayout constrainedLayout;
    private LinearLayout streamerVideoLinearLayout;
    private TextView chatTitleTextView;
    private boolean isKeyboardShown = false;

    private String screenName = "[ACTIVITY]:test_chat_layout";  //  Fragment 일 경우 FRAGMENT 로 할 수도 있음
    private String tag_check = screenName + "[CHECK]";  // 특정 값을 확인할 때 사용
    private String tag_execute = screenName + "[EXECUTE]";  // 매서드나 다른 실행 가능한 코드를 실행할 때 사용
    private String tag_event = screenName + "[EVENT]";  // 특정 이벤트 발생을 확인할 때 사용

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_chat_layout);

        buttonLayout = findViewById(R.id.buttonLayout);
        scrollView = findViewById(R.id.scrollView);
        messagesLayout = findViewById(R.id.messageLayout);
        editText = findViewById(R.id.editText);
        sendButton = findViewById(R.id.sendButton);
        chatTitleTextView = findViewById(R.id.chatTitleTextView);
        constrainedLayout = findViewById(R.id.constrainedLayout);
        streamerVideoLinearLayout = findViewById(R.id.streamerVideoLinearLayout);

        // Add a global layout listener to the root layout
        buttonLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(tag_event, "onGlobalLayout");
                Rect r = new Rect();
                buttonLayout.getWindowVisibleDisplayFrame(r);

                int screenHeight = buttonLayout.getRootView().getHeight();

                // Calculate the height difference between the screen height and the visible display frame
                int heightDiff = screenHeight - (r.bottom - r.top);

                if (heightDiff > screenHeight * 0.15 && !isKeyboardShown) {
                    Log.d(tag_check, "keyboard is up");
                    // Keyboard is visible
                    isKeyboardShown = true;

                    // 실시간 채팅 scrollView 영역
                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
                    params1.height = getResources().getDimensionPixelSize(R.dimen.scroll_view_adjustment);
                    scrollView.setLayoutParams(params1);
                    Log.d(tag_check, "params1.height : " + params1.height + ", " + getResources().getDimensionPixelSize(R.dimen.scroll_view_adjustment));

                    // 방송 정보 영역
                    RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) constrainedLayout.getLayoutParams();
                    params2.height = getResources().getDimensionPixelSize(R.dimen.info_view_adjustment);
                    constrainedLayout.setLayoutParams(params2);
                    Log.d(tag_check, "params2.height : " + params2.height + ", " + getResources().getDimensionPixelSize(R.dimen.info_view_adjustment));

                    // 스트리밍 영상 영역
                    RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) streamerVideoLinearLayout.getLayoutParams();
                    params3.height = getResources().getDimensionPixelSize(R.dimen.video_view_adjustment);
                    streamerVideoLinearLayout.setLayoutParams(params3);
                    Log.d(tag_check, "params3.height : " + params3.height + ", " + getResources().getDimensionPixelSize(R.dimen.video_view_adjustment));

                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                } else if (heightDiff <= screenHeight * 0.15 && isKeyboardShown){
                    Log.d(tag_check, "keyboard is closed");
                    isKeyboardShown = false;

                    // 실시간 채팅 scrollView 영역
                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) scrollView.getLayoutParams();
                    params1.height = getResources().getDimensionPixelSize(R.dimen.scroll_view_height);
                    scrollView.setLayoutParams(params1);
                    Log.d(tag_check, "params1.height : " + params1.height + ", " + getResources().getDimensionPixelSize(R.dimen.scroll_view_adjustment));

                    // 방송 정보 영역
                    RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) constrainedLayout.getLayoutParams();
                    params2.height = getResources().getDimensionPixelSize(R.dimen.info_view_height);
                    Log.d(tag_check, "params2.height : " + params2.height + ", " + getResources().getDimensionPixelSize(R.dimen.info_view_adjustment));
                    constrainedLayout.setLayoutParams(params2);

                    // 스트리밍 영상 영역
                    RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) streamerVideoLinearLayout.getLayoutParams();
                    params3.height = getResources().getDimensionPixelSize(R.dimen.video_view_height);
                    Log.d(tag_check, "params3.height : " + params3.height + ", " + getResources().getDimensionPixelSize(R.dimen.video_view_adjustment));
                    streamerVideoLinearLayout.setLayoutParams(params3);
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerMessage();
            }
        });
    }
    private void registerMessage() {
        String message = editText.getText().toString().trim();
        if (!message.isEmpty()) {
            TextView textView = new TextView(this);
            textView.setTextSize(50);
            textView.setText(message);
            messagesLayout.addView(textView);


            // Clear the input field
            editText.setText("");

            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(tag_execute, "scroll to the bottom. getBottom() value is " + scrollView.getBottom());
                    scrollView.fullScroll(scrollView.FOCUS_DOWN);
                }
            });
        }
    }
}
