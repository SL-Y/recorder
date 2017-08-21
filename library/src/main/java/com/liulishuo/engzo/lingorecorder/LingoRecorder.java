package com.liulishuo.engzo.lingorecorder;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.liulishuo.engzo.lingorecorder.processor.AudioProcessor;
import com.liulishuo.engzo.lingorecorder.processor.WavProcessor;
import com.liulishuo.engzo.lingorecorder.recorder.AndroidRecorder;
import com.liulishuo.engzo.lingorecorder.recorder.IRecorder;
import com.liulishuo.engzo.lingorecorder.recorder.WavFileRecorder;
import com.liulishuo.engzo.lingorecorder.utils.LOG;
import com.liulishuo.engzo.lingorecorder.utils.WrapBuffer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by wcw on 3/28/17.
 * 核心类
 */

public class LingoRecorder {

    private Map<String, AudioProcessor> audioProcessorMap = new HashMap<>();
    private InternalRecorder internalRecorder;

    private final static int MESSAGE_RECORD_STOP = 1;
    private final static int MESSAGE_PROCESS_STOP = 2;
    private final static int MESSAGE_AVAILABLE = 3;
    private final static String KEY_DURATION = "duration";
    private final static String KEY_FILEPATH = "filePath";

    private OnRecordStopListener onRecordStopListener;
    private OnProcessStopListener onProcessStopListener;

    //指可以开始录音了
    private boolean available = true;

    private Handler handler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //处理消息
            switch (msg.what) {
                case MESSAGE_RECORD_STOP:
                    handleRecordStop(msg);
                    break;
                case MESSAGE_PROCESS_STOP:
                    handleProcessStop(msg);
                    break;
                case MESSAGE_AVAILABLE:
                    available = true;
                    LOG.d("record available now");
                    break;
            }
        }

        //通过onRecordStopListener回调onRecordStop方法
        private void handleRecordStop(Message msg) {
            //获取到msg中的duration和filepath
            long durationInMills = msg.getData().getLong(KEY_DURATION, -1);
            String outputFilePath = msg.getData().getString(KEY_FILEPATH);
            if (onRecordStopListener != null) {
//                封装成一个result对象,整体回调出去
                OnRecordStopListener.Result result = new OnRecordStopListener.Result();
                result.durationInMills = durationInMills;
                result.outputFilePath = outputFilePath;
                onRecordStopListener.onRecordStop((Throwable) msg.obj, result);
            }
//            InternalRecorder设为空
            internalRecorder = null;
            LOG.d("record end");
        }
        //通过onProcessStopListener回调onProcessStop方法
        private void handleProcessStop(Message msg) {
            if (onProcessStopListener != null) {
                onProcessStopListener.onProcessStop((Throwable) msg.obj, audioProcessorMap);
            }
            LOG.d("process end");
        }
    };

    public boolean isAvailable() {
        return available;
    }

    //判断internalRecorder是否已经为空了,如果为空,指录音阶段已经结束,但是process阶段还不一定
    public boolean isRecording() {
        return internalRecorder != null;
    }

    public void start() {
        start(null);
    }

    public void start(String outputFilePath) {
        LOG.d("start record");
        IRecorder recorder = null;
//        如果wavFilePath不为空,new一个WavFileRecorder,available设为false
        if (wavFilePath != null) {
            recorder = new WavFileRecorder(wavFilePath);

            //只有process结束掉,available才能为true代表重新可以录音了,wavFileRecorder是不接受停止的
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // wavFileRecorder not support stop
            // LingoRecorder will be available until process finish
            available = false;

            //为空new一个AndroidRecorder
        } else {
            recorder = new AndroidRecorder(sampleRate, channels, bitsPerSample);
        }
        //把recorder传进去交给InternalRecorder维护
//        InternalRecorder内部构造生成一个thread
        internalRecorder = new InternalRecorder(recorder, outputFilePath, audioProcessorMap.values(), handler);
        //内部执行了thread的start方法,启动了哪个线程
        internalRecorder.start();
    }

    //执行internalRecorder的stop方法
    //internalRecorder置为空
    public void stop() {
        if (internalRecorder != null) {
            LOG.d("end record");
            available = false;
            LOG.d("record unavailable now");
            internalRecorder.stop();
            internalRecorder = null;
        }
    }
    //执行internalRecorder的cancel方法
    //internalRecorder置为空
    public void cancel() {
        if (internalRecorder != null) {
            available = false;
            internalRecorder.cancel();
            internalRecorder = null;
        }
    }
    //在handleRecordStop的时候会回调里面的listener
    public void setOnRecordStopListener(OnRecordStopListener onRecordStopListener) {
        this.onRecordStopListener = onRecordStopListener;
    }

    //在handleProcessStop的时候会回调里面的listener
    public void setOnProcessStopListener(OnProcessStopListener onProcessStopListener) {
        this.onProcessStopListener = onProcessStopListener;
    }

    //把processor放到audioProcessorMap中
    public void put(String processorId, AudioProcessor processor) {
        audioProcessorMap.put(processorId, processor);
    }

    public AudioProcessor getWithKey(String processorId){
        return audioProcessorMap.get(processorId);
    }

    public interface OnRecordStopListener {
        //用于封装durationInMills和outputFilePath的对象
        class Result {
            private long durationInMills;
            private String outputFilePath;

            public long getDurationInMills() {
                return durationInMills;
            }

            public String getOutputFilePath() {
                return outputFilePath;
            }
        }
        //回调方法
        void onRecordStop(Throwable throwable, Result result);
    }


    public interface OnProcessStopListener {
        void onProcessStop(Throwable throwable, Map<String, AudioProcessor> map);
    }

    //初始化一些变量
    private int sampleRate = 16000;
    private int channels = 1;
    private int bitsPerSample = 16;
    private String wavFilePath;

    public LingoRecorder sampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    public LingoRecorder channels(int channels) {
        this.channels = channels;
        return this;
    }

    public LingoRecorder bitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
        return this;
    }

    public LingoRecorder wavFile(String filePath) {
        this.wavFilePath = filePath;
        return this;
    }

    @Deprecated
    public LingoRecorder testFile(String filePath) {
        this.wavFilePath = filePath;
        return this;
    }

    //内部维护一个子线程
    private static class InternalRecorder implements Runnable {
        //单个process的轮询flag,设为false之后会停止转码过程
        private volatile boolean shouldRun;
        private volatile boolean cancel;
        private volatile Throwable processorsError;

        private Thread thread;
        private IRecorder recorder;
        private Collection<AudioProcessor> audioProcessors;
        private Handler handler;
        private String outputFilePath;

        InternalRecorder(IRecorder recorder, String outputFilePath, Collection<AudioProcessor> audioProcessors, Handler handler) {
            thread = new Thread(this);
            this.audioProcessors = audioProcessors;
            this.handler = handler;
            this.recorder = recorder;
            this.outputFilePath = outputFilePath;
        }

        //shouldRun设置为false
        //cancel设置为true
        //比stop多了一个cancel
        void cancel() {
            shouldRun = false;
            cancel = true;
        }

        //shouldRun设置为false
        void stop() {
            shouldRun = false;
        }
        //开启线程
        void start() {
            thread.start();
        }

        //shouldRun设置为true
        @Override
        public void run() {
            shouldRun = true;
            int buffSize = recorder.getBufferSize();

            byte[] bytes = new byte[buffSize];

            //生成一个链表栈
            LinkedBlockingQueue<Object> processorQueue = new LinkedBlockingQueue<>();
            //返回一个处理音频用的线程,开启这个线程
            //这个线程用来转码成设置好的所有文件类型,异步生成一个本地文件
            Thread processorThread = createProcessThread(processorQueue);
            processorThread.start();

            WavProcessor wavProcessor = null;
            if (outputFilePath != null) {
                //要转换成wav的processor
                wavProcessor = new WavProcessor(outputFilePath);
            }

            Throwable recordException = null;

            try {
                //从外面传进来的recorder在子线程里开始录制
                recorder.startRecording();
                //如果outputFilePath != null的话,录和转码在同一个线程里
                //没必要执行这里的wavProcessor
                if (wavProcessor != null) {
                    //初始化本地文件
                    wavProcessor.start();
                }
                //开始轮询
                while (shouldRun) {
                    //把record下来的东西读取到bytes中,返回这一次读取到的count
                    int result = recorder.read(bytes, buffSize);
                    LOG.d("read buffer result = " + result);
                    //如果这个count还>0,说明没有读取完
                    if (result > 0) {
                        //new一个buffer包裹
                        WrapBuffer wrapBuffer = new WrapBuffer();
//                        把byte和size封装进去
                        wrapBuffer.setBytes(Arrays.copyOf(bytes, bytes.length));
                        wrapBuffer.setSize(result);
//                        放到待处理的processor栈中
                        processorQueue.put(wrapBuffer);
                        if (wavProcessor != null) {
                            //写到一个wav文件里
                            wavProcessor.flow(bytes, result);
                        }
                        //说明读取完了
                    } else if (result < 0) {
                        LOG.d("exit read from recorder result = " + result);
                        //停止轮询
                        shouldRun = false;
                        break;
                    }
                }
                //给文件的头部写上size信息
                if (wavProcessor != null) {
                    wavProcessor.end();
                }
            } catch (Throwable e) {
                LOG.e(e);
                recordException = e;
            } finally {
                //初始化为false
                shouldRun = false;
                //释放掉转换wav对象
                if (wavProcessor != null) {
                    wavProcessor.release();
                }

                //notify stop record
                //先停止掉录制那边
                //封装一个message用handler告诉主线程可以停止RECORD了
                Message message = Message.obtain();
                message.what = MESSAGE_RECORD_STOP;
                Bundle bundle = new Bundle();
                bundle.putLong(KEY_DURATION, recorder.getDurationInMills());
                bundle.putString(KEY_FILEPATH, outputFilePath);
                // TODO: 2017/8/14   暂不需要
//                if(outputFilePath!=null){
//                    bundle.putString(KEY_FILEPATH, outputFilePath);
//                }else{
//                    for(AudioProcessor audioProcessor : audioProcessors){
//                        if(audioProcessor.get){
//
//                        }
//                    }
//                }
                message.setData(bundle);
                message.obj = recordException;
                handler.sendMessage(message);

                //ensure processors' tread has been end
                try {
                    //待处理的byte包栈中放入end,代表没有待处理的文件了
                    processorQueue.put("end");
                    //
                    if (cancel) {
//                        一个线程意味着在该线程完成任务之前停止其正在进行的一切，有效地中止其当前的操作。但不会中断一个正在运行的线程。

                        //终止掉process线程
                        processorThread.interrupt();
                    }
//                    比如在线程B中调用了线程A的Join()方法，直到线程A执行完毕后，才会继续执行线程B。
                    processorThread.join();
                    LOG.d("processorThread end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //再停止掉转码这边

                //notify processors end
                Message msg = Message.obtain();
                msg.what = MESSAGE_PROCESS_STOP;
                msg.obj = processorsError;
                handler.sendMessage(msg);
                //释放recorder,把内部的读取流关掉
                recorder.release();
                //又可以开始录音了
                handler.sendEmptyMessage(MESSAGE_AVAILABLE);
            }
        }

        private Thread createProcessThread(final LinkedBlockingQueue<Object> processorQueue) {
            return new Thread("process audio data") {

                @Override
                public void run() {
                    super.run();
                    Object value;
                    try {
                        //设置一些文件的头文件 初始化
                        for (AudioProcessor audioProcessor : audioProcessors) {
                            checkIfNeedCancel();
                            audioProcessor.start();
                        }
                        //然后不停的轮询,从processorQueue中取出recorder录制好的二进制buffer
                        while ((value = processorQueue.take()) != null) {
                            if (value instanceof WrapBuffer) {
                                //遍历一遍需要转换的那个格式,也就是说一个buffer序列化出三个文件
                                for (AudioProcessor audioProcessor : audioProcessors) {
                                    //检查是否需要取消
                                    checkIfNeedCancel();
                                    WrapBuffer wrapBuffer = (WrapBuffer) value;
                                    //具体实现为 把内存里的buffer取出来往writer里写二进制,也就是写到一个本地文件里序列化
                                    //每处理一段buffer,会走一次timerprocessor的flow获取当前的时间,让该处理的都处理完才会获取当前时间
                                    audioProcessor.flow(wrapBuffer.getBytes(), wrapBuffer.getSize());
                                }
                                //遍历这个转码格式是否需要退出,上面的for循环会把待处理的buffer全部处理完
                                for (AudioProcessor audioProcessor : audioProcessors) {
//                                    这里会检查cancel的值
                                    checkIfNeedCancel();
                                    //只有timerprocessor的needExit()方法在倒计时结束后会返回true
                                    if (audioProcessor.needExit()) {
                                        //需要退出了
                                        LOG.d(String.format("exit because %s", audioProcessor));
                                        shouldRun = false;
                                        break;
                                    }
                                }
                            } else {
                                break;
                            }
                        }
//                        while轮询完之后,全部end掉,也就是序列化结束后,给头文件写上文件有多大
                        for (AudioProcessor audioProcessor : audioProcessors) {
                            checkIfNeedCancel();
                            audioProcessor.end();
                        }
                    } catch (InterruptedException e) {
                        processorsError = new CancelProcessingException(e);
                    } catch (Throwable e) {
                        processorsError = e;
                        LOG.e(e);
                    } finally {
                        //执行释放操作
                        shouldRun = false;
                        for (AudioProcessor audioProcessor : audioProcessors) {
                            audioProcessor.release();
                        }

                        // TODO: 2017/8/14

                    }
                }
            };
        }

        private void checkIfNeedCancel() {
            if (cancel) {
                throw new CancelProcessingException();
            }
        }
    };

    public static class CancelProcessingException extends RuntimeException {

        public CancelProcessingException() {
            super("cancel processing");
        }

        public CancelProcessingException(Throwable throwable) {
            super(throwable);
        }
    }
}
