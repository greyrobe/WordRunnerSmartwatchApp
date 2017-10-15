package com.apps.greyrobe.wordrunner.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.apps.greyrobe.wordrunner.R;
import com.apps.greyrobe.wordrunner.books.Library;
import com.apps.greyrobe.wordrunner.books.LibraryController;
import com.apps.greyrobe.wordrunner.rest.api.LoadJSONTask;
import com.apps.greyrobe.wordrunner.oauth.OAuthListener;
import com.apps.greyrobe.wordrunner.oauth.TokenRetriever;

public class MyGoogleBooksLibrary extends WearableActivity implements OAuthListener, LoadJSONTask.Listener {
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

        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove("booksApiToken");
        editor.commit();
    }

    public void getToken(View view) {
        tokenRetriever.getAccessToken();
    }

    public Activity getActivity() {
        return this;
    }

    public void receiveApiToken(String token) {
        updateStatus("Token received. Token: " + token);
        new LoadJSONTask(this).execute("https://www.googleapis.com/books/v1/mylibrary/bookshelves/7/volumes?country=US", token);
    }

    public void onLoaded(Library library) {
        if(library != null) {
            LibraryController libraryController = new LibraryController(library);
            updateStatus(libraryController.getBookTitles());
        }
        else {
            tokenRetriever.refreshToken();
        }
    }

    public void updateStatus(String status) { textView.setText(status); }

    @Override
    public void onDestroy() {
        tokenRetriever.mClient.destroy();
        super.onDestroy();
    }

    public void onError() {
        updateStatus("Error retrieving book library.");
    }
}
