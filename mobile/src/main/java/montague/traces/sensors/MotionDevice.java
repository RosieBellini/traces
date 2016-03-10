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

/**
 * Created by kyle montague on 05/12/2014.
 */


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import montague.traces.storage.Logger;

public class MotionDevice {
    int accAccuracy;
    int gyroAccuracy;

    private Logger mAccLogger;
    private Logger mGyroLogger;

    private static String ACCELEROMETER = "ACCELEROMETER";
    private static String GYRO = "GYRO";
    private Context mContext;

    public static int LATENCY = 1000*60*2; //2 minutes
    HandlerThread mAccHandlerThread = new HandlerThread("accThread");
    HandlerThread mGyroHandlerThread = new HandlerThread("gyroThread");
    Handler accHandler;
    Handler gyroHandler;

    boolean isActive=false;


    int threadIndex=0;
    private SensorManager sm;




    private static MotionDevice mShared;
    public static MotionDevice shared(Context context, long timestamp){
        if(mShared == null)
            mShared = new MotionDevice(context, timestamp);
        return mShared;
    }


    public MotionDevice(Context context, long timestamp){
        mContext = context;
        mAccLogger = new Logger(MotionDevice.ACCELEROMETER,LATENCY,timestamp, Logger.FileFormat.csv);
        mGyroLogger = new Logger(MotionDevice.GYRO,LATENCY,timestamp, Logger.FileFormat.csv);
    }

    public void start() {
        if (sm == null)
            sm = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        accAccuracy = 1;
        gyroAccuracy = 1;
        if (Build.VERSION.SDK_INT >= 21){
                mAccHandlerThread = new HandlerThread("accThread"+threadIndex);
                mAccHandlerThread.start();
                accHandler = new Handler(mAccHandlerThread.getLooper());

                mGyroHandlerThread = new HandlerThread("gyroThread"+threadIndex);
                mGyroHandlerThread.start();
                gyroHandler = new Handler(mGyroHandlerThread.getLooper());
            sm.registerListener(hardwareListener, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST,LATENCY,accHandler);
            sm.registerListener(hardwareListener, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST,LATENCY,gyroHandler);
        }else{
            sm.registerListener(hardwareListener, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST,LATENCY);
            sm.registerListener(hardwareListener, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST,LATENCY);
        }
        threadIndex++;
        isActive = true;
    }

    public void stop() {
        if (sm == null || isActive ==false)
            return;
        sm.unregisterListener(hardwareListener);
        mAccHandlerThread.quitSafely();
        mGyroHandlerThread.quitSafely();
        mAccLogger.flush();
        mGyroLogger.flush();
        reset();
        isActive = false;
        sm = null;
    }

    private void reset() {
        accAccuracy = 0;
        gyroAccuracy = 0;
        sensorTimeReference = 0l;
        myTimeReference = 0l;
    }

    private long sensorTimeReference = 0l;
    private long myTimeReference = 0l;

    private SensorEventListener hardwareListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accAccuracy = acc;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    gyroAccuracy = acc;
                    break;
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //output to csv file timestmp,x,y,z
            // set reference times
            if(sensorTimeReference == 0l && myTimeReference == 0l) {
                sensorTimeReference = event.timestamp;
                myTimeReference = System.currentTimeMillis();
            }
            // set event timestamp to current time in milliseconds
            event.timestamp = myTimeReference + Math.round((event.timestamp - sensorTimeReference) / 1000000.0);
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccLogger.writeAsync(event.timestamp + "," + event.values[0] + "," + event.values[1] + "," + event.values[2]+","+accAccuracy);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    mGyroLogger.writeAsync(event.timestamp + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + ","+gyroAccuracy);
                    break;
            }
        }
    };


    public String getAccFilename(){
        return mAccLogger.getFilename();
    }

    public String getGyroFilename(){
        return mGyroLogger.getFilename();
    }
}
