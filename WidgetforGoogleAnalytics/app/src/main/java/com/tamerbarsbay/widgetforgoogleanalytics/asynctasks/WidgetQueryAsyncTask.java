package com.tamerbarsbay.widgetforgoogleanalytics.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
public class WidgetQueryAsyncTask extends AsyncTask<Void, Void, Void> {

    private Analytics.Data.Ga.Get[] mQueries;
    private static HashMap<String, String> mData = new HashMap<String, String>();
    private static String[] mTopVersions = new String[3];
    private Context mContext;
    private int mAppWidgetId;

    public WidgetQueryAsyncTask(Context context, Analytics.Data.Ga.Get[] queries, int appWigdetId) {
        mContext = context;
        mAppWidgetId = appWigdetId;
        mQueries = queries;
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (int i=0; i < mQueries.length; i++) {
            Analytics.Data.Ga.Get query = mQueries[i];
            try {
                InputStream is = query.executeAsInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                String result = sb.toString();
                is.close();
                handleJson(query.getDimensions(), result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        EventBus.getInstance().post(new WidgetQueryAsyncTaskResultEvent(mContext, mData, mTopVersions, mAppWidgetId));
    }

    private void handleJson(String dimensions, String result) {
        try {
            JSONObject jObject = new JSONObject(result);
            JSONArray rows = jObject.getJSONArray("rows");
            for (int i=0; i < rows.length(); i++) {
                JSONArray row = rows.getJSONArray(i);
                String title = row.getString(0).trim();
                String value = row.getString(1).trim();
                if (dimensions == UsersPurchasesWidget.DIMENS_APPVERSION) {
                    mTopVersions[i] = title;
                    mData.put(title, value);
                    Log.d("Tamer", "Title: " + title + ". Value: " + value);
                } else {
                    mData.put(title, value);
                    Log.d("Tamer", "Title: " + title + ". Value: " + value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
