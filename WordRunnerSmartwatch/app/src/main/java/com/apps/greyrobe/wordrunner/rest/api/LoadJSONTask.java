package com.apps.greyrobe.wordrunner.rest.api;

import android.os.AsyncTask;

import com.apps.greyrobe.wordrunner.books.Book;
import com.apps.greyrobe.wordrunner.books.Library;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoadJSONTask extends AsyncTask<String, Void, Library> {

    public LoadJSONTask(Listener listener) {

        mListener = listener;
    }

    public interface Listener {

        void onLoaded(Library library);

        void onError();
    }

    private Listener mListener;

    @Override
    protected Library doInBackground(String... strings) {
        try {

            String stringResponse = loadJSON(strings);
            Gson gson = new Gson();

            Library library = gson.fromJson(stringResponse, Library.class);
            return library;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(Library library) {

        if (library != null) {

            mListener.onLoaded(library);

        } else {

            mListener.onError();
        }
    }

    private String loadJSON(String... strings) throws IOException {
        String jsonURL = strings[0];
        String token = null;
        //Case token provided
        if(strings.length == 2) {
            token = strings[1];
        }

        URL url = new URL(jsonURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        if(token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        conn.connect();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();

        while ((line = in.readLine()) != null) {

            response.append(line);
        }

        in.close();
        return response.toString();
    }
}