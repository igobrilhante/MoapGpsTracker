package arida.ufc.br.moapgpstracker;

import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.moap.chart.SpeedOverTimeChart;
import org.moap.overlays.GoogleMapsOverlay;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import arida.ufc.br.moap.core.beans.LatLonPoint;
import arida.ufc.br.moap.core.beans.MovingObject;
import arida.ufc.br.moap.core.beans.Trajectory;
import arida.ufc.br.moap.datamodelapi.imp.TrajectoryModelImpl;

public class HistoryActivity extends MapActivity {

	private enum ResponseType {
		INTERNET_ISSUE, OK, ERROR
	};

	// List of points provided by the remote Server in response to a request
	private JSONArray list_of_points;
	private final DateTimeFormatter fmt = DateTimeFormat
			.forPattern("yyyy-M-d'+'H:m:s");
	private final String TAG = "HistoryActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);

		DateTime today = new DateTime();

		getHistory(today.minusDays(5), today);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_history, menu);
		return true;
	}

	@SuppressWarnings("unchecked")
	private void onChartMenu() {
		TrajectoryModelImpl<LatLonPoint, DateTime> model = getTrajectoryHistory();
		
		if(model!=null){
			Log.w(TAG, "onCharMenu");
			
			SpeedOverTimeChart analysis = new SpeedOverTimeChart(model);
	
			XYMultipleSeriesDataset dataset = analysis.createDataset();
			XYMultipleSeriesRenderer renderer = analysis.createRenderer();
	
			if (dataset != null && renderer != null) {
				try {
					Intent intent = ChartFactory.getTimeChartIntent(this, dataset,
							renderer, "HH:mm");
					// Intent intent = ChartFactory.getLineChartIntent(this,
					// dataset,renderer);
	
					startActivity(intent);
				} catch (Exception ex) {
					Utilities.LogError("ERROR CHART", ex);
				}
	
			} else {
				Utilities.LogDebug(String.format("Dataset - %s\nRenderer - ",
						dataset.toString(), renderer.toString()));
			}
		}
		else{
			Log.w(TAG, "onCharMenu - model is null");
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		int idx = item.getItemId();
		switch (idx) {
		case R.id.history_chart:
			onChartMenu();
			break;
		default:
			break;
		}

		return true;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void getHistory(DateTime begin, DateTime end) {

		new AsyncTask<DateTime, Void, ResponseType>() {

			@Override
			protected ResponseType doInBackground(DateTime... params) {
				// TODO Auto-generated method stub

				String url = "http://sw4.us/ufc/";

				// Parameters
				url += "?q=2&" + "id=1&" + "start=" + params[0].toString(fmt)
						+ "&end=" + params[1].toString(fmt);

				Log.d(TAG, url);

				HttpGet httpGet = new HttpGet(url);

				// BasicHttpParams p = new BasicHttpParams();
				// p.setParameter("q", Integer.toString(2));
				// p.setParameter("id", "1");
				// p.setParameter("start", params[0].toString(fmt));
				// p.setParameter("end", params[1].toString(fmt));
				// httpGet.setParams(p);

				Log.d("HistoryActivity", "URI: " + httpGet.getURI().toString());
				HttpClient client = new DefaultHttpClient();
				HttpResponse response = null;

				try {
					response = client.execute(httpGet);
				} catch (Exception e) {
					Log.w("HistoryActivity", "No internet connection", e);

					return ResponseType.INTERNET_ISSUE;
				}
				JSONObject jsonObject = null;
				try {
					jsonObject = new JSONObject(IOUtils.toString(response
							.getEntity().getContent()));
				} catch (Exception e) {
					Log.e("HistoryActivity", "Wrong JSON format", e);
					return ResponseType.ERROR;
				}

				try {

					if (jsonObject.has("meta")) {

						int code = jsonObject.getJSONObject("meta").getInt(
								"code");

						if (code == 200) {

							Log.d("HistoryActivity", "code " + code);

							list_of_points = jsonObject.getJSONObject("result")
									.getJSONArray("point");
						} else {

							Log.w("HistoryActivity",
									"Cannot connect to the server: " + code);
							return ResponseType.INTERNET_ISSUE;
						}
					}
				} catch (Exception e) {
					Log.e("HistoryActivity", "ERROR 2", e);
				}

				return ResponseType.OK;

			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				ProgressBar pb = (ProgressBar) findViewById(R.id.history_progress_bar);
				pb.setEnabled(true);
				pb.setVisibility(View.VISIBLE);
				pb.setIndeterminate(true);
				Log.d("HistoryActivity", "onPreExecute");

			}

			@Override
			protected void onPostExecute(ResponseType v) {

				ProgressBar pb = (ProgressBar) findViewById(R.id.history_progress_bar);
				pb.setEnabled(false);
				pb.setVisibility(View.INVISIBLE);
				pb.setIndeterminate(false);

				Log.d("HistoryActivity", "onPosExecute");

				// Check the result of the process
				switch (v) {
				case OK:
					drawTrajectoryHistory();
					break;
				case INTERNET_ISSUE:
					Toast.makeText(getApplicationContext(),
							"Cannot connect to the Server", Toast.LENGTH_SHORT)
							.show();
					break;
				case ERROR:
					break;
				default:
					break;
				}

			}

		}.execute(begin, end);

	}

	@SuppressWarnings({ "unchecked"})
	private TrajectoryModelImpl<LatLonPoint, DateTime> getTrajectoryHistory() {
		
		Log.d(TAG, "get trajectory history");
		
		TrajectoryModelImpl<LatLonPoint, DateTime> model = null;
		if (this.list_of_points != null) {
			
			int size = this.list_of_points.length();

			model = new TrajectoryModelImpl<LatLonPoint, DateTime>();

			MovingObject mo = model.factory().newMovingObject(
					Session.getUserName());

			Trajectory<LatLonPoint, DateTime> traj = model.factory()
					.newTrajectory(mo + "_0", mo);
			try {
				Log.d(TAG, "Loading trajectory model from JSON");
				for (int i = 0; i < size; i++) {
					JSONObject object = this.list_of_points.getJSONObject(i);
					double lat = object.getDouble("lat");
					double lon = object.getDouble("long");
					LatLonPoint point = new LatLonPoint(lon, lat);
					String date = object.getString("time");

					DateTime datetime = fmt.parseDateTime(date);
					traj.addPoint(point, datetime);

				}
				model.addTrajectory(traj);
			} catch (Exception e) {

			}

		}
		return model;
	}

	/*
	 * Receive JSONArray with {time,lat,long} and draw on the map through
	 * GoogleMapsOverlay
	 */
	private void drawTrajectoryHistory() {

		Log.d(TAG, "Draw trajectory history");

		// Map settings
		MapView mapView = (MapView) findViewById(R.id.history_map_view);
		mapView.setBuiltInZoomControls(true);
		mapView.displayZoomControls(true);
		mapView.setClickable(true);

		// Overlay list
		List<Overlay> overlayList = mapView.getOverlays();
		overlayList.clear();

		// Google Maps overlay
		GoogleMapsOverlay overlay = new GoogleMapsOverlay(Color.BLUE);
		if (this.list_of_points != null) {

			int size = this.list_of_points.length();

			try {

				Log.d(TAG, "Loading trajectory model from JSON");

				for (int i = 0; i < size; i++) {
					JSONObject object = this.list_of_points.getJSONObject(i);
					double lat = object.getDouble("lat");
					double lon = object.getDouble("long");

					GeoPoint geoPoint = new GeoPoint(
							Utilities.convertCoordinates(lat),
							Utilities.convertCoordinates(lon));
					OverlayItem oi = new OverlayItem(geoPoint, "", "");

					overlay.addOverlayItem(oi);
				}

				overlayList.add(overlay);
			} catch (Exception e) {

			}

			// View the map
			mapView.invalidate();
		}

	}

}
