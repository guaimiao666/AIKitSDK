package com.example.aikitsdk;

public interface RecognitionCallback {
    void onResult(String result);
    void onEvent(int event);
    void onError(int errorCode, String msg);
}
