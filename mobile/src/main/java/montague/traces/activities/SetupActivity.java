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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import montague.traces.R;
import montague.traces.storage.FileExplore;


public class SetupActivity extends Activity {



    TextView mInfoView;
    NumberPicker letterP;
    NumberPicker numberP;
    NumberPicker directionP;
    String[] abc = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z".split(",");


    ArrayList<String> calibrationPoints = new ArrayList<>();
    CalibrationActivity.mode mode = CalibrationActivity.mode.STANDING;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_setup);
        Button doneButton = (Button)findViewById(R.id.eventButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList(CalibrationActivity.EXTRA_CALIBRATION_POINTS,calibrationPoints);
                bundle.putString(CalibrationActivity.EXTRA_CALIBRATION_MODE,mode.toString());
                Intent _intent = new Intent(SetupActivity.this,CalibrationActivity.class);
                _intent.putExtras(bundle);
                _intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(_intent);
                finish();
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


        Button addButton = (Button)findViewById(R.id.calibrationButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ch = abc[letterP.getValue()];
                calibrationPoints.add(ch+","+numberP.getValue()+","+directionP.getValue());
                updateList();
            }
        });



        mInfoView = (TextView)findViewById(R.id.debugInfo);
        mInfoView.setText("");



    }


    @Override
    protected void onResume(){
        super.onResume();

    }


    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                SetupActivity.this);
        alertDialogBuilder.setTitle("Quit Calibration Setup");
        alertDialogBuilder
                .setMessage("Are you sure you want to quit?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        //todo return no result
                        finish();
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


    private void updateList(){
        if(calibrationPoints.size() == 0)
            return;

        String output = "";
        for(String value :calibrationPoints){
            String[] values = value.split(",");
            output+= "["+values[0]+""+values[1]+" - "+values[2]+"], ";
        }
        mInfoView.setText(output);
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.setup_menu, menu);
        return true;
    }

    private final int REQUEST_FILE = 8945;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
//            case R.id.action_setup_qmb_locations:
//                loadCSVAsset("qmb_locations.csv",true);
//                mode = CalibrationActivity.mode.STANDING;
//                updateList();
//                break;
//            case R.id.action_setup_qmb_hallway:
//                //load walking data
//                loadCSVAsset("qmb_hallway.csv",false);
//                mode= CalibrationActivity.mode.WALKING;
//                updateList();
//                break;
//            case R.id.action_setup_qmb_route:
//                //load walking data
//                loadCSVAsset("qmb_route.csv",false);
//                mode= CalibrationActivity.mode.WALKING;
//                updateList();
//                break;
//            case R.id.action_setup_core7_locations:
//                loadCSVAsset("core7_locations.csv",true);
//                mode= CalibrationActivity.mode.STANDING;
//                updateList();
//                break;
//            case R.id.action_setup_core7_route:
//                loadCSVAsset("core7_locations.csv",false);
//                mode= CalibrationActivity.mode.WALKING;
//                updateList();
//                break;
//            case R.id.action_setup_cpark_z1z2:
//                loadCSVAsset("cpark_z1z2.csv",false);
//                mode= CalibrationActivity.mode.STANDING;
//                updateList();
//                break;
            case R.id.action_load_CSV:
                Intent i = new Intent(this, FileExplore.class);
                startActivityForResult(i, REQUEST_FILE);
                break;

        }
        return super.onOptionsItemSelected(item);
    }


    /* Called when the second activity's finished */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_FILE:
                if (resultCode == RESULT_OK) {
                    Bundle res = data.getExtras();
                    String filename = res.getString(FileExplore.EXTRA_FILEPATH);
                    loadCSVFile(filename,false);
                    mode= CalibrationActivity.mode.STANDING;
                }
                break;
        }
    }




    final int DIRECTIONS =4;
    private void loadCSVAsset(String filename, boolean allDirections){
        List<String> csvData = null;
        try {
            csvData = readCsv(this,filename, true);
            setupData(csvData,allDirections);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCSVFile(String filename, boolean allDirections){
        List<String> csvData = null;
        try {
            csvData = readCsv(this,filename, false);
            setupData(csvData,allDirections);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupData(List<String> csvData, boolean allDirections){
        if(csvData != null){
            calibrationPoints = new ArrayList<>();
            for(String rowData:csvData){
                String[] row = rowData.split(",");
                if(allDirections){
                    for(int x=0;x<DIRECTIONS;x++){
                        calibrationPoints.add(row[0]+","+row[1]+","+(x+1));
                    }
                }else{
                    String dir = (row.length == 3)?row[2]:"0";
                    calibrationPoints.add(row[0]+","+row[1]+","+dir);
                }
            }
        }
    }


    public final List<String> readCsv(Context context, String filename, boolean isAsset) throws IOException {
        ArrayList<String> output = new ArrayList<String>();
        InputStream csvStream;
        if(isAsset) {
            AssetManager assetManager = context.getAssets();
            csvStream = assetManager.open(filename);
        }else{
            File f = new File(filename);
            csvStream = new FileInputStream(f);
        }


        BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream));

        String line;
        while ((line = reader.readLine()) != null) {
           output.add(line);
        }

        return output;
    }
}
