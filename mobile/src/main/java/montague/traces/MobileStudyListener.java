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

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by kyle montague on 11/05/15.
 */
public class MobileStudyListener extends WearableListenerService implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    public static String ACTION_START_SYNC = "TRACES.MSG.SYNC_START";
    public static String ACTION_SYNC_COMPLETE = "TRACES.MSG.SYNC_COMPLETE";
    public static String ACTION_START_LOGGER = "TRACES.MSG.LOGGER_START";
    public static String ACTION_STOP_LOGGER = "TRACES.MSG.LOGGER_STOP";
    public static String ACTION_SEND_FILES = "TRACES.MSG.SEND_FILES";
    public static String ACTION_REMOVE_FILES = "TRACES.MSG.REMOVE_FILES";

    public static String ACTION_START_SENSORS = "TRACES.MSG.SENSORS_START";
    public static String ACTION_STOP_SENSORS = "TRACES.MSG.SENSORS_STOP";

    public static String ACTION_ZIPPED_FILES = "/logfile";
    public static String ACTION_GET_ALL_FILES = "TRACES.MSG.SEND_ALL_FILES";

    public static String ACTION_GET_SERVICE_STATUS = "TRACES.MSG.GET_SERVICE_STATUS";


    public static final String RESPONSE_MESSAGE_SERVICE_STATUS = "TRACES.MSG.SERVICE_STATUS";

    public static String ACTION_CONFIRM_DATA_SYNCED = "TRACES.MSG.CONFIRM_DATA_SYNCED";

    private static final long TIMEOUT_MS = 10000;
    private static final String TAG = "MOBILE_LISTENER";
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent){
        final String message = new String(messageEvent.getData());
        String[]values = message.split(",");

        if(messageEvent.getPath().contains(ACTION_SYNC_COMPLETE)) {

        }else if(messageEvent.getPath().contains(RESPONSE_MESSAGE_SERVICE_STATUS)){
            boolean status = Boolean.parseBoolean(message);
        }else{
            super.onMessageReceived(messageEvent);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().contains(ACTION_ZIPPED_FILES)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset logfile = dataMapItem.getDataMap().getAsset("file");
                String name = dataMapItem.getDataMap().getString("name");
                long created = dataMapItem.getDataMap().getLong("created");
                long timestamp = dataMapItem.getDataMap().getLong("timestamp");
                String folder = dataMapItem.getDataMap().getString("folder");
                String deviceName = dataMapItem.getDataMap().getString("deviceName",null);

                boolean success = false;
                if(saveFileFromAsset(logfile,name, deviceName)) {
                    success = true;

                }
                sendMessage(MobileStudyListener.this,ACTION_CONFIRM_DATA_SYNCED, System.currentTimeMillis()+","+String.valueOf(success),deviceName);
            }
        }
    }

    public void sendMessage(Context context, final String key, final String message){
        sendMessage(context,key,message,null);
    }

    public void sendMessage(Context context, final String key, final String message, final String deviceName){
        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        }
        mGoogleApiClient.connect();

        final boolean shouldLoop = (deviceName == null)?true:false;



            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = null;
                        if(!node.getId().equalsIgnoreCase("cloud") && shouldLoop) {
                            result = Wearable.MessageApi.sendMessage(
                                    mGoogleApiClient,
                                    node.getId(),
                                    key,
                                    message.getBytes()).await();
                        }else if(node.getDisplayName().equalsIgnoreCase(deviceName)){
                            result = Wearable.MessageApi.sendMessage(
                                    mGoogleApiClient,
                                    node.getId(),
                                    key,
                                    message.getBytes()).await();
                        }
                        if (result!= null && result.getStatus().isSuccess())
                            Log.v(TAG, "success!! sent to: " + node.getDisplayName());
                    }
                }
            }).start();

    }


    private boolean saveFileFromAsset(Asset asset, String name, String folder) {


        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return false;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            return false;
        }
        // decode the stream into a bitmap
        // Get folder for output
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsolutePath() +"/"+ getResources().getString(R.string.app_name)+"/"+folder+"/");


        if (!dir.exists()) { dir.mkdirs(); } // Create folder if needed

        // Read data from the Asset and write it to a file on external storage
        final File file = new File(dir, name);
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = assetInputStream.read(data, 0, data.length)) != -1) {
                fOut.write(data, 0, nRead);
            }

            fOut.flush();
            fOut.close();
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }


    public void startSync(Context context, long timestamp){
        sendMessage(context,ACTION_START_SYNC,String.valueOf(timestamp));
    }

    public void stopSync(Context context, long timestamp){
        sendMessage(context,ACTION_SYNC_COMPLETE,String.valueOf(timestamp));
    }

    public void startLogger(Context context, long timestamp){
        sendMessage(context,ACTION_START_LOGGER,String.valueOf(timestamp));
    }

    public void stopLogger(Context context, long timestamp){
        sendMessage(context,ACTION_STOP_LOGGER,String.valueOf(timestamp));
    }

    public void requestFiles(Context context, long timestamp){
        sendMessage(context,ACTION_SEND_FILES,String.valueOf(timestamp));
    }

    public void removeFiles(Context context, long timestamp){
        sendMessage(context,ACTION_REMOVE_FILES,String.valueOf(timestamp));
    }

    public void startSensors(Context context, long timestamp){
        sendMessage(context,ACTION_START_SENSORS,String.valueOf(timestamp));
    }
    public void stopSensors(Context context, long timestamp){
        sendMessage(context,ACTION_STOP_SENSORS,String.valueOf(timestamp));
    }

    public void sendAllFiles(Context context, long timestamp){
        sendMessage(context,ACTION_GET_ALL_FILES,String.valueOf(timestamp));
    }

    public void getSensorStatus(Context context, long timestamp){
        sendMessage(context,ACTION_GET_SERVICE_STATUS,String.valueOf(timestamp));
    }

}
