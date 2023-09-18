package com.example.pianissimo.Modules;

import android.os.Build;

// emulator 에서 실행 중인지 실제 device 에서 실행 중인지 판별. emulator 에서 실행 시 true return, 실제 기기에서 실행 시 false return.
public class checkIsEmulator {
    public static boolean check() {
        String model = Build.MODEL;
        return model.contains("sdk") || model.contains("Emulator");
    }
}
