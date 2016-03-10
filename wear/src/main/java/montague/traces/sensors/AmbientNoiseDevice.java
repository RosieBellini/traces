/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Kyle Montague
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package montague.traces.sensors;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;

import montague.traces.storage.Logger;

/**
 * Created by Kyle Montague on 10/11/15.
 */
public class AmbientNoiseDevice {

    public static String NAME = "AMBIENT_NOISE";
    private static AmbientNoiseDevice mShared;
    public static AmbientNoiseDevice shared(long timestamp){
        if(mShared ==null)
            mShared = new AmbientNoiseDevice(timestamp);
        return mShared;
    }


    private Logger mLogger;
    private SoundMeter soundMeter;

    long SAMPLE_RATE = 100;
    long SAMPLE_DURATION = 5000;
    boolean shouldSample = false;
    long startTime;
    ArrayList<Double> audioSamples;
    Handler sampleHandler = new Handler();
    Handler sampleDoneHandler = new Handler();
    Runnable measure = new Runnable() {
        @Override
        public void run() {
            if(shouldSample) {
                double value =soundMeter.getAmplitude();
                audioSamples.add(value);
                sampleHandler.postDelayed(measure, SAMPLE_RATE);
            }
        }
    };

    Runnable stopSampling = new Runnable() {
        @Override
        public void run() {
            stop();
        }
    };



    private long sampleDelay = 1000*60*5; //5 minutes
    boolean shouldLoop = false;
    Handler sampleLoopHandler = new Handler();
    Runnable startLoop = new Runnable() {
        @Override
        public void run() {
            if(shouldLoop) {
                sampleSensor();
                sampleLoopHandler.postDelayed(startLoop, sampleDelay);
            }
        }
    };

    public void startSampling(long delay){
        sampleDelay=delay;
        shouldLoop =true;
        sampleSensor();
        sampleLoopHandler.postDelayed(startLoop,sampleDelay);
    }

    public void stopSampling(){
        shouldLoop = false;
        stop();
        mLogger.flush();
    }

    public AmbientNoiseDevice(long timestamp){
        soundMeter = new SoundMeter();
        mLogger = new Logger(NAME,500,timestamp, Logger.FileFormat.csv);
    }



    public void sampleSensor(long duration){
        start();
        sampleDoneHandler.postDelayed(stopSampling, duration);
    }

    public void sampleSensor(){
        sampleSensor(SAMPLE_DURATION);
    }

    private void start(){
        sampleHandler.removeCallbacks(measure);
        soundMeter.start();
        audioSamples = new ArrayList<>();
        shouldSample = true;
        startTime = System.currentTimeMillis();
        sampleHandler.postDelayed(measure, SAMPLE_RATE);
    }

    private void stop(){
        shouldSample = false;
        soundMeter.stop();
        writeToLog();
    }


    private void writeToLog(){
        if(audioSamples.size() > 0) {
            String values = "[";
            for (double v : audioSamples) {
                values += v + ";";
            }
            values += "]";
            mLogger.writeAsync(String.format("%d,%s", startTime, values));
            audioSamples = new ArrayList<>();
        }
    }




    public String getFilename(){
        return mLogger.getFilename();
    }


    public class SoundMeter {

        static final private double EMA_FILTER = 0.6;

        private AudioRecord mRecorder = null;
        private double mEMA = 0.0;

        private volatile double amplitude = 0;
        private boolean mIsRecording =false;

        public static final int AUDIO_SAMPLE_RATE = 16000;
        private short[] mBuffer;
        private void initRecorder() {
            int bufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            mBuffer = new short[bufferSize];
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        }

        private void startRecorder() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mRecorder.startRecording();
                    mIsRecording = true;
                    while (mIsRecording) {
                        double sum = 0;
                        int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
                        for (int i = 0; i < readSize; i++) {
                            sum += mBuffer[i] * mBuffer[i];
                        }
                        if (readSize > 0) {
                            amplitude = sum / (readSize*1.0);
                        }
                    }
                }
            }).start();
        }



        public void start() {
            if (mRecorder == null) {
                initRecorder();
                startRecorder();
            }
        }

        public void stop() {
            Log.d("RECORDER", "STOPPED");
            mIsRecording = false;
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
        }




        public double getAmplitude() {
            return amplitude;
        }

        public double getAmplitudeEMA() {
            double amp = getAmplitude();
            mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
            return mEMA;
        }
    }

}
