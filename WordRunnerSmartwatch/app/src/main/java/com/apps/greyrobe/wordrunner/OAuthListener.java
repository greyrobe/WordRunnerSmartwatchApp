package com.apps.greyrobe.wordrunner;

import android.app.Activity;

/**
 * Created by James on 10/13/2017.
 */

public interface OAuthListener {
    Activity getActivity();
    String getToken();
    void updateStatus(String s);
}
