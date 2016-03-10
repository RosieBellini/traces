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

import android.app.Application;
import android.os.Environment;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.connexience.api.StorageClient;
import com.connexience.api.WorkflowClient;
import com.connexience.api.model.EscDocument;
import com.connexience.api.model.EscFolder;
import com.connexience.api.model.EscMetadataItem;
import com.connexience.api.model.EscWorkflow;

import junit.framework.Assert;

import java.io.File;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    public void setUp(){
        testESC();
    }


    public  void testESC(){


            try {
                StorageClient client = new StorageClient("demo.escapp.net", 80, false, "beside@dundee.ac.uk", "carehomes");
                WorkflowClient wfClient = new WorkflowClient(client);

                EscFolder home = client.homeFolder();

                EscDocument[] docs = client.folderDocuments(home.getId());  // List docs
                EscFolder[] folders = client.listChildFolders(home.getId());    // Child folders

                if(docs.length > 0) {
                    File myFile = new File(Environment.getExternalStorageDirectory() + "/TRACES/" + docs[0].getName());
                    client.download(docs[0], myFile);
                }

                // Attach metadata to docs[0]
                EscMetadataItem md = new EscMetadataItem();
                md.setCategory("BESIDE");
                md.setMetadataType(EscMetadataItem.METADATA_TYPE.TEXT);
                md.setName("PhoneID");
                md.setStringValue("Kyle");

                client.addMetadataToDocument(docs[0].getId(), md);
                Log.e("ESC", "Home folder ID: " + home.getId());
                Assert.assertNotNull(home.getId());

                EscWorkflow uploadWorkflow = wfClient.getWorkflow("50482");
                wfClient.executeWorkflowOnDocument(uploadWorkflow.getId(), docs[0].getId());

            } catch (Exception e){
                Log.e("ESC", "Error: " + e.getMessage());
                Assert.fail();
            }


    }

}
