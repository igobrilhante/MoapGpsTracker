package org.moap.logger.server;

import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import arida.ufc.br.moapgpstracker.HistoryActivity.ResponseType;

import com.mendhak.gpslogger.common.IActionListener;

public class ServerHelper implements IActionListener {
	// http://sw4.us/ufc/default.php?PHPSESSID=ak71dgl77rlcot4ig73hktshr6&q=2&id=1&start=0&end=2013-12-06+03:16:18
	final String basic_url = "http://sw4.us/ufc/default.php";
	final String TAG = "ServerHelper";
	private SharedPreferences sharedPrefs;
	private final Context context;
	private boolean ticket = true;

	public ServerHelper(Context context, SharedPreferences sharedPrefs) {
		this.context = context;
		this.sharedPrefs = sharedPrefs;
	}

	public void OnComplete() {
		// TODO Auto-generated method stub

	}

	public void OnFailure() {
		// TODO Auto-generated method stub

	}

	public void signoutRequest(final String login_name) {
		Log.i(TAG, "signoutRequest "+ticket);
		if (ticket) {
			ticket = false;
			AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					// TODO Auto-generated method stub

					String signin_url = basic_url + "?PHPSESSID" + "&q=0"
							+ "login=" + login_name;

					HttpGet http_get = new HttpGet(signin_url);

					HttpClient client = new DefaultHttpClient();
					HttpResponse response = null;

					try {
						response = client.execute(http_get);
					} catch (Exception e) {
						Log.w(TAG, "No internet connection", e);
						// Toast.makeText(context, "No internet connection",
						// Toast.LENGTH_SHORT).show();
						return "No internet connection";
					}

					JSONObject jsonObject = null;
					try {
						jsonObject = new JSONObject(IOUtils.toString(response
								.getEntity().getContent()));
					} catch (Exception e) {
						Log.e(TAG, "Wrong JSON format", e);
						return "Wrong JSON format";
					}

					try {

						if (jsonObject.has("meta")) {

							int code = jsonObject.getJSONObject("meta").getInt(
									"code");

							if (code == 200) {

								Log.d(TAG, "code " + code);

							} else {

								// Log.w(TAG,
								// "Problem in the server: " + code
								// + " Result: "
								// + jsonObject.getString("result"));
								//
								//
								// return jsonObject.getString("result");
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "ERROR 2", e);
					}

					return "Success";
				}
				
				@Override
				protected void onProgressUpdate(Void... v){
					Toast.makeText(context, "Logging out", Toast.LENGTH_SHORT).show();
				}

				@Override
				protected void onPostExecute(String v) {
					Toast.makeText(context, v, Toast.LENGTH_SHORT).show();
					ticket = true;
				}

			};

			task.execute();
		}
	}

	public void signinRequest(final String login_name, final String pass) {
		Log.i(TAG, "signinRequest "+ticket);
		if (ticket) {
			ticket = false;

			AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					// TODO Auto-generated method stub

					String signin_url = basic_url + "?PHPSESSID" + "&q=0"
							+ "login=" + login_name + "&pass=" + pass;

					HttpGet http_get = new HttpGet(signin_url);

					HttpClient client = new DefaultHttpClient();
					HttpResponse response = null;

					try {
						response = client.execute(http_get);
					} catch (Exception e) {
						Log.w(TAG, "No internet connection", e);
						// Toast.makeText(context, "No internet connection",
						// Toast.LENGTH_SHORT).show();
						return "No internet connection";
					}

					JSONObject jsonObject = null;
					try {
						jsonObject = new JSONObject(IOUtils.toString(response
								.getEntity().getContent()));
					} catch (Exception e) {
						Log.e(TAG, "Wrong JSON format", e);
						return "Wrong JSON format";
					}

					try {

						if (jsonObject.has("meta")) {

							int code = jsonObject.getJSONObject("meta").getInt(
									"code");

							if (code == 200) {

								Log.d(TAG, "code " + code);

								String token = jsonObject
										.getString("PHPSESSID");

								sharedPrefs
										.edit()
										.putString(
												"user.gpstrackerserver.token",
												token).commit();

							} else {

								Log.w(TAG,
										"Problem in the server: "
												+ code
												+ " Result: "
												+ jsonObject
														.getString("result"));

								return jsonObject.getString("result");
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "ERROR 2", e);
					}

					return "Success";
				}

				@Override
				protected void onProgressUpdate(Void... v){
					Toast.makeText(context, "Logging in", Toast.LENGTH_SHORT).show();
				}
				
				@Override
				protected void onPostExecute(String v) {
					Toast.makeText(context, v, Toast.LENGTH_SHORT).show();
					ticket = true;

				}

			};

			task.execute();
		}
	}

	public void signupRequest(final String name, final String login_name,
			final String pass) {
		Log.i(TAG, "signupRequest "+ticket);
		if (ticket) {
			ticket = false;
			AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

				@Override
				protected String doInBackground(Void... params) {
					// TODO Auto-generated method stub

					String signup_url = basic_url + "?PHPSESSID" + "&q=4"
							+ "login=" + login_name + "&pass=" + pass
							+ "&name=" + URLEncoder.encode(name);

					HttpGet http_get = new HttpGet(signup_url);

					HttpClient client = new DefaultHttpClient();
					HttpResponse response = null;

					try {
						response = client.execute(http_get);
					} catch (Exception e) {
						Log.w(TAG, "No internet connection", e);
						// Toast.makeText(context, "No internet connection",
						// Toast.LENGTH_SHORT).show();
						return "No internet connection";
					}

					JSONObject jsonObject = null;
					try {
						jsonObject = new JSONObject(IOUtils.toString(response
								.getEntity().getContent()));
					} catch (Exception e) {
						Log.e(TAG, "Wrong JSON format", e);
						return ResponseType.ERROR.toString();
					}

					try {

						if (jsonObject.has("meta")) {

							int code = jsonObject.getJSONObject("meta").getInt(
									"code");

							if (code == 200) {

								Log.d(TAG, "code " + code);

								// String token =
								// jsonObject.getString("PHPSESSID");
								//
								// sharedPrefs.edit().putString("user.gpstrackerserver.token",
								// token).commit();

							} else {

								Log.w(TAG,
										"Problem in the server: "
												+ code
												+ " Result: "
												+ jsonObject
														.getString("result"));

								return jsonObject.getString("result");
							}
						}
					} catch (Exception e) {
						Log.e(TAG, "ERROR 2", e);
					}

					return "Success";
				}

				@Override
				protected void onProgressUpdate(Void... v){
					Toast.makeText(context, "Signing up", Toast.LENGTH_SHORT).show();
				}

				@Override
				protected void onPostExecute(String v) {

					Toast.makeText(context, v, Toast.LENGTH_SHORT).show();
					ticket = true;
				}

			};

			task.execute();
		}
	}

}
