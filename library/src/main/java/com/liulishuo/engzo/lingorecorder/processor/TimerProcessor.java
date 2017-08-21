package com.liulishuo.engzo.lingorecorder.processor;

public class TimerProcessor implements AudioProcessor {

    private long mTimeInMills = Integer.MAX_VALUE;
    private long startTime = 0;
    private CallBack callBack;


    private boolean needExit = false;
    private long flowTime;

    public TimerProcessor(long timeInMills) {
        mTimeInMills = timeInMills;
    }

    public TimerProcessor(long timeInMills,CallBack callBack) {
        mTimeInMills = timeInMills;
        this.callBack=callBack;
    }

    @Override
    public void start() {
        flowTime=0;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void flow(byte[] bytes, int size) {
        flowTime = System.currentTimeMillis() - startTime;
        needExit = flowTime >= mTimeInMills;
        if(callBack!=null){
            callBack.showFlowTime(flowTime);
        }
    }

    @Override
    public boolean needExit() {
        return needExit;
    }

    @Override
    public void end() {

    }

    @Override
    public void release() {

    }

    public long getFlowTime(){
        return flowTime;
    }
    public interface CallBack{
        void showFlowTime(long flowTime);
    }
}
