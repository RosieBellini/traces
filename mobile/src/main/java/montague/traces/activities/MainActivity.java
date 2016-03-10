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

package montague.traces.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import montague.traces.MobileStudyListener;
import montague.traces.R;
import montague.traces.SensorService;
import montague.traces.storage.Compress;
import montague.traces.storage.Logger;
import montague.traces.storage.LoggerUtils;


public class MainActivity extends Activity {

    MenuItem itemFileTrans;
    private static final int SYNC_REQUEST = 66766;
    private PowerManager.WakeLock wakeLock;
    boolean mLogging = false;

    String syncLog = "";

    TextView mDebug;

    Logger mEventLogger;
    static String EventLogger = "EventLog";
    private MobileStudyListener listener;
    int eventID;
    long created;

    NumberPicker letterP;
    NumberPicker numberP;
    NumberPicker directionP;
    String[] abc = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z".split(",");


    ArrayList<String> calibrationPoints = new ArrayList<>();

    View stateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TracesWakeLock");


        setContentView(R.layout.activity_main);
        eventID = 0;
        Button logButton = (Button)findViewById(R.id.eventButton);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logValues("X","X");
            }
        });


        letterP = (NumberPicker)findViewById(R.id.letterPicker);
        letterP.setMinValue(0);
        letterP.setMaxValue(25);
        letterP.setDisplayedValues(abc);
        letterP.setBackgroundColor(getResources().getColor(R.color.white));
        letterP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        numberP = (NumberPicker)findViewById(R.id.numberPicker);
        numberP.setMinValue(0);
        numberP.setMaxValue(50);
        numberP.setWrapSelectorWheel(false);
        numberP.setBackgroundColor(getResources().getColor(R.color.white));
        numberP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        directionP = (NumberPicker)findViewById(R.id.directionPicker);
        directionP.setMinValue(1);
        directionP.setMaxValue(4);
        directionP.setBackgroundColor(getResources().getColor(R.color.white));
        directionP.setWrapSelectorWheel(true);
        directionP.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);


        Button calibration = (Button)findViewById(R.id.calibrationButton);
        calibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mEventLogger != null){
                    String ch = abc[letterP.getValue()];
                    logValues(ch+""+numberP.getValue(),directionP.getValue()+"");
                    beep();
                    setStateView(1);
                }
            }
        });

        stateView = findViewById(R.id.stateView);
        stateView.setBackgroundResource(R.color.white);

        mDebug = (TextView)findViewById(R.id.debugInfo);
        mDebug.setText("Event ID:"+eventID);
        setupSensors();


    }

    private void logValues(String location, String direction) {
        if(mEventLogger != null){
            mEventLogger.writeAsync(System.currentTimeMillis()+","+eventID+","+location+","+direction);
            eventID++;
            mDebug.setText("Event ID:"+eventID);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_start_stop){
            if(!item.isChecked()){
                //item is checked start
                if(!mLogging) {
                    mLogging = true;
                    startSensorService();
                    if(wakeLock!= null && !wakeLock.isHeld())
                        wakeLock.acquire();
                    item.setChecked(true);
                }


            }else{
                //item is not checked stop.
                if(mLogging == true) {
                    mLogging = false;
                    stopSensorService();
                    if(wakeLock != null && wakeLock.isHeld())
                        wakeLock.release();
                    item.setChecked(false);
                }
                mEventLogger.flush();

            }
        }else if(id == R.id.action_sensor_status) {
            requestStatus();
        }
        return super.onOptionsItemSelected(item);
    }



    private void requestStatus() {
        listener.getSensorStatus(getApplicationContext(),created);
    }


    private void stopSensorService(){
        Intent i = new Intent(MainActivity.this, SensorService.class);
        stopService(i);
    }

    private void startSensorService(){
        Intent i = new Intent(MainActivity.this, SensorService.class);
        i.putExtra(SensorService.EXTRA_TIMESTAMP, LoggerUtils.dateTimeStamp(System.currentTimeMillis()));
        MainActivity.this.startService(i);
    }

    @Override
    protected void onActivityResult(int aRequestCode, int aResultCode, Intent data) {
        switch (aRequestCode) {
            case SYNC_REQUEST:
                if(aResultCode == Activity.RESULT_OK) {
                    Uri values = data.getData();
                    syncLog = values.toString();
                    listener.stopSensors(getApplicationContext(),created);
                }
                break;
        }
        super.onActivityResult(aRequestCode, aResultCode, data);
    }


    @Override
    protected void onResume(){
        super.onResume();

    }


    @Override
    protected void onPause(){
        super.onPause();
        mEventLogger.flush();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
//        if(listener !=null && itemFileTrans != null && itemFileTrans.isEnabled()) {
//            listener.stopLogger(getApplicationContext(), created);
//        }
        stopSensorService();

        zipSubFolders();
        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                MainActivity.this);
        alertDialogBuilder.setTitle("Quit Traces");
        alertDialogBuilder
                .setMessage("Are you sure you want to quit?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        if(mEventLogger != null)
                            mEventLogger.flush();
                        MainActivity.this.finish();
                        if(wakeLock != null && wakeLock.isHeld())
                            wakeLock.release();
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }




    private void zipSubFolders(){
        String rootDir = Environment.getExternalStorageDirectory().getPath()+"/"+ getResources().getString(R.string.app_name);
        String zipDir = rootDir+"/"+LoggerUtils.getName(getApplicationContext());

        File zipFolder = new File(zipDir);
        if(!zipFolder.exists())
            zipFolder.mkdirs();


        File root = new File(rootDir);
        File[] files = root.listFiles();
        if(files!=null && files.length > 0){
            for(File file:files){
                if(file.isDirectory()){
                    String zipName = zipDir+"/"+file.getName()+".zip";
                    Compress c = new Compress(file.getAbsolutePath(),zipName);
                    if(c.zip()){
                        //c.DeleteRecursive(file);
                   }
                }
            }
        }
    }


    /**
     *
     *  SENSOR CODE
     *
     */

    private void setupSensors(){
        created = LoggerUtils.dateTimeStamp(System.currentTimeMillis());

        mEventLogger = new Logger(EventLogger,100,created,Logger.FileFormat.csv);
        listener = new MobileStudyListener();

    }

    long CAL_DELAY = 10*1000;
    Handler handleBeep;
    Runnable runBeep;
    /**
     * CALIBRATION BEEPS
     */
    private void beep(){

        if(runBeep==null){
            runBeep= new Runnable() {
                @Override
                public void run() {
                    setStateView(2);
                    logValues("X","X");
                    changeValueByOne(directionP,true);
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        if(handleBeep == null){
            handleBeep = new Handler();
        }
        handleBeep.removeCallbacks(runBeep);
        handleBeep.postDelayed(runBeep,CAL_DELAY);
    }


    private void setStateView(int state){
        switch (state){
            case 0:
                stateView.setVisibility(View.INVISIBLE);
                break;
            case 1:
                stateView.setVisibility(View.VISIBLE);
                stateView.setBackgroundResource(R.color.red);
                break;
            case 2:
                stateView.setVisibility(View.VISIBLE);
                stateView.setBackgroundResource(R.color.green);
        }
    }



    private void changeValueByOne(final NumberPicker higherPicker, final boolean increment) {
        Method method;
        try {
            // refelction call for
            // higherPicker.changeValueByOne(true);
            method = higherPicker.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
            method.setAccessible(true);
            method.invoke(higherPicker, increment);

        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        }
    }


}
