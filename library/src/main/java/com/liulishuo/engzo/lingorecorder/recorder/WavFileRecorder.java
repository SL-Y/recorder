package com.liulishuo.engzo.lingorecorder.recorder;

import android.support.annotation.NonNull;

import com.liulishuo.engzo.lingorecorder.utils.LOG;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by wcw on 4/5/17.
 */
//做测试用的类
public class WavFileRecorder implements IRecorder {

    private String filePath;
    private FileInputStream fis;

    private long payloadSize;

    private static final int sampleRate = 16000;
    private static final int bitsPerSample = 16;
    private static final int nChannels = 1;

    public WavFileRecorder(String filePath) {
        this.filePath = filePath;
    }
    //读取一次多少字节
    @Override
    public int getBufferSize() {
        return 1024;
    }

    @Override
    public void startRecording() throws Exception {
        //生成出一个读取流,
        fis = new FileInputStream(filePath);
        //读取流跳到44
        long skip = fis.skip(44);
        LOG.d("skip size = " + skip);
        payloadSize = 0;
    }

    @Override
    public int read(@NonNull byte[] bytes, int buffSize) throws Exception {
        //开始读取
        int count = fis.read(bytes, 0, buffSize);
        //总大小加上count
        payloadSize += count;
        return count;
    }

    @Override
    public void release() {
        try {
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getDurationInMills() {
        return (long) (payloadSize * 8.0 * 1000 / bitsPerSample / sampleRate / nChannels );
    }
}
