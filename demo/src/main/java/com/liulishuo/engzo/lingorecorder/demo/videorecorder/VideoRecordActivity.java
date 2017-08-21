package com.liulishuo.engzo.lingorecorder.demo.videorecorder;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.kiwi.ui.StickerConfigMgr;
import com.kiwi.ui.widget.KwControlView;
import com.liulishuo.engzo.lingorecorder.demo.R;
import com.liulishuo.engzo.lingorecorder.demo.photobutton.CaptureLayout;
import com.liulishuo.engzo.lingorecorder.demo.photobutton.lisenter.CaptureLisenter;
import com.liulishuo.engzo.lingorecorder.demo.photobutton.lisenter.TypeLisenter;
import com.liulishuo.engzo.lingorecorder.demo.photobutton.render.GLRenderer;
import com.qiniu.pili.droid.shortvideo.PLAudioEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLCameraSetting;
import com.qiniu.pili.droid.shortvideo.PLErrorCode;
import com.qiniu.pili.droid.shortvideo.PLFaceBeautySetting;
import com.qiniu.pili.droid.shortvideo.PLFocusListener;
import com.qiniu.pili.droid.shortvideo.PLMicrophoneSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordStateListener;
import com.qiniu.pili.droid.shortvideo.PLShortVideoRecorder;
import com.qiniu.pili.droid.shortvideo.PLVideoEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoFilterListener;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class VideoRecordActivity extends Activity implements PLRecordStateListener, PLVideoSaveListener, PLFocusListener {
    private static final String TAG = "VideoRecordActivity";

    /**
     * NOTICE: KIWI needs extra cost
     */
    private static final boolean USE_KIWI = true;

    private PLShortVideoRecorder mShortVideoRecorder;

    //    private SectionProgressBar mSectionProgressBar;
    private CustomProgressDialog mProcessingDialog;
    //    private View mRecordBtn;
//    private View mDeleteBtn;
//    private View mConcatBtn;
    private View mSwitchCameraBtn;
    private View mSwitchFlashBtn;
    private FocusIndicator mFocusIndicator;
    private SeekBar mAdjustBrightnessSeekBar;

    private boolean mFlashEnabled;
    private String mRecordErrorMsg;
    private boolean mIsEditVideo = false;

    private GestureDetector mGestureDetector;

    private PLCameraSetting mCameraSetting;

    private KiwiTrackWrapper mKiwiTrackWrapper;
    private KwControlView mControlView;

    private int mFocusIndicatorX;
    private int mFocusIndicatorY;

    private boolean isRecording;

    public int textureId;


    private int CAMERA_STATE = -1;
    private static final int STATE_IDLE = 0x010;
    private static final int STATE_RUNNING = 0x020;
    private static final int STATE_WAIT = 0x030;

    private boolean takePictureing = false;
    private boolean stopping = false;
    private boolean isBorrow = false;
    private boolean isTooShort;
    private MediaPlayer mMediaPlayer;
    private GLSurfaceView preview;
    private ViewGroup rootView;
    private GLSurfaceView play_back;
    private GLRenderer glRenderer;
    private MediaPlayer mediaPlayer;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);



        LayoutInflater inflater = getLayoutInflater();  //调用Activity的getLayoutInflater)
        rootView = (ViewGroup) inflater.inflate(R.layout.activity_record, null);

        setContentView(rootView);


        //片段的progressBar
//        mSectionProgressBar = (SectionProgressBar) findViewById(R.id.record_progressbar);
        //预览区
        preview = (GLSurfaceView) findViewById(R.id.preview);
        play_back = (GLSurfaceView) findViewById(R.id.play_back);
        play_back.setEGLContextClientVersion(2);
//        SurfaceTexture viewById = (GLSurfaceView)findViewById(R.id.preview);
//        mRecordBtn = findViewById(R.id.record);
        final CaptureLayout mCaptureLayout = (CaptureLayout) findViewById(R.id.layout_capture);
        mCaptureLayout.setDuration(10 * 1000);

        mCaptureLayout.setCaptureLisenter(new CaptureLisenter() {
            @Override
            public void takePictures() {
                //不使用拍照功能
            }

            @Override
            public void recordShort(long time) {
                if (CAMERA_STATE != STATE_RUNNING && stopping) {
                    return;
                }
                stopping = false;
                mCaptureLayout.setTextWithAnimation("录制时间过短");
                mShortVideoRecorder.endSection();
                isTooShort = true;
                mCaptureLayout.isRecord(false);
                CAMERA_STATE = STATE_WAIT;

                isBorrow = false;
//                mSwitchCamera.setRotation(0);
//                mSwitchCamera.setVisibility(VISIBLE);
//                CameraInterface.getInstance().setSwitchView(mSwitchCamera);

            }

            @Override
            public void recordStart() {
                if (CAMERA_STATE != STATE_IDLE && stopping) {
                    return;
                }

                mCaptureLayout.isRecord(true);
                isBorrow = true;
                CAMERA_STATE = STATE_RUNNING;

                //开始录像
                //当前进度条为0
//                if(!isRecording){
//                    if (mShortVideoRecorder.beginSection()) {
//                        //进度条不为0了
//                        isRecording=!isRecording;
////                        更新按钮的状态
////                    updateRecordingBtns(true);
//                    } else {
//                        ToastUtils.s(VideoRecordActivity.this, "无法开始视频段录制");
//                        Log.i("CJT", "startRecorder error");
//                        mCaptureLayout.isRecord(false);
//                        CAMERA_STATE = STATE_WAIT;
//                        stopping = false;
//                        isBorrow = false;
//                    }
//                }
                if (isTooShort) {
                    mShortVideoRecorder.deleteLastSection();
                    isRecording = false;
                    isTooShort = false;
                }

                if (!isRecording && mShortVideoRecorder.beginSection()) {
                    //进度条不为0了
                    isRecording = !isRecording;
//                        更新按钮的状态
//                    updateRecordingBtns(true);
                } else {
                    ToastUtils.s(VideoRecordActivity.this, "无法开始视频段录制");
                    Log.i("CJT", "startRecorder error");
                    mCaptureLayout.isRecord(false);
                    CAMERA_STATE = STATE_WAIT;
                    stopping = false;
                    isBorrow = false;
                }
            }

            @Override
            public void recordEnd(long time) {
                Log.i(TAG, "recordEnd: ");
                if (isRecording) {
                    boolean b = mShortVideoRecorder.endSection();


                    if (b) {

                        Log.d("", "");


//                        playMedia();
                    }
                }
            }

            @Override
            public void recordZoom(float zoom) {
                Log.i(TAG, "recordZoom: ");
            }

            //在button检查当前录音机有没有被占用了,如果占用了走到这里,已注释
            @Override
            public void recordError() {
                //错误回调
                Log.i(TAG, "recordError: ");
            }
        });
        mCaptureLayout.setTypeLisenter(new TypeLisenter() {
            @Override
            public void cancel() {
                play_back.setVisibility(View.GONE);
                preview.setVisibility(View.VISIBLE);

                Log.i(TAG, "cancel: ");
                if (!mShortVideoRecorder.deleteLastSection()) {
                    ToastUtils.s(VideoRecordActivity.this, "回删视频段失败");
                }
//                while(mShortVideoRecorder.deleteLastSection()){
//
//                }
//                ToastUtils.s(VideoRecordActivity.this, "无法开始视频段录制");
                //录像layout重新初始化一下
                Log.i("CJT", "startRecorder error");
                mCaptureLayout.isRecord(false);
                CAMERA_STATE = STATE_WAIT;
                stopping = false;
                isBorrow = false;

                //flag改为可录像状态
                isRecording = !isRecording;
//                if (CAMERA_STATE == STATE_WAIT) {
//                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
//                        mMediaPlayer.stop();
//                        mMediaPlayer.release();
//                        mMediaPlayer = null;
//                    }
//                    handlerPictureOrVideo(type, false);
//                }
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

            }

            @Override
            public void confirm() {
                play_back.setVisibility(View.GONE);
                preview.setVisibility(View.VISIBLE);


                Log.i(TAG, "confirm: ");

                if (!mShortVideoRecorder.deleteLastSection()) {
                    ToastUtils.s(VideoRecordActivity.this, "回删视频段失败");
                }

                mCaptureLayout.isRecord(false);
                CAMERA_STATE = STATE_WAIT;
                stopping = false;
                isBorrow = false;

                isRecording=false;

                if(filePath!=null&&!TextUtils.isEmpty(filePath)){
//                    PlaybackActivity.start(VideoRecordActivity.this, filePath);
                    showChooseDialog();
                }


                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
//                mProcessingDialog.show();
                //弹出对话框确认用户是否剪辑
//                showChooseDialog();
//                if (CAMERA_STATE == STATE_WAIT) {
//                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
//                        mMediaPlayer.stop();
//                        mMediaPlayer.release();
//                        mMediaPlayer = null;
//                    }
//                    handlerPictureOrVideo(type, true);
//                }
            }
        });

        mSwitchCameraBtn = findViewById(R.id.switch_camera);
        mSwitchFlashBtn = findViewById(R.id.switch_flash);
        mFocusIndicator = (FocusIndicator) findViewById(R.id.focus_indicator);
        mAdjustBrightnessSeekBar = (SeekBar) findViewById(R.id.adjust_brightness);

        //进行处理的对话框
        mProcessingDialog = new CustomProgressDialog(this);
        mProcessingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mShortVideoRecorder.cancelConcat();
            }
        });

        //video的录像器
        mShortVideoRecorder = new PLShortVideoRecorder();
        //录像状态的监听
        mShortVideoRecorder.setRecordStateListener(this);
        //焦点的监听
        mShortVideoRecorder.setFocusListener(this);

        int previewSizeRatio = getIntent().getIntExtra("PreviewSizeRatio", 0);
        int previewSizeLevel = getIntent().getIntExtra("PreviewSizeLevel", 0);
        int encodingSizeLevel = getIntent().getIntExtra("EncodingSizeLevel", 0);
        int encodingBitrateLevel = getIntent().getIntExtra("EncodingBitrateLevel", 0);

        //camera的设置
        mCameraSetting = new PLCameraSetting();
        //设置camera用哪个吧
        PLCameraSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();

        mCameraSetting.setCameraId(facingId);
        //把camera拍到的内容显示在preview上的一些设置
        mCameraSetting.setCameraPreviewSizeRatio(getPreviewSizeRatio(0));
        mCameraSetting.setCameraPreviewSizeLevel(getPreviewSizeLevel(previewSizeLevel));

        //麦克风的设置
        PLMicrophoneSetting microphoneSetting = new PLMicrophoneSetting();

        //video编码的设置
        PLVideoEncodeSetting videoEncodeSetting = new PLVideoEncodeSetting(this);
        videoEncodeSetting.setEncodingSizeLevel(getEncodingSizeLevel(17));
//        videoEncodeSetting.setEncodingSizeLevel({1280, 720});
        videoEncodeSetting.setEncodingBitrate(getEncodingBitrateLevel(6));

        //音频的设置
        PLAudioEncodeSetting audioEncodeSetting = new PLAudioEncodeSetting();

        //record录像的设置
        PLRecordSetting recordSetting = new PLRecordSetting();
        recordSetting.setMaxRecordDuration(RecordSettings.DEFAULT_MAX_RECORD_DURATION);
        recordSetting.setVideoCacheDir(Config.VIDEO_STORAGE_DIR);
        recordSetting.setVideoFilepath(Config.RECORD_FILE_PATH);

        //美颜的设置
        PLFaceBeautySetting faceBeautySetting = new PLFaceBeautySetting(1.0f, 0.5f, 0.5f);

//        //全部设置给recorder
        mShortVideoRecorder.prepare(preview, mCameraSetting, microphoneSetting, videoEncodeSetting,
                audioEncodeSetting, USE_KIWI ? null : faceBeautySetting, recordSetting);


        if (USE_KIWI) {
            StickerConfigMgr.setSelectedStickerConfig(null);

            mKiwiTrackWrapper = new KiwiTrackWrapper(this, mCameraSetting.getCameraId());
            mKiwiTrackWrapper.onCreate(this);

            mControlView = (KwControlView) findViewById(R.id.kiwi_control_layout);
            mControlView.setOnEventListener(mKiwiTrackWrapper.initUIEventListener());
            mControlView.setVisibility(VISIBLE);

            mShortVideoRecorder.setVideoFilterListener(new PLVideoFilterListener() {

                private int surfaceWidth;
                private int surfaceHeight;
                private boolean isTrackerOnSurfaceChangedCalled;

                @Override
                public void onSurfaceCreated() {
                    mKiwiTrackWrapper.onSurfaceCreated(VideoRecordActivity.this);
                }

                @Override
                public void onSurfaceChanged(int width, int height) {
                    surfaceWidth = width;
                    surfaceHeight = height;
                }

                @Override
                public void onSurfaceDestroy() {
                    mKiwiTrackWrapper.onSurfaceDestroyed();
                }

                @Override
                public int onDrawFrame(int texId, int texWidth, int texHeight, long l) {
                    if (!isTrackerOnSurfaceChangedCalled) {
                        isTrackerOnSurfaceChangedCalled = true;
                        mKiwiTrackWrapper.onSurfaceChanged(surfaceWidth, surfaceHeight, texWidth, texHeight);
                    }
                    VideoRecordActivity.this.textureId = texId;
                    return mKiwiTrackWrapper.onDrawFrame(texId, texWidth, texHeight);
                }
            });
        }

        //设置最短的录像时长
//        mSectionProgressBar.setFirstPointTime(RecordSettings.DEFAULT_MIN_RECORD_DURATION);
//        设置最长的录像时长
//        mSectionProgressBar.setTotalTime(RecordSettings.DEFAULT_MAX_RECORD_DURATION);

        //录像按钮的touch
//        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                int action = event.getAction();
//                if (action == MotionEvent.ACTION_DOWN) {
//                    //开始录像
//                    if (mShortVideoRecorder.beginSection()) {
////                        更新按钮的状态
//                        updateRecordingBtns(true);
//                    } else {
//                        ToastUtils.s(VideoRecordActivity.this, "无法开始视频段录制");
//                    }
//                } else if (action == MotionEvent.ACTION_UP) {
//                    //结束一段录制,等待新的一段录制
//                    mShortVideoRecorder.endSection();
//                    //                        更新按钮的状态
//                    updateRecordingBtns(false);
//                }
//
//                return false;
//            }
//        });


        //手势识别
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                mFocusIndicatorX = (int) e.getX() - mFocusIndicator.getWidth() / 2;
                mFocusIndicatorY = (int) e.getY() - mFocusIndicator.getHeight() / 2;
                //手动的对焦
                mShortVideoRecorder.manualFocus(mFocusIndicator.getWidth(), mFocusIndicator.getHeight(), (int) e.getX(), (int) e.getY());
                return false;
            }
        });
        preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
//                交给手势识别处理
                mGestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
        //手动地把段progess,调到0
//        onSectionCountChanged(0, 0);
    }

    private void playMedia() {
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            } else {
                mMediaPlayer.reset();
            }
//                                    Log.i("CJT", "URL = " + url);

            String filePath = "/storage/emulated/0/ShortVideo/record.mp4";
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            mMediaPlayer.setDataSource(fis.getFD());


            //用同一个SurfaceView
            SurfaceTexture surfaceTexture = new SurfaceTexture(textureId);
            Surface surface = new Surface(surfaceTexture);

            mMediaPlayer.setSurface(surface);
            surface.release();
            mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer
                    .OnVideoSizeChangedListener() {
                @Override
                public void
                onVideoSizeChanged(MediaPlayer mp, int width, int height) {
//                                            updateVideoViewSize(mMediaPlayer.getVideoWidth(), mMediaPlayer
//                                                    .getVideoHeight());
                }
            });
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                    Log.d("", "");
                }
            });
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //更新录像按钮
    private void updateRecordingBtns(boolean isRecording) {
        mSwitchCameraBtn.setEnabled(!isRecording);
//        mRecordBtn.setActivated(isRecording);
//        progressButton.setActivated(isRecording);
    }

//    /**
//     * TextureView resize
//     */
//    public void updateVideoViewSize(float videoWidth, float videoHeight) {
//        if (videoWidth > videoHeight) {
//            FrameLayout.LayoutParams videoViewParam;
//            int height = (int) ((videoHeight / videoWidth) * getWidth());
//            videoViewParam = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
//                    height);
//            videoViewParam.gravity = Gravity.CENTER;
////            videoViewParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
//            mVideoView.setLayoutParams(videoViewParam);
//        }
//    }

    //手动的捕捉一个关键帧
//    public void onCaptureFrame(View v) {
//        mShortVideoRecorder.captureFrame(new PLCaptureFrameListener() {
//            @Override
//            public void onFrameCaptured(PLVideoFrame capturedFrame) {
//                if (capturedFrame == null) {
//                    Log.e(TAG, "capture frame failed");
//                    return;
//                }
//
//                Log.i(TAG, "captured frame width: " + capturedFrame.getWidth() + " height: " + capturedFrame.getHeight() + " timestamp: " + capturedFrame.getTimestampMs());
//                try {
//                    //转为bitmap保存起来
//                    FileOutputStream fos = new FileOutputStream(Config.CAPTURED_FRAME_FILE_PATH);
//                    capturedFrame.toBitmap().compress(Bitmap.CompressFormat.JPEG, 100, fos);
//                    fos.close();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            ToastUtils.s(VideoRecordActivity.this, "截帧已保存到路径：" + Config.CAPTURED_FRAME_FILE_PATH);
//                        }
//                    });
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }


    /**
     * 生命周期
     **/
    @Override
    protected void onResume() {
        super.onResume();
//        progressButton.setEnabled(false);
        if (mKiwiTrackWrapper != null) {
            mKiwiTrackWrapper.onResume(this);
        }
        mShortVideoRecorder.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mKiwiTrackWrapper != null) {
            mKiwiTrackWrapper.onPause(this);
        }
        updateRecordingBtns(false);
        mShortVideoRecorder.pause();


//        if (mediaPlayer != null) {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mKiwiTrackWrapper != null) {
            mKiwiTrackWrapper.onDestroy(this);
        }
        mShortVideoRecorder.destroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
   /* 设置生命周期结束*/


    //视频回删一段
//    public void onClickDelete(View v) {
//        if (!mShortVideoRecorder.deleteLastSection()) {
//            ToastUtils.s(this, "回删视频段失败");
//        }
//    }

//    //点击钩号
//    public void onClickConcat(View v) {
//
//        mProcessingDialog.show();
//        //弹出对话框确认用户是否剪辑
//        showChooseDialog();
//    }

    //点击切换换一个摄像头
    public void onClickSwitchCamera(View v) {
        if (mKiwiTrackWrapper != null) {
            mKiwiTrackWrapper.switchCamera(mCameraSetting.getCameraId());

        }
        mShortVideoRecorder.switchCamera();

    }

    //点击是否打开闪光灯
    public void onClickSwitchFlash(View v) {
        mFlashEnabled = !mFlashEnabled;
        mShortVideoRecorder.setFlashEnabled(mFlashEnabled);
        mSwitchFlashBtn.setActivated(mFlashEnabled);
    }

    //准备开始录制的一些初始化回调
    @Override
    public void onReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwitchFlashBtn.setVisibility(mShortVideoRecorder.isFlashSupport() ? VISIBLE : GONE);
                mFlashEnabled = false;
                mSwitchFlashBtn.setActivated(mFlashEnabled);
//                mRecordBtn.setEnabled(true);
//                progressButton.setEnabled(true);

                //调整亮度值
//                refreshSeekBar();
                ToastUtils.s(VideoRecordActivity.this, "可以开始拍摄咯");
            }
        });
    }

    //录制出现error
    @Override
    public void onError(int code) {
        if (code == PLErrorCode.ERROR_SETUP_CAMERA_FAILED) {
            mRecordErrorMsg = "摄像头配置错误";
        } else if (code == PLErrorCode.ERROR_SETUP_MICROPHONE_FAILED) {
            mRecordErrorMsg = "麦克风配置错误";
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, mRecordErrorMsg);
            }
        });
    }

    //拍摄的太短的回调
    @Override
    public void onDurationTooShort() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, "该视频段太短了");
            }
        });
    }

    //开始录制
    @Override
    public void onRecordStarted() {
        Log.i(TAG, "record start time: " + System.currentTimeMillis());
//        mSectionProgressBar.setCurrentState(SectionProgressBar.State.START);
    }

    @Override
    public void onRecordStopped() {
        Log.i(TAG, "record stop time: " + System.currentTimeMillis());

        Thread thread = Thread.currentThread();
        Log.i(TAG, "thread: " + thread);

        mIsEditVideo = false;
        if(!isTooShort){
            mShortVideoRecorder.concatSections(VideoRecordActivity.this);
        }
//        mSectionProgressBar.setCurrentState(SectionProgressBar.State.PAUSE);
    }


    //拍摄好一段后的回调
    @Override
    public void onSectionIncreased(long incDuration, long totalDuration, int sectionCount) {
        Log.i(TAG, "section increased incDuration: " + incDuration + " totalDuration: " + totalDuration + " sectionCount: " + sectionCount);


//        onSectionCountChanged(sectionCount, totalDuration);
//        mSectionProgressBar.addBreakPointTime(totalDuration);
    }

    //删除前面一段视频的回删
    @Override
    public void onSectionDecreased(long decDuration, long totalDuration, int sectionCount) {
        Log.i(TAG, "section decreased decDuration: " + decDuration + " totalDuration: " + totalDuration + " sectionCount: " + sectionCount);
//        onSectionCountChanged(sectionCount, totalDuration);
//        mSectionProgressBar.removeLastBreakPoint();
    }

    //录制到最大时长的回调
    @Override
    public void onRecordCompleted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, "已达到拍摄总时长");
            }
        });
    }

    //保存到本地,进度更新的回调
    @Override
    public void onProgressUpdate(float percentage) {
        mProcessingDialog.setProgress((int) (100 * percentage));
    }

    //保存到本地失败的回调
    @Override
    public void onSaveVideoFailed(final int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProcessingDialog.dismiss();
                ToastUtils.s(VideoRecordActivity.this, "拼接视频段失败: " + errorCode);
            }
        });
    }

    //取消
    @Override
    public void onSaveVideoCanceled() {
        mProcessingDialog.dismiss();
    }

    //保存成功
    @Override
    public void onSaveVideoSuccess(final String filePath) {

        this.filePath=filePath;
        Log.i(TAG, "concat sections success filePath: " + filePath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                if (glRenderer == null) {
//                    glRenderer = new GLRenderer(VideoRecordActivity.this, filePath);//

//                    MediaPlayer mediaPlayer = new MediaPlayer();//
//                    try {//
//                        mediaPlayer.setDataSource(VideoRecordActivity.this,Uri.parse(filePath));//
//                    } catch (IOException e) {//
//                        e.printStackTrace();//
//                    }//
//                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);//
//                    mediaPlayer.setLooping(true);//
//                    mediaPlayer.setOnVideoSizeChangedListener(glRenderer);//
//                    glRenderer.setMediaPlayer(mediaPlayer);//
//
//                    play_back.setRenderer(glRenderer);//
//                    play_back.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//
//                }
                play_back.setVisibility(View.VISIBLE);
                preview.setVisibility(View.GONE);
                if(glRenderer == null){
                    glRenderer = new GLRenderer(VideoRecordActivity.this);
                    play_back.setRenderer(glRenderer);
                    play_back.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
                if(mediaPlayer==null){
                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(VideoRecordActivity.this, Uri.parse(filePath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setOnVideoSizeChangedListener(glRenderer);

                    glRenderer.setMediaPlayer(mediaPlayer);
                }


            }
        });


//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                //清空录像的东西
//                mShortVideoRecorder.deleteLastSection();
//                //把flag重新设为没有录像的状态
//                isRecording = !isRecording;
//
//                mProcessingDialog.dismiss();
//                if (mIsEditVideo) {
////                    VideoEditActivity.start(VideoRecordActivity.this, filePath);
//                } else {
////                    PlaybackActivity.start(VideoRecordActivity.this, filePath);
//                    VideoTexturePlaybackActivity.start(VideoRecordActivity.this, filePath, AVOptions.MEDIA_CODEC_SW_DECODE);
//                }
//            }
//        });


        // TODO: 2017/8/18
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                // TODO: 2017/8/16
//                //清空录像的东西
//                mShortVideoRecorder.deleteLastSection();
//                //把flag重新设为没有录像的状态
//                isRecording=!isRecording;
//
//                mProcessingDialog.dismiss();
//                if (mIsEditVideo) {
////                    VideoEditActivity.start(VideoRecordActivity.this, filePath);
//                } else {
////                    PlaybackActivity.start(VideoRecordActivity.this, filePath);
//                    VideoTexturePlaybackActivity.start(VideoRecordActivity.this, filePath, AVOptions.MEDIA_CODEC_SW_DECODE);
//                }
//            }
//        });


    }

    //刷新,重新初始化
//    private void refreshSeekBar() {
//        final int max = mShortVideoRecorder.getMaxExposureCompensation();
//        final int min = mShortVideoRecorder.getMinExposureCompensation();
//        boolean brightnessAdjustAvailable = (max != 0 || min != 0);
//        Log.e(TAG, "max/min exposure compensation: " + max + "/" + min + " brightness adjust available: " + brightnessAdjustAvailable);
//
//        findViewById(R.id.brightness_panel).setVisibility(brightnessAdjustAvailable ? VISIBLE : GONE);
//        mAdjustBrightnessSeekBar.setOnSeekBarChangeListener(!brightnessAdjustAvailable ? null : new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//                if (i <= Math.abs(min)) {
//                    mShortVideoRecorder.setExposureCompensation(i + min);
//                } else {
//                    mShortVideoRecorder.setExposureCompensation(i - max);
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//        mAdjustBrightnessSeekBar.setMax(max + Math.abs(min));
//        mAdjustBrightnessSeekBar.setProgress(Math.abs(min));
//    }

    //视频段数改变的时候执行
//    private void onSectionCountChanged(final int count, final long totalTime) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                mDeleteBtn.setEnabled(count > 0);
////                mConcatBtn.setEnabled(totalTime >= RecordSettings.DEFAULT_MIN_RECORD_DURATION);
//            }
//        });
//    }

    private PLCameraSetting.CAMERA_PREVIEW_SIZE_RATIO getPreviewSizeRatio(int position) {
        return RecordSettings.PREVIEW_SIZE_RATIO_ARRAY[position];
    }

    private PLCameraSetting.CAMERA_PREVIEW_SIZE_LEVEL getPreviewSizeLevel(int position) {
        return RecordSettings.PREVIEW_SIZE_LEVEL_ARRAY[position];
    }

    private PLVideoEncodeSetting.VIDEO_ENCODING_SIZE_LEVEL getEncodingSizeLevel(int position) {
        return RecordSettings.ENCODING_SIZE_LEVEL_ARRAY[position];
    }

    private int getEncodingBitrateLevel(int position) {
        return RecordSettings.ENCODING_BITRATE_LEVEL_ARRAY[position];
    }

    private PLCameraSetting.CAMERA_FACING_ID chooseCameraFacingId() {
        if (PLCameraSetting.hasCameraFacing(PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD)) {
            return PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        } else if (PLCameraSetting.hasCameraFacing(PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT)) {
            return PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            return PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        }
    }

    //弹出是否编辑的对话框
    private void showChooseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.if_edit_video));
        builder.setPositiveButton(getString(R.string.dlg_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
           mIsEditVideo = true;

                Toast.makeText(VideoRecordActivity.this,"跳到编辑页面",Toast.LENGTH_SHORT).show();
//                mShortVideoRecorder.concatSections(VideoRecordActivity.this);
            }
        });
        builder.setNegativeButton(getString(R.string.dlg_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsEditVideo = false;
                Toast.makeText(VideoRecordActivity.this,"不编辑,开始上传",Toast.LENGTH_SHORT).show();
//                mShortVideoRecorder.concatSections(VideoRecordActivity.this);
            }
        });


        // TODO: 2017/8/16
        //把之前录的清空
        builder.setCancelable(false);
        builder.create().show();
    }

    @Override
    public void onManualFocusStart(boolean result) {
        if (result) {
            Log.i(TAG, "manual focus begin success");
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mFocusIndicator.getLayoutParams();
            lp.leftMargin = mFocusIndicatorX;
            lp.topMargin = mFocusIndicatorY;
            mFocusIndicator.setLayoutParams(lp);
            mFocusIndicator.focus();
        } else {
            mFocusIndicator.focusCancel();
            Log.i(TAG, "manual focus not supported");
        }
    }

    @Override
    public void onManualFocusStop(boolean result) {
        Log.i(TAG, "manual focus end result: " + result);
        if (result) {
            mFocusIndicator.focusSuccess();
        } else {
            mFocusIndicator.focusFail();
        }
    }

    @Override
    public void onManualFocusCancel() {
        Log.i(TAG, "manual focus canceled");
        mFocusIndicator.focusCancel();
    }

    @Override
    public void onAutoFocusStart() {
        Log.i(TAG, "auto focus start");
    }

    @Override
    public void onAutoFocusStop() {
        Log.i(TAG, "auto focus stop");
    }
}
