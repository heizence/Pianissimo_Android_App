package com.example.pianissimo.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.pianissimo.Modules.dialog_confirm;
import com.example.pianissimo.R;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pianissimo.Modules.httpRequestAPIs;

public class MainActivity extends AppCompatActivity {
    private Context context = this;
    public static Context contextOfApplication; // activity class 가 아닌 다른 class 에서 context 에 접근해야 할 때 사용.
    private SharedPreferences sharedPref;
    private String app_token;   // 앱에 저장된 jwt token

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextOfApplication = getApplicationContext();

        sharedPref = context.getSharedPreferences(getString(R.string.sharedPreferenceMain), Context.MODE_PRIVATE);

        app_token = sharedPref.getString(getString(R.string.store_app_token), "");
        // 저장된 토큰이 있을 때
        if (app_token.length() != 0) {
            Intent intent = new Intent(context, signedin_main.class);
            System.out.println("## [MainActivity] has token : " + app_token);
            startActivity(intent);
        }
        // 저장된 토큰이 없을 때
        else {
            System.out.println("## [MainActivity] no token");
            Intent intent = new Intent(this, signin.class);
            startActivity(intent);
        }
        finish();
    }

    // activity class 가 아닌 다른 class 에서 context 에 접근해야 할 때 사용.
    public static Context getContextOfApplication(){
        return contextOfApplication;
    }
}
