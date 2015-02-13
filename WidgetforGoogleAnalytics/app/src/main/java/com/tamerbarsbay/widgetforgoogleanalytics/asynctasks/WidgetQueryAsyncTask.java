package com.tamerbarsbay.widgetforgoogleanalytics.asynctasks;

import android.os.AsyncTask;

import com.google.api.services.analytics.Analytics;
import com.tamerbarsbay.widgetforgoogleanalytics.EventBus;
import com.tamerbarsbay.widgetforgoogleanalytics.UsersPurchasesWidget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by Tamer on 2/13/2015.
 */
public class WidgetQueryAsyncTask extends AsyncTask<Integer, Void, Void> {

    private Analytics.Data.Ga.Get mQuery;
    private static HashMap<String, String> data = new HashMap<String, String>();
    private static String[] topVersions = new String[3];

    public WidgetQueryAsyncTask(Analytics.Data.Ga.Get query) {
        mQuery = query;
    }

    @Override
    protected Void doInBackground(Integer... params) {
        int queryType = params[0].intValue();
        try {
            InputStream is = mQuery.executeAsInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            String result = sb.toString();
            is.close();
            handleJson(queryType, result);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        EventBus.getInstance().post(new WidgetQueryAsyncTaskResultEvent(data, topVersions));
    }

    private void handleJson(int queryType, String result) {
        try {
            JSONObject jObject = new JSONObject(result);
            JSONArray rows = jObject.getJSONArray("rows");
            for (int i=0; i < rows.length(); i++) {
                JSONArray row = rows.getJSONArray(i);
                String title = row.getString(0).trim();
                String value = row.getString(1).trim();
                if (queryType == UsersPurchasesWidget.QUERY_TOP_VERSIONS) {
                    topVersions[i] = title;
                    data.put(title, value);
                } else {
                    data.put(title, value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
