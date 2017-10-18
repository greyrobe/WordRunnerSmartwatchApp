package com.apps.greyrobe.wordrunner.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.apps.greyrobe.wordrunner.R;
import com.apps.greyrobe.wordrunner.books.Book;
import com.apps.greyrobe.wordrunner.books.Library;
import com.apps.greyrobe.wordrunner.books.LibraryController;
import com.apps.greyrobe.wordrunner.rest.api.LoadJSONTask;
import com.apps.greyrobe.wordrunner.oauth.OAuthListener;
import com.apps.greyrobe.wordrunner.oauth.TokenRetriever;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyGoogleBooksLibrary extends WearableActivity implements OAuthListener, LoadJSONTask.Listener {
    private ListView listView;
    private TextView textView;
    private Button button;
    private TokenRetriever tokenRetriever;
    private List<Book> books = new ArrayList<Book>();
    private  ArrayAdapter<Book> bookListArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_google_books_library);
        listView = (ListView) findViewById(R.id.listView1);
        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button2);
        setAmbientEnabled();

        textView.setMovementMethod(new ScrollingMovementMethod());
        bookListArrayAdapter = new ArrayAdapter<Book>(
                this,
                android.R.layout.simple_list_item_1,
                books
        );
        listView.setAdapter(bookListArrayAdapter);
        tokenRetriever = new TokenRetriever(this);
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
            List<Book> newBooks = Arrays.asList(library.getBooks());
            books.clear();
            books.addAll(newBooks);
            bookListArrayAdapter.notifyDataSetChanged();
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
        tokenRetriever.refreshToken();
    }
}
