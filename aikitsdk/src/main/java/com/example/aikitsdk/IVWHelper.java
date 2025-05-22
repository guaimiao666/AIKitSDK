package com.example.aikitsdk;

import android.content.Context;
import android.util.Log;

import com.iflytek.aikit.core.AiAudio;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiStatus;

import java.util.List;

public class IVWHelper implements Helper {

    private final String TAG = "IVWHelper";
    private Context context;
    private final String ABILITYID = "e867a88f2";
    private RecognitionCallback callback;
    private AiHandle aiHandle;
    private boolean bReady = false;

    public static IVWHelper getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final IVWHelper INSTANCE = new IVWHelper();
    }

    @Override
    public void init(Context context) {
        this.context = context;
    }

    @Override
    public void setCallback(RecognitionCallback callback) {
        this.callback = callback;
    }

    @Override
    public void registerListener() {
        AiHelper.getInst().registerListener(ABILITYID, edListener);
    }

    /**
     * 启动唤醒
     * 1.设置自定义唤醒数据
     * 2.创建唤醒会话
     */
    @Override
    public void start() {
        AiRequest.Builder customBuilder = AiRequest.builder();
        //key：数据标识 value： 唤醒词配置文件 index： 数据索引,用户可自定义设置
        customBuilder.customText("key_word", AIKitSDK.getInstance().getWorkDir() + "/keyword.txt", 0);
        int ret = AiHelper.getInst().loadData(ABILITYID, customBuilder.build());
        if (ret != 0) {
            Log.w(TAG, "加载自定义唤醒词失败! ret:" + ret);
            return;
        }
        int[] indexs = {0};
        ret = AiHelper.getInst().specifyDataSet(ABILITYID, "key_word", indexs);//从缓存中把个性化资源设置到引擎中
        if (ret != 0) {
            Log.w(TAG, "加载自定义唤醒词失败! ret:" + ret);
            return;
        }
        AiRequest.Builder paramBuilder = AiRequest.builder();
        paramBuilder.param("wdec_param_nCmThreshold", "0 0:800");
        paramBuilder.param("gramLoad", true);
        aiHandle = AiHelper.getInst().start(ABILITYID, paramBuilder.build(), null);
        if (aiHandle.getCode() != 0) {
            Log.w(TAG, "唤醒会话创建失败! code:" + aiHandle.getCode());
            return;
        }
        bReady = true;
    }

    /**
     * 结束会话
     */
    @Override
    public void end() {
        if (aiHandle != null) {
            AiHelper.getInst().end(aiHandle);
            bReady = false;
            aiHandle = null;
        }
    }

    /**
     * 写入音频数据
     *
     * @param data   音频数据
     * @param status 音频状态
     */
    @Override
    public void write(byte[] data, AiStatus status) {
        if (!bReady) {
            Log.w(TAG, "唤醒能力未准备完成!");
            return;
        }
        AiRequest.Builder dataBuilder = AiRequest.builder();

        /**
         * 送入音频需要标识音频的状态，第一帧为起始帧，status要传AiStatus.BEGIN,最后一帧为结束帧，status要传AiStatus.END,其他为中间帧，status要传AiStatus.CONTINUE
         * 音频要求16bit，16K，单声道的pcm音频。
         * 建议每次发送音频间隔40ms，每次发送音频字节数为一帧音频大小的整数倍。
         */
        AiAudio aiAudio = AiAudio.get("wav").data(data).status(status).valid();
        dataBuilder.payload(aiAudio);

        int ret = AiHelper.getInst().write(dataBuilder.build(), aiHandle);
        if (ret != 0) {
            Log.w(TAG, "唤醒数据写入失败! ret:" + ret);
        }
    }

    /**
     * 能力监听回调
     */
    private AiListener edListener = new AiListener() {
        @Override
        public void onResult(int handleID, List<AiResponse> outputData, Object usrContext) {
            if (null != outputData && outputData.size() > 0) {
                Log.i(TAG, "onResult:handleID:" + handleID + ":" + outputData.size() + "," +
                        "usrContext:" + usrContext);
                for (int i = 0; i < outputData.size(); i++) {
                    Log.d(TAG, "onResult:handleID:" + handleID + ":" + outputData.get(i).getKey());
                    String key = outputData.get(i).getKey();   //引擎结果的key
                    byte[] bytes = outputData.get(i).getValue(); //识别结果
                    String result = new String(bytes);
                    Log.d(TAG, "key=" + key);
                    Log.d(TAG, "value=" + result);
                    Log.d(TAG, "status=" + outputData.get(i).getStatus());
                    if ((key.equals("func_wake_up") || key.equals("func_pre_wakeup"))) {
                        if (callback != null) {
                            callback.onResult(result);
                        }
                    }
                }
            }
        }

        @Override
        public void onEvent(int handleID, int event, List<AiResponse> eventData, Object o) {
            Log.i(TAG, "onEvent:" + handleID + ",event:" + event);
            if (callback != null) {
                callback.onEvent(event);
            }

            if (event == 2) {
                ESRHelper.getInstance().start();
                AIKitSDK.getInstance().setCanWriteEsr(true);
            }
        }

        @Override
        public void onError(int handleID, int errorCode, String msg, Object o) {
            Log.w(TAG, "错误通知，能力执行终止,Ability " + handleID + " ERROR::" + msg + ",err code:" + errorCode);
            if (callback != null) {
                callback.onError(errorCode, msg);
            }
        }
    };
}
