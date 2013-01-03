package com.mendhak.gpslogger.loggers;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.loopj.android.http.RequestParams;

public class ServerLogger implements ILogger {

	private final String url = "http://sw4.us/ufc/default.php";
	private final String token;
	private final String TAG = "ServerLogger";
	private final String id;

	public ServerLogger(String token, String id){
		this.token = token;
		this.id = id;
	}

	public void write(Location loc) throws Exception {
		// TODO Auto-generated method stub
//		String preparedUrl = prepareURL(this.id, loc.getLatitude(), loc.getLongitude());
//		AsyncHttpClient httpClient = new AsyncHttpClient();
		
		// Parameters
//		String new_url =  url+ "?PHPSESSID="+token+"q=1&" + "id="+id + "&lat=" + Double.toString(loc.getLatitude())
//				+ "&long=" + Double.toString(loc.getLongitude());
		
//		RequestParams p = new RequestParams();
//		p.put("PHPSESSID", token);
//		p.put("q", "1");
//		p.put("id", id);
//		p.put("lat", Double.toString(loc.getLatitude()) );
//		p.put("long", Double.toString(loc.getLongitude()) );
		
		new AsyncTask<String, Void, Void>(){

			@Override
			protected Void doInBackground(String... params) {
				// Parameters
				String new_url = url + "?PHPSESSID="+params[0]+"&q=1&" + "id="+params[1]+ "&lat=" + params[2]
						+ "&long=" + params[3];
				
				
				Log.d(TAG, new_url);

				HttpGet httpGet = new HttpGet(new_url);
				
				HttpClient client = new DefaultHttpClient();
				HttpResponse response = null;

				try {
					response = client.execute(httpGet);
				} catch (Exception e) {
					Log.w(TAG, "No internet connection", e);
				}
				
				JSONObject jsonObject = null;
				try {
					jsonObject = new JSONObject(IOUtils.toString(response
							.getEntity().getContent()));
				} catch (Exception e) {
					Log.e("HistoryActivity", "Wrong JSON format", e);
				}

				try {

					if (jsonObject.has("meta")) {

						int code = jsonObject.getJSONObject("meta").getInt(
								"code");

						if (code == 200) {

							Log.d(TAG, "code " + code);

						} else {

							Log.w(TAG,
									"Cannot connect to the server: " + code + " "+jsonObject.getJSONObject("result").toString());
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "ERROR 2", e);
				}
				
				return null;
			}
			
		}.execute(token,id,Double.toString(loc.getLatitude()),Double.toString(loc.getLongitude()));
		
//		try{
//			Log.d(TAG,"Logging to "+url+p.toString());
//			httpClient.get(url,p,new AsyncHttpResponseHandler() {
//			     @Override
//			     public void onSuccess(String response) {
//			    	 try {
//						JSONObject resp = new JSONObject(response);
//						int code = resp.getJSONObject("meta").getInt("code");
//						if(code==200){
//							Log.d(TAG,"Sucess logging to "+url +" "+code);
//						}
//						else{
//							Log.d(TAG,"Failure logging to "+url +" "+code);
//						}
//					} catch (JSONException e) {
//						// TODO Auto-generated catch block
//						Log.d(TAG,"Error parsing JSON: "+url);
//					}
//			     }
//			     @Override
//			     public void onFailure(Throwable e, String response) {
//			    	 Log.d(TAG,"Failure logging to "+url +" "+response);
//			     }
//			 });
//		}
//		catch(Exception e){
//			Log.e(TAG, "Could not log to "+preparedUrl,e);
//		}
		
				
				

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
