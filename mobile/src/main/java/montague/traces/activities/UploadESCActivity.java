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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.connexience.api.StorageClient;
import com.connexience.api.WorkflowClient;
import com.connexience.api.misc.IProgressInfo;
import com.connexience.api.model.EscDocument;
import com.connexience.api.model.EscDocumentVersion;
import com.connexience.api.model.EscFolder;
import com.connexience.api.model.EscMetadataItem;
import com.connexience.api.model.EscWorkflow;

import java.io.File;
import java.util.ArrayList;

import montague.traces.R;
import montague.traces.storage.FileUtils;
import montague.traces.utilities.Network;

public class UploadESCActivity extends Activity {

    TextView infoText;
    Button  uploadButton;
    ProgressBar itemProgressBar;
    ProgressBar overallProgressBar;


    StorageClient client;
    WorkflowClient wfClient;
    EscFolder home;
    EscFolder data_folder;


    String[] files;
    ArrayList<String> folders;

    String mEmail;
    String mPassword;


    int overallProg =0;
    int overallStep = 10;

    final String TAG = "ESC";
    private IProgressInfo progressCallback = new IProgressInfo() {
        @Override
        public void reportBegin(long l) {
            setProgressMax((int) l);
        }

        @Override
        public void reportProgress(long l) {
            updateProgress((int)l);
        }

        @Override
        public void reportEnd(long l) {

        }
    };
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_esc);

        infoText = (TextView)findViewById(R.id.progressInfo);
        uploadButton = (Button)findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upload();
            }
        });

        itemProgressBar = (ProgressBar)findViewById(R.id.itemProgressBar);
        overallProgressBar = (ProgressBar)findViewById(R.id.overallProgressBar);

        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onResume(){
        super.onResume();
        showFoldersToUpload();

        if(Network.isNetworkOnline(getApplicationContext())) {

            if(sp == null)
                sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            mEmail = sp.getString(getString(R.string.PREF_ESC_USERNAME),"beside@dundee.ac.uk");
            mPassword = sp.getString(getString(R.string.PREF_ESC_PASSWORD),"null");
            if(mPassword.equals("null")){
                Toast.makeText(getApplicationContext(),"Invalid login details for e-Science Central. Please check login details and try again",Toast.LENGTH_LONG).show();
                finish();
            }else {
                setup();
            }

        }else{
            infoText.setText("No Network Connection");
            uploadButton.setEnabled(false);        }


    }

    private void setup(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client = new StorageClient("demo.escapp.net", 80, false, mEmail, mPassword);
                    wfClient = new WorkflowClient(client);
                    home = client.homeFolder();
                } catch (Exception e) {
                    //e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Unable to connect to e-Science Central. Check account details and try again.",Toast.LENGTH_LONG).show();
                }
            }
        }).start();
    }

    private void upload() {

        uploadButton.setEnabled(false);
        infoText.setText(R.string.msg_upload_attempting);
        itemProgressBar.setVisibility(View.VISIBLE);
        itemProgressBar.setProgress(0);

        overallProgressBar.setVisibility(View.VISIBLE);
        overallProgressBar.setProgress(0);
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    EscWorkflow uploadWorkflow = wfClient.getWorkflow("50626");
                    //EscDocument[] docs = client.folderDocuments(home.getId());  // List docs
                    if(data_folder == null) {
                        EscFolder[] folders = client.listChildFolders(home.getId());    // Child folders
                        if(folders!=null){
                            for(EscFolder f:folders)
                                if(f.getName().equals("Data"))
                                {
                                    data_folder = f;
                                }
                        }
                    }

                    if(data_folder == null) {
                        Log.d(TAG,"no DATA folder");
                        return;
                    }

                    EscFolder[] subFolders = client.listChildFolders(data_folder.getId());


                    overallProgressBar.setMax(folders.size()*overallStep);
                    overallProg = 0;

                    for(String folder:folders) {
                        EscFolder sub_data_folder = null;
                        String folderName = new File(folder).getName().toLowerCase();

                        if(subFolders!=null){
                            for(EscFolder f:subFolders){
                                if(f.getName().equals(folderName)){
                                    sub_data_folder = f;
                                }
                            }
                        }

                        if(sub_data_folder == null){
                            sub_data_folder = client.createChildFolder(data_folder.getId(),folderName);
                        }



                        files = FileUtils.listAllZipFiles(folder);
                        if(files!=null) {
                            for (String f : files) {
                                itemProgressBar.setProgress(0);
                                File zipFile = new File(folder,f);

                                EscDocument doc = client.createDocumentInFolder(sub_data_folder.getId(), zipFile.getName());
                                if (doc != null) {
                                    EscMetadataItem md = new EscMetadataItem();
                                    md.setCategory("BESIDE");
                                    md.setMetadataType(EscMetadataItem.METADATA_TYPE.TEXT);
                                    md.setName("device_id");
                                    md.setObjectId(doc.getId());
                                    md.setStringValue(folderName);
                                    client.addMetadataToDocument(doc.getId(), md);
                                    EscDocumentVersion dv = client.upload(doc, zipFile, progressCallback);
                                    Log.d(TAG, "ZIP:" + zipFile.length() + " ESC:" + dv.getSize());
                                    if (dv.getSize() >= zipFile.length()) {
                                        zipFile.delete();
                                        wfClient.executeWorkflowOnDocument(uploadWorkflow.getId(), doc.getId());
                                    }
                                }
                            }
                        }
                        overallProg++;
                    }

                    //50626




                    UploadESCActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            uploadButton.setEnabled(true);
                            infoText.setText(R.string.msg_upload_success);
                            showFoldersToUpload();
                        }
                    });

                } catch (Exception e){
                    Log.d(TAG, "Error: " + e.getMessage());
                }

            }
        }).start();



    }

    private void setProgressMax(final int value){
        UploadESCActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                itemProgressBar.setMax(value);
            }
        });
    }

    private void updateProgress(final int value){
        UploadESCActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                itemProgressBar.setProgress(value);
                float itemP = itemProgressBar.getProgress()*1.0f/itemProgressBar.getMax();
                overallProgressBar.setProgress((overallProg*overallStep)+(int)(itemP*overallStep));
            }
        });
    }

//    private void showFilesToUpload(){
//        files = FileUtils.listAllZipFiles();
//        if(files==null) {
//            infoText.setText(R.string.msg_upload_no_files);
//            uploadButton.setEnabled(false);
//        }
//        else{
//            String output=getString(R.string.msg_upload_file_list)+"\n";
//            for(String f:files){
//                output+=new File(f).getName()+"\n";
//            }
//            infoText.setText(output);
//            uploadButton.setEnabled(true);
//        }
//    }

    private void showFoldersToUpload(){
        folders = FileUtils.listAllSubFolders();
        if(folders == null || folders.size() == 0) {
            infoText.setText(R.string.msg_upload_no_files);
            uploadButton.setEnabled(false);
        }
        else{
            String output=getString(R.string.msg_upload_file_list)+"\n";
            for(String f:folders){
                output+=new File(f).getName()+"\n";
            }
            infoText.setText(output);
            uploadButton.setEnabled(true);
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_upload_esc, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }



}
