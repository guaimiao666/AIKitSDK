package com.example.aikitsdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiStatus;
import com.iflytek.aikit.core.BaseLibrary;
import com.iflytek.aikit.core.CoreListener;
import com.iflytek.aikit.core.LogLvl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIKitSDK {
    private static final String TAG = "AIKitSDK";
    private static AIKitSDK aiKitSDK;
    private Context context;
    private int authResult = -1;
    private AIKitSDKParam sdkParam;
    private final AtomicBoolean isInit = new AtomicBoolean(false);
    private final AtomicBoolean canWriteEsr = new AtomicBoolean(false);

    public static AIKitSDK getInstance() {
        if (aiKitSDK == null) {
            aiKitSDK = new AIKitSDK();
        }
        return aiKitSDK;
    }

    /**
     * 初始化SDK
     *
     * @param context  上下文
     * @param sdkParam SDK参数
     */
    public void initSDK(Context context, AIKitSDKParam sdkParam) {
        this.context = context;
        this.sdkParam = sdkParam;
        if (sdkParam == null) {
            Log.w(TAG, "SDK参数为空!");
            return;
        }
        if (TextUtils.isEmpty(sdkParam.getWorkDir())) {
            Log.w(TAG, "工作目录路径为空!");
            return;
        }
        createWorkDir(sdkParam.getWorkDir());
        writeKeyWord(sdkParam.getWorkDir() + "/keyword.txt", sdkParam.getKeyWord());
        initSDK_(sdkParam.getAppId(), sdkParam.getApikey(), sdkParam.getApiSecret(), sdkParam.getWorkDir(), sdkParam.getAbilities());
    }

    private void initSDK_(String appId, String apikey, String apiSecret, String workDir, String[] abilities) {
        if (abilities == null || abilities.length == 0) {
            Log.w(TAG, "能力数组为空!");
            return;
        }
        String abilityStr = Utils.getAbilityStr(abilities);
        AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, workDir + "/aeeLog.txt");
        //设定初始化参数
        //能力id列表 唤醒：e867a88f2  合成e2e44feff  命令词e75f07b62  合成轻量版ece9d3c90
        BaseLibrary.Params params = BaseLibrary.Params.builder()
                .appId(appId)  //您的应用ID，可从控制台查看
                .apiKey(apikey) //您的APIKEY，可从控制台查看
                .apiSecret(apiSecret) //您的APISECRET，可从控制台查看
                .workDir(workDir) //SDK的工作目录，需要确保有读写权限。一般用于存放离线能力资源，日志存放目录等使用。
                .ability(abilityStr) //初始化时使用几个能力就填几个能力的id，以;分开，如：使用唤醒+合成填入"e867a88f2;e2e44feff"
                .build();
        //初始化SDK
        new Thread(() -> AiHelper.getInst().initEntry(context, params)).start();
        AiHelper.getInst().registerListener(coreListener);// 注册SDK 初始化状态监听
    }

    private CoreListener coreListener = (errType, code) -> {
        switch (errType) {
            case AUTH:
                authResult = code;
                if (code == 0) {
                    Log.i(TAG, "SDK授权成功");
                    isInit.set(true);
                    //初始化语音唤醒
                    initIVW();
                    //初始化语音识别
                    initESR();
                } else {
                    Log.i(TAG, "SDK授权失败，授权码为:" + authResult);
                    isInit.set(false);
                }
                break;
            case HTTP:
                Log.i(TAG, "SDK状态：HTTP认证结果" + code);
                break;
            default:
                isInit.set(false);
                Log.w(TAG, "SDK状态：其他错误" + code);
        }
    };

    /**
     * 创建工作目录
     *
     * @param workDir 工作目录
     */
    private void createWorkDir(String workDir) {
        File file = new File(workDir);
        if (!file.exists()) {
            boolean success = file.mkdirs();
            Log.d(TAG, "创建工作目录" + (success ? "成功" : "失败"));
        }
    }

    private void writeKeyWord(String path, String keyWord) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(keyWord);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 写入音频数据
     *
     * @param data   音频数据
     * @param status 音频状态
     */
    public void writeAudioData(byte[] data, AiStatus status) {
        if (!canWriteEsr.get()) {
            IVWHelper.getInstance().write(data, status);
        } else {
            ESRHelper.getInstance().write(data, status);
        }
    }

    /**
     * 写入音频数据
     *
     * @param data   音频数据
     */
    public void writeAudioData(byte[] data) {
        if (!canWriteEsr.get()) {
            IVWHelper.getInstance().write(data, AiStatus.BEGIN);
        } else {
            ESRHelper.getInstance().write(data, AiStatus.BEGIN);
        }
    }

    /**
     * 设置语音唤醒监听回调
     *
     * @param callback
     */
    public void setIVWCallback(RecognitionCallback callback) {
        IVWHelper.getInstance().setCallback(callback);
    }

    /**
     * 设置语音识别监听回调
     *
     * @param callback
     */
    public void setEsrCallback(RecognitionCallback callback) {
        ESRHelper.getInstance().setCallback(callback);
    }

    /**
     * 获取授权结果
     *
     * @return
     */
    public int getAuthResult() {
        return authResult;
    }

    /**
     * 获取语言
     *
     * @return
     */
    public int getLanguage() {
        return sdkParam.getLanguage();
    }

    /**
     * 获取唤醒词
     *
     * @return
     */
    public String getKeyWord() {
        return sdkParam.getKeyWord();
    }

    /**
     * 获取工作目录
     *
     * @return
     */
    public String getWorkDir() {
        return sdkParam.getWorkDir();
    }

    /**
     * 是否初始化成功
     *
     * @return
     */
    public boolean isInit() {
        return isInit.get();
    }

    /**
     * 设置是否写入ESR
     *
     * @param canWriteEsr
     */
    public void setCanWriteEsr(boolean canWriteEsr) {
        this.canWriteEsr.set(canWriteEsr);
    }

    /**
     * 释放SDK
     */
    public void unInit() {
        isInit.set(false);
        IVWHelper.getInstance().unInit();
        ESRHelper.getInstance().unInit();
        AiHelper.getInst().unInit();
    }

    /**
     * 初始化语音唤醒
     */
    private void initIVW() {
        IVWHelper.getInstance().init(context);
        IVWHelper.getInstance().registerListener();
        IVWHelper.getInstance().start();
    }

    /**
     * 初始化语音识别
     */
    private void initESR() {
        ESRHelper.getInstance().init(context);
        ESRHelper.getInstance().registerListener();
        ESRHelper.getInstance().initEngine();
        ESRHelper.getInstance().start();
    }

}
