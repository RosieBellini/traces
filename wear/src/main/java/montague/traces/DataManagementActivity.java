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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import montague.traces.storage.Compress;

import static montague.traces.DataManagementActivity.MODE.ARE_YOU_SURE;
import static montague.traces.DataManagementActivity.MODE.DATA_SYNC;
import static montague.traces.DataManagementActivity.MODE.DELETING_DATA;
import static montague.traces.DataManagementActivity.MODE.FAILED_SYNC;
import static montague.traces.DataManagementActivity.MODE.SENDING_DATA;
import static montague.traces.DataManagementActivity.MODE.SUCCESS_DELETE;
import static montague.traces.DataManagementActivity.MODE.SUCCESS_SYNC;
import static montague.traces.DataManagementActivity.MODE.ZIPPING_DATA;

/**
 * Created by Kyle Montague on 23/11/15.
 */
public class DataManagementActivity extends Activity {


    public static String EXTRA_MODE = "EXTRA_MODE";

    public enum MODE{
        CHECK_DATA,
        CHECK_CONNECTION,
        DATA_SYNC,
        DELETE,
        DELETING_DATA,
        SUCCESS_DELETE,
        SUCCESS_SYNC,
        FAILED_SYNC,
        SENDING_DATA,
        ZIPPING_DATA,
        ARE_YOU_SURE,
        CALIBRATE_MAG_SENSOR
    }

    TextView mText;
    Button mButton;
    ImageButton mBack;
    ProgressBar mProgress;
    MODE mMode = DATA_SYNC;
    Vibrator mVibrate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mVibrate = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mText = (TextView) findViewById(R.id.debugInfo);
                mButton = (Button) findViewById(R.id.eventButton);
                mProgress = (ProgressBar) findViewById(R.id.progressBar);
                mBack = (ImageButton) findViewById(R.id.backButton);
                mBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
                setupIntent(getIntent());
                setupInterface(mMode);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setupIntent(intent);
    }



    @Override
    protected void onResume() {
        super.onResume();

        if(studyListener==null)
            studyListener = new StudyListener(getApplicationContext());
    }

    private void setupIntent(Intent intent){
        Bundle bundle = intent.getExtras();
        if(bundle.containsKey(EXTRA_MODE)){
            MODE tmp = (MODE)bundle.getSerializable(EXTRA_MODE);
            if(tmp != null)
                mMode = tmp;
        }


        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selected();
            }
        });
    }

    private void selected() {
        switch (mMode) {
            case CHECK_DATA:
                if(checkData()) {
                    mMode = SUCCESS_SYNC;
                    setupInterface(mMode);
                }else
                    mText.setText(R.string.text_info_check_again);
                break;
            case CHECK_CONNECTION:
                if(checkConnected())
                    mText.setText(R.string.text_connected_mobile);
                else
                    mText.setText(R.string.text_check_ble_again);
                break;
            case DATA_SYNC:
                sendData();
                break;
            case DELETE:
                removeData();
                break;
            case FAILED_SYNC:
                sendData();
                break;
            default:
                success();
                break;
        }
    }

    private boolean removeData() {
        if(checkData()) {
            if(deleteCount == 0){
                setupInterface(ARE_YOU_SURE);
                deleteCount++;
            }else {
                setupInterface(DELETING_DATA);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String parentName = Environment.getExternalStorageDirectory().getPath() + "/" + getResources().getString(R.string.app_name);
                        Compress.DeleteRecursive(new File(parentName));
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mMode = SUCCESS_DELETE;
                                setupInterface(mMode);
                            }
                        });
                    }
                }).start();
            }
        }else{
            //no data
        }
        return false;
    }

    private boolean sendData() {
        if(checkData() && checkConnected()) {
            setupInterface(ZIPPING_DATA);
            new Thread(new Runnable() {
                @Override
                public void run() {

                    if(studyListener.sendAllData(getApplicationContext())) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                setupInterface(SENDING_DATA);
                            }
                        });
                    }else{
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mMode = FAILED_SYNC;
                                setupInterface(mMode);
                            }
                        });
                    }
                }
            }).start();
        }else{
            //error message.
        }
        return false;
    }
    StudyListener studyListener;
    private boolean checkConnected() {
        return studyListener.isNear();
    }

    private boolean checkData() {
        String parentName = Environment.getExternalStorageDirectory().getPath() + "/" + getResources().getString(R.string.app_name);
        File parent = new File(parentName);
        if(parent.exists()){
            File[] files = parent.listFiles();
            if(files != null && files.length != 0)
                return true;
        }
        return false;
    }




    private void success(){
       finish();
    }

    int deleteCount = 0;

    private void setupInterface(MODE mode){
        long[] pattern = {0, 1000};
        if(mButton.getVisibility() == View.INVISIBLE)
            mButton.setVisibility(View.VISIBLE);

        if(mBack.getVisibility() == View.INVISIBLE)
            mBack.setVisibility(View.VISIBLE);

        if(mProgress.getVisibility() == View.VISIBLE)
            mProgress.setVisibility(View.INVISIBLE);

        if(mButton!=null && mText != null) {
            switch (mode) {
                case CHECK_DATA:
                    mButton.setText(R.string.text_check_data);
                    mText.setText("");
                    break;
                case CHECK_CONNECTION:
                    mButton.setText(R.string.text_check_connection);
                    mText.setText(R.string.text_info_ble_on);
                    break;
                case DATA_SYNC:
                    checkConnected();
                    if(checkData()) {
                        mButton.setText(R.string.text_send_data);
                        mText.setText(R.string.text_info_sync_data);
                    }else{
                        mButton.setText(R.string.text_done);
                        mButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
                        mText.setText(R.string.text_info_no_data);
                    }


                    break;
                case DELETE:
                    deleteCount = 0;
                    if(checkData()) {
                        mButton.setText(R.string.text_remove_data);
                        mText.setText(R.string.text_info_remove_data);
                    }else{
                        mButton.setText(R.string.text_done);
                        mButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
                        mText.setText(R.string.text_info_no_data);
                    }
                    break;
                case ARE_YOU_SURE:
                    mButton.setText(R.string.text_yes_delete);
                    mText.setText(R.string.text_info_confirm_delete);
                    break;
                case SUCCESS_SYNC:
                    mButton.setText(R.string.text_done);
                    mText.setText(R.string.text_info_success);

                    mVibrate.vibrate(pattern,-1);
                    break;
                case SENDING_DATA:
                    mButton.setVisibility(View.INVISIBLE);
                    mText.setText(R.string.text_info_sending_data);
                    mProgress.setVisibility(View.VISIBLE);
                    break;
                case FAILED_SYNC:
                    mButton.setText(R.string.text_try_again);
                    mText.setText(R.string.text_info_sending_failed);
                    break;
                case ZIPPING_DATA:
                    mButton.setVisibility(View.INVISIBLE);
                    mText.setText(R.string.text_info_zipping_data);
                    mProgress.setVisibility(View.VISIBLE);
                    break;
                case DELETING_DATA:
                    mButton.setVisibility(View.INVISIBLE);
                    mText.setText(R.string.text_info_deleting_data);
                    mProgress.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS_DELETE:
                    mButton.setText(R.string.text_done);
                    mText.setText(R.string.text_info_success_delete);

                    mVibrate.vibrate(pattern,-1);
                    break;
                case CALIBRATE_MAG_SENSOR:
                    mButton.setText(R.string.text_done);
                    mButton.setVisibility(View.INVISIBLE);
                    mText.setText(R.string.text_info_calibration);
                    mProgress.setVisibility(View.VISIBLE);
                    mBack.setVisibility(View.INVISIBLE);
                    Handler calHandler = new Handler();
                    calHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mText.setText(R.string.text_info_calibration_complete);
                            mButton.setVisibility(View.VISIBLE);
                            mProgress.setVisibility(View.INVISIBLE);
                            mVibrate.vibrate(new long[]{0, 1000, 100,1000},-1);
                        }
                    },1000*20);
                    break;
            }
        }
    }
}
