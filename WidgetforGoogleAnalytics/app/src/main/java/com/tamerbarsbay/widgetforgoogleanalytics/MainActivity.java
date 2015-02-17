package com.tamerbarsbay.widgetforgoogleanalytics;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.analytics.Analytics;
import com.squareup.otto.Subscribe;
import com.tamerbarsbay.widgetforgoogleanalytics.asynctasks.QueryAsyncTask;
import com.tamerbarsbay.widgetforgoogleanalytics.asynctasks.QueryAsyncTaskResultEvent;
import com.tamerbarsbay.widgetforgoogleanalytics.asynctasks.TokenAsyncTask;
import com.tamerbarsbay.widgetforgoogleanalytics.asynctasks.TokenAsyncTaskResultEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class MainActivity extends ActionBarActivity {

    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1001;

    private static final String ANALYTICS_ID = "ga:86969176";
    private String START_DATE = "2015-02-08";
    private String END_DATE = "2015-02-08";
    private static final String DIMENSIONS = "ga:userType";
    private static final String METRIC_USERS = "ga:users";

    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();

    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    Analytics service;

    String mEmail;

    ArrayList<String> results;

    private final static String REPORTING_SCOPE = "https://www.googleapis.com/auth/analytics.readonly";
    private final static String mScopes = "oauth2:" + REPORTING_SCOPE;

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Subscribe
    public void onTokenAyncTaskResult(TokenAsyncTaskResultEvent event) {
        String token = event.getToken();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("token", token).apply();
        getToken(token);
    }

    @Subscribe
    public void onQueryAsyncTaskResult(QueryAsyncTaskResultEvent event) {
        String result = event.getResult();
        handleJson(result);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getInstance().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getInstance().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDates();

        results = new ArrayList<String>();
        getUsername();
    }

    private void setDates() {
        TimeZone tz = TimeZone.getTimeZone("America/Chicago");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(tz);
        Calendar c = Calendar.getInstance(tz);
        String today = format.format(new Date());
        START_DATE = today;
        END_DATE = today;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                // With the account name acquired, go get the auth token
                getUsername();
            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, "Must pick an account to proceed", Toast.LENGTH_SHORT).show();
            }
        }
        // Later, more code will go here to handle the result from some exceptions...
    }

    private void getUsername() {
        if (mEmail == null) {
            pickUserAccount();
        } else {
            if (isDeviceOnline()) {
                String token = PreferenceManager.getDefaultSharedPreferences(this).getString("token", null);
                new TokenAsyncTask(MainActivity.this, mEmail, mScopes).execute();
            } else{
                Toast.makeText(this, "No network connection available", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void handleException(final Exception e) {
        // Because this call comes from the AsyncTask, we must ensure that the following
        // code instead executes on the UI thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            MainActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    public void getToken(String token) {
        GoogleCredential credential = new GoogleCredential();
        credential.setAccessToken(token);
        service = new Analytics.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Widget-For-Google-Analytics")
                .build();
        if (service != null) {
            try {
                Analytics.Data.Ga.Get query = service.data().ga()
                        .get(ANALYTICS_ID, START_DATE, END_DATE, METRIC_USERS)
                        .setOutput("json")
                        .setDimensions(DIMENSIONS);
                new QueryAsyncTask(query).execute();
                Analytics.Data.Ga.Get query2 = service.data().ga()
                        .get(ANALYTICS_ID, START_DATE, END_DATE, "ga:totalEvents")
                        .setOutput("json")
                        .setDimensions("ga:eventAction")
                        .setFilters("ga:eventAction==Premium Purchased");
                new QueryAsyncTask(query2).execute();
                Analytics.Data.Ga.Get query3 = service.data().ga()
                        .get(ANALYTICS_ID, START_DATE, END_DATE, METRIC_USERS)
                        .setOutput("json")
                        .setDimensions("ga:appVersion")
                        .setSort("-ga:users")
                        .setMaxResults(3);
                new QueryAsyncTask(query3).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleJson(String result) {
        try {
            JSONObject jObject = new JSONObject(result);
            JSONArray rows = jObject.getJSONArray("rows");
            for (int i=0; i < rows.length(); i++) {
                JSONArray row = rows.getJSONArray(i);
                String title = row.getString(0);
                String value = row.getString(1);
                String full = title + ": " + value;
                results.add(full);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, results);
        ListView lv = (ListView)findViewById(R.id.main_listview);
        lv.setAdapter(adapter);
    }
}
