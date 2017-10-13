package com.apps.greyrobe.wordrunner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.authentication.OAuthClient;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.android.gms.wearable.DataMap.TAG;

public class MyGoogleBooksLibrary extends WearableActivity implements OAuthListener{
    private TextView textView;
    private Button button;
    private TokenRetriever tokenRetriever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_google_books_library);
        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button2);
        setAmbientEnabled();

        tokenRetriever = new TokenRetriever(this);

    }

    public void getToken(View view) {
        getToken();
    }

    public Activity getActivity() {
        return this;
    }

    public String getToken() {
        return tokenRetriever.getStoredBooksApiToken();
    }

    public void updateStatus(String status) {
        textView.setText(status);
    }

    @Override
    public void onDestroy() {
        tokenRetriever.mClient.destroy();
        super.onDestroy();
    }
}
