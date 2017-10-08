package com.apps.greyrobe.wordrunner;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.wearable.authentication.OAuthClient;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

public class MyGoogleBooksLibrary extends Activity{
    private OAuthClient mClient;
    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_google_books_library);
        text = (TextView) findViewById(R.id.textView);

        mClient = OAuthClient.create(this);
        startOAuthFlow();
    }

    public void startOAuthFlow() {
        // Construct the redirect_uri used in your OAuth request.
        // OAuthClient.ANDROID_WEAR_REDIRECT_URL ensures the response will be received by Android Wear.
        // The receiving app's package name is required as the 3rd path component in the redirect_uri.
        // This allows Wear to ensure other apps cannot reuse your redirect_uri to receive responses.
        String redirectUri = OAuthClient.ANDROID_WEAR_REDIRECT_URL_PREFIX + "com.apps.greyrobe.wordrunner";
        // Construct the appropriate request for the OAuth provider you wish to integrate with.
        String requestUrl =
                "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=578018086969-49sjdv6lq3d52dfop8t9bprgohsevidg.apps.googleusercontent.com" +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/books";
        // Send it. This will open an authentication UI on the phone.
        mClient.sendAuthorizationRequest(Uri.parse(requestUrl), new MyOAuthCallback());
    }

    private final class MyOAuthCallback extends OAuthClient.Callback {
        @Override
        public void onAuthorizationResponse(Uri requestUrl, Uri resultUrl) {
            text.setText("Request URL: " + requestUrl + "\nResult URL: " + resultUrl);
        }

        @Override
        public void onAuthorizationError(int errorCode) {
            text.setText("Error Code: " + errorCode);
        }
    }

    @Override
    public void onDestroy() {
        mClient.destroy();
        super.onDestroy();
    }
}
