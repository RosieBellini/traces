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

package montague.traces.storage;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by kylemontague on 22/07/15.
 */
public class LoggerUtils {

    public static long dateTimeStamp(long timestamp){

        //If you want the current timestamp :
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(timestamp));
        c.set(Calendar.HOUR,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        return c.getTime().getTime();
    }


    public static String getName(Context context){
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
