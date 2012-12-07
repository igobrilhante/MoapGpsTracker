package org.moap.gpstracker.oauth;

import com.mendhak.gpslogger.common.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.graphics.Bitmap;
import arida.ufc.br.moapgpstracker.R;
import arida.ufc.br.moapgpstracker.R.id;
import arida.ufc.br.moapgpstracker.R.layout;

public class ActivityWebView extends Activity{
    private static final String TAG = "ActivityWebView";
    private SharedPreferences sharedPrefs;
    
    /**
     * Get these values after registering your oauth app at: https://foursquare.com/oauth/
     */

    


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        Log.d(this.getClass().getSimpleName(), "On create");
        String url =
            "https://foursquare.com/oauth2/authenticate" + 
                "?client_id=" + FoursquareCredentials.CLIENT_ID + 
                "&response_type=token" + 
                "&redirect_uri=" + FoursquareCredentials.CLIENT_CALLBACK;
        
        // If authentication works, we'll get redirected to a url with a pattern like:  
        //
        //    http://YOUR_REGISTERED_REDIRECT_URI/#access_token=ACCESS_TOKEN
        //
        // We can override onPageStarted() in the web client and grab the token out.
        WebView webview = (WebView)findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        this.sharedPrefs = this.getSharedPreferences("moap", Context.MODE_PRIVATE);
        webview.setWebViewClient(new WebViewClient() {
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                String fragment = "#access_token=";
                int start = url.indexOf(fragment);
                if (start > -1) {
                    // You can use the accessToken for api calls now.
                    String accessToken = url.substring(start + fragment.length(), url.length());
        			
                    Log.v(TAG, "OAuth complete, token: [" + accessToken + "].");
                    sharedPrefs.edit().putString("user.foursquare.token", accessToken).commit();
                	
//                    Toast.makeText(ActivityWebView.this, "Token: " + accessToken, Toast.LENGTH_SHORT).show();
                    Utilities.toastMensage(ActivityWebView.this, getResources().getText(R.string.suc_login).toString()).show();
                    finish();
                }
            }
        });
        webview.loadUrl(url);
        
    }
}
