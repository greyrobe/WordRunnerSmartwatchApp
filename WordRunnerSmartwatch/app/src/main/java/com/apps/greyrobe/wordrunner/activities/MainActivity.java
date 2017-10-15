package com.apps.greyrobe.wordrunner.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.apps.greyrobe.wordrunner.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private TextView mTextView;
    private Button toggleBtn;
    private Thread storyReader;
    private String[] story;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                toggleBtn = (Button) stub.findViewById(R.id.btn_toggle);
                loadStory();
                createReaderThread();
            }
        });
    }

    public void toggleButton(View view)
    {
        if (toggleBtn.getText().equals("Start"))
        {
            storyReader.start();
            toggleBtn.setText("Stop");
        }
        else
        {
            storyReader.interrupt();
            toggleBtn.setText("Start");
        }
    }

    private void createReaderThread()
    {
        storyReader = new Thread()
        {
            int wordNum = 0;
            Thread wordSetterThread = new Thread()
            {
                @Override
                public void run() {
                    mTextView.setText(story[wordNum]);
                }
            };
            @Override
            public void run()
            {
                try
                {
                    for(; wordNum < story.length; wordNum++)
                    {
                        runOnUiThread(wordSetterThread);
                        Thread.sleep(100 + story[wordNum].length()*50);
                    }
                }
                catch (InterruptedException e) {}
            }
        };
    }

    private void loadStory()
    {
        try {
            String str = "";
            StringBuffer buf = new StringBuffer();
            InputStream is = this.getResources().openRawResource(R.raw.story);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            if (is != null) {
                while ((str = reader.readLine()) != null) {
                    buf.append(str);
                }
            }
            story = buf.toString().split(" ");
            is.close();
        }
        catch (IOException e)
        {
            mTextView.setText("Couldn't load file");
        }
    }
}
