package com.example.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

import com.example.aikitsdk.AIKitSDK;
import com.example.aikitsdk.AIKitSDKParam;
import com.example.aikitsdk.RecognitionCallback;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initSDK();
        initAudio();
    }

    private void initSDK() {
        AIKitSDKParam.Builder builder = new AIKitSDKParam.Builder()
                .appId("dc6e71ab")
                .apikey("1756c857948e6c415268cfa61564eb5a")
                .apiSecret("YWVlYmE4MTQwNDMxNTYyOTFmZDE5ODkx")
                .abilities(new String[]{"e75f07b62", "e867a88f2"})
                .keyWord("你好测试")
                .language(0)
                .workDir("/sdcard/aikit");

        AIKitSDK.getInstance().setIVWCallback(new RecognitionCallback() {
            @Override
            public void onResult(String result) {

            }

            @Override
            public void onEvent(int event) {

            }

            @Override
            public void onError(int errorCode, String msg) {

            }
        });
        AIKitSDK.getInstance().setEsrCallback(new RecognitionCallback() {
            @Override
            public void onResult(String result) {
                if (result.contains("打开空调") || result.contains("关闭空调")) {
                    Log.i(TAG, "已识别");
                }
            }

            @Override
            public void onEvent(int event) {

            }

            @Override
            public void onError(int errorCode, String msg) {

            }
        });
        AIKitSDK.getInstance().initSDK(this, builder.build());
    }

    private int minBufferSize;
    private AudioRecord audioRecord;
    private RecordAudioThread recordAudioThread;
    private boolean mRecordThreadExitFlag = false;

    @SuppressLint("MissingPermission")
    private void initAudio() {
        minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        if (recordAudioThread == null) {
            mRecordThreadExitFlag = false;
            recordAudioThread = new RecordAudioThread();
        }
        try {
            recordAudioThread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void unInitAudio() {
        if (recordAudioThread != null) {
            mRecordThreadExitFlag = true;
            recordAudioThread = null;
        }
        try {
            audioRecord.stop();
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 音频采集线程
     */
    class RecordAudioThread extends Thread {
        @Override
        public void run() {
            if (audioRecord == null)
                return;
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                audioRecord.startRecording();
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] audioBuffer = new byte[minBufferSize];
            while (!mRecordThreadExitFlag) {
                try {
                    int ret = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    if (ret < 0) {
                        break;
                    }
                    Log.i("Function", "Audio");
                    AIKitSDK.getInstance().writeAudioData(audioBuffer);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unInitAudio();
        AIKitSDK.getInstance().unInit();
    }
}