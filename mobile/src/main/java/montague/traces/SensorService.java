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

package montague.traces;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;

import montague.traces.sensors.SensorController;
import montague.traces.storage.LoggerUtils;

public class SensorService extends Service {

    public final String TAG = "SENSOR_SERVICE";

    public final static String EXTRA_TIMESTAMP= "EXTRA_TIMESTAMP";
    public final static String EXTRA_ACTION = "EXTRA_ACTION";


    private final IBinder mBinder = new SensorBinder();

    private SensorController mSensors;
    private long created;

    public SensorService() {
        Log.d(TAG,"created");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        created = intent.getLongExtra(EXTRA_TIMESTAMP,System.currentTimeMillis());
        created = LoggerUtils.dateTimeStamp(created);
        Log.d(TAG,"onStart:"+created);
        mSensors = new SensorController(this,created);

        if(mSensors !=null)
            mSensors.start();

        return Service.START_REDELIVER_INTENT;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind");
        return mBinder;
    }



    @Override
    public boolean stopService(Intent intent){
        Log.d(TAG,"stopped:"+DateUtils.getRelativeTimeSpanString(created,System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS));
        return true;

    }




    public class SensorBinder extends Binder{
        SensorService getService(){
            Log.d(TAG,"getService");
            return SensorService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy:"+DateUtils.getRelativeTimeSpanString(created,System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS));
        if(mSensors!= null)
            mSensors.stop();
        super.onDestroy();
    }
}
