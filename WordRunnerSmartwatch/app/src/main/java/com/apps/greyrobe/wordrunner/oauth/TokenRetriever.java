package com.apps.greyrobe.wordrunner.oauth;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.android.gms.wearable.DataMap.TAG;

/**
 * Created by James on 10/11/2017.
 */

public class TokenRetriever extends OAuthClient.Callback{
    public OAuthListener listener;
    public OAuthClient mClient;
    private static final String CLIENT_ID = "578018086969-uqeqcn5ru2eiqgotbvba6ebpe9cvjpvc.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "PKg2FUFt5oAls4jyqAGkckiT";
    private String redirectUri;
    private String token;

    public TokenRetriever(OAuthListener listener) {
        this.listener = listener;
        mClient = OAuthClient.create(listener.getActivity());
    }

    public void getAccessToken() {
        SharedPreferences sp = listener.getActivity().getPreferences(Context.MODE_PRIVATE);
        token = sp.getString("accessToken", null);
        if(token != null) {
            listener.receiveApiToken(token);
        }
        else {
            startOAuthFlow();
        }
    }

    public String getRefreshToken() {
        SharedPreferences sp = listener.getActivity().getPreferences(Context.MODE_PRIVATE);
        token = sp.getString("refreshToken", null);
        if(token != null) {
            return token;
        }
        else {
            startOAuthFlow();
        }
        return null;
    }

    public void setAccessToken(String token) {
        SharedPreferences sp = listener.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("accessToken", token);
        editor.commit();
    }

    public void setRefreshToken(String token) {
        SharedPreferences sp = listener.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("refreshToken", token);
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
                        "https://accounts.google.com/o/oauth2/auth?" +
                        "scope=https://www.googleapis.com/auth/books" +
                        "&access_type=offline" +
                        "&redirect_uri=" + redirectUri +
                        "&response_type=code" +
                        "&client_id=" + CLIENT_ID;
        // Send it. This will open an authentication UI on the phone.
        mClient.sendAuthorizationRequest(Uri.parse(requestUrl), this);
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

    @Override
    public void onAuthorizationError(final int error) {
        Log.e(TAG, "onAuthorizationError called: " + error);
    }

    @Override
    public void onAuthorizationResponse(Uri requestUrl, final Uri responseUrl) {
        Log.d(TAG, "onResult(). requestUrl:" + requestUrl + " responseUrl: " + responseUrl);
        updateStatus("Request completed. Response URL: " + responseUrl);

        /**
         * ASynchronous task to submit the POST request containing the Authorization code
         * to get the API token.
         */
        Runnable runnable =
                new Runnable() {
                    public void run() {
                        String code = responseUrl.getQueryParameter("code");
                        if (TextUtils.isEmpty(code)) {
                            updateStatus("Google OAuth 2.0 API token exchange failed. No code query parameter in response URL");
                        }
                        HttpURLConnection httpPost = createHttpPostObject();
                        String urlParams =
                                        "code=" + code +
                                        "&client_id=" + CLIENT_ID +
                                        "&client_secret=" + CLIENT_SECRET +
                                        "&redirect_uri=" + redirectUri +
                                        "&grant_type=" + "authorization_code";
                        String[] tokens = null;
                        if (httpPost != null) {
                            tokens = acquireTokens(httpPost, urlParams);
                            setAccessToken(tokens[0]);
                            setRefreshToken(tokens[1]);
                            updateStatus("Google OAuth 2.0 API token retrieved. Access Token: " + tokens[0] + " Refresh Token: " + tokens[1]);
                            listener.receiveApiToken(tokens[0]);
                        }

                    }
                };
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(runnable);
    }

    private HttpURLConnection createHttpPostObject() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://accounts.google.com/o/oauth2/token");
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getClass().toString() + "occurred. Stack trace:\n\n"
                    + Log.getStackTraceString(e));
        }
        return conn;
    }

    private String[] acquireTokens(HttpURLConnection conn, String urlParameters) {
        String[] tokens = new String[2];
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

            updateStatus(
                    "Google OAuth 2.0 API token exchange occurred. Response: "
                            + response.toString());
            try {
                JSONObject jsonResponse = new JSONObject(response.toString());
                tokens[0] = jsonResponse.getString("access_token");
                tokens[1] = jsonResponse.getString("refresh_token");
                if (TextUtils.isEmpty(tokens[0]) && TextUtils.isEmpty(tokens[1])) {
                    updateStatus(
                            "Google OAuth 2.0 API token exchange failed. No access token in response.");
                }
            } catch (JSONException e) {
                Log.e(TAG, "Bad JSON returned:\n\n" + Log.getStackTraceString(e));
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception occurred:\n\n" + Log.getStackTraceString(e));
        }
        return tokens;
    }

    public void refreshToken() {
        Runnable runnable =
                new Runnable() {
                    public void run() {
                        String refreshToken = getRefreshToken();
                        if(refreshToken == null) {
                            updateStatus("Google OAuth 2.0 API refresh token not found. Initiating OAuth2.0 ...");
                            return;
                        }
                        HttpURLConnection httpPost = createHttpPostObject();
                        String urlParams =
                                        "&client_id=" + CLIENT_ID +
                                        "&client_secret=" + CLIENT_SECRET +
                                        "&refresh_token=" + refreshToken +
                                        "&grant_type=" + "refresh_token";
                        String[] tokens = acquireTokens(httpPost, urlParams);
                        setAccessToken(tokens[0]);
                        listener.receiveApiToken(tokens[0]);
                    }
                };
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(runnable);
    }
}
