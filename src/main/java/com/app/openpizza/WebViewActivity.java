package com.app.testapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

import static com.app.testapp.CommonUtilities.DISPLAY_MESSAGE_ACTION;
import static com.app.testapp.CommonUtilities.EXTRA_MESSAGE;
import static com.app.testapp.CommonUtilities.SENDER_ID;


public class WebViewActivity extends ActionBarActivity {

    ImageView iv;
    WebView webView;
    ProgressBar progress;

    // Asyntask
    AsyncTask<Void, Void, Void> mRegisterTask;

    // Alert dialog manager
    AlertDialogManager alert = new AlertDialogManager();

    // Connection detector
    ConnectionDetector cd;

    public static String name;
    public static String email;
    private SharedPreferences settings;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        settings = getSharedPreferences("registerPrefs", Context.MODE_PRIVATE);

        if(!settings.getBoolean("isRegistered",false)) {
            Log.d("WebViewActivity","isRegistered: "+settings.getBoolean("isRegistered",false));
            Log.d("Main Activity", "Started Main");
            cd = new ConnectionDetector(getApplicationContext());

            // Check if Internet present
		if (!cd.isConnectingToInternet()) {
			// Internet Connection is not present
			alert.showAlertDialog(WebViewActivity.this,
					"Internet Connection Error",
					"Please connect to working Internet connection", false);
			// stop executing code by return
			return;
		}

            // Getting name, email from intent
            Intent i = getIntent();

            name = i.getStringExtra("name");
            email = i.getStringExtra("email");
           // Toast.makeText(this, "Name: " + name + " email: " + email, Toast.LENGTH_LONG).show();
            // Make sure the device has the proper dependencies.
            GCMRegistrar.checkDevice(this);

            // Make sure the manifest was properly set - comment out this line
            // while developing the app, then uncomment it when it's ready.
            GCMRegistrar.checkManifest(this);
            Log.d("Main", "Checked");

            registerReceiver(mHandleMessageReceiver, new IntentFilter(
                    DISPLAY_MESSAGE_ACTION));

            // Get GCM registration id
            final String regId = GCMRegistrar.getRegistrationId(this);
            Log.d("RegId", regId);
            // Check if regid already presents
            if (regId.equals("")) {

                // Registration is not present, register now with GCM
                GCMRegistrar.register(this, SENDER_ID);
            } else {
                // Device is already registered on GCM
                if (GCMRegistrar.isRegisteredOnServer(this)) {
                    // Skips registration.
                    Toast.makeText(getApplicationContext(), "Already registered with GCM", Toast.LENGTH_LONG).show();
                } else {
                    // Try to register again, but not in the UI thread.
                    // It's also necessary to cancel the thread onDestroy(),
                    // hence the use of AsyncTask instead of a raw thread.
                    final Context context = this;
                    mRegisterTask = new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... params) {
                            // Register on our server
                            // On server creates a new user
                            Log.d("Register","Trying to regsiter");
                            ServerUtilities.register(context, name, email, regId);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            Log.d("Register","Registration Complete");

                            mRegisterTask = null;
                        }

                    };
                    mRegisterTask.execute();
                    Log.d("Main", "Ended Main");
                }
            }
        }



        //webView.setWebViewClient(new MyBrowser());
        iv= (ImageView) findViewById(R.id.imageView);
        webView= (WebView) findViewById(R.id.web_view);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setMax(100);
        webView.setWebChromeClient(new MyBrowser());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.loadUrl("http://google.com");

    }

    private class MyBrowser extends WebChromeClient {



        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            WebViewActivity.this.setValue(newProgress);
            if(newProgress==100)
                iv.setVisibility(View.GONE);
        }
    }

    private void setValue(int newProgress) {
        this.progress.setProgress(newProgress);
        if(newProgress==100)
            this.progress.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Receiving push messages
     * */
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
            // Waking up mobile if it is sleeping
            WakeLocker.acquire(getApplicationContext());

            /**
             * Take appropriate action on this message
             * depending upon your app requirement
             * For now i am just displaying it on the screen
             * */

            // Showing received message
            //lblMessage.append(newMessage + "\n");
            Toast.makeText(getApplicationContext(), "New Message: " + newMessage, Toast.LENGTH_LONG).show();

            // Releasing wake lock
            WakeLocker.release();
        }
    };

    @Override
    protected void onDestroy() {
        if (mRegisterTask != null) {
            mRegisterTask.cancel(true);
        }
        try {
            unregisterReceiver(mHandleMessageReceiver);
            GCMRegistrar.onDestroy(this);
        } catch (Exception e) {
            Log.e("UnRegisterReceiverError", "> " + e.getMessage());
        }
        super.onDestroy();
    }

}
