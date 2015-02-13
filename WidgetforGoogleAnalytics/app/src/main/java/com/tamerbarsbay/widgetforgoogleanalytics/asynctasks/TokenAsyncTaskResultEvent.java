package com.tamerbarsbay.widgetforgoogleanalytics.asynctasks;

/**
 * Created by Tamer on 2/9/2015.
 */
public class TokenAsyncTaskResultEvent {

    private String token;

    public TokenAsyncTaskResultEvent(String result) {
        this.token = result;
    }

    public String getToken() {
        return token;
    }

}
