package com.tamerbarsbay.widgetforgoogleanalytics.asynctasks;

import android.os.AsyncTask;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.tamerbarsbay.widgetforgoogleanalytics.EventBus;
import com.tamerbarsbay.widgetforgoogleanalytics.MainActivity;

import java.io.IOException;

/**
 * Created by Tamer on 2/9/2015.
 */
public class TokenAsyncTask extends AsyncTask<Void, Void, String> {
    MainActivity mActivity;
    String mScope;
    String mEmail;

    public TokenAsyncTask(MainActivity activity, String name, String scope) {
        this.mActivity = activity;
        this.mScope = scope;
        this.mEmail = name;
    }

    /**
     * Executes the asynchronous job. This runs when you call execute()
     * on the AsyncTask instance.
     */
    @Override
    protected String doInBackground(Void... params) {
        try {
            String token = fetchToken();
            if (token != null) {
                return token;
            }
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        EventBus.getInstance().post(new TokenAsyncTaskResultEvent(result));
    }

    /**
     * Gets an authentication token from Google and handles any
     * GoogleAuthException that may occur.
     */
    protected String fetchToken() throws IOException {
        try {
            return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
        } catch (UserRecoverableAuthException userRecoverableException) {
            mActivity.handleException(userRecoverableException);
        } catch (GoogleAuthException fatalException) {
        }
        return null;
    }
}