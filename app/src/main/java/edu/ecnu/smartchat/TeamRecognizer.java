package edu.ecnu.smartchat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.core.app.ActivityCompat;

import com.iflytek.aikit.core.AiAudio;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiStatus;
import com.iflytek.aikit.core.BaseLibrary;
import com.iflytek.aikit.core.CoreListener;
import com.iflytek.aikit.core.ErrType;
import com.iflytek.aikit.core.LogLvl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.ecnu.smartchat.base.MyLog;


/**
 * @brief 语音识别球队的识别器
 * <p>
 * 基于讯飞AIKit离线语音唤醒SDK，实现语音唤醒功能。
 * 支持唤醒词："法国球队"、"摩洛哥球队"、"西班牙球队"、"巴西球队"、"比利时球队"、"英格兰球队"
 * <p>
 * 调用流程：
 * 1. initialize() - 初始化SDK并加载唤醒词资源
 * 2. recognizeStart() - 开始录音并持续检测唤醒词
 * 3. onRecognizeResult()回调 - 收到唤醒结果
 * 4. recognizeStop() - 停止录音和检测
 * 5. release() - 释放SDK资源
 */
public class TeamRecognizer {

    public interface IInitCallback {
        /**
         * @brief 初始化完成回调
         * @param errCode 错误代码，0表示初始化成功
         */
        void onRecognizerInitDone(int errCode);
    }

    public interface IRecognizeCallback {
        /**
         * @brief 识别结果回调
         * @param outputData 识别到的信息
         */
        void onRecognizeResult(List<AiResponse> outputData);
    }


    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// Constant Definition ////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    private static final String TAG = "[AiBasic]/TeamRecog";

    /**
     * 讯飞AIKit应用凭证（需替换为实际值）
     */
    private static final String appId = "60bdf218";
    private static final String apiKey = "cbb964d54600f124c8adbdb75c68d3e0";
    private static final String apiSecret = "Y2ZkNjIzYTcwNDgwYjBkOGM0MWVjNGZi";

    /**
     * 能力ID（离线语音唤醒）
     */
    private static final String ABILITY_IVW = "e867a88f2";

    /**
     * SDK工作目录
     */
    private static final String WORK_DIR = "/sdcard/iflytek/";
    private static final String RES_DIR = "/sdcard/iflytek/ivw";

    /**
     * 唤醒词文件名称
     */
    private static final String KEYWORD_FILE_NAME = "keyword.txt";


    /**
     * 音频参数 - PCM 16kHz 单声道 16bit
     */
    private final int BUFFER_SIZE = 1280;

    /**
     * Handler消息常量
     */
    private static final int MSG_START = 1;
    private static final int MSG_WRITE = 2;

    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// Variable Definition ////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    private Context mContext;
    private AiHandle mAiHandle;
    private volatile boolean mIsInitialized = false;
    private Thread mRecordThread;
    private AudioRecord mAudioRecord;
    private IRecognizeCallback mRecognizeCallback;
    private IInitCallback mInitCallback;
    private boolean mFirstFrame = true;
    private int mFrameCount = 0;  // 诊断用：帧计数
    private AtomicBoolean mIsRecognizing = new AtomicBoolean(false);  // 是否正在识别中
    private Handler mRecordHandler;
    private String mPendingWakeupWords;


    /**
     * 鉴权监听回调（使用 CoreListener，与 Demo 一致）
     */
    private CoreListener mCoreListener = new CoreListener() {
        @Override
        public void onAuthStateChange(ErrType errType, int code) {
            MyLog.d(TAG, "<onAuthStateChange> errType=" + errType + ", code=" + code);
            switch (errType) {
                case AUTH:
                    MyLog.d(TAG, "<onAuthStateChange> SDK状态：授权结果码" + code);
                    if (code == 0) {
                        mIsInitialized = true;
                    }
                    if (mInitCallback != null) {
                        mInitCallback.onRecognizerInitDone(code);
                    }
                    break;
                case HTTP:
                    MyLog.d(TAG, "<onAuthStateChange> SDK状态：HTTP认证结果" + code);
                    if (code != 0 && mInitCallback != null) {
                        mInitCallback.onRecognizerInitDone(code);
                    }
                    break;
                default:
                    MyLog.d(TAG, "<onAuthStateChange> SDK状态：其他错误");
                    break;
            }
        }
    };


    /**
     * 能力监听回调
     */
    private AiListener mAiRespListener = new AiListener() {
        @Override
        public void onResult(int handleID, List<AiResponse> outputData, Object usrContext) {
            if (outputData == null || outputData.size() <= 0) {
                MyLog.d(TAG, "<onResult> outputData is null");
                return;
            }
            MyLog.d(TAG, "<onResult> handleID=" + handleID + ", outputData.size=" + outputData.size());

            for (int i = 0; i < outputData.size(); i++) {
                byte[] bytes = outputData.get(i).getValue();
                if (bytes == null) {
                    continue;
                }
                String key = outputData.get(i).getKey();
                MyLog.d(TAG, "<onResult> key=" + key + ", valueLen=" + bytes.length);

            }

            // 回调识别结果给上层
            if (mRecognizeCallback != null) {
                mRecognizeCallback.onRecognizeResult(outputData);
            }
        }

        @Override
        public void onEvent(int handleID, int event, List<AiResponse> eventData, Object usrContext) {
            MyLog.d(TAG, "<onEvent> handleID=" + handleID + ", event=" + event);
            // event: 0=未知错误, 1=开始, 2=结束, 3=超时, 4=进行中
        }

        @Override
        public void onError(int handleID, int err, String msg, Object usrContext) {
            MyLog.d(TAG, "<onError> handleID=" + handleID + ", err=" + err + ", msg=" + msg);
        }
    };


    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// Public Methods ////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * @brief 初始化识别器，初始化AIKit SDK并加载唤醒词资源
     * @param ctx 上下文
     * @param initCallback 初始化回调
     * @return true表示初始化流程已启动
     */
    public boolean initialize(final Context ctx, final IInitCallback initCallback) {
        this.mContext = ctx;
        this.mInitCallback = initCallback;


        // 配置SDK日志
        AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, WORK_DIR + "/aeeLog.txt");

        // 注册SDK核心状态监听
        AiHelper.getInst().registerListener(mCoreListener);


        // 初始化参数构建（使用 BaseLibrary.Params，与 Demo 一致）
        BaseLibrary.Params params = BaseLibrary.Params.builder()
                .appId(appId)
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .workDir(WORK_DIR)
                .build();

        // 初始化SDK（在新线程中调用 initEntry，与 Demo 一致）
        final Context appCtx = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                AiHelper.getInst().initEntry(appCtx, params);
            }
        }).start();
        MyLog.d(TAG, "<initialize> SDK init called, waiting for auth callback");

        MyLog.d(TAG, "<initialize> done");
        return true;
    }


    /**
     * @brief 释放识别器，停止识别并释放SDK资源
     */
    public void release() {
        // 先停止识别
        recognizeStop();

        // 逆初始化SDK
        AiHelper.getInst().unInit();
        mIsInitialized = false;
        mContext = null;
        mInitCallback = null;
        mRecognizeCallback = null;

        MyLog.d(TAG, "<release> done");
    }

    /**
     * @brief 开始识别：注册能力监听、加载唤醒词、创建会话、启动录音
     * <p>
     * 使用带 Looper 的 Handler 线程处理音频帧，与 SDK Demo 一致。
     * SDK 的 AiListener 回调依赖调用 write() 线程的 Looper 来分发，
     * 如果线程没有 Looper，onResult/onEvent/onError 回调将无法触发。
     *
     * @param wakeupWords      唤醒词组合，以逗号分隔，例如 "法国球队,西班牙球队,摩洛哥球队"
     * @param recognizeCallback 识别结果回调
     */
    public void recognizeStart(final String wakeupWords, final IRecognizeCallback recognizeCallback) {
        if (mIsRecognizing.get()) {
            MyLog.d(TAG, "<recognizeStart> already recognizing, skip");
            return;
        }
        this.mRecognizeCallback = recognizeCallback;
        this.mPendingWakeupWords = wakeupWords;
        mIsRecognizing.set(true);
        mFrameCount = 0;  // 重置帧计数

        // 注册能力结果监听（必须在start之前注册，否则SDK回调无法触发）
        AiHelper.getInst().registerListener(ABILITY_IVW, mAiRespListener);

        // 启动带 Looper 的录音线程（loadData/start/write 均在此线程执行）
        mRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mRecordHandler = new Handler(Looper.myLooper()) {
                    @Override
                    public void handleMessage(android.os.Message msg) {
                        switch (msg.what) {
                            case MSG_START:
                                handleStart();
                                break;
                            case MSG_WRITE:
                                handleWrite((AiStatus) msg.obj);
                                break;
                        }
                    }
                };
                mRecordHandler.sendEmptyMessage(MSG_START);
                Looper.loop();
                MyLog.d(TAG, "<recordThread> looper quit");
            }
        }, "TeamRecognizer_AudioRecord");
        mRecordThread.start();

        MyLog.d(TAG, "<recognizeStart> done");
    }


    /**
     * @brief 停止识别：停止录音、结束会话
     * <p>
     * 设置 mIsRecognizing=false 后，Handler 线程在处理下一帧时会检测到，
     * 自动写入 END 帧、停止录音、结束会话、退出 Looper。
     */
    public void recognizeStop() {
        MyLog.d(TAG, "<recognizeStop>");
        mIsRecognizing.set(false);

        // 等待录音线程结束（Handler 线程会自动退出 Looper）
        if (mRecordThread != null) {
            try {
                mRecordThread.join(3000);
            } catch (InterruptedException e) {
                MyLog.d(TAG, "<recognizeStop> join recordThread interrupted");
            }
            mRecordThread = null;
        }
        mRecordHandler = null;

        // 兜底：确保会话已结束
        if (mAiHandle != null && mAiHandle.isSuccess()) {
            int ret = AiHelper.getInst().end(mAiHandle);
            MyLog.d(TAG, "<recognizeStop> fallback end session, ret=" + ret);
            mAiHandle = null;
        }

        mRecognizeCallback = null;
        MyLog.d(TAG, "<recognizeStop> done");
    }


    /**
     * @brief 获取当前是否正在识别中
     * @return true表示正在识别
     */
    public boolean isRecognizing() {
        return mIsRecognizing.get();
    }

    /**
     * @brief 获取SDK是否已初始化成功
     * @return true表示已初始化
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }


    ////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// Private Methods ///////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////


    /**
     * Handler 线程处理 START 消息：加载唤醒词、创建会话、启动录音、发送第一帧
     * <p>
     * 此方法在带 Looper 的 Handler 线程上执行，与 SDK Demo 的 start() 等价。
     */
    private void handleStart() {
        // 将唤醒词组合写入文件
        keyword2File(mPendingWakeupWords);

        // 加载唤醒词个性化数据
        String keywordPath = RES_DIR + "/" + KEYWORD_FILE_NAME;
        AiRequest.Builder customBuilder = AiRequest.builder();
        customBuilder.customText("key_word", keywordPath, 0);
        int ret = AiHelper.getInst().loadData(ABILITY_IVW, customBuilder.build());
        if (ret != 0) {
            MyLog.d(TAG, "<handleStart> loadData failed, ret=" + ret + ", keywordPath=" + keywordPath);
            mIsRecognizing.set(false);
            return;
        }
        MyLog.d(TAG, "<handleStart> loadData success");

        // 指定使用的个性化数据集合
        int[] indexs = {0};
        ret = AiHelper.getInst().specifyDataSet(ABILITY_IVW, "key_word", indexs);
        if (ret != 0) {
            MyLog.d(TAG, "<handleStart> specifyDataSet failed, ret=" + ret);
            mIsRecognizing.set(false);
            return;
        }
        MyLog.d(TAG, "<handleStart> specifyDataSet success");

        // 创建会话
        AiRequest.Builder paramBuilder = AiRequest.builder();
        paramBuilder.param("wdec_param_nCmThreshold", "0 0:800");
        paramBuilder.param("gramLoad", true);
        mAiHandle = AiHelper.getInst().start(ABILITY_IVW, paramBuilder.build(), null);
        if (!mAiHandle.isSuccess()) {
            MyLog.d(TAG, "<handleStart> start failed, code=" + mAiHandle.getCode());
            mIsRecognizing.set(false);
            return;
        }
        MyLog.d(TAG, "<handleStart> session started");

        // 创建录音器并开始录音
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            MyLog.d(TAG, "<handleStart> permission denied");
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            MyLog.d(TAG, "<handleStart> AudioRecord init failed");
            mIsRecognizing.set(false);
            return;
        }
        mAudioRecord.startRecording();
        MyLog.d(TAG, "<handleStart> audio recording started");

        // 发送第一帧（BEGIN）
        android.os.Message msg = new android.os.Message();
        msg.what = MSG_WRITE;
        msg.obj = AiStatus.BEGIN;
        mRecordHandler.sendMessage(msg);
    }

    /**
     * Handler 线程处理 WRITE 消息：读取一帧音频并送入 SDK
     * <p>
     * 每次只处理一帧，处理完后通过消息队列发送下一帧，与 SDK Demo 的 Handler 模式一致。
     * 当 mIsRecognizing 变为 false 时，写入 END 帧、停止录音、结束会话、退出 Looper。
     */
    private void handleWrite(AiStatus status) {
        byte[] data = new byte[BUFFER_SIZE];
        int read = mAudioRecord.read(data, 0, BUFFER_SIZE);
        mFrameCount++;

        // 诊断：前3帧和每100帧打印一次音频数据状态
        if (mFrameCount <= 3 || mFrameCount % 100 == 0) {
            boolean allZero = true;
            int checkLen = Math.min(20, read > 0 ? read : 0);
            for (int i = 0; i < checkLen; i++) {
                if (data[i] != 0) {
                    allZero = false;
                    break;
                }
            }
            MyLog.d(TAG, "<handleWrite> frame=" + mFrameCount + ", read=" + read + ", allZero=" + allZero + ", status=" + status);
        }

        // 检查是否已停止，若是则将状态改为 END
        if (!mIsRecognizing.get()) {
            status = AiStatus.END;
        }

        if (AudioRecord.ERROR_INVALID_OPERATION != read && read > 0) {
            writeAudioData(data, status);
        }

        if (mIsRecognizing.get()) {
            // 继续发送下一帧
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_WRITE;
            msg.obj = AiStatus.CONTINUE;
            mRecordHandler.sendMessage(msg);
        } else {
            // 停止录音
            try {
                mAudioRecord.stop();
                mAudioRecord.release();
            } catch (Exception e) {
                MyLog.d(TAG, "<handleWrite> release AudioRecord error: " + e.getMessage());
            }
            mAudioRecord = null;
            MyLog.d(TAG, "<handleWrite> audio recording stopped");

            // 结束会话
            if (mAiHandle != null && mAiHandle.isSuccess()) {
                int ret = AiHelper.getInst().end(mAiHandle);
                MyLog.d(TAG, "<handleWrite> session ended, ret=" + ret);
                mAiHandle = null;
            }

            // 退出 Looper，线程结束
            Looper.myLooper().quit();
        }
    }

    /**
     * @breif 写入一帧音频数据到SDK
     */
    private  void writeAudioData(byte[] part, AiStatus status) {
        AiRequest.Builder dataBuilder = AiRequest.builder();
        int ret = 0;

        /**
         * 送入音频需要标识音频的状态，第一帧为起始帧，status要传AiStatus.BEGIN,最后一帧为结束帧，status要传AiStatus.END,其他为中间帧，status要传AiStatus.CONTINUE
         * 音频要求16bit，16K，单声道的pcm音频。
         * 建议每次发送音频间隔40ms，每次发送音频字节数为一帧音频大小的整数倍。
         */
        AiAudio aiAudio = AiAudio.get("wav").data(part).status(status).valid();
        dataBuilder.payload(aiAudio);

        ret = AiHelper.getInst().write(dataBuilder.build(), mAiHandle);
        if (ret != 0) {
            MyLog.d(TAG, "<writeAudioData> ret=" + ret + ", status=" + status);
        } else if (mFrameCount <= 3) {
            MyLog.d(TAG, "<writeAudioData> ret=0 (success), status=" + status + ", frame=" + mFrameCount);
        }
    }



    /**
     * @brief 将唤醒词写入相应的文件中
     * @param wakeupWords: 唤醒词列表，以逗号分隔，例如 "法国球队,西班牙球队,摩洛哥球队"
     * @return false: 写入失败，true: 写入成功
     */
    private boolean keyword2File(final String wakeupWords) {
        try {
            File keywordFile = new File(RES_DIR + "/keyword.txt");
            if (keywordFile.exists()) {
                //强制清空内容
                keywordFile.delete();
            }
            File binFile = new File(RES_DIR + "/keyword.bin");
            if (binFile.exists()) {
                binFile.delete();
            }
            String temp = wakeupWords;
            if (temp.isEmpty()) {
                temp = "任意球队";
            }
            String str = temp.replace("，", ",");
            String[] keywords = str.split(",");
            if (!keywordFile.exists()) {
                keywordFile.createNewFile();
            }
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(keywordFile),
                    "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            for (int i = 0; i < keywords.length; i++) {
                bufferedWriter.write(keywords[i]);
                bufferedWriter.write(";");
                bufferedWriter.newLine();//写入换行
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        MyLog.d(TAG, "<keyword2File> write to file success");
        return true;
    }
}
