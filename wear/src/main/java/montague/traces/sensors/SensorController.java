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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;

import montague.traces.R;

/**
 * Created by Kyle Montague on 10/05/15.
 */
public class SensorController {

    private final SharedPreferences sp;
    BLEDevice mBLE;
    MotionDevice mMotion;
    BatteryDevice mBattery;
    MagnetDevice mMagnet;
    TemperatureDevice mTemperture;
    AmbientNoiseDevice mNoise;
    Context mContext;

    private final long AUDIO_SAMPLE_DELAY = 1000*60*5; //5 minutes



    public SensorController(Context context, long timestamp){
        mContext=context;
        sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        mBLE = BLEDevice.shared(context,timestamp);
        mMotion = MotionDevice.shared(context,timestamp);
        mBattery = BatteryDevice.shared(context,timestamp);
        mMagnet = MagnetDevice.shared(context,timestamp);
        mTemperture = TemperatureDevice.shared(timestamp);
        mNoise = AmbientNoiseDevice.shared(timestamp);
    }

    public void start(){
        if(sp.getBoolean(mContext.getString(R.string.PREF_SENSOR_BLE),true))
            mBLE.start();
        //check if both acc and gyro should be enabled
        if(sp.getBoolean(mContext.getString(R.string.PREF_SENSOR_ACC),true))
            mMotion.ENABLE_ACC = true;
        if(sp.getBoolean(mContext.getString(R.string.PREF_SENSOR_GYRO),false))
            mMotion.ENABLE_GYRO = true;
        if(mMotion.ENABLE_ACC || mMotion.ENABLE_GYRO)
            mMotion.start();
        if(sp.getBoolean(mContext.getString(R.string.PREF_SENSOR_MAG),true))
            mMagnet.start();
        if(sp.getBoolean(mContext.getString(R.string.PREF_SENSOR_AMBIENT_NOISE),true))
            mNoise.startSampling(AUDIO_SAMPLE_DELAY);

        mBattery.start();
    }

    public void stop(){
        if(sp.getBoolean(mContext.getString(R.string.PREF_SENSOR_BLE),true))
            mBLE.stop();
        if(mMotion.ENABLE_ACC || mMotion.ENABLE_GYRO)
            mMotion.stop();
        if(sp.getBoolean(mContext.getString(R.string.PREF_SENSOR_MAG),true))
            mMagnet.stop();
        if(sp.getBoolean(mContext.getString(R.string.PREF_SENSOR_AMBIENT_NOISE),true))
            mNoise.stopSampling();
        mTemperture.stop();
        mBattery.stop();
    }


    public ArrayList<String> getFiles(){
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.add(mBLE.getFilename());
        tmp.add(mMotion.getAccFilename());
        tmp.add(mMotion.getGyroFilename());
        tmp.add(mMagnet.getFilename());
        tmp.add(mBattery.getFilename());
        tmp.add(mTemperture.getFilename());
        tmp.add(mNoise.getFilename());
        return tmp;
    }
}
