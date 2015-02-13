package com.tamerbarsbay.widgetforgoogleanalytics;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.analytics.Analytics;
import com.squareup.otto.Subscribe;
import com.tamerbarsbay.widgetforgoogleanalytics.asynctasks.QueryAsyncTask;
import com.tamerbarsbay.widgetforgoogleanalytics.asynctasks.WidgetQueryAsyncTaskResultEvent;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Implementation of App Widget functionality.
 */
public class UsersPurchasesWidget extends AppWidgetProvider {

    private static final String LABEL_NEW_VISITOR = "New Visitor";
    private static final String LABEL_CURRENT_VISITOR = "Returning Visitor";
    private static final String LABEL_PREMIUM_PURCHASES = "Premium Purchased";

    public static final int QUERY_USER_TYPES = 0;
    public static final int QUERY_PREMIUM_PURCHASES = 1;
    public static final int QUERY_TOP_VERSIONS = 2;

    private static final String ANALYTICS_ID = "ga:86969176";
    private static String START_DATE = "2015-02-08";
    private static String END_DATE = "2015-02-08";
    private static final String DIMENSIONS = "ga:userType";
    private static final String METRIC_USERS = "ga:users";

    private static final String mEmail = "tamerbars@gmail.com";
    private static final String REPORTING_SCOPE = "https://www.googleapis.com/auth/analytics.readonly";
    private static final String mScopes = "oauth2:" + REPORTING_SCOPE;

    private static final String UPDATE_ARRIVALS = "com.tamerbarsbay.widgetforgoogleanalytics.WIDGET_UPDATE_DATA";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        EventBus.getInstance().register(this);
    }

    @Override
    public void onDisabled(Context context) {
        EventBus.getInstance().unregister(this);
    }

    @Subscribe
    public void onQueryAsyncTaskResult(WidgetQueryAsyncTaskResultEvent event) {
        HashMap<String, String> data = event.getData();
        String[] topVersions = event.getTopVersions();
        updateWidgetValues(data, topVersions);
    }

    private static void updateWidgetValues(HashMap<String, String> data, String[] topVersions) {
        
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        setDates();

        if (isDeviceOnline(context)) {
            String token = PreferenceManager.getDefaultSharedPreferences(context).getString("token", null);
            if (token == null) {
                Toast.makeText(context, "Null token", Toast.LENGTH_SHORT).show();
            } else {
                executeQueries(context, token);
            }
        } else {
            Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show();
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.users_purchases_widget);

        String currentUsers = results.get(LABEL_CURRENT_VISITOR);
        String newUsers = results.get(LABEL_NEW_VISITOR);
        String premiumPurchases = results.get(LABEL_PREMIUM_PURCHASES);
        if (premiumPurchases == null) {
            premiumPurchases = "0";
        }

        String topVersionOne = "";
        String topVersionTwo = "";
        String topVersionThree = "";
        String topVersionOneUsers = "";
        String topVersionTwoUsers = "";
        String topVersionThreeUsers = "";
        try {
            topVersionOne = TOP_VERSIONS[0];
            topVersionTwo = TOP_VERSIONS[1];
            topVersionThree = TOP_VERSIONS[2];
            topVersionOneUsers = results.get(topVersionOne);
            topVersionTwoUsers = results.get(topVersionTwo);
            topVersionThreeUsers = results.get(topVersionThree);
        } catch (ArrayIndexOutOfBoundsException e) {
            Toast.makeText(context, "Error loading data", Toast.LENGTH_SHORT).show();
            return;
        }

        views.setTextViewText(R.id.widget_current_users_data, currentUsers);
        views.setTextViewText(R.id.widget_new_users_data, newUsers);
        views.setTextViewText(R.id.widget_premium_purchases_data, premiumPurchases);
        views.setTextViewText(R.id.widget_version_label_1, topVersionOne);
        views.setTextViewText(R.id.widget_version_label_2, topVersionTwo);
        views.setTextViewText(R.id.widget_version_label_3, topVersionThree);
        views.setTextViewText(R.id.widget_version_users_1, topVersionOneUsers);
        views.setTextViewText(R.id.widget_version_users_2, topVersionTwoUsers);
        views.setTextViewText(R.id.widget_version_users_3, topVersionThreeUsers);

        views.setOnClickPendingIntent(R.id.widget_layout, createRefreshIntent(context, appWidgetId));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Get the id of the widget instance being updated and create an instance of the
        // AppWidgetManager class
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // Handle the intent appropriately depending on what it was
        if (intent.getAction().equals(UPDATE_ARRIVALS)) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private static boolean isDeviceOnline(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private static void setDates() {
        TimeZone tz = TimeZone.getTimeZone("America/Chicago");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(tz);
        Calendar c = Calendar.getInstance(tz);
        String today = format.format(new Date());
        START_DATE = today;
        END_DATE = today;
    }

    private static void executeQueries(Context context, String token) {
        GoogleCredential credential = new GoogleCredential();
        credential.setAccessToken(token);
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Analytics service = new Analytics.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Widget-For-Google-Analytics")
                .build();
        if (service == null) {
            // refresh the token
            Toast.makeText(context, "Null service", Toast.LENGTH_SHORT).show();
            return;
        }
        if (service != null) {
            try {
                Analytics.Data.Ga.Get query = service.data().ga()
                        .get(ANALYTICS_ID, START_DATE, END_DATE, METRIC_USERS)
                        .setOutput("json")
                        .setDimensions(DIMENSIONS);
                new QueryAsyncTask(query).execute(QUERY_USER_TYPES);
                Analytics.Data.Ga.Get query2 = service.data().ga()
                        .get(ANALYTICS_ID, START_DATE, END_DATE, "ga:totalEvents")
                        .setOutput("json")
                        .setDimensions("ga:eventAction")
                        .setFilters("ga:eventAction==Premium Purchased");
                new QueryAsyncTask(query2).execute(QUERY_PREMIUM_PURCHASES);
                Analytics.Data.Ga.Get query3 = service.data().ga()
                        .get(ANALYTICS_ID, START_DATE, END_DATE, METRIC_USERS)
                        .setOutput("json")
                        .setDimensions("ga:appVersion")
                        .setSort("-ga:users")
                        .setMaxResults(3);
                QueryAsyncTask thread = new QueryAsyncTask(query3);
                thread.execute(QUERY_TOP_VERSIONS);
                try {
                    thread.get(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    Log.d("Tamer", "Timeout exception");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static PendingIntent createRefreshIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(UPDATE_ARRIVALS);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }
}


