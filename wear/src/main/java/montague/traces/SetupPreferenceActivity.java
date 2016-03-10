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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.view.WearableListView;
import android.widget.Toast;

import montague.traces.services.SensorService;
import montague.traces.storage.Logger;
import montague.traces.storage.LoggerUtils;
import preference.WearPreferenceActivity;


public class SetupPreferenceActivity extends WearPreferenceActivity {


    boolean isLogging = false;
    Logger studyLogger;
    SharedPreferences sp;
    boolean hasChanged = false;
    boolean canQuit = false;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        isLogging = sp.getBoolean(getString(R.string.PREF_SENSOR_STATUS), false);

        boolean serviceActive = isMyServiceRunning(SensorService.class);
        if(serviceActive != isLogging){
            isLogging = serviceActive;
            sp.edit().putBoolean(getString(R.string.PREF_SENSOR_STATUS), isLogging).commit();
        }
        studyLogger = new Logger(getString(R.string.FILE_STUDY_DETAILS),1,LoggerUtils.dateTimeStamp(System.currentTimeMillis()),Logger.FileFormat.csv);
    }

    final int SAVE_POS = 0;
    final int SENSOR_ON_OFF_POS = 1;
    final int SENSOR_SETTINGS_POS = 2;
    final int EVENT_LOGGER_POS = 3;
    final int SEND_DATA_POS = 6;
    final int REMOVE_DATA_POS = 7;
    @Override
    public void onClick(WearableListView.ViewHolder viewHolder){
        super.onClick(viewHolder);
        if(viewHolder.getAdapterPosition()==SAVE_POS){
            if(hasChanged){
                String userID = sp.getString(getString(R.string.PREF_USER_ID),"Not set");
                String homeID = sp.getString(getString(R.string.PREF_HOME_ID),"Not set");
                studyLogger.writeAsync(String.format("%d,%b,%s,%s",System.currentTimeMillis(),isLogging,userID,homeID));
                studyLogger.flush();
                hasChanged = false;
                Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();
                finish();
            }else if(!canQuit){
                Toast.makeText(this,"No Changes \nTap again to quit",Toast.LENGTH_SHORT).show();
                canQuit = true;
            }else{
                Toast.makeText(this,"Quit with no changes.",Toast.LENGTH_SHORT).show();
                canQuit = false;
                finish();

            }
        }else if(viewHolder.getAdapterPosition()==SENSOR_ON_OFF_POS){
            sensorsChanged();
            Intent i = new Intent(SetupPreferenceActivity.this, SensorService.class);
            if(isLogging){
                i.putExtra(SensorService.EXTRA_TIMESTAMP, LoggerUtils.dateTimeStamp(System.currentTimeMillis()));
                SetupPreferenceActivity.this.startService(i);
                //todo check the magnetic field sensor accuracy.

                Intent magCheck = new Intent(SetupPreferenceActivity.this,DataManagementActivity.class);
                magCheck.putExtra(DataManagementActivity.EXTRA_MODE,DataManagementActivity.MODE.CALIBRATE_MAG_SENSOR);
                startActivity(magCheck);
            }else{
                stopService(i);
            }
            hasChanged = true;
        }else if(viewHolder.getAdapterPosition() == SENSOR_SETTINGS_POS){
            if(isLogging){
                Toast.makeText(this,"You must disable logging to configure settings.",Toast.LENGTH_SHORT).show();
            }else {
                Intent i = new Intent(this, SensorPreferenceActivity.class);
                startActivity(i);
            }
        }else if(viewHolder.getAdapterPosition() == EVENT_LOGGER_POS) {
            //todo move over into data management activity.
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra(getString(R.string.EXTRA_CREATED_TIMESTAMP), LoggerUtils.dateTimeStamp(System.currentTimeMillis()));
            startActivity(i);
        }else if(viewHolder.getAdapterPosition()==SEND_DATA_POS){
            if(isLogging){
                Toast.makeText(this,"You must disable logging before you can send log data.",Toast.LENGTH_SHORT).show();
            }else {
                studyLogger.flush();
                Intent i = new Intent(this, DataManagementActivity.class);
                i.putExtra(DataManagementActivity.EXTRA_MODE, DataManagementActivity.MODE.DATA_SYNC);
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
            }
        }else if(viewHolder.getAdapterPosition()==REMOVE_DATA_POS){
            if(isLogging){
                Toast.makeText(this,"You must disable logging before you can delete log data.",Toast.LENGTH_SHORT).show();
            }else {
                Intent i = new Intent(this, DataManagementActivity.class);
                i.putExtra(DataManagementActivity.EXTRA_MODE, DataManagementActivity.MODE.DELETE);
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
            }
        }else{
            hasChanged = true;
        }
    }

    private boolean sensorsChanged(){
        boolean tmp = sp.getBoolean(getString(R.string.PREF_SENSOR_STATUS),false);
        if(isLogging != tmp) {
            isLogging = tmp;
            return true;
        }
        return false;
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

}
