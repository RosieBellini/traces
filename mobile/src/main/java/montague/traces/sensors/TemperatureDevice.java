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

import montague.traces.storage.Logger;

/**
 * Created by kylemontague on 10/11/15.
 */
public class TemperatureDevice {

    private static final String TAG = "ESTIMOTE";
    private static TemperatureDevice mShared;
    public static TemperatureDevice shared(long timestamp){
        if(mShared==null){
            mShared = new TemperatureDevice(timestamp);
        }
        return mShared;
    }

    public TemperatureDevice(long timestamp){
        mLogger = new Logger(NAME,500,timestamp, Logger.FileFormat.csv);
    }

    public static String NAME = "TEMPERATURE";
    private Logger mLogger;

    public void addTemp(long timestamp, String MAC, float temperature, long elapsedTime, int batteryVoltage){
        mLogger.writeAsync(timestamp+","+MAC+","+temperature+","+elapsedTime+","+batteryVoltage);
    }


    public void stop(){
        mLogger.flush();
    }

    public String getFilename(){
        return mLogger.getFilename();
    }
}
