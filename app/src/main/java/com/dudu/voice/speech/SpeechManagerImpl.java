package com.dudu.voice.speech;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.IMergeRule;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Util;
import com.aispeech.export.engines.AILocalGrammarEngine;
import com.aispeech.export.engines.AILocalTTSEngine;
import com.aispeech.export.engines.AILocalWakeupDnnEngine;
import com.aispeech.export.engines.AIMixASREngine;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIAuthListener;
import com.aispeech.export.listeners.AILocalGrammarListener;
import com.aispeech.export.listeners.AILocalWakeupDnnListener;
import com.aispeech.export.listeners.AITTSListener;
import com.aispeech.speech.AIAuthEngine;
import com.dudu.aios.ui.voice.VoiceEvent;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.commonlib.CommonLib;
import com.dudu.voice.BaseVoiceManager;
import com.dudu.voice.FloatWindowUtils;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.dudu.voice.semantic.engine.SemanticReminder;
import com.dudu.voice.semantic.parser.SpeechJsonParser;
import com.dudu.voice.window.MessageType;
import com.dudu.workflow.driving.CarStatusUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;


/**
 * Created by 赵圣琪 on 2015/10/27.
 */
public class SpeechManagerImpl extends BaseVoiceManager {

    private static final String TTS_FILE_NAME = "zhilingf.v0.4.23.bin";
    private static final int UNDERSTAND_TIMEOUT = 25 * 1000;
    public Subscription reAuthSub;
    private AILocalWakeupDnnEngine mWakeupEngine;
    private AIAuthEngine mAuthEngine;
    private AILocalGrammarEngine mGrammarEngine;
    private AIMixASREngine mAsrEngine;

    private AILocalTTSEngine mTTSEngine;
    private volatile boolean mSpeaking = false;
    private BlockingQueue<TTSTask> mTTSQueue;

    private boolean waitwakeup = false;

    private boolean isUnderstandTimeOut = true;

    private SemanticReminder semanticReminder;

    private HandlerThread mAsrThread;
    private AsrHandler mAsrHandler;
    private HandlerThread mSpeechInitThread;
    private InitHandler mInitHandler;
    private Subscription changeCanUnderstandSub;

    private long upDateGrammerTimer;

    public SpeechManagerImpl() {
        mTTSQueue = new ArrayBlockingQueue<>(500, true);
        semanticReminder = new SemanticReminder();
        mAsrThread = new HandlerThread("mAsrThread");
        mAsrThread.start();
        mAsrHandler = new AsrHandler(mAsrThread.getLooper());
        mSpeechInitThread = new HandlerThread("mSpeechInitThread");
        mSpeechInitThread.start();
        mInitHandler = new InitHandler(mSpeechInitThread.getLooper());
    }

    private Runnable understandTimeoutRunnable = () -> {
        if (isUnderstandTimeOut) {
            log.debug("识别超时");
            stopUnderstanding();
            clearMisUnderstandCount();
            mTTSQueue.clear();
            reminderSpeak(SemanticReminder.ReminderType.UNDERSTAND_MISUNDERSTAND, true);
        }
    };

    private void initWakeupEngine() {

        mInitHandler.sendEmptyMessage(InitHandler.INIT_WAKEUPENGINE);
    }

    private void initAuthEngine() {
        mInitHandler.sendEmptyMessage(InitHandler.INIT_AUTH);
    }

    /**
     * 初始化资源编译引擎
     */
    private void initGrammarEngine() {
        mInitHandler.sendEmptyMessage(InitHandler.INIT_GRAMMARENGINE);
    }

    private void initAsrEngine() {
        mInitHandler.sendEmptyMessage(InitHandler.INIT_ASRENGINE);
    }

    private void initTTSEngine() {
        mInitHandler.sendEmptyMessage(InitHandler.INIT_TTSENGINE);
    }

    private void importLocalGrammar() {
        mInitHandler.sendEmptyMessage(InitHandler.IMPORT_GRAMMAR);
    }

    @Override
    public void onInit() {

        mInitHandler.sendEmptyMessage(InitHandler.INIT_ALL);

        if (initStep == 0 && reInitSub != null) {
            reInitSub.unsubscribe();
            reInitSub = null;
        }
    }


    private void initOther() {

        initWakeupEngine();

        initGrammarEngine();

        initTTSEngine();

    }


    @Override
    public void startWakeup() {
        if (CarStatusUtils.isSpeechSleeped()) {
            return;
        }
        mAsrHandler.sendEmptyMessage(AsrHandler.START_WAKEUP);
    }

    @Override
    public void stopWakeup() {
        mAsrHandler.sendEmptyMessage(AsrHandler.STOP_WAKEUP);
    }

    @Override
    public void startUnderstanding() {
        mAsrHandler.sendEmptyMessage(AsrHandler.START_UNDERSTAND);
        understandTimeout();
    }

    @Override
    public void stopUnderstanding() {
        mAsrHandler.sendEmptyMessage(AsrHandler.STOP_UNDERSTAND);
    }

    @Override
    public void startSpeaking(String playText, TTSType type, boolean showMessage) {

        log.trace("startSpeaking  type {}", type);
        if (checkMisUnderstandCount()) {
            mTTSQueue.clear();
            type = TTSType.TTS_DO_NOTHING;
            playText = Constants.UNDERSTAND_EXIT;
        }

        if (showMessage) {
            FloatWindowUtils.getInstance().showMessage(playText, MessageType.MESSAGE_INPUT);
        }

        if (mTTSEngine != null) {
            startSpeakingWithQueue(playText, type);
        }

    }

    private void startSpeakingWithQueue(String playText, TTSType type) {

        if (mSpeaking) {
            mTTSQueue.add(new TTSTask(playText, type));
            return;
        }
        mType = type;
        mSpeaking = true;
        mInitHandler.sendMessage(mInitHandler.obtainMessage(InitHandler.START_TTS, playText));
    }

    private void speakQueueNext() {
        if (mTTSQueue.isEmpty()) {
            return;
        }

        TTSTask task = mTTSQueue.poll();
        if (task != null) {
            mSpeaking = true;

            mType = task.type;
            mInitHandler.sendMessage(mInitHandler.obtainMessage(InitHandler.START_TTS, task.playText));
        }
    }

    @Override
    public void stopSpeaking() {
        if (mTTSEngine != null) {
            mSpeaking = false;
            mTTSQueue.clear();
            mInitHandler.sendEmptyMessage(InitHandler.STOP_TTS);
        }
    }

    @Override
    public void onStop() {

        log.debug("onStop 停止语义理解，开启唤醒监听...");
        isUnderstandTimeOut = false;
        clearMisUnderstandCount();
        stopUnderstanding();
        mAsrHandler.sendEmptyMessage(AsrHandler.CANCEL_UNDERSTAND);
        startWakeup();
        if (mHandler != null && understandTimeoutRunnable != null) {
            mHandler.removeCallbacks(understandTimeoutRunnable);
        }
        if (changeCanUnderstandSub != null) {
            changeCanUnderstandSub.unsubscribe();
        }
        canStartUnderstand = false;
        changeCanUnderstandSub = Observable.timer(1000, TimeUnit.MILLISECONDS).subscribe(aLong -> {
            canStartUnderstand = true;
        }, throwable -> log.error("onStop", throwable));
        if (BtPhoneUtils.isSyncBt) {
            log.debug("is understanding ,initAsr when stop ");
            initAsrEngine();
            BtPhoneUtils.isSyncBt = false;
        }
    }

    @Override
    public void onDestroy() {

        mInitHandler.sendEmptyMessage(InitHandler.DESTROY_ALL);
    }

    @Override
    public void updateNativeGrammar() {
        log.debug("更新语法文件...canupdate {}", isCanUpdateGrammar());
        if (isInitOver && isCanUpdateGrammar()) {
            upDateGrammerTimer = System.currentTimeMillis();
            importLocalGrammar();
        }
    }

    private boolean isCanUpdateGrammar() {
        if (System.currentTimeMillis() - upDateGrammerTimer > 60 * 1000) {
            return true;
        }
        return false;
    }

    @Override
    public void reminderSpeak(SemanticReminder.ReminderType type, boolean showMessage) {
        log.debug("reminderSpeak showMessage:{}", showMessage);
        mTTSQueue.clear();
        startSpeaking(semanticReminder.getReminder(type), TTSType.TTS_START_UNDERSTANDING, showMessage);
    }

    private static class TTSTask {
        String playText;

        TTSType type;

        public TTSTask(String playText, TTSType type) {
            this.playText = playText;
            this.type = type;
        }
    }

    private class AISpeechListenerImpl implements AILocalWakeupDnnListener {

        @Override
        public void onInit(int status) {
            if (status == AIConstant.OPT_SUCCESS) {
                log.debug("唤醒引擎初始化成功...initStep[{}]", initStep++);
                startWakeup();
            } else {
                log.error("唤醒引擎初始化失败...");
            }
        }

        @Override
        public void onError(AIError aiError) {
            log.error("wake up onError:{}", aiError.toString());

        }

        @Override
        public void onWakeup(String s, double v, String s1) {
            log.debug("唤醒成功...");
            log.debug("wakeup stop,asr start !");
            startVoiceService();
            waitwakeup = false;

        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onRecorderReleased() {
        }

        @Override
        public void onReadyForSpeech() {
            log.debug("wake up onReadyForSpeech");
            waitwakeup = true;
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
        }
    }


    /**
     * 语法编译引擎回调接口，用以接收相关事件
     */
    private class AILocalGrammarListenerImpl implements AILocalGrammarListener {

        @Override
        public void onError(AIError error) {
            log.error("资源生成发生错误 {}...", error.toString());
        }

        @Override
        public void onUpdateCompleted(String recordId, String path) {
            log.debug("语法文件更新完毕...initStep[{}]", initStep++);
            if (!BtPhoneUtils.isSyncBt) {
                initAsrEngine();
            }
        }

        @Override
        public void onInit(int status) {
            if (status == 0) {
                log.debug("本地编译引擎初始化成功...initStep[{}]", initStep++);
                if (new File(Util.getResourceDir(CommonLib.getInstance().getContext()) + File.separator + AILocalGrammarEngine.OUTPUT_NAME)
                        .exists()) {
                    log.debug("本地语法文件存在，直接开始初始化识别引擎...");
                    initAsrEngine();
                } else {
                    log.debug("本地语法文件不存在，导入语法文件...");
                    importLocalGrammar();
                }
            } else {
                log.error("资源定制引擎加载失败...");
            }
        }
    }

    /**
     * 本地识别引擎回调接口，用以接收相关事件
     */
    public class AIASRListenerImpl implements AIASRListener {

        @Override
        public void onBeginningOfSpeech() {
            log.debug("检测到用户开始说话...");
            if (mHandler != null && understandTimeoutRunnable != null) {
                mHandler.removeCallbacks(understandTimeoutRunnable);
            }
        }

        @Override
        public void onEndOfSpeech() {
            log.debug("检测到语音停止，开始识别...");
            isUnderstandTimeOut = true;
            understandTimeout();
        }

        @Override
        public void onReadyForSpeech() {
            log.debug("请说话...");
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onError(AIError error) {
            isUnderstandTimeOut = false;

            log.error("识别发生错误, errorId :{},waitwakeup: {}", error.getErrId(), waitwakeup);

            if (error.getErrId() == SpeechConstant.WAKEUP_ERROR) {
                log.error("麦克风被占用...");
            }

            stopUnderstanding();
            incrementMisUnderstandCount();

            if (waitwakeup) {
                startWakeup();
                return;
            }

            if (error.getErrId() == SpeechConstant.ERROR_NO_INPUT) {
                reminderSpeak(SemanticReminder.ReminderType.ERROR_NO_INPUT, true);
            } else {
                reminderSpeak(SemanticReminder.ReminderType.UNDERSTAND_MISUNDERSTAND, true);
            }
        }

        @Override
        public void onResults(AIResult results) {
            isUnderstandTimeOut = false;
            log.debug(results.getResultObject().toString());
            log.debug("语义识别 onResults waitwakeup {}", waitwakeup);

            stopUnderstanding();

            if (waitwakeup) {
                startWakeup();
                return;
            }

            SemanticEngine.getProcessor().processSemantic(SpeechJsonParser.getInstance().
                    parseSemanticJson(results.toString()));
        }

        @Override
        public void onInit(int status) {
            if (status == 0) {
                log.debug("混合识别引擎加载成功...initStep[{}]", initStep++);
                isInitOver = true;
            } else {
                log.error("混合识别引擎加载失败...");
                onDestroy();
                SpeechManagerImpl.this.onInit();
            }
        }

        @Override
        public void onRecorderReleased() {
            log.debug("识别引擎录音释放...");
        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }
    }

    private class AILocalTTSListenerImpl implements AITTSListener {

        @Override
        public void onInit(int status) {
            if (status == AIConstant.OPT_SUCCESS) {
                log.debug("语音合成初始化成功...initStep[{}]", initStep++);

            } else {
                log.error("初始化失败! code:" + status);
            }
        }

        @Override
        public void onProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished) {

        }

        @Override
        public void onError(String utteranceId, AIError error) {
            log.debug("语音合成 onError {}", error.toString());
        }

        @Override
        public void onReady(String utteranceId) {
            log.debug("TTS onReady");

        }

        @Override
        public void onCompletion(String utteranceId) {
            mSpeaking = false;
            log.debug("TTS onCompletion speak type:{},checkMisUnderstandCount {}", mType, checkMisUnderstandCount());
            switch (mType) {
                case TTS_DO_NOTHING:
                    if (checkMisUnderstandCount()) {
                        EventBus.getDefault().post(VoiceEvent.THRICE_UNSTUDIED);
                        FloatWindowUtils.getInstance().removeFloatWindow();
                    }
                    speakQueueNext();
                    break;
                case TTS_START_WAKEUP:
                    startWakeup();
                    break;
                case TTS_START_UNDERSTANDING:
                    log.debug("TTS onCompletion startUnderstanding waitWakeup :{},canStartUnderstand{}", waitwakeup, canStartUnderstand);
                    mTTSQueue.clear();
                    if (canStartUnderstand) {
                        startUnderstanding();
                    }
                    break;
            }
        }
    }

    private void understandTimeout() {
        if (mHandler != null && understandTimeoutRunnable != null) {
            mHandler.removeCallbacks(understandTimeoutRunnable);
        }
        mHandler.postDelayed(understandTimeoutRunnable, UNDERSTAND_TIMEOUT);
    }

    private class AsrHandler extends android.os.Handler {
        public static final int START_UNDERSTAND = 1;
        public static final int STOP_UNDERSTAND = 2;
        public static final int CANCEL_UNDERSTAND = 3;
        public static final int START_WAKEUP = 4;
        public static final int STOP_WAKEUP = 5;

        public AsrHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case START_UNDERSTAND:
                        handlerstartUnderstand();
                        break;
                    case STOP_UNDERSTAND:
                        handlerstopUnderstand();
                        break;
                    case CANCEL_UNDERSTAND:
                        handlercancelUnderstand();
                        break;
                    case START_WAKEUP:
                        handlerstartwakeup();
                        break;
                    case STOP_WAKEUP:
                        handlerstopwakeup();
                        break;

                }
            } catch (Exception e) {
                log.error("AsrHandler handleMessage error:{}", e);
            }

        }

        private void handlerstartUnderstand() {
            log.debug("startUnderstanding");
            if (mAsrEngine != null) {
                if (waitwakeup) {
                    log.debug("开启语音听写前，停止语音唤醒...");
                    handlerstopwakeup();
                }
                log.debug("开始语义理解...");
                mAsrEngine.start();
                isUnderstandTimeOut = true;
//                waitwakeup = false;
            } else {
                log.debug("startUnderstanding error mAsrEngine is null");
            }
        }

        private void handlerstopUnderstand() {
            if (mAsrEngine != null) {
                log.debug("结束语义理解...");
                mAsrEngine.stopRecording();
            }
        }

        private void handlercancelUnderstand() {
            if (mAsrEngine != null) {
                mAsrEngine.cancel();
            }
        }

        private void handlerstartwakeup() {
            if (mWakeupEngine != null) {
                log.debug("开始唤醒监听...");
                mWakeupEngine.start();
            }
        }

        private void handlerstopwakeup() {
            if (mWakeupEngine != null) {
                log.debug("停止唤醒监听...");
                mWakeupEngine.stop();
                waitwakeup = false;
            }
        }
    }

    private class InitHandler extends Handler {
        public static final int INIT_ALL = 0;
        public static final int INIT_AUTH = 1;
        public static final int INIT_GRAMMARENGINE = 2;
        public static final int INIT_TTSENGINE = 3;
        public static final int INIT_WAKEUPENGINE = 4;
        public static final int IMPORT_GRAMMAR = 5;
        public static final int INIT_ASRENGINE = 6;
        public static final int DESTROY_ALL = 7;
        public static final int START_TTS = 8;
        public static final int STOP_TTS = 9;

        public InitHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case INIT_ALL:
                    initAll();
                    break;
                case INIT_AUTH:
                    intAuth();
                    break;
                case INIT_ASRENGINE:
                    initAsr();
                    break;
                case INIT_GRAMMARENGINE:
                    initgrammar();
                    break;
                case INIT_TTSENGINE:
                    initTTS();
                    break;
                case INIT_WAKEUPENGINE:
                    initWakeup();
                    break;
                case IMPORT_GRAMMAR:
                    importGrammar();
                    break;
                case DESTROY_ALL:
                    destroyAll();
                    break;
                case START_TTS:
                    startTTS((String) msg.obj);
                    break;
                case STOP_TTS:
                    stopTTS();
                    break;
            }
        }

        private void initAll() {
            if (initStep > 0) {
                return;
            }
            log.debug("语音初始化...initStep[{}]", initStep++);

            isInitOver = false;
            initTime = System.currentTimeMillis();
            intAuth();
        }

        private void intAuth() {
            log.debug("初始化授权...initStep[{}]", initStep++);
            if (mContext != null) {
                mAuthEngine = AIAuthEngine.getInstance(mContext);
                try {
                    mAuthEngine.init(SpeechConstant.APPKEY, SpeechConstant.SECRETKEY, "444b-a34a-eafc-8f95");
                } catch (FileNotFoundException e) {
                    log.error("语音授权文件没有找到...");
                }

                if (!mAuthEngine.isAuthed()) {
                    mAuthEngine.setOnAuthListener(new AIAuthListener() {
                        @Override
                        public void onAuthSuccess() {

                            initOther();
                        }

                        @Override
                        public void onAuthFailed(String s) {
                            log.debug("授权失败 {}", s);
                            reAuthSub = Observable.timer(10, TimeUnit.SECONDS).subscribe(aLong -> {
                                doAuth();
                            }, throwable -> log.error("onAuthFailed: {}", throwable));
                        }
                    });
                    doAuth();
                } else {
                    initOther();
                }
            }
        }

        private void initgrammar() {
            log.debug("初始化本地识别引擎...initStep[{}]", initStep++);

            if (mGrammarEngine != null) {
                mGrammarEngine.destroy();
            }
            mGrammarEngine = AILocalGrammarEngine.createInstance();
            mGrammarEngine.setResFileName(SpeechConstant.ebnfc_res);
            if (mContext != null) {
                mGrammarEngine.init(mContext, new AILocalGrammarListenerImpl(), SpeechConstant.APPKEY,
                        SpeechConstant.SECRETKEY);
                mGrammarEngine.setDeviceId(Util.getIMEI(mContext));
            }
        }

        private void initTTS() {
            log.debug("初始化TTS引擎...initStep[{}]", initStep++);

            if (mTTSEngine != null) {
                mTTSEngine.destory();
            }

            mTTSEngine = AILocalTTSEngine.createInstance();
            mTTSEngine.setResource(TTS_FILE_NAME);
            mTTSEngine.setRealBack(true);
            mTTSEngine.setUseCahce(false, 20);
//            mTTSEngine.setSpeechVolume(60);
            if (mContext != null) {
                mTTSEngine.init(mContext, new AILocalTTSListenerImpl(), SpeechConstant.APPKEY, SpeechConstant.SECRETKEY);
                mTTSEngine.setLeftMargin(20);
                mTTSEngine.setRightMargin(20);
                mTTSEngine.setSpeechRate(0.85f);
            }
//        mTTSEngine.setSavePath("/sdcard/speech/"
//                + System.currentTimeMillis() + ".wav");
        }

        private void importGrammar() {
            log.debug("导入本地语法文件...initStep[{}]", initStep++);

            // 生成ebnf语法
            GrammarHelper gh = new GrammarHelper(mContext);
            String contactString = gh.getConatcts();
            String appString = gh.getApps();

            if (TextUtils.isEmpty(contactString)) {
                contactString = "无联系人";
            }

            String ebnf = gh.importAssets(contactString, appString, "grammar.xbnf");

            log.trace("voice local grammar :{}", ebnf);
            if (mGrammarEngine != null) {
                mGrammarEngine.setEbnf(ebnf);
                mGrammarEngine.update();
            }
        }

        private void initWakeup() {
            log.debug("初始化唤醒引擎...initStep[{}]", initStep++);

            mWakeupEngine = AILocalWakeupDnnEngine.createInstance();
//            mWakeupEngine.setEchoWavePath("/sdcard/speech/wakeup");
            mWakeupEngine.setResBin(SpeechConstant.wakeup_dnn_res);
            if (mContext != null) {
                mWakeupEngine.init(mContext, new AISpeechListenerImpl(),
                        SpeechConstant.APPKEY, SpeechConstant.SECRETKEY);
                mWakeupEngine.setStopOnWakeupSuccess(true);
                mWakeupEngine.setWords(new String[]{"hai yi wa"});
                mWakeupEngine.setDeviceId(Util.getIMEI(mContext));
                float[] threshold = {0.1f};
                mWakeupEngine.setThreshold(threshold);//设置阈值，越大越不容易唤醒
            }
        }


        private void initAsr() {
            log.debug("初始化混合识别引擎...initStep[{}]", initStep++);

            if (mAsrEngine != null) {
                mAsrEngine.destroy();
            }

//        AIConstant.setNewEchoEnable(true);//打开AEC，适用于aiengine2.7.4（包括2.7.4）之后的
//        AIConstant.setEchoCfgFile("aec.cfg");//设置AEC的配置文件
            if (CommonLib.getInstance().getVersionManage().isDemoVersionFlag()) {
                // demo 版本才开log
                AIConstant.openLog();
            }
            mAsrEngine = AIMixASREngine.createInstance();
            mAsrEngine.setResBin(SpeechConstant.ebnfr_res);
            mAsrEngine.setNetBin(AILocalGrammarEngine.OUTPUT_NAME, true);
            mAsrEngine.setVadResource(SpeechConstant.vad_res);
//        mAsrEngine.setServer("ws://s-test.api.aispeech.com:10000");
            mAsrEngine.setServer("ws://s.api.aispeech.com");
            mAsrEngine.setRes("aicar");
            mAsrEngine.setUseXbnfRec(true);
            mAsrEngine.setUsePinyin(true);
            mAsrEngine.setUseForceout(false);
            mAsrEngine.setAthThreshold(0.6f);
            mAsrEngine.setIsRelyOnLocalConf(true);
            mAsrEngine.setLocalBetterDomains(new String[]{"phone", "volume", "cmd", "choose_page", "choose_cmd", "choose_strategy",
                    "fault_cmd", "map", "brightness", "play_video", "common_navi_cmd", "common_adr_cmd"});
//            mAsrEngine.setCloudNotGoodAtDomains(new String[]{"股票"});
            mAsrEngine.setIsPreferCloud(true);
            mAsrEngine.setWaitCloudTimeout(5000);
            mAsrEngine.setPauseTime(1000);
            mAsrEngine.setUseConf(true);
            mAsrEngine.setNoSpeechTimeOut(5000);
            mAsrEngine.setVersion("0.6.14");
            mAsrEngine.setCloudVadEnable(true);

            // 自行设置合并规则:
            // 1. 如果无云端结果,则直接返回本地结果
            // 2. 如果有云端结果,则直接返回云端结果
            mAsrEngine.setMergeRule(new IMergeRule() {

                @Override
                public AIResult mergeResult(AIResult localResult, AIResult cloudResult) {
                    AIResult result = null;
                    try {
                        if (cloudResult == null) {
                            // 为结果增加标记,以标示来源于云端还是本地
                            JSONObject localJsonObject = new JSONObject(localResult.getResultObject()
                                    .toString());
                            localJsonObject.put("src", "native");

                            localResult.setResultObject(localJsonObject);
                            result = localResult;
                        } else {
                            JSONObject cloudJsonObject = new JSONObject(cloudResult.getResultObject()
                                    .toString());
                            cloudJsonObject.put("src", "cloud");
                            cloudResult.setResultObject(cloudJsonObject);
                            result = cloudResult;
                        }
                    } catch (JSONException e) {

                    }

                    return result;
                }
            });

            if (LauncherApplication.getContext().isNeedSaveVoice()) {
                log.debug("save voice file...");
                mAsrEngine.setUploadEnable(true);
                mAsrEngine.setTmpDir("/sdcard/speech");
                mAsrEngine.setTmpDirMaxSize(1024 * 1024 * 100);
            }

            if (mContext != null) {
                mAsrEngine.init(mContext, new AIASRListenerImpl(),
                        SpeechConstant.APPKEY, SpeechConstant.SECRETKEY);
                mAsrEngine.setUseCloud(true);

            }
        }

        private void doAuth() {
            final boolean authRet = mAuthEngine.doAuth();
            if (authRet) {
                log.debug("语音授权成功...");
            } else {
                log.error("语音授权失败...");
            }
        }

        private void destroyAll() {
            log.debug("语音销毁...");

            if (mWakeupEngine != null) {
                mWakeupEngine.destroy();
                mWakeupEngine = null;
            }

            if (mAsrEngine != null) {
                mAsrEngine.destroy();
                mAsrEngine = null;
            }

            if (mGrammarEngine != null) {
                mGrammarEngine.destroy();
                mGrammarEngine = null;
            }

            if (mTTSEngine != null) {
                mTTSEngine.destory();
                mTTSEngine = null;
            }

            if (mAuthEngine != null) {
                mAuthEngine.destroy();
                mAuthEngine = null;
            }

            isInitOver = false;
            initStep = 0;
        }

        private void startTTS(String playText) {
            if (mTTSEngine != null) {
                mTTSEngine.speak(playText, "1024");
            }
        }

        private void stopTTS() {
            log.debug("停止说话...");
            if (mTTSEngine != null) {
                mTTSEngine.stop();
            }
        }
    }

}
