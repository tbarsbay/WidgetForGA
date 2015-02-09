package com.tamerbarsbay.widgetforgoogleanalytics;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
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
import com.google.api.services.analytics.model.GaData;

import java.io.IOException;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1001;

    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();

    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    Analytics service;

    String mEmail;

    private final static String REPORTING_SCOPE = "https://www.googleapis.com/auth/analytics.readonly";
    private final static String mScopes = "oauth2:" + REPORTING_SCOPE;

    private static final String ANALYTICS_ID = "ga:86969176";
    private static final String START_DATE = "2015-02-08";
    private static final String END_DATE = "2015-02-08";
    private static final String DIMENSIONS = "ga:userType";
    private static final String METRIC_USERS = "ga:users";

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        GoogleApiClient client = new GoogleApiClient.Builder(this)
                .addApi(GoogleAnalytics.API)
                .addScope()
                */
        getUsername();
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
                new GetUsernameTask(MainActivity.this, mEmail, mScopes).execute();
                /*
                GaData.Query query = new GaData.Query();
                query.setStartDate("2015-02-08");
                query.setEndDate("2015-02-08");
                query.setIds("ga:86969176");
                query.setDimensions("ga:userType");
                List<String> metrics = new ArrayList<String>();
                metrics.add("ga:users");
                query.setMetrics(metrics); */

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

    private void doStuff(String token) {
        GoogleCredential credential = new GoogleCredential();
        credential.setAccessToken(token);
        service = new Analytics.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Widget-For-Google-Analytics")
                .build();
        if (service != null) {
            Log.d("Tamer", "Service is not null!!!!");
            try {
                GaData data = service.data().ga().get(ANALYTICS_ID, START_DATE, END_DATE, "ga:User").execute();
                printDataTable(data);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("Tamer", "IOException at data get");
            }
        } else {
            Log.d("Tamer", "Service is null UGH");
        }
    }

    private void printDataTable(GaData gaData) {
        if (gaData.getTotalResults() > 0) {
            Log.d("Tamer", "Data Table:");

            // Print the rows of data.
            for (List<String> rowValues : gaData.getRows()) {
                for (String value : rowValues) {
                    Log.d("Tamer", String.format("%-32s", value));
                }
                Log.d("Tamer", "");
            }
        } else {
            Log.d("Tamer", "No results found");
        }
    }

    static class GetUsernameTask extends AsyncTask<Void, Void, Void> {
        MainActivity mActivity;
        String mScope;
        String mEmail;

        GetUsernameTask(MainActivity activity, String name, String scope) {
            this.mActivity = activity;
            this.mScope = scope;
            this.mEmail = name;
        }

        /**
         * Executes the asynchronous job. This runs when you call execute()
         * on the AsyncTask instance.
         */
        @Override
        protected Void doInBackground(Void... params) {
            try {
                String token = fetchToken();
                if (token != null) {
                    // Insert the good stuff here.
                    // Use the token to access the user's Google data.
                    Log.d("Tamer", "Token is not null");
                    mActivity.doStuff(token);
                } else {
                    Log.d("Tamer", "Token is null");
                }
            } catch (IOException e) {
                Log.d("Tamer", "IOException");
            }
            return null;
        }

        /**
         * Gets an authentication token from Google and handles any
         * GoogleAuthException that may occur.
         */
        protected String fetchToken() throws IOException {
            try {
                return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
            } catch (UserRecoverableAuthException userRecoverableException) {
                Log.d("Tamer", "UserRecoverableException");
                mActivity.handleException(userRecoverableException);
            } catch (GoogleAuthException fatalException) {
                Log.d("Tamer", "GoogleAuthException");
            }
            return null;
        }
    }
}
