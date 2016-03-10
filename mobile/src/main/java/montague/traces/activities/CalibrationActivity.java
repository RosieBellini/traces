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
import android.widget.ToggleButton;

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


public class CalibrationActivity extends Activity {


    public static String EXTRA_CALIBRATION_POINTS = "EXTRA_CALIBRATION_POINTS";
    public static String EXTRA_CALIBRATION_MODE = "EXTRA_CALIBRATION_MODE";


    public enum mode{
        STANDING,
        WALKING
    };

    MenuItem itemFileTrans;
    private static final int SYNC_REQUEST = 66766;
    private PowerManager.WakeLock wakeLock;
    boolean mLogging = false;


    String syncLog = "";

    TextView mDebug;
    TextView mProgress;

    Button previousButton;
    Button nextButton;

    ToggleButton modeToggle;

    Logger mEventLogger;
    static String EventLogger = "EventLog";
    private MobileStudyListener listener;
    int eventID;
    long created;

    mode mMode = mode.STANDING;

    String[] abc = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z".split(",");


    ArrayList<String> calibrationPoints;
    int calibrationIndex;

    View stateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TracesWakeLock");


        setContentView(R.layout.activity_calibration);
        eventID = 0;

        Button calibration = (Button)findViewById(R.id.calibrationButton);
        calibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mEventLogger != null){
                    String[] values = calibrationPoints.get(calibrationIndex).split(",");
                    logValues(values[0]+""+values[1],values[2]);
                    if(mMode == mode.STANDING) {
                        beep();
                        setStateView(1);
                    }else{
                        next();
                    }
                }
            }
        });



        stateView = findViewById(R.id.stateView);
        stateView.setBackgroundResource(R.color.black);

        mDebug = (TextView)findViewById(R.id.debugInfo);
        mProgress = (TextView)findViewById(R.id.progressInfo);


        previousButton = (Button)findViewById(R.id.previousButton);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previous();
            }
        });

        modeToggle = (ToggleButton)findViewById(R.id.toggleButton);
        modeToggle.setText(mMode.toString());
        modeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMode == mode.STANDING){
                    mMode = mode.WALKING;
                }else{
                    mMode = mode.STANDING;
                }
                modeToggle.setText(mMode.toString());
            }
        });

        nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

        calibrationIndex =0;
        setupSensors();


        Bundle extras = getIntent().getExtras();
        if(extras!=null){
            calibrationPoints = extras.getStringArrayList(EXTRA_CALIBRATION_POINTS);
            mMode = mode.valueOf(extras.getString(EXTRA_CALIBRATION_MODE,mode.STANDING.toString()));
            modeToggle.setText(mMode.toString());
            updateProgress();
        }else{
            calibrationPoints = new ArrayList<>();
        }


    }

    private void logValues(String location, String direction) {
        if(mEventLogger != null){
            mEventLogger.writeAsync(System.currentTimeMillis()+","+eventID+","+location+","+direction);
            eventID++;
            mDebug.setText(location+" - "+direction+" [Logged: "+eventID+"]");
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
            Intent i = new Intent(CalibrationActivity.this, SensorService.class);
            i.putExtra(SensorService.EXTRA_TIMESTAMP, LoggerUtils.dateTimeStamp(System.currentTimeMillis()));
            if(!item.isChecked()){
                //item is checked start
                if(mLogging == false) {
                    mLogging = true;
                    CalibrationActivity.this.startService(i);
                    if(wakeLock!= null && !wakeLock.isHeld())
                        wakeLock.acquire();
                    item.setChecked(true);
                }
            }else{
                //item is not checked stop.
                if(mLogging == true) {
                    mLogging = false;
                    stopService(i);
                    if(wakeLock != null && wakeLock.isHeld())
                        wakeLock.release();
                    item.setChecked(false);
                }
                mEventLogger.flush();

            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteFromWatch() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                CalibrationActivity.this);
        alertDialogBuilder.setTitle("Delete All");
        alertDialogBuilder
                .setMessage("Are you sure you want to delete all watch data?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        listener.removeFiles(getApplicationContext(),created);
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


    private void stopWatch(){
        //item is not checked stop.

        listener.stopSensors(getApplicationContext(), created);
        mLogging = false;
        stopSensorService();
        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        itemFileTrans.setEnabled(true);
        mEventLogger.flush();
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

    private void stopSensorService(){
        Intent i = new Intent(CalibrationActivity.this, SensorService.class);
        stopService(i);
    }

    private void startSensorService(){
        Intent i = new Intent(CalibrationActivity.this, SensorService.class);
        i.putExtra(SensorService.EXTRA_TIMESTAMP, LoggerUtils.dateTimeStamp(System.currentTimeMillis()));
        CalibrationActivity.this.startService(i);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                CalibrationActivity.this);
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
                        CalibrationActivity.this.finish();
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
        File root = new File(rootDir);
        File[] files = root.listFiles();
        if(files!=null && files.length > 0){
            for(File file:files){
                if(file.isDirectory()){
                    String zipName = file.getAbsolutePath()+".zip";
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

    long CAL_DELAY = 30*1000;//todo make this a setting somewhere - asked sebastian.
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
                    next();
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


    private void next(){
        if(calibrationIndex < calibrationPoints.size()-1){
            calibrationIndex++;
            updateProgress();
        }
    }

    long MOVE_DELAY = 2000;
    private void updateProgress() {

        int remaining = (calibrationPoints.size()-1)-calibrationIndex;

        long minToGo = ((remaining*(CAL_DELAY+MOVE_DELAY))/1000)/60;


        mProgress.setText((calibrationIndex + 1) + "/" + calibrationPoints.size()+"\t [ETA: "+minToGo+" minutes]");
        String[] values= calibrationPoints.get(calibrationIndex).split(",");
        mDebug.setText(values[0]+""+values[1]+" - "+values[2]);

        if(calibrationIndex > 0)
            previousButton.setVisibility(View.VISIBLE);
        else
            previousButton.setVisibility(View.INVISIBLE);


        if(calibrationIndex < calibrationPoints.size()-1)
            nextButton.setVisibility(View.VISIBLE);
        else
            nextButton.setVisibility(View.INVISIBLE);
    }

    private void previous(){
        if(calibrationIndex > 0){
            calibrationIndex--;
            updateProgress();
        }
    }
}
