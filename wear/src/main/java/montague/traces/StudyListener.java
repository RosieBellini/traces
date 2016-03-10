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

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.util.ArrayList;

import montague.traces.services.SensorService;
import montague.traces.storage.Compress;
import montague.traces.storage.LoggerUtils;

/**
 * Created by kyle montague on 11/05/15.
 */
public class StudyListener extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public static final int MESSAGE_START_SENSORS = 1;
    public static final int MESSAGE_STOP_SENSORS = 2;
    public static final int MESSAGE_SEND_DATA = 3;
    public static final int MESSAGE_REMOVE_DATA = 4;
    public static final int MESSAGE_STOP_COMPLETELY = 5;
    public static final int MESSAGE_START = 0;
    public static final int MESSAGE_SEND_ALL_DATA = 9;


    public static final String RESPONSE_MESSAGE_SERVICE_STATUS = "TRACES.MSG.SERVICE_STATUS";


    public static final String ACTION_SET_STUDY_DETAILS = "TRACES.MSG.SET_STUDY_DETAILS";
    public static final String ACTION_GET_STUDY_DETAILS = "TRACES.MSG.GET_STUDY_DETAILS";


    public static String ACTION_START_SYNC = "TRACES.MSG.SYNC_START";
    public static String ACTION_SYNC_COMPLETE = "TRACES.MSG.SYNC_COMPLETE";
    public static String ACTION_START_LOGGER = "TRACES.MSG.LOGGER_START";
    public static String ACTION_STOP_LOGGER = "TRACES.MSG.LOGGER_STOP";

    public static String ACTION_START_SENSORS = "TRACES.MSG.SENSORS_START";
    public static String ACTION_STOP_SENSORS = "TRACES.MSG.SENSORS_STOP";

    public static String ACTION_SEND_FILES = "TRACES.MSG.SEND_FILES";
    public static String ACTION_REMOVE_FILES = "TRACES.MSG.REMOVE_FILES";


    public static String ACTION_ZIPPED_FILES = "/logfile";
    public static String ACTION_GET_ALL_FILES = "TRACES.MSG.SEND_ALL_FILES";

    public static String ACTION_GET_SERVICE_STATUS = "TRACES.MSG.GET_SERVICE_STATUS";
    
    
    public static String ACTION_CONFIRM_DATA_SYNCED = "TRACES.MSG.CONFIRM_DATA_SYNCED";



    private GoogleApiClient mGoogleApiClient;
    private String TAG = "STUDYLISTENER";


    public StudyListener(Context context){
        init(context);
    }

    public StudyListener(){

    }


    @Override
    public void onCreate() {
        super.onCreate();
        init(StudyListener.this);
    }


    public void init(Context context){
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }


    public boolean isNear(){
       return isConnected;
    }

    boolean isConnected = false;

    @Override
    public void onConnected(Bundle bundle) {
        isConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        isConnected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        isConnected = false;
    }



    @Override
    public void onMessageReceived(MessageEvent messageEvent){
        final String message = new String(messageEvent.getData());
        String[]values = message.split(",");
        long created =0;
        if(values.length >0 && values[0].length() > 1)
            created = Long.valueOf(values[0]);

        Intent i = new Intent(StudyListener.this,MainActivity.class);
        i.putExtra("created",created);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_NO_HISTORY);

        String msgPath = messageEvent.getPath();
        
        if(msgPath.contains(ACTION_SEND_FILES)){
            i.putExtra("message",MESSAGE_SEND_DATA);
            startActivity(i);
        }else if(msgPath.contains(ACTION_GET_ALL_FILES)){
            sendAllData(StudyListener.this);
        }else if(msgPath.contains(ACTION_REMOVE_FILES)) {
            String parentName = Environment.getExternalStorageDirectory().getPath() + "/" + getResources().getString(R.string.app_name);
            Compress.DeleteRecursive(new File(parentName));

        }else if(msgPath.contains(ACTION_START_LOGGER) || msgPath.contains(ACTION_SYNC_COMPLETE)) {
            i = new Intent(StudyListener.this,MainActivity.class);
            i.putExtra("message",MESSAGE_START);
            i.putExtra("created",created);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(i);
        }else if(msgPath.contains(ACTION_STOP_LOGGER)) {
            i.putExtra("message",MESSAGE_STOP_COMPLETELY);
            startActivity(i);
        }else if(msgPath.contains(ACTION_START_SENSORS)) {
            i = new Intent(StudyListener.this, SensorService.class);
            i.putExtra(SensorService.EXTRA_TIMESTAMP, LoggerUtils.dateTimeStamp(created));
            StudyListener.this.startService(i);

        }else if(msgPath.contains(ACTION_STOP_SENSORS)) {
//            i.putExtra("message",MESSAGE_STOP_SENSORS);
//            startActivity(i);

            i = new Intent(StudyListener.this, SensorService.class);
            stopService(i);
            
        }else if(msgPath.contains(ACTION_START_SYNC)) {
            i = new Intent(StudyListener.this, SyncActivity.class);
            i.putExtra("created", created);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(i);
        }else if(msgPath.contains(ACTION_GET_SERVICE_STATUS)) {
            boolean status = isMyServiceRunning(SensorService.class);
            sendMessage(this, RESPONSE_MESSAGE_SERVICE_STATUS, status + "");
        }else if(msgPath.contains(ACTION_SET_STUDY_DETAILS)){
            if(values.length > 3){
                setStudyDetails(values[1],values[2]);
            }
        }else if(msgPath.contains(ACTION_CONFIRM_DATA_SYNCED)){
            if(values.length > 1){
                boolean state = Boolean.valueOf(values[1]);
                i = new Intent(StudyListener.this, DataManagementActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                i.putExtra(DataManagementActivity.EXTRA_MODE, (state)?DataManagementActivity.MODE.SUCCESS_SYNC:DataManagementActivity.MODE.FAILED_SYNC);
                startActivity(i);
            }
        }else{
            super.onMessageReceived(messageEvent);
        }
    }

    public void sendMessage(Context context, final String key, final String message){
        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }

        mGoogleApiClient.connect();

        if (mGoogleApiClient.isConnected()) {
            Log.v(TAG, "Is Connected");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                mGoogleApiClient,
                                node.getId(),
                                key,
                                message.getBytes()).await();
                        if (!result.getStatus().isSuccess()) {
                            Log.v(TAG, "error");
                        } else {
                            Log.v(TAG, "success!! sent to: " + node.getDisplayName());
                        }
                    }
                }
            }).start();
        } else {
            Log.v(TAG, "Is NOT Connected");
        }
    }

    public void sendLogfile(Context context, String filepath, String folder, long created){
        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }
            File f = new File(filepath);
            if (!f.exists())
                return;

            String deviceName = getName(context);
            Asset asset = Asset.createFromUri(Uri.fromFile(f));
            PutDataMapRequest dataMap = PutDataMapRequest.create(ACTION_ZIPPED_FILES+"-"+deviceName);
            dataMap.getDataMap().putAsset("file", asset);
            dataMap.getDataMap().putString("name", f.getName());
            dataMap.getDataMap().putString("folder",folder);
            dataMap.getDataMap().putLong("created",created);
            dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());
            dataMap.getDataMap().putString("deviceName",deviceName);
            PutDataRequest request = dataMap.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private boolean setStudyDetails(String userID, String homeID){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(getString(R.string.PREF_USER_ID),userID);
        editor.putString(getString(R.string.PREF_HOME_ID), homeID);
        editor.apply();
        return editor.commit();
    }

    private String[] getStudyDetails(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String[] values = new String[2];
        values[0] = sp.getString(getString(R.string.PREF_USER_ID),"null");
        values[1] = sp.getString(getString(R.string.PREF_HOME_ID),"null");
        return values;
    }

    public boolean sendAllData(final Context context) {
        String parentName = Environment.getExternalStorageDirectory().getPath() + "/" + context.getResources().getString(R.string.app_name);
        File parent = new File(parentName);
        if(!parent.exists()){
            parent.mkdirs();
            return false;
        }
        ArrayList<String> zips = Compress.ZipRecursive2(parent);
        if (zips != null && zips.size() > 0) {
            final StudyListener sl = new StudyListener();
            if(zips.size() == 1){
                sendLogfile(context, zips.get(0), "WEAR", System.currentTimeMillis());

                //Compress.DeleteRecursive(new File(zips.get(0)));
            }else {
                try {
                    int i = 0;
                    for (String zip : zips) {
                        long time = SystemClock.uptimeMillis() + (40000 * i) + 3000;
                        if (new File(zip).exists()) {
                            final String z = zip;
                            Handler handler = new Handler();
                            handler.postAtTime(new Runnable() {
                                @Override
                                public void run() {
                                    sl.sendLogfile(context, z, "WEAR", System.currentTimeMillis());
                                    //delete the folder and files within in as we no longer need them once the zip is created.
                                    //Compress.DeleteRecursive(new File(parentName));
                                    //Compress.DeleteRecursive(new File(z));
                                }
                            }, time);
                        } else {
                            return false;
                        }

                    }
                }catch (Exception error){
                    return false;
                }
            }
            return true;
        }
        return false; //no zip files, so nothing sent.
    }


    private String getName(Context context){
        // Reconstitute the pairing device name from the model and the last 4 digits of the bluetooth MAC
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String wearName = sp.getString("DEVICE_NAME",null);
        if(wearName == null) {
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            String btAddress = "No Bluetooth";
            if (btAdapter != null)
                btAddress = btAdapter.getAddress();
            if ((btAddress != null) && (!btAddress.equals("No Bluetooth"))) {
                wearName = android.os.Build.MODEL;
                String[] tokens = btAddress.split(":");
                wearName += " " + tokens[4] + tokens[5];
                wearName = wearName.toUpperCase();
                sp.edit().putString("DEVICE_NAME",wearName).apply();
            } else {
                wearName = "No Bluetooth";
            }
        }
        return wearName;
    }
}
