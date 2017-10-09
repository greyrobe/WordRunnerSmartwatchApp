package com.apps.greyrobe.wordrunner;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class MyGoogleBooksLibrary extends Activity{
    private OAuthClient mClient;
    private TextView textView;
    private Button button;
    private static final String CLIENT_ID = "578018086969-uqeqcn5ru2eiqgotbvba6ebpe9cvjpvc.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "PKg2FUFt5oAls4jyqAGkckiT";
    private String redirectUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_google_books_library);
        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button2);

        mClient = OAuthClient.create(this);
    }

    public void startOAuthFlow(View view) {
        // Construct the redirect_uri used in your OAuth request.
        // OAuthClient.ANDROID_WEAR_REDIRECT_URL ensures the response will be received by Android Wear.
        // The receiving app's package name is required as the 3rd path component in the redirect_uri.
        // This allows Wear to ensure other apps cannot reuse your redirect_uri to receive responses.
        redirectUri = OAuthClient.ANDROID_WEAR_REDIRECT_URL_PREFIX + getApplicationContext().getPackageName();
        // Construct the appropriate request for the OAuth provider you wish to integrate with.
        String requestUrl =
                "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + CLIENT_ID +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/books";
        // Send it. This will open an authentication UI on the phone.
        mClient.sendAuthorizationRequest(Uri.parse(requestUrl), new MyOAuthCallback(this));
    }

    /**
     * Helper method to update display with fetched results on the activity view.
     *
     * @param text Returned text to display
     */
    private void updateStatus(final String text) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(text);
                    }
                });
    }

    private class MyOAuthCallback extends OAuthClient.Callback {
        protected @Nullable
        String accessToken;
        private MyGoogleBooksLibrary libraryActivity;

        public MyOAuthCallback(MyGoogleBooksLibrary libraryActivity) {
            this.libraryActivity = libraryActivity;
        }

        @Override
        public void onAuthorizationError(final int error) {
            Log.e(TAG, "onAuthorizationError called: " + error);
        }

        @Override
        public void onAuthorizationResponse(Uri requestUrl, final Uri responseUrl) {
            Log.d(TAG, "onResult(). requestUrl:" + requestUrl + " responseUrl: " + responseUrl);
            libraryActivity.updateStatus("Request completed. Response URL: " + responseUrl);

            Runnable runnable =
                    new Runnable() {
                        public void run() {
                            HttpURLConnection httpPost = createHttpPostObject(responseUrl);
                            String code = responseUrl.getQueryParameter("code");
                            if (TextUtils.isEmpty(code)) {
                                libraryActivity.updateStatus("Google OAuth 2.0 API token exchange failed. No code query parameter in response URL");
                            }
                            String urlParams =
                                "code=" + code +
                                "&client_id=" + CLIENT_ID +
                                "&client_secret=" + CLIENT_SECRET +
                                "&redirect_uri=" + redirectUri +
                                "&grant_type=" + "authorization_code";
                            if (httpPost != null) {
                                acquireToken(httpPost, urlParams);
                                accessAPI();
                            }
                        }
                    };
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(runnable);
        }

        private HttpURLConnection createHttpPostObject(Uri responseUrl) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://www.googleapis.com/oauth2/v4/token");
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getClass().toString() + "occurred. Stack trace:\n\n"
                    + Log.getStackTraceString(e));
            }
            return conn;
        }

        private void acquireToken(HttpURLConnection conn, String urlParameters) {
            try {
                // Send post request
                conn.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code : " + responseCode);

                // Retrieve post response
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.d(TAG, response.toString());

                libraryActivity.updateStatus(
                        "Google OAuth 2.0 API token exchange occurred. Response: "
                                + response.toString());
                try {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String accessToken = jsonResponse.getString("access_token");
                    if (TextUtils.isEmpty(accessToken)) {
                        libraryActivity.updateStatus(
                                "Google OAuth 2.0 API token exchange failed. No access token in response.");
                        return;
                    }
                    this.accessToken = accessToken;
                } catch (JSONException e) {
                    Log.e(TAG, "Bad JSON returned:\n\n" + Log.getStackTraceString(e));
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception occurred:\n\n" + Log.getStackTraceString(e));
            }
        }

        private void accessAPI() {
            try {
                URL url = new URL("https://www.googleapis.com/books/v1/volumes?q=summer+again&country=US");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.connect();
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = in.readLine()) != null) {response.append(line);}
                in.close();
                Log.d(TAG, response.toString());

                if(response == null) {
                    Log.e(TAG, "Could not execute HTTP request. No response returned.");
                    return;
                }

                libraryActivity.updateStatus("Google OAuth 2.0 API request occurred. Response: " + response.toString());
            }catch (Exception e) {
                Log.e(TAG, "Exception occurred\n\n" + Log.getStackTraceString(e));
            }
        }
    }

    @Override
    public void onDestroy() {
        mClient.destroy();
        super.onDestroy();
    }
}
