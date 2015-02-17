package com.tamerbarsbay.widgetforgoogleanalytics.asynctasks;

import android.content.Context;

import java.util.HashMap;

/**
 * Created by Tamer on 2/13/2015.
 */
public class WidgetQueryAsyncTaskResultEvent {

    private HashMap<String, String> data;
    private String[] topVersions;
    private Context context;
    private int appWidgetId;

    public WidgetQueryAsyncTaskResultEvent(Context _context, HashMap<String, String> _data, String[] _topVersions, int _appWidgetId) {
        this.data = _data;
        this.topVersions = _topVersions;
        this.context = _context;
        this.appWidgetId = _appWidgetId;
    }

    public HashMap<String, String> getData() {
        return data;
    }

    public String[] getTopVersions() {
        return topVersions;
    }

    public Context getContext() { return context; }

    public int getAppWidgetId() { return appWidgetId; }
}
