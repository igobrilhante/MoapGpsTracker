package com.mendhak.gpslogger.loggers;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;

import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class ServerLogger implements ILogger {

	private final String url = "http://sw4.us/ufc/?";
	private String id;
	private final String TAG = "ServerLogger";

	public ServerLogger(String id) {
		this.id = id;
	}

	public void write(Location loc) throws Exception {
		// TODO Auto-generated method stub
		String preparedUrl = prepareURL(this.id, loc.getLatitude(), loc.getLongitude());
		Log.d(TAG, "Logging to "+preparedUrl);
		
			new AsyncTask<String, Void, Void>(){

				@Override
				protected Void doInBackground(String... params) {
					try {
					HttpGet httpGet = new HttpGet(params[0]);
					HttpClient client = new DefaultHttpClient();
					client.execute(httpGet);
					} catch (Exception e) {
						Log.e(TAG, "Cannot write", e);
					}
					return null;
				}
				
			}.execute(preparedUrl);

	}

	public void annotate(String description, Location loc) throws Exception {
		// TODO Auto-generated method stub

	}

	public void annotate(String name, String description, Location loc)
			throws Exception {
		// TODO Auto-generated method stub

	}

	public String getName() {
		// TODO Auto-generated method stub
		return "Server";
	}

	private String prepareURL(String id, double lat, double lon) {
		return url + "q=1&" + "id=" + id + "&lat=" + lat + "&lon=" + lon;
	}

}
