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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import montague.traces.storage.Logger;

/**
 * Created by kylemontague on 10/05/15.
 */
public class MagnetDevice {

    private static MagnetDevice mShared;
    private Context mContext;
    private Logger mMagLogger;

    private SensorManager sm;
    public static int LATENCY = 1000*60*5; //5 minutes
    boolean isActive=false;
    int accuracy =0;

    private HandlerThread mMagHandlerThread;
    static String Magnet = "Magnet";
    int threadIndex=0;

    public static MagnetDevice shared(Context context, long timestamp){
        if(mShared == null)
            mShared = new MagnetDevice(context, timestamp);
        return mShared;
    }


    public MagnetDevice(Context context, long timestamp){
        mContext = context;
        mMagLogger = new Logger(MagnetDevice.Magnet,LATENCY/10,timestamp, Logger.FileFormat.csv);
    }


    public void start(){
        if (sm == null)
            sm = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        isActive = true;
        if (Build.VERSION.SDK_INT >= 21) {
            mMagHandlerThread = new HandlerThread("magThread" + threadIndex);
            mMagHandlerThread.start();
            Handler magHandler = new Handler(mMagHandlerThread.getLooper());
            sm.registerListener(hardwareListener, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST, LATENCY, magHandler);
            threadIndex++;
        }else {
            sm.registerListener(hardwareListener, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST, LATENCY);
        }
    }


    public void stop(){
        if (sm == null || isActive ==false)
            return;
        sm.unregisterListener(hardwareListener);
        if(mMagHandlerThread != null) {
            mMagHandlerThread.quitSafely();
        }
        mMagLogger.flush();
        reset();
        isActive = false;
        sm = null;
    }

    private void reset() {
        accuracy=0;
        sensorTimeReference = 0l;
        myTimeReference = 0l;
    }

    private long sensorTimeReference = 0l;
    private long myTimeReference = 0l;


    private SensorEventListener hardwareListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
            switch (sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    accuracy = acc;
                    break;

            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //output to csv file timestmp,x,y,z


            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if(sensorTimeReference == 0l && myTimeReference == 0l) {
                        sensorTimeReference = event.timestamp;
                        myTimeReference = System.currentTimeMillis();
                    }
                    event.timestamp = myTimeReference + Math.round((event.timestamp - sensorTimeReference) / 1000000.0);
                    mMagLogger.writeAsync(event.timestamp + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + ","+accuracy);
                    break;
            }
        }
    };


    public String getFilename(){
        return mMagLogger.getFilename();
    }
}
