package com.example.aikitsdk;

import android.content.Context;

import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiStatus;

public interface Helper {

    void init(Context context);

    void registerListener();

    void start();

    void end();

    void write(byte[] data, AiStatus status);

    void setCallback(RecognitionCallback callback);

    void unInit();

}
