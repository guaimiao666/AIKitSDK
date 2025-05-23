package com.example.aikitsdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.aikit.core.AiAudio;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiStatus;
import com.iflytek.aikit.core.FucUtil;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class ESRHelper implements Helper{
    private final String TAG = "ESRHelper";
    private Context context;
    private final String ABILITYID = "e75f07b62";
    private RecognitionCallback callback;
    private boolean engineInit;
    private boolean isLoadData;
    private int index = 0;
    private AiHandle aiHandle;
    private boolean bReady = false;

    public static ESRHelper getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final ESRHelper INSTANCE = new ESRHelper();
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

    @Override
    public void start() {
//        unLoadData();
        if (!isLoadData) {//加载个性化资源至缓存。个性化资源如果没有变动的话，仅需加载一次，不用每次start都去加载。如果变动了则需要先卸载(调用unLoadData)，再重新加载
            /*****
             * FSA:fsa命令词文件
             */
            String filePath = FucUtil.getFilePath(context, "fsa.txt");
            if(TextUtils.isEmpty(filePath)){
                Log.w(TAG, "未找到语音识别文件!");
                return;
            }
            AiRequest.Builder customBuilder = AiRequest.builder();
            customBuilder.customText("FSA", filePath, index);
            int ret = AiHelper.getInst().loadData(ABILITYID, customBuilder.build());
            if (ret != 0) {
                Log.w(TAG, "加载自定义识别文件失败! ret:" + ret);
                return;
            }
            isLoadData = true;
        }
        int ret = 0;
        int[] indexs = {index};
        ret = AiHelper.getInst().specifyDataSet(ABILITYID, "FSA", indexs);//从缓存中把个性化资源设置到引擎中
        if (ret != 0) {
            Log.w(TAG, "加载自定义识别文件失败! ret:" + ret);
            return;
        }
        AiRequest.Builder paramBuilder = AiRequest.builder();
        paramBuilder.param("languageType", AIKitSDK.getInstance().getLanguage());//0:中文, 1:英文
        paramBuilder.param("vadEndGap", AIKitSDK.getInstance().getLanguage() == 0 ? 60 : 75);//子句分割时间间隔，中文建议60，英文建议75
        paramBuilder.param("vadOn", true);//vad开关
        paramBuilder.param("beamThreshold", AIKitSDK.getInstance().getLanguage() == 0 ? 20 : 25);//解码控制beam的阈值，中文建议20，英文建议25
        paramBuilder.param("hisGramThreshold", 3000);//解码Gram阈值，建议值3000
        paramBuilder.param("vadLinkOn", false);//vad子句连接开关
        paramBuilder.param("vadSpeechEnd", 80);//vad后端点
        paramBuilder.param("vadResponsetime", 1000);//vad前端点
        paramBuilder.param("postprocOn", false);//后处理开关
        aiHandle = AiHelper.getInst().start(ABILITYID, paramBuilder.build(), null);
        if (aiHandle.getCode() != 0) {
            Log.w(TAG, "创建语音识别会话失败!");
            return;
        }
        bReady = true;
    }

    @Override
    public void end() {
        if (aiHandle != null) {
            AiHelper.getInst().end(aiHandle);
            bReady = false;
            aiHandle = null;
        }
    }

    @Override
    public void write(byte[] data, AiStatus status) {
        if (!bReady) {
            Log.w(TAG, "语音识别能力未准备完成!");
            return;
        }
        AiRequest.Builder dataBuilder = AiRequest.builder();
        int ret = 0;
        /**
         * 送入音频需要标识音频的状态，第一帧为起始帧，status要传AiStatus.BEGIN,最后一帧为结束帧，status要传AiStatus.END,其他为中间帧，status要传AiStatus.CONTINUE
         * 音频要求16bit，16K，单声道的pcm音频。
         * 建议每次发送音频间隔40ms，每次发送音频字节数为一帧音频大小的整数倍。
         */
        AiAudio aiAudio = AiAudio.get("audio").data(data).status(status).valid();
        dataBuilder.payload(aiAudio);

        ret = AiHelper.getInst().write(dataBuilder.build(), aiHandle);
        if (ret == 0) {
            ret = AiHelper.getInst().read(ABILITYID, aiHandle);
            if (ret != 0) {
                Log.w(TAG, "read失败! ret:" + ret);
            }
        } else {
            Log.w(TAG, "write失败! ret:" + ret);
        }
    }

    @Override
    public void unInit() {
        unLoadData();
        unInitEngine();
        end();
    }

    /**
     * 能力监听回调
     */
    private AiListener edListener = new AiListener() {
        @Override
        public void onResult(int handleID, List<AiResponse> outputData, Object usrContext) {
            if (null != outputData && !outputData.isEmpty()) {
                Log.i(TAG, "onResult:handleID:" + handleID + ":" + outputData.size() + "," +
                        "usrContext:" + usrContext);
                for (int i = 0; i < outputData.size(); i++) {
                    Log.d(TAG, "onResult:handleID:" + handleID + ":" + outputData.get(i).getKey());
                    String result = null;
                    /**
                     * key的取值以及含义
                     * pgs:progressive格式的结果，即可以实时刷屏
                     * htk:带有分词信息的结果，每一个分词结果占一行
                     * plain:类比于htk，把一句话结果中的所有分词拼成完整一句，若有后处理，则也含有后处理的结果信息，plain是每一段话的最终结果
                     * vad:语音端点检测结果(需要打开vad功能才会返回)bg:前端点，ed:后端点。单位:帧(10ms)
                     * readable:json格式的结果。
                     */
                    String key = outputData.get(i).getKey();   //引擎结果的key
                    byte[] bytes = outputData.get(i).getValue(); //识别结果
                    try {
                        result = new String(bytes, "GBK");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, key + "： " + result);
                    if (key.contains("plain") && callback != null) {
                        callback.onResult(result);
                    }
                }
                if (outputData.get(0).getStatus() == 2) {
                    //这里结束会话
                }
            }
        }

        @Override
        public void onEvent(int handleID, int event, List<AiResponse> eventData, Object o) {
            Log.i(TAG, "onEvent:" + handleID + ",event:" + event);
            if (callback != null) {
                callback.onEvent(event);
            }
            //未知错误 结束 超时
            if (event == 0 || event == 3 ||event == 2) {
                AIKitSDK.getInstance().setCanWriteEsr(false);
                end();
            }
        }

        @Override
        public void onError(int handlerId, int errorCode, String msg, Object o) {
            Log.w(TAG, "错误通知，能力执行终止,Ability " + handlerId + " ERROR::" + msg + ",err code:" + errorCode);
            if (callback != null) {
                callback.onError(errorCode, msg);
            }
            AIKitSDK.getInstance().setCanWriteEsr(false);
            end();
        }
    };

    /**
     * 卸载资源
     */
    private void unLoadData() {
        int ret = 0;
        if (isLoadData) {
            ret = AiHelper.getInst().unLoadData(ABILITYID, "FSA", index);
            if (ret != 0) {
                Log.w(TAG, "卸载资源失败! ret:" + ret);
            } else {
                Log.i(TAG, "卸载资源成功! ret:" + ret);
                isLoadData = false;
            }
        }
    }


    /**
     * 引擎初始化
     */
    public void initEngine() {
        int ret = 0;
        if (!engineInit) {
            AiRequest.Builder engineBuilder = AiRequest.builder();
            engineBuilder.param("decNetType", "fsa");
            engineBuilder.param("punishCoefficient", 0.0);
            engineBuilder.param("wfst_addType", AIKitSDK.getInstance().getLanguage()); // 0中文，1英文
            ret = AiHelper.getInst().engineInit(ABILITYID, engineBuilder.build());
            if (ret != 0) {
                Log.w(TAG, "引擎初始化失败! ret:" + ret);
                return;
            }
            Log.i(TAG, "引擎初始化成功! ret:" + ret);
            engineInit = true;
        }
    }

    /**
     * 引擎卸载
     */
    public void unInitEngine() {
        int ret = 0;
        if (engineInit) {
            ret = AiHelper.getInst().engineUnInit(ABILITYID);
            if (ret != 0) {
                Log.w(TAG, "引擎卸载失败! ret:" + ret);
                return;
            }
            Log.i(TAG, "引擎卸载成功! ret:" + ret);
            engineInit = false;
        }
    }
}
