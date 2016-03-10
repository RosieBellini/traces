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

import java.util.ArrayList;

/**
 * Created by Kyle Montague on 10/05/15.
 */
public class SensorController {

    BLEDevice mBLE;
    MotionDevice mMotion;
    BatteryDevice mBattery;
    MagnetDevice mMagnet;
    TemperatureDevice mTemperture;
//    AmbientNoiseDevice mNoise;

    private final long AUDIO_SAMPLE_DELAY = 1000*60*5; //5 minutes



    public SensorController(Context context, long timestamp){
        mBLE = BLEDevice.shared(context,timestamp);
        mMotion = MotionDevice.shared(context,timestamp);
        mBattery = BatteryDevice.shared(context,timestamp);
        mMagnet = MagnetDevice.shared(context,timestamp);
        mTemperture = TemperatureDevice.shared(timestamp);
//        mNoise = AmbientNoiseDevice.shared(timestamp);
    }

    public void start(){
        mBLE.start();
        mMotion.start();
        mBattery.start();
        mMagnet.start();
//        mNoise.startSampling(AUDIO_SAMPLE_DELAY);

    }

    public void stop(){
        mBLE.stop();
        mMotion.stop();
        mBattery.stop();
        mMagnet.stop();
//        mNoise.stopSampling();
        mTemperture.stop();
    }


    public ArrayList<String> getFiles(){
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.add(mBLE.getFilename());
        tmp.add(mMotion.getAccFilename());
        tmp.add(mMotion.getGyroFilename());
        tmp.add(mMagnet.getFilename());
        tmp.add(mBattery.getFilename());
        tmp.add(mTemperture.getFilename());
//        tmp.add(mNoise.getFilename());
        return tmp;
    }
}
