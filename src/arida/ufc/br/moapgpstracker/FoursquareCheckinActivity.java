package arida.ufc.br.moapgpstracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.moap.gpstracker.oauth.ActivityWebView;
import org.moap.gpstracker.oauth.FoursquareCredentials;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.LoggerService;

import oauth.signpost.OAuth;
import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.Checkin;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;
import fi.foyt.foursquare.api.io.DefaultIOHandler;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SyncStateContract.Constants;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import arida.ufc.br.moapgpstracker.R;
import arida.ufc.br.moapgpstracker.R.layout;
import arida.ufc.br.moapgpstracker.R.menu;

public class FoursquareCheckinActivity extends ListActivity {

	private FoursquareApi foursquareApi;
	private SharedPreferences prefs;
	private List<CompactVenue> veneusMap;
	private final String TAG = "FoursquareCheckinActivity";
	private final String PLACE_ID_FIELD = "";
	private final String PLACE_LAT_FIELD = "";
	private final String PLACE_LON_FIELD = "";
	private final String PLACE_NAME_FIELD = "";
	private final String PLACE_ADDRESS_FIELD = "";
	private double lat = 0.0;
	private double lon = 0.0;
	private Location location = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fscheckin);
		Log.d(this.getClass().getSimpleName(), "On create");
		// Get Lat and Lon
		if (getIntent().getExtras() != null) {
			this.location = (Location) getIntent().getExtras().get("loc");
			if(this.location!=null){

				this.lat =	this.location.getLatitude();
				this.lon =  this.location.getLongitude();
			}
			
//			this.lat = -3.73;
//			this.lon = -38.5;
			veneusMap = new ArrayList<CompactVenue>();
			getFoursquareApi();

			if (this.foursquareApi != null) {
				getListView().setOnItemClickListener(
						new AdapterView.OnItemClickListener() {

							public void onItemClick(AdapterView<?> parent,
									View view, int position, long id) {

								 CompactVenue venue = veneusMap.get((int) id);
								 checkin(venue);
								 
								 Bundle extras = new Bundle();
								 extras.putInt("option", 1);
								 extras.putString("annotation_key","checkin");
								 extras.putString("annotation_value", venue.getName() );
								 extras.putParcelable("loc", location);
								 
								 Intent service = new Intent(FoursquareCheckinActivity.this,LoggerService.class);
								 service.putExtras(extras);
								 startService(service);
								 
								 finish();
							}

						});

				new PlacesListRefresher().execute();
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_fscheckin, menu);
		return true;
	}
	
	private void checkin(final CompactVenue venue){
		
		AsyncTask<CompactVenue, Void, Boolean> task = new AsyncTask<CompactVenue, Void, Boolean>(){

			@Override
			protected Boolean doInBackground(CompactVenue... params) {
				CompactVenue venue = params[0];
				getFoursquareApi();
				try {
					Result<Checkin> result = getFoursquareApi().checkinsAdd(venue.getId(), null, null, null, null, null, null, null);
					 return true;
				} catch (FoursquareApiException e) {
					// TODO Auto-generated catch block
					Log.e(FoursquareCheckinActivity.class.getSimpleName(), "CHECKIN", e);
				}
				
				return false;
			}
			
			@Override
		    protected void onPreExecute(){
				super.onPreExecute();
				ProgressBar pb = (ProgressBar)findViewById(R.id.checking_progress_bar);
				pb.setEnabled(true);
				pb.setVisibility(View.VISIBLE);
				pb.setIndeterminate(true);
				Log.d("FoursquareCheckinActivity", "onPreExecute");
				
			}
			
			@Override
			protected void onPostExecute(Boolean result){
				ProgressBar pb = (ProgressBar)findViewById(R.id.checking_progress_bar);
				pb.setEnabled(false);
				pb.setVisibility(View.INVISIBLE);
				pb.setIndeterminate(false);
				Log.d("FoursquareCheckinActivity", "onPosExecute");
				if(result){
					Toast.makeText(FoursquareCheckinActivity.this,"Check-in  achieved at "+venue.getName(), Toast.LENGTH_LONG).show();
				}
				else{
					Toast.makeText(FoursquareCheckinActivity.this,"Check-in was not achieved", Toast.LENGTH_LONG).show();
				}
			}
			
		};
		
		task.execute(venue);
		
		
	}

	public FoursquareApi getFoursquareApi() {
		if (this.foursquareApi == null) {
			this.prefs = this.getSharedPreferences("moap", Context.MODE_PRIVATE);
			String token = (String) this.prefs.getString(
					"user.foursquare.token", "");

			// There is no access token
			if (token == "") {
				Log.d("FoursquareCheckin", "No token found");
				Intent intent = new Intent(getApplicationContext(),
						ActivityWebView.class);
				startActivity(intent);
			} else {
				Log.d("FoursquareCheckin", "Token found");
				this.foursquareApi = new FoursquareApi(
						FoursquareCredentials.CLIENT_ID, // Client ID
						FoursquareCredentials.CLIENT_SECRET, // Client Secret
						FoursquareCredentials.CLIENT_CALLBACK, // Callback
						token, new DefaultIOHandler());
			}

		}
		return this.foursquareApi;

	}

	private class PlacesListRefresher extends AsyncTask<Uri, Void, Void> {

		private final String TAG = "PlacesListRefresher";

		@Override
		protected Void doInBackground(Uri... params) {

			try {
				
				Log.d(this.getClass().getSimpleName(), "Retrieving places at "	+ lat + "," + lon);
				Result<VenuesSearchResult> venues = getFoursquareApi()
						.venuesSearch(lat + "," + lon, null, null, null, null,
								null, null, null, null, null, null);
				if(venues!=null){
					if(venues.getMeta().getCode()==200){
						CompactVenue[] compactVenues = venues.getResult().getVenues();
						Log.i(TAG, "found " + compactVenues.length + " places");
						for (CompactVenue compactVenue : compactVenues) {
							veneusMap.add(compactVenue);
						
						}
					}
					else{
						// TODO: Proper error handling
					      Log.d(this.getClass().getSimpleName(),"Error occured: ");
					      Log.d(this.getClass().getSimpleName(),"  code: " + venues.getMeta().getCode());
					      Log.d(this.getClass().getSimpleName(),"  type: " + venues.getMeta().getErrorType());
					      Log.d(this.getClass().getSimpleName(),"  detail: " + venues.getMeta().getErrorDetail()); 
					}
				}
			} catch (Exception ex) {
				Log.e(TAG, "Error retrieving venues", ex);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			
			Collections.sort(veneusMap, new Comparator<CompactVenue>() {

				public int compare(CompactVenue lhs, CompactVenue rhs) {
					// TODO Auto-generated method stub
					
					return lhs.getLocation().getDistance().compareTo(rhs.getLocation().getDistance());
				}
			});
			
			setListAdapter(new FoursquareTableAdapter(veneusMap));
		}

	}

	private CompactVenue getVenueMapFromAdapter(int position) {
		return (((FoursquareTableAdapter) getListAdapter()).getItem(position));
	}

	static class ViewHolder {
		TextView txtPlaceName;
		TextView txtPlaceAddress;
		TextView txtPlaceDistance;
		RadioButton radio;
		RelativeLayout layout;
	}

	private class FoursquareTableAdapter extends ArrayAdapter<CompactVenue> {
		FoursquareTableAdapter(List<CompactVenue> list) {
			super(FoursquareCheckinActivity.this, R.layout.place_list_row,
					list);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(
						R.layout.place_list_row, parent, false);
				holder = new ViewHolder();
				holder.txtPlaceName = (TextView) convertView
						.findViewById(R.id.row_placename);
				holder.txtPlaceAddress = (TextView) convertView
						.findViewById(R.id.row_placeaddress);
				holder.layout = (RelativeLayout) convertView
						.findViewById(R.id.row_layout);
				holder.txtPlaceDistance = (TextView)convertView.findViewById(R.id.row_placedistance);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			CompactVenue venue = getVenueMapFromAdapter(position);

			try {
				holder.txtPlaceName.setText(venue.getName());
				double distance = venue.getLocation().getDistance();
				holder.txtPlaceDistance.setText(distance+" "+getResources().getString(R.string.txt_meters));
				if (venue.getLocation().getAddress() != null
						&& venue.getLocation().getAddress().length() > 0) {
					holder.txtPlaceAddress.setText(venue.getLocation()
							.getAddress());
				} else {
					holder.txtPlaceAddress.setText(R.string.no_address_info_found);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return (convertView);
		}
	}

}
