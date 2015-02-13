package com.tamerbarsbay.widgetforgoogleanalytics.asynctasks;

import java.util.HashMap;

/**
 * Created by Tamer on 2/13/2015.
 */
public class WidgetQueryAsyncTaskResultEvent {

    private HashMap<String, String> data;
    private String[] topVersions;

    public WidgetQueryAsyncTaskResultEvent(HashMap<String, String> _data, String[] _topVersions) {
        this.data = _data;
        this.topVersions = _topVersions;
    }

    public HashMap<String, String> getData() {
        return data;
    }

    public String[] getTopVersions() {
        return topVersions;
    }
}
