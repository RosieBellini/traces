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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import montague.traces.storage.Logger;
import montague.traces.storage.LoggerUtils;

/**
 * Created by Kyle Montague on 11/05/15.
 */
public class SyncActivity extends Activity {

    Button syncButton;
    TextView syncText;

    long created;
    Logger mLogger;
    static String SYNC = "SYNC";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        created = LoggerUtils.dateTimeStamp(System.currentTimeMillis());
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            created = extras.getLong("created");
        }


        mLogger = new Logger(SYNC,10,created,Logger.FileFormat.csv);

        syncText = (TextView) findViewById(R.id.debugInfo);
        syncButton = (Button)findViewById(R.id.syncButton);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogger.writeAsync(String.valueOf(System.currentTimeMillis()));
                mLogger.flush();
                finish();
            }
        });
    }
}
