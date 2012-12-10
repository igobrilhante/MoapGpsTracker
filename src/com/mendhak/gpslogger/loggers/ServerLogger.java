package com.mendhak.gpslogger.loggers;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class ServerLogger implements ILogger {

	private final String url = "http://sw4.us/ufc/";
	private String id;
	private final String TAG = "ServerLogger";

	public ServerLogger(String id) {
		this.id = id;
	}

	public void write(Location loc) throws Exception {
		// TODO Auto-generated method stub
		String preparedUrl = prepareURL(this.id, loc.getLatitude(), loc.getLongitude());
		AsyncHttpClient httpClient = new AsyncHttpClient();
		
		RequestParams p = new RequestParams();
		p.put("q", "1");
		p.put("id", id);
		p.put("lat", Double.toString(loc.getLatitude()) );
		p.put("long", Double.toString(loc.getLongitude()) );
		
		try{
			Log.d(TAG,"Logging to "+url+p.toString());
			httpClient.get(url,p,new AsyncHttpResponseHandler() {
			     @Override
			     public void onSuccess(String response) {
			    	 try {
						JSONObject resp = new JSONObject(response);
						int code = resp.getJSONObject("meta").getInt("code");
						if(code==200){
							Log.d(TAG,"Sucess logging to "+url +" "+code);
						}
						else{
							Log.d(TAG,"Failure logging to "+url +" "+code);
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						Log.d(TAG,"Error parsing JSON: "+url);
					}
			     }
			     @Override
			     public void onFailure(Throwable e, String response) {
			    	 Log.d(TAG,"Failure logging to "+url +" "+response);
			     }
			 });
		}
		catch(Exception e){
			Log.e(TAG, "Could not log to "+preparedUrl,e);
		}
		
				
				

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
		return url + "q=1&" + "id=" + id + "&lat=" + lat + "&long=" + lon;
	}

}
