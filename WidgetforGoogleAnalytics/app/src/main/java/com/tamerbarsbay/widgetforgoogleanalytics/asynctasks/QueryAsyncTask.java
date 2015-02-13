package com.tamerbarsbay.widgetforgoogleanalytics.asynctasks;

import android.os.AsyncTask;

import com.google.api.services.analytics.Analytics;
import com.tamerbarsbay.widgetforgoogleanalytics.EventBus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Tamer on 2/9/2015.
 */
public class QueryAsyncTask extends AsyncTask<Void, Void, String> {

    Analytics.Data.Ga.Get mQuery;

    public QueryAsyncTask(Analytics.Data.Ga.Get query) {
        mQuery = query;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            InputStream is = mQuery.executeAsInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line + "\n");
            }
            String result = sb.toString();
            is.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        EventBus.getInstance().post(new QueryAsyncTaskResultEvent(result));
    }
}