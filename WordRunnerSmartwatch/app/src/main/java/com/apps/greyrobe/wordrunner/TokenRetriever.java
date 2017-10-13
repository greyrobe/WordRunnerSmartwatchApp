package com.apps.greyrobe.wordrunner;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.wearable.authentication.OAuthClient;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.android.gms.wearable.DataMap.TAG;

/**
 * Created by James on 10/11/2017.
 */

public class TokenRetriever {
    public OAuthListener listener;
    public OAuthClient mClient;
    private static final String CLIENT_ID = "578018086969-uqeqcn5ru2eiqgotbvba6ebpe9cvjpvc.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "PKg2FUFt5oAls4jyqAGkckiT";
    private String redirectUri;

    public TokenRetriever(OAuthListener listener) {
        this.listener = listener;
        mClient = OAuthClient.create(listener.getActivity());
    }

    public String getStoredBooksApiToken() {
        SharedPreferences sp = listener.getActivity().getPreferences(Context.MODE_PRIVATE);
        String token = sp.getString("booksApiToken", null);
        if(token == null) {
            startOAuthFlow();
        }
        return token;
    }

    protected void setStoredBooksApiToken(String token) {
        SharedPreferences sp = listener.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("booksApiToken", token);
        editor.commit();
    }

    protected void startOAuthFlow() {

        // Construct the redirect_uri used in your OAuth request.
        // OAuthClient.ANDROID_WEAR_REDIRECT_URL ensures the response will be received by Android Wear.
        // The receiving app's package name is required as the 3rd path component in the redirect_uri.
        // This allows Wear to ensure other apps cannot reuse your redirect_uri to receive responses.
        redirectUri = OAuthClient.ANDROID_WEAR_REDIRECT_URL_PREFIX + listener.getActivity().getApplicationContext().getPackageName();
        // Construct the appropriate request for the OAuth provider you wish to integrate with.
        String requestUrl =
                "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=" + CLIENT_ID +
                        "&redirect_uri=" + redirectUri +
                        "&response_type=code" +
                        "&scope=https://www.googleapis.com/auth/books";
        // Send it. This will open an authentication UI on the phone.
        mClient.sendAuthorizationRequest(Uri.parse(requestUrl), new MyOAuthCallback());
    }

    /**
     * Helper method to update display with fetched results on the activity view.
     *
     * @param text Returned text to display
     */
    private void updateStatus(final String text) {
        listener.getActivity().runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        listener.updateStatus(text);
                    }
                });
    }

    private class MyOAuthCallback extends OAuthClient.Callback {
        protected @Nullable
        String accessToken;

        @Override
        public void onAuthorizationError(final int error) {
            Log.e(TAG, "onAuthorizationError called: " + error);
        }

        @Override
        public void onAuthorizationResponse(Uri requestUrl, final Uri responseUrl) {
            Log.d(TAG, "onResult(). requestUrl:" + requestUrl + " responseUrl: " + responseUrl);
            listener.updateStatus("Request completed. Response URL: " + responseUrl);

            Runnable runnable =
                    new Runnable() {
                        public void run() {
                            HttpURLConnection httpPost = createHttpPostObject(responseUrl);
                            String code = responseUrl.getQueryParameter("code");
                            if (TextUtils.isEmpty(code)) {
                                listener.updateStatus("Google OAuth 2.0 API token exchange failed. No code query parameter in response URL");
                            }
                            String urlParams =
                                    "code=" + code +
                                    "&client_id=" + CLIENT_ID +
                                    "&client_secret=" + CLIENT_SECRET +
                                    "&redirect_uri=" + redirectUri +
                                    "&grant_type=" + "authorization_code";
                            if (httpPost != null) {
                                String token = acquireToken(httpPost, urlParams);
                                if(token != null) {
                                    listener.updateStatus("Google OAuth 2.0 API token retrieved. Token: " + token);
                                    setStoredBooksApiToken(token);
                                    listener.updateStatus("Token Stored. Ready to use API.");
                                }
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

        private String acquireToken(HttpURLConnection conn, String urlParameters) {
            try {
                // Send post request
                conn.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

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

                listener.updateStatus(
                        "Google OAuth 2.0 API token exchange occurred. Response: "
                                + response.toString());
                try {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String accessToken = jsonResponse.getString("access_token");

                    if (TextUtils.isEmpty(accessToken)) {
                        listener.updateStatus(
                                "Google OAuth 2.0 API token exchange failed. No access token in response.");
                    }
                    this.accessToken = accessToken;
                } catch (JSONException e) {
                    Log.e(TAG, "Bad JSON returned:\n\n" + Log.getStackTraceString(e));
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception occurred:\n\n" + Log.getStackTraceString(e));
            }
            return accessToken;
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

                listener.updateStatus("Google OAuth 2.0 API request occurred. Response: " + response.toString());
            }catch (Exception e) {
                Log.e(TAG, "Exception occurred\n\n" + Log.getStackTraceString(e));
            }
        }
    }
}
