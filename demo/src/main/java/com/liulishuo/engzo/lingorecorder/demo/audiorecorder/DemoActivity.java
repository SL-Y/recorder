package com.liulishuo.engzo.lingorecorder.demo.audiorecorder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.liulishuo.engzo.lingorecorder.LingoRecorder;
import com.liulishuo.engzo.lingorecorder.demo.R;
import com.liulishuo.engzo.lingorecorder.processor.AudioProcessor;
import com.liulishuo.engzo.lingorecorder.processor.TimerProcessor;
import com.liulishuo.engzo.lingorecorder.processor.WavProcessor;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Map;




/**
 * Created by wcw on 4/11/17.
 */

public class DemoActivity extends AppCompatActivity {

    public static final String WAV = "wav";
    public static final String FLAC = "androidFlac";
    public static final String SCORER = "localScorer";
    public static final String TIMER = "timer";
        public static final String SAVE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath();
        public static final String SAVE_NAME_WAV = File.separator+"test.wav";
        public static final String SAVE_NAME_FLAC = File.separator+"test.flac";

    private static final int REQUEST_CODE_PERMISSION = 10010;
    private TextView resultView;
    private Button recordBtn;
    private LingoRecorder lingoRecorder;
    private TextView titleView;
    private Chronometer chronometer;
    private RelativeLayout audioLayout;
    private ImageView micIcon;
    private TextView info;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        resultView = (TextView) findViewById(R.id.resultView);
        titleView = (TextView) findViewById(R.id.titleView);
        recordBtn = (Button) findViewById(R.id.recordBtn);
        audioLayout = (RelativeLayout) findViewById(R.id.audio_layout);
        micIcon = (ImageView) findViewById(R.id.mic_icon);
        info = (TextView) findViewById(R.id.tv_info);
        recordBtn.setOnTouchListener(touchListener);
        //安卓的计时器
        chronometer = (Chronometer) findViewById(R.id.time_display);

        String spokenText = "i will study english very hard";

        recordBtn.setText("start");
        titleView.setText("请说 " + spokenText);

        lingoRecorder = new LingoRecorder();

//        lingoRecorder.put(WAV, new WavProcessor("/sdcard/test.wav"));
        lingoRecorder.put(WAV, new WavProcessor(SAVE_PATH+SAVE_NAME_WAV));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            lingoRecorder.put(FLAC, new AndroidFlacProcessor(SAVE_PATH+SAVE_NAME_FLAC));
//            lingoRecorder.put(FLAC, new AndroidFlacProcessor("/sdcard/test.flac"));
        }

        //实际上是往processes中加一个处理对象
        TimerProcessor timerProcessor = new TimerProcessor(5000, new TimerProcessor.CallBack() {
            @Override
            public void showFlowTime(final long flowTime) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(DemoActivity.this, "flowTime\n"+flowTime,Toast.LENGTH_SHORT).show();


                    }
                });
            }
        });

        lingoRecorder.put(TIMER, timerProcessor);
        lingoRecorder.put(SCORER, new LocalScorerProcessor(getApplication(), spokenText));


        lingoRecorder.setOnRecordStopListener(new LingoRecorder.OnRecordStopListener() {
            @Override
            public void onRecordStop(Throwable error, Result result) {
                if (error != null) {
                    Toast.makeText(DemoActivity.this, "录音出错\n" + Log.getStackTraceString(error),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DemoActivity.this,"录音阶段结束",Toast.LENGTH_SHORT).show();
//                            Toast.LENGTH_SHORT).show();

//                    Toast.makeText(DemoActivity.this, String.format("输出文件路径 = %s 录音时长 = %d 毫秒", result.getOutputFilePath(), result.getDurationInMills()),
//                            Toast.LENGTH_SHORT).show();
                }
                recordBtn.setText("start");

//            sendVoiceNewView.onDetachedFromWindow();

            }
        });

        //设置停止process转码后的具体操作
        lingoRecorder.setOnProcessStopListener(new LingoRecorder.OnProcessStopListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onProcessStop(Throwable error, Map<String, AudioProcessor> map) {
                if (error != null) {
                    resultView.setText(Log.getStackTraceString(error));
                } else {
                    //获取到这些process
                    WavProcessor wavProcessor = (WavProcessor) map.get(WAV);
                    AndroidFlacProcessor flacProcessor = (AndroidFlacProcessor) map.get(FLAC);
                    LocalScorerProcessor scorerProcessor = (LocalScorerProcessor) map.get(SCORER);

                    StringBuilder sb = new StringBuilder();
                    if (wavProcessor != null) {
                        //转码后文件有多大
                        sb.append(String.format("wav file path = %s size = %s\n", wavProcessor.getFilePath()
                                , formatFileSize(wavProcessor.getFilePath())));
                    }
                    if (flacProcessor != null) {
                        sb.append(String.format("flac file path = %s size = %s \n", flacProcessor.getFilePath()
                                , formatFileSize(flacProcessor.getFilePath())));
                    }

                    if (scorerProcessor != null) {
                        sb.append(String.format("Got score = %d\n", scorerProcessor.getScore()));
                    }
                    //显示到界面上
                    resultView.setText(sb.toString());
                }
            }
        });


//            @Override
//            public void onClick(View v) {
//                if (!checkRecordPermission()) {
//                    return;
//                }
//                // isAvailable 为 false 为录音正在处理时，需要保护避免在这个时候操作录音器
//                if (lingoRecorder.isAvailable()) {
//                    if (lingoRecorder.isRecording()) {
//                        lingoRecorder.stop();
//                    } else {
//                        // need get permission
//                        lingoRecorder.start("/sdcard/test2.wav");
//                        resultView.setText("");
//                        recordBtn.setText("stop");
//                    }
//                }
//            }
//        });
    }

    public void stopRecordUnSave(boolean isTimeShort) {
        if (lingoRecorder.isAvailable()) {
            if (lingoRecorder.isRecording()) {
                lingoRecorder.stop();
//                File targetFile = new File("/sdcard/test.wav");

                File targetFile = new File(SAVE_PATH+SAVE_NAME_WAV);
                if (targetFile.exists()) {
                    //不保存直接删掉
                    targetFile.delete();
                    if(isTimeShort){
                        Toast.makeText(DemoActivity.this, "时间太短,已删", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(DemoActivity.this, "文件已删除", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        }
    }

    public void stopRecordWithSave() {
        if (lingoRecorder.isAvailable()) {
            if (lingoRecorder.isRecording()) {
                lingoRecorder.stop();
//                Toast.makeText(DemoActivity.this, "文件以保存至：" + path, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startRecord() {
        if (!checkRecordPermission()) {
            return;
        }
        // isAvailable 为 false 为录音正在处理时，需要保护避免在这个时候操作录音器
//        if (lingoRecorder.isAvailable()) {
//            if (lingoRecorder.isRecording()) {
//                lingoRecorder.stop();
//            } else {
//                // need get permission
//                lingoRecorder.start("/sdcard/test2.wav");
//                resultView.setText("");
//                recordBtn.setText("stop");
//            }
//        }

        if (lingoRecorder.isAvailable()) {
            if (!lingoRecorder.isRecording()) {
              // need get permission

                //不执行这个,这种方式转码的processor可能会block掉record的线程,导致录音不全
//                lingoRecorder.start("/sdcard/test2.wav");
                //执行这个,这种方式是优化过得,分别在两条线程里玩
                lingoRecorder.start();
                resultView.setText("");
                recordBtn.setText("stop");

//            }
            }
        }
    }

    private boolean isCancel;
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean ret = false;
            float downY = 0;
            int action = event.getAction();
            switch (v.getId()) {
                case R.id.recordBtn:
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            startAnim(true);
                            startRecord();
                            ret = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            stopAnim();
                            if (isCancel) {
                                isCancel = false;
                                stopRecordUnSave(false);
                                Toast.makeText(DemoActivity.this, "取消保存", Toast.LENGTH_SHORT).show();
                            } else {

                                TimerProcessor timerProcessor = (TimerProcessor)lingoRecorder.getWithKey(TIMER);
                                if(timerProcessor !=null){
                                    long flowTime = timerProcessor.getFlowTime();
                                    if(flowTime<2000){
                                        stopRecordUnSave(true);
                                    }else{
                                        stopRecordWithSave();
                                    }
                                }
//                                int duration = getDuration(chronometer.getText().toString());
//                                switch (duration) {
//                                    case -1:
//                                        break;
//                                    case -2:
//                                        stopRecordUnSave();
//
//                                        break;
//                                    default:
//                                        stopRecordWithSave();
////                                        String path = mediaUtils.getTargetFilePath();
////                                        Toast.makeText(DemoActivity.this, "文件以保存至：" + path, Toast.LENGTH_SHORT).show();
//                                        break;
//                                }
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float currentY = event.getY();
                            if (downY - currentY > 10) {
                                moveAnim();
                                isCancel = true;
                            } else {
                                isCancel = false;
                                startAnim(false);
                            }
                            break;
                    }
                    break;
            }
            return ret;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.check_permission_fail, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            findViewById(R.id.recordBtn).performClick();
        }
    }

    private String duration;
    private int getDuration(String str) {
        String a = str.substring(0, 1);
        String b = str.substring(1, 2);
        String c = str.substring(3, 4);
        String d = str.substring(4);
        if (a.equals("0") && b.equals("0")) {
            if (c.equals("0") && Integer.valueOf(d) < 1) {
                return -2;
            } else if (c.equals("0") && Integer.valueOf(d) > 1) {
                duration = d;
                return Integer.valueOf(d);
            } else {
                duration = c + d;
                return Integer.valueOf(c + d);
            }
        } else {
            duration = "60";
            return -1;
        }

    }

    //检查权限
    private boolean checkRecordPermission() {
        if (PermissionChecker.checkSelfPermission(DemoActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || PermissionChecker.checkSelfPermission(DemoActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    new AlertDialog.Builder(DemoActivity.this)
                            .setTitle(R.string.check_permission_title)
                            .setMessage(R.string.check_permission_content)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION);
                                    }
                                }
                            }).show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION);
                }
            }
            return false;
        }
        return true;
    }

    //转换成多少m多少kb
    private static String formatFileSize(String path) {
        File file = new File(path);
        long size = file.length();
        String[] units = {"B", "kB", "MB", "GB", "TB"};
        if (size > 0) {
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024.0));
            return new DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups)) + " " + units[digitGroups];
        } else {
            return "0B";
        }
    }

    private void startAnim(boolean isStart){
        audioLayout.setVisibility(View.VISIBLE);
        info.setText("上滑取消");
        recordBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.mic_pressed_bg));
        micIcon.setBackgroundDrawable(null);
        micIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_mic_white_24dp));
        if (isStart){
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.setFormat("%S");
            chronometer.start();
        }
    }

    private void stopAnim(){
        audioLayout.setVisibility(View.GONE);
        recordBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.mic_bg));
        chronometer.stop();
    }

    private void moveAnim(){
        info.setText("松手取消");
        micIcon.setBackgroundDrawable(null);
        micIcon.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_undo_black_24dp));
    }
}
