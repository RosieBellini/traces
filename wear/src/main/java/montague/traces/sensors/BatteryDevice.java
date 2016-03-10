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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import montague.traces.services.SensorService;
import montague.traces.storage.Logger;

/**
 * Created by kyle montague on 10/05/15.
 */
public class BatteryDevice extends BroadcastReceiver {

    private static BatteryDevice mShared;
    public static BatteryDevice shared(Context context, long timestamp){
        if(mShared == null)
            mShared = new BatteryDevice(context, timestamp);
        return mShared;
    }


    Context mContext;
    Logger mLogger;
    boolean isRegistered = false;
    static String Battery = "Battery";

    public BatteryDevice(Context context, long timestamp){
        mContext = context;
        mLogger = new Logger(BatteryDevice.Battery,20,timestamp, Logger.FileFormat.csv);
    }

    public void start(){
        if(!isRegistered) {
            mContext.registerReceiver(this, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            isRegistered = true;
        }
    }

    public void stop(){
        if(isRegistered) {
            mContext.unregisterReceiver(this);
            isRegistered = false;
        }
        mLogger.flush();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int batteryLevel = intent.getIntExtra(
                BatteryManager.EXTRA_LEVEL, 0);
        int maxLevel = intent
                .getIntExtra(BatteryManager.EXTRA_SCALE, 0);
        int batteryHealth = intent.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN);
        int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
        //float batteryPercentage = ((float) batteryLevel / (float) maxLevel) * 100;
        mLogger.writeAsync(System.currentTimeMillis()+","+batteryLevel+","+temp+","+batteryHealth);

        //stop sensors when battery reaches switch off threshold (3%)
        if(batteryLevel <= 3) {
            Intent i = new Intent(context, SensorService.class);
            context.stopService(i);
        }
    }


    public String getFilename(){
        return mLogger.getFilename();
    }
}
