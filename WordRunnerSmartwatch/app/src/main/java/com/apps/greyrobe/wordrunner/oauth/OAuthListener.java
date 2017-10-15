package com.apps.greyrobe.wordrunner.oauth;

import android.app.Activity;

/**
 * Created by James on 10/13/2017.
 */

public interface OAuthListener {
    Activity getActivity();
    void updateStatus(String status);
    void receiveApiToken(String token);
}
