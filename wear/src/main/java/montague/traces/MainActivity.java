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

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import montague.traces.services.SensorService;
import montague.traces.storage.Compress;
import montague.traces.storage.Logger;
import montague.traces.storage.LoggerUtils;

public class MainActivity extends Activity {

    private TextView mDebug;
    private Button mLogEvent;
    private ImageButton mBack;
    private int eventID=0;
    private Logger mEventLogger;
    private static String EventLog = "EventLog";
    long created;
    boolean isRunning=false;

    //private SensorController mSensors;
    private WakeLock wakeLock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "TracesWakeLock");


        created = LoggerUtils.dateTimeStamp(System.currentTimeMillis());

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mDebug = (TextView) stub.findViewById(R.id.debugInfo);
                //mDebug.setText("Event ID:"+eventID);
                mLogEvent = (Button) stub.findViewById(R.id.eventButton);
                mLogEvent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mEventLogger != null) {
                            mEventLogger.writeAsync(System.currentTimeMillis()+","+eventID);
                            eventID++;
                            mDebug.setText("Event ID:"+eventID);
                        }
                    }
                });

                ProgressBar pb = (ProgressBar)findViewById(R.id.progressBar);
                pb.setVisibility(View.INVISIBLE);

                mBack = (ImageButton)findViewById(R.id.backButton);
                mBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    created = extras.getLong("created");
                }
                mDebug.setText("Timestamp: "+created);
            }
        });


        //mSensors = new SensorController(getApplicationContext(),created);
        mEventLogger = new Logger(EventLog,50,created,Logger.FileFormat.csv);
    }

    @Override
    protected void onNewIntent(Intent intent){
        Bundle bundle = intent.getExtras();
        int message = bundle.getInt("message");
        created = bundle.getLong("created");

        switch (message){
            case 0:
                if(mDebug!=null)
                    mDebug.setText("TIMESTAMP:"+created);
                break;
            case StudyListener.MESSAGE_START_SENSORS: //start sensors
                start();
                break;
            case StudyListener.MESSAGE_STOP_SENSORS: //stop sensors
                stop();
                break;
            case StudyListener.MESSAGE_SEND_DATA: //zip and send files
                sendFiles();
                break;
            case StudyListener.MESSAGE_REMOVE_DATA: //remove files
                deleteAllData();
                break;
            case StudyListener.MESSAGE_STOP_COMPLETELY: //remove files
                if(wakeLock.isHeld())
                    wakeLock.release();
                finish();
                break;
            case StudyListener.MESSAGE_SEND_ALL_DATA: //remove files
                sendAllData();
                break;
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (mEventLogger != null)
            mEventLogger.flush();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
//        if(mSensors != null){
//            mSensors.stop();
//        }
        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }


    Handler handler = new Handler();
    private void sendFiles(){
        stop();
        handler.postDelayed(new Runnable(){
            public void run() {
                String zipName = Environment.getExternalStorageDirectory().getPath()+"/"+getResources().getString(R.string.app_name)+"/WEAR_"+created+".zip";
                String parentName = mEventLogger.parentDirectory();
                Compress compress = new Compress(parentName,zipName);
                if(compress.zip()) {
                    if (new File(zipName).exists()) {
                        Toast.makeText(getApplicationContext(), "SENDING ZIP", Toast.LENGTH_SHORT).show();
                        StudyListener sl = new StudyListener();
                        sl.sendLogfile(getApplicationContext(), zipName, "WEAR", created);
                        //delete the folder and files within in as we no longer need them once the zip is created.
                        //Compress.DeleteRecursive(new File(zipName));
                    } else {
                        Toast.makeText(getApplicationContext(), "COULD NOT ZIP/SEND", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }, 2000);

    }

    private void sendAllData() {

        String parentName = Environment.getExternalStorageDirectory().getPath() + "/" + getResources().getString(R.string.app_name);
        ArrayList<String> zips = Compress.ZipRecursive2(new File(parentName));

        if (zips != null) {
            final StudyListener sl = new StudyListener();
            if(zips.size() == 1){
                Toast.makeText(getApplicationContext(), zips.get(0), Toast.LENGTH_LONG).show();
                sl.sendLogfile(getApplicationContext(), zips.get(0), "WEAR", created);
                //Compress.DeleteRecursive(new File(zips.get(0)));
            }else {
                int i = 0;
                for (String zip : zips) {
                    long time = SystemClock.uptimeMillis() + (40000 * i) + 3000;
                    if (new File(zip).exists()) {
                        final String z = zip;
                        handler.postAtTime(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), z, Toast.LENGTH_LONG).show();
                                //Toast.makeText(getApplicationContext(), "SENDING ZIP", Toast.LENGTH_SHORT).show();
                                sl.sendLogfile(getApplicationContext(), z, "WEAR", created);
                                //delete the folder and files within in as we no longer need them once the zip is created.
                                //Compress.DeleteRecursive(new File(parentName));
                                //Compress.DeleteRecursive(new File(z));
                            }
                        }, time);
                    } else {
                        Toast.makeText(getApplicationContext(), "COULD NOT ZIP/SEND", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        }
    }

    private void deleteAllData(){
        String parentName = Environment.getExternalStorageDirectory().getPath() + "/" + getResources().getString(R.string.app_name);
        Compress.DeleteRecursive(new File(parentName));
    }

    private void start(){
        Intent i = new Intent(MainActivity.this, SensorService.class);
        i.putExtra(SensorService.EXTRA_TIMESTAMP, created);
        MainActivity.this.startService(i);

//        if(!isRunning) {
//            //mSensors.start();
//            //isRunning = true;
            if(wakeLock != null && wakeLock.isHeld() == false)
                wakeLock.acquire();
//        }

        // Get the SensorManager
        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // List of Sensors Available
        List<Sensor> msensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor tmp: msensorList){
            Log.d("SENSORS",tmp.getName());
        }
    }
    private void stop(){
        Intent i = new Intent(MainActivity.this, SensorService.class);
        MainActivity.this.stopService(i);

//        if(isRunning) {
//            mSensors.stop();
            if(wakeLock != null && wakeLock.isHeld())
                wakeLock.release();
            if (mEventLogger != null)
                mEventLogger.flush();
//        }
    }


}
