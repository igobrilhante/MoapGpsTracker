/*
 *    This file is part of GPSLogger for Android.
 *
 *    GPSLogger for Android is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    GPSLogger for Android is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

//TODO: Move GPSMain email now call to gpsmain to allow closing of progress bar

package com.mendhak.gpslogger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import arida.ufc.br.moapgpstracker.FoursquareCheckinActivity;
import arida.ufc.br.moapgpstracker.HistoryActivity;
import arida.ufc.br.moapgpstracker.R;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.FileLoggerFactory;
import com.mendhak.gpslogger.loggers.ILogger;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.senders.IFileSender;
import com.mendhak.gpslogger.senders.dropbox.DropBoxAuthorizationActivity;
import com.mendhak.gpslogger.senders.dropbox.DropBoxHelper;
import com.mendhak.gpslogger.senders.email.AutoEmailActivity;
import com.mendhak.gpslogger.senders.gdocs.GDocsHelper;
import com.mendhak.gpslogger.senders.gdocs.GDocsSettingsActivity;
import com.mendhak.gpslogger.senders.opengts.OpenGTSActivity;
import com.mendhak.gpslogger.senders.osm.OSMHelper;

public class GpsMainActivity extends Activity implements
		OnCheckedChangeListener, IGpsLoggerServiceClient, View.OnClickListener,
		IActionListener {

	/**
	 * General all purpose handler used for updating the UI from threads.
	 */
	private static Intent serviceIntent;
	private GpsLoggingService loggingService;
	private SharedPreferences sharedPrefs;
	public static final String MOAP = "moap";
	public static final String MOAP_USER = "moap.user";

	/**
	 * Provides a connection to the GPS Logging Service
	 */
	private final ServiceConnection gpsServiceConnection = new ServiceConnection() {

		public void onServiceDisconnected(ComponentName name) {
			loggingService = null;
		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			Utilities
					.LogDebug("on service conenction: " + name + " " + service);
			loggingService = ((GpsLoggingService.GpsLoggingBinder) service)
					.getService();
			GpsLoggingService.SetServiceClient(GpsMainActivity.this);

			// Button buttonSinglePoint = (Button)
			// findViewById(R.id.buttonSinglePoint);
			//
			// buttonSinglePoint.setOnClickListener(GpsMainActivity.this);

			if (Session.isStarted()) {
				if (Session.isSinglePointMode()) {
					SetMainButtonEnabled(false);
				} else {
					SetMainButtonChecked(true);
					// SetSinglePointButtonEnabled(false);
				}

				DisplayLocationInfo(Session.getCurrentLocationInfo());
			}

			// Form setup - toggle button, display existing location info
			ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
			buttonOnOff.setOnCheckedChangeListener(GpsMainActivity.this);

		}
	};

	/**
	 * Event raised when the form is created for the first time
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utilities.LogDebug("GpsMainActivity.onCreate");

		super.onCreate(savedInstanceState);

		Utilities.LogInfo("GPSLogger started");

		setContentView(R.layout.main2);

		initialize();

		/**
		 * Set user name for logging
		 */
		// if(!hasUsername()){
		// changeUser();
		// }
		// else{
		TextView tvUser = (TextView) findViewById(R.id.txtUser);
		tvUser.setText(Session.getUserName());
		// }

		// addPreferencesFromResource(R.xml.initialsettings);
		// PreferenceManager.setDefaultValues(this, R.xml.initialsettings,
		// false);

		// Moved to onResume to update the list of loggers
		GetPreferences();

		StartAndBindService();

	}

	private void foursquareCheckin() {

		if (Session.getAchievedAnnotations().contains("checkin")) {
			Utilities.MsgBox(getString(R.string.not_yet),
					"Cannot check in until next point", GetActivity());
			return;
		}

		if (Session.hasValidLocation()) {
			Intent intent = new Intent(getApplicationContext(),
					FoursquareCheckinActivity.class);
			intent.putExtra("loc", Session.getCurrentLocationInfo());

			// intent.putExtra("lat", Session.getCurrentLatitude());
			// intent.putExtra("lon", Session.getCurrentLongitude());
			startActivity(intent);
		} else {
			Utilities.MsgBox(getString(R.string.not_yet),
					"No location recorded", this);
		}

	}

	/*
	 * Has username
	 */

	private boolean hasUsername() {
		// Shared preferences
		this.sharedPrefs = this
				.getSharedPreferences(MOAP, Context.MODE_PRIVATE);

		String user = this.sharedPrefs.getString(MOAP_USER, "");
		Log.e("GpsMainActivity", "hasUsername - User: " + user);
		if (user == "") {
			return false;
		}

		// this.sharedPrefs.edit().putString(MOAP_USER, user).commit();
		return true;
	}

	private void initialize() {
		
		if(!hasUsername()){
			changeUser();
		}
		else{
			this.sharedPrefs = this.getSharedPreferences(MOAP, Context.MODE_PRIVATE);

			String user = this.sharedPrefs.getString(MOAP_USER, "");
			update(user);
		}

	}

	private void update(String user) {
		Log.e("GpsMainActivity", "Update user "+user);
		TextView tvUser = (TextView) findViewById(R.id.txtUser);
		tvUser.setText(user);
		Session.setUserName(user);
	}

	private void clearData() {
		Log.d("GpsMainActivity", "Clear data");
		WebView webview = (WebView) findViewById(R.id.webview);
		if (webview != null) {
			webview.clearCache(true);
			webview.clearFormData();
			webview.clearHistory();
		}

		this.sharedPrefs = this
				.getSharedPreferences(MOAP, Context.MODE_PRIVATE);
		if (this.sharedPrefs != null) {
			this.sharedPrefs.edit().clear().commit();
		}
	}

	/*
	 * Set username into shared preferences
	 */
	private void changeUser() {

		Log.d("GpsMainActivity", "change user");
		// Clear preferences
		clearData();

		// Shared preferences
		// this.sharedPrefs =

		AlertDialog.Builder alert = new AlertDialog.Builder(
				GpsMainActivity.this);

		alert.setTitle("User name");
		alert.setMessage("Select a username containing number or letters");

		// Set an EditText view to get user input
		final EditText input = new EditText(getApplicationContext());
		input.setPressed(true);

		alert.setView(input);
		alert.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						final String user = Utilities.CleanDescription(input
								.getText().toString());

						sharedPrefs = getSharedPreferences(MOAP,
								Context.MODE_PRIVATE);

						SharedPreferences.Editor editor = sharedPrefs.edit();

						editor.putString(MOAP_USER, user);

						editor.commit();

						update(user);

					}
				});
		alert.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Cancelled.
					}
				});

		alert.show();

	}

	@Override
	protected void onStart() {
		Utilities.LogDebug("GpsMainActivity.onStart");
		super.onStart();
		StartAndBindService();

	}

	@Override
	protected void onResume() {
		Utilities.LogDebug("GpsMainactivity.onResume");
		super.onResume();
		GetPreferences();
		StartAndBindService();

	}

	/**
	 * Starts the service and binds the activity to it.
	 */
	private void StartAndBindService() {
		Utilities.LogDebug("StartAndBindService - binding now");
		serviceIntent = new Intent(this, GpsLoggingService.class);
		Utilities.LogDebug("Intent: " + serviceIntent);
		// Start the service in case it isn't already running
		startService(serviceIntent);
		// Now bind to service
		bindService(serviceIntent, gpsServiceConnection,
				Context.BIND_AUTO_CREATE);
		Session.setBoundToService(true);
	}

	/**
	 * Stops the service if it isn't logging. Also unbinds.
	 */
	private void StopAndUnbindServiceIfRequired() {
		Utilities.LogDebug("GpsMainActivity.StopAndUnbindServiceIfRequired");
		if (Session.isBoundToService()) {

			unbindService(gpsServiceConnection);
			Session.setBoundToService(false);
		}

		if (!Session.isStarted()) {
			Utilities.LogDebug("StopServiceIfRequired - Stopping the service");
			serviceIntent = new Intent(this, GpsLoggingService.class);
			stopService(serviceIntent);
		}

	}

	@Override
	protected void onPause() {

		Utilities.LogDebug("GpsMainActivity.onPause");
		StopAndUnbindServiceIfRequired();
		super.onPause();
		GetPreferences();
	}

	@Override
	protected void onDestroy() {

		Utilities.LogDebug("GpsMainActivity.onDestroy");
		StopAndUnbindServiceIfRequired();
		super.onDestroy();

	}

	/**
	 * Called when the toggle button is clicked
	 */
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Utilities.LogDebug("GpsMainActivity.onCheckedChanged");
		TextView txtUser = (TextView) findViewById(R.id.txtUser);
		// EditText goal = (EditText) findViewById(R.id.goalComponent);
//		Utilities.LogDebug("User: " + txtUser.getText().toString().trim());
		// Utilities.LogDebug("Goa: " + goal);

		if (isChecked) {
			if (hasUsername()) {
				GetPreferences();
				SetSinglePointButtonEnabled(false);
				loggingService.SetupAutoSendTimers();

				loggingService.StartLogging();
			} else {
				changeUser();
			}

		} else {
			// Activity list

			SetSinglePointButtonEnabled(true);
			loggingService.StopLogging();
		}

	}

	/**
	 * Called when the single point button is clicked
	 */
	public void onClick(View view) {
		Utilities.LogDebug("GpsMainActivity.onClick");

		if (!Session.isStarted()) {
			SetMainButtonEnabled(false);
			loggingService.StartLogging();
			Session.setSinglePointMode(true);
		} else if (Session.isStarted() && Session.isSinglePointMode()) {
			loggingService.StopLogging();
			SetMainButtonEnabled(true);
			Session.setSinglePointMode(false);
		}
	}

	public void SetSinglePointButtonEnabled(boolean enabled) {
		// Button buttonSinglePoint = (Button)
		// findViewById(R.id.buttonSinglePoint);
		// buttonSinglePoint.setEnabled(enabled);
	}

	public void SetMainButtonEnabled(boolean enabled) {
		ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
		buttonOnOff.setEnabled(enabled);
	}

	public void SetMainButtonChecked(boolean checked) {
		ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
		buttonOnOff.setChecked(checked);
	}

	/**
	 * Gets preferences chosen by the user
	 */
	private void GetPreferences() {
		Utilities.PopulateAppSettings(getApplicationContext());
		ShowPreferencesSummary();
	}

	/**
	 * Displays a human readable summary of the preferences chosen by the user
	 * on the main form
	 */

	private void ShowPreferencesSummary()

	{
		Utilities.LogDebug("GpsMainActivity.ShowPreferencesSummary");
		try {
			TextView txtLoggingTo = (TextView) findViewById(R.id.txtLoggingTo);
			TextView txtFrequency = (TextView) findViewById(R.id.txtFrequency);
			TextView txtDistance = (TextView) findViewById(R.id.txtDistance);
			// TextView txtAutoEmail = (TextView)
			// findViewById(R.id.txtAutoEmail);

			List<ILogger> loggers = FileLoggerFactory.GetFileLoggers();

			if (loggers.size() > 0) {

				ListIterator<ILogger> li = loggers.listIterator();
				String logTo = li.next().getName();
				while (li.hasNext()) {
					logTo += ", " + li.next().getName();
				}
				txtLoggingTo.setText(logTo);

			} else {

				txtLoggingTo.setText(R.string.summary_loggingto_screen);

			}

			if (AppSettings.getMinimumSeconds() > 0) {
				String descriptiveTime = Utilities.GetDescriptiveTimeString(
						AppSettings.getMinimumSeconds(),
						getApplicationContext());

				txtFrequency.setText(descriptiveTime);
			} else {
				txtFrequency.setText(R.string.summary_freq_max);

			}

			if (AppSettings.getMinimumDistanceInMeters() > 0) {
				if (AppSettings.shouldUseImperial()) {
					int minimumDistanceInFeet = Utilities
							.MetersToFeet(AppSettings
									.getMinimumDistanceInMeters());
					txtDistance
							.setText(((minimumDistanceInFeet == 1) ? getString(R.string.foot)
									: String.valueOf(minimumDistanceInFeet)
											+ getString(R.string.feet)));
				} else {
					txtDistance
							.setText(((AppSettings.getMinimumDistanceInMeters() == 1) ? getString(R.string.meter)
									: String.valueOf(AppSettings
											.getMinimumDistanceInMeters())
											+ getString(R.string.meters)));
				}
			} else {
				txtDistance.setText(R.string.summary_dist_regardless);
			}

			if (AppSettings.isAutoSendEnabled()) {
				String autoEmailResx;

				if (AppSettings.getAutoSendDelay() == 0) {
					autoEmailResx = "autoemail_frequency_whenistop";
				} else {

					autoEmailResx = "autoemail_frequency_"
							+ String.valueOf(AppSettings.getAutoSendDelay())
									.replace(".", "");
				}

				// String autoEmailDesc =
				// getString(getResources().getIdentifier(
				// autoEmailResx, "string", getPackageName()));
				//
				// txtAutoEmail.setText(autoEmailDesc);
			} else {
				TableRow trAutoEmail = (TableRow) findViewById(R.id.trAutoEmail);
				trAutoEmail.setVisibility(View.INVISIBLE);
			}

			onFileName(Session.getCurrentFileName());
		} catch (Exception ex) {
			Utilities.LogError("ShowPreferencesSummary", ex);
		}

	}

	/**
	 * Handles the hardware back-button press
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Utilities.LogInfo("KeyDown - " + String.valueOf(keyCode));

		if (keyCode == KeyEvent.KEYCODE_BACK && Session.isBoundToService()) {
			StopAndUnbindServiceIfRequired();
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Called when the menu is created.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionsmenu, menu);

		return true;

	}

	/**
	 * Called when one of the menu items is selected.
	 */

	public boolean onOptionsItemSelected(MenuItem item) {

		int itemId = item.getItemId();
		Utilities.LogInfo("Option item selected - "
				+ String.valueOf(item.getTitle()));

		switch (itemId) {
		case R.id.mnuSettings:
			Intent settingsActivity = new Intent(getApplicationContext(),
					GpsSettingsActivity.class);
			startActivity(settingsActivity);
			break;
		case R.id.mnuComment:
			addAnnotationByName("comment");
			break;
		case R.id.mnuActivity:
			addAnnotationByName("activity");
			break;
		case R.id.mnuCheckin:
			foursquareCheckin();
			break;
		case R.id.mnuHistory:
			Intent history = new Intent(GpsMainActivity.this,
					HistoryActivity.class);
			startActivity(history);
			break;
			// case R.id.mnuOSM:
			// UploadToOpenStreetMap();
			// break;
			// case R.id.mnuDropBox:
			// UploadToDropBox();
			// break;
			// case R.id.mnuGDocs:
			// UploadToGoogleDocs();
			// break;
			// case R.id.mnuOpenGTS:
			// SendToOpenGTS();
			// break;
			// case R.id.mnuEmail:
			// SelectAndEmailFile();
			// break;
			// case R.id.mnuAnnotate:
			// Annotate();
			// break;
			// case R.id.mnuShare:
			// Share();
			// break;
			// case R.id.mnuEmailnow:
			// EmailNow();
			// break;
		case R.id.mnuChangeUser:
			changeUser();
			break;
		case R.id.mnuExit:
			loggingService.StopLogging();
			loggingService.stopSelf();
			finish();
			break;
		/*
		 * Menu Map View
		 */
		case R.id.mnuMapView:
			getMapView();
			break;

		case R.id.mnOpen:
			selectMultipleFiles();
			break;

		}

		return false;
	}

	private void EmailNow() {
		Utilities.LogDebug("GpsMainActivity.EmailNow");

		if (AppSettings.isAutoSendEnabled()) {
			loggingService.ForceEmailLogFile();
		} else {

			Intent pref = new Intent()
					.setClass(this, GpsSettingsActivity.class);
			pref.putExtra("autosend_preferencescreen", true);
			startActivity(pref);

		}

	}

	/**
	 * Allows user to send a GPX/KML file along with location, or location only
	 * using a provider. 'Provider' means any application that can accept such
	 * an intent (Facebook, SMS, Twitter, Email, K-9, Bluetooth)
	 */
	private void Share() {
		Utilities.LogDebug("GpsMainActivity.Share");
		try {

			final String locationOnly = getString(R.string.sharing_location_only);
			final File gpxFolder = new File(
					Environment.getExternalStorageDirectory(), "MoapGpsTracker");
			if (gpxFolder.exists()) {

				File[] enumeratedFiles = gpxFolder.listFiles();

				Arrays.sort(enumeratedFiles, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return -1
								* Long.valueOf(f1.lastModified()).compareTo(
										f2.lastModified());
					}
				});

				List<String> fileList = new ArrayList<String>(
						enumeratedFiles.length);

				for (File f : enumeratedFiles) {
					fileList.add(f.getName());
				}

				fileList.add(0, locationOnly);
				final String[] files = fileList.toArray(new String[fileList
						.size()]);

				final Dialog dialog = new Dialog(this);
				dialog.setTitle(R.string.sharing_pick_file);
				dialog.setContentView(R.layout.filelist);
				ListView thelist = (ListView) dialog
						.findViewById(R.id.listViewFiles);

				thelist.setAdapter(new ArrayAdapter<String>(
						getApplicationContext(),
						android.R.layout.simple_list_item_single_choice, files));

				thelist.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> av, View v,
							int index, long arg) {
						dialog.dismiss();
						String chosenFileName = files[index];

						final Intent intent = new Intent(Intent.ACTION_SEND);

						// intent.setType("text/plain");
						intent.setType("*/*");

						if (chosenFileName.equalsIgnoreCase(locationOnly)) {
							intent.setType("text/plain");
						}

						intent.putExtra(Intent.EXTRA_SUBJECT,
								getString(R.string.sharing_mylocation));
						if (Session.hasValidLocation()) {
							String bodyText = getString(
									R.string.sharing_googlemaps_link, String
											.valueOf(Session
													.getCurrentLatitude()),
									String.valueOf(Session
											.getCurrentLongitude()));
							intent.putExtra(Intent.EXTRA_TEXT, bodyText);
							intent.putExtra("sms_body", bodyText);
						}

						if (chosenFileName.length() > 0
								&& !chosenFileName
										.equalsIgnoreCase(locationOnly)) {
							intent.putExtra(Intent.EXTRA_STREAM, Uri
									.fromFile(new File(gpxFolder,
											chosenFileName)));
						}

						startActivity(Intent.createChooser(intent,
								getString(R.string.sharing_via)));

					}
				});
				dialog.show();
			} else {
				Utilities.MsgBox(getString(R.string.sorry),
						getString(R.string.no_files_found), this);
			}
		} catch (Exception ex) {
			Utilities.LogError("Share", ex);
		}

	}

	private void SelectAndEmailFile() {
		Utilities.LogDebug("GpsMainActivity.SelectAndEmailFile");

		Intent settingsIntent = new Intent(getApplicationContext(),
				AutoEmailActivity.class);

		if (!Utilities.IsEmailSetup()) {

			startActivity(settingsIntent);
		} else {
			ShowFileListDialog(settingsIntent,
					FileSenderFactory.GetEmailSender(this));
		}

	}

	private void SendToOpenGTS() {
		Utilities.LogDebug("GpsMainActivity.SendToOpenGTS");

		Intent settingsIntent = new Intent(getApplicationContext(),
				OpenGTSActivity.class);

		if (!Utilities.IsOpenGTSSetup()) {
			startActivity(settingsIntent);
		} else {
			IFileSender fs = FileSenderFactory.GetOpenGTSSender(
					getApplicationContext(), this);
			ShowFileListDialog(settingsIntent, fs);
		}
	}

	private void UploadToGoogleDocs() {
		Utilities.LogDebug("GpsMainActivity.UploadToGoogleDocs");

		if (!GDocsHelper.IsLinked(getApplicationContext())) {
			startActivity(new Intent(GpsMainActivity.this,
					GDocsSettingsActivity.class));
			return;
		}

		Intent settingsIntent = new Intent(GpsMainActivity.this,
				GDocsSettingsActivity.class);
		ShowFileListDialog(settingsIntent,
				FileSenderFactory.GetGDocsSender(getApplicationContext(), this));
	}

	private void UploadToDropBox() {
		Utilities.LogDebug("GpsMainActivity.UploadToDropBox");

		final DropBoxHelper dropBoxHelper = new DropBoxHelper(
				getApplicationContext(), this);

		if (!dropBoxHelper.IsLinked()) {
			startActivity(new Intent("com.mendhak.gpslogger.DROPBOX_SETUP"));
			return;
		}

		Intent settingsIntent = new Intent(GpsMainActivity.this,
				DropBoxAuthorizationActivity.class);
		ShowFileListDialog(settingsIntent,
				FileSenderFactory.GetDropBoxSender(getApplication(), this));

	}

	private void getMapView() {
		Utilities.LogDebug("GpsMainActivity.getMapVew");

		String[] array = { Session.getCurrentFileName() + ".txt" };
		startMapView(array);

	}

	private void startMapView(String[] array) {
		Intent mapViewIntent = new Intent(GpsMainActivity.this,
				GoogleMapsViewActivity.class);
		mapViewIntent.putExtra("arida.ufc.br.moap.TrajectoryView", array);

		startActivity(mapViewIntent);

	}

	/**
	 * Uploads a GPS Trace to OpenStreetMap.org.
	 */

	private void UploadToOpenStreetMap() {
		Utilities.LogDebug("GpsMainactivity.UploadToOpenStreetMap");

		if (!OSMHelper.IsOsmAuthorized(getApplicationContext())) {
			startActivity(OSMHelper
					.GetOsmSettingsIntent(getApplicationContext()));
			return;
		}

		Intent settingsIntent = OSMHelper
				.GetOsmSettingsIntent(getApplicationContext());

		ShowFileListDialog(settingsIntent,
				FileSenderFactory.GetOsmSender(getApplicationContext(), this));

	}

	/**
	 * Select multiple files
	 */
	private void selectMultipleFiles() {
		Utilities.LogDebug("GpsMainActivity.SelectMultipleFiles");
		try {

			// final String locationOnly =
			// getString(R.string.sharing_location_only);
			File gpxFolder = new File(
					Environment.getExternalStorageDirectory(), "MoapGpsTracker");
			Utilities.LogDebug(Environment.getExternalStorageDirectory()
					.toString());
			if (gpxFolder.exists()) {
				Utilities.LogDebug("GPX Folder exists");
				File[] enumeratedFiles = gpxFolder
						.listFiles(new FilenameFilter() {

							public boolean accept(File dir, String filename) {
								// TODO Auto-generated method stub

								if (filename.endsWith(".txt")) {
									return true;
								}

								return false;
							}
						});

				Arrays.sort(enumeratedFiles, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return -1
								* Long.valueOf(f1.lastModified()).compareTo(
										f2.lastModified());
					}
				});

				List<String> fileList = new ArrayList<String>(
						enumeratedFiles.length);

				for (File f : enumeratedFiles) {
					fileList.add(f.getName());
				}

				// fileList.add(0, locationOnly);
				final String[] files = fileList.toArray(new String[fileList
						.size()]);

				final Dialog dialog = new Dialog(this);
				dialog.setTitle(R.string.sharing_pick_file);
				dialog.setContentView(R.layout.filelist);

				final ListView thelist = (ListView) dialog
						.findViewById(R.id.listViewFiles);
				thelist.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
				thelist.setAdapter(new ArrayAdapter<String>(
						getApplicationContext(),
						android.R.layout.simple_list_item_multiple_choice,
						files));

				Button buttonOk = (Button) dialog
						.findViewById(R.id.buttonSelectFileOk);
				buttonOk.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {

						SparseBooleanArray selectedItems = thelist
								.getCheckedItemPositions();
						ArrayList<String> list = new ArrayList<String>();
						for (int i = 0; i < files.length; i++) {
							// If the item was selected
							if (selectedItems.get(i)) {
								Utilities
										.LogDebug("File selected: " + files[i]);
								list.add(files[i]);
							}
						}

						dialog.dismiss();

						if (list.size() > 0) {
							String[] array = new String[list.size()];
							array = list.toArray(array);
							startMapView(array);
						}

					}

				});

				Button buttonCancel = (Button) dialog
						.findViewById(R.id.buttonSelectFileCancel);
				buttonCancel.setOnClickListener(new View.OnClickListener() {

					public void onClick(View v) {
						// Nothing to do
						dialog.dismiss();
					}

				});

				dialog.show();
			} else {
				Utilities.MsgBox(getString(R.string.sorry),
						getString(R.string.no_files_found), this);
			}
		} catch (Exception ex) {
			Utilities.LogError("Exception in SelectMultipleFiles", ex);
		}
	}

	private void ShowFileListDialog(final Intent settingsIntent,
			final IFileSender sender) {

		final File gpxFolder = new File(
				Environment.getExternalStorageDirectory(), "GPSLogger");

		if (gpxFolder.exists()) {
			File[] enumeratedFiles = gpxFolder.listFiles(sender);

			Arrays.sort(enumeratedFiles, new Comparator<File>() {
				public int compare(File f1, File f2) {
					return -1
							* Long.valueOf(f1.lastModified()).compareTo(
									f2.lastModified());
				}
			});

			List<String> fileList = new ArrayList<String>(
					enumeratedFiles.length);

			for (File f : enumeratedFiles) {
				fileList.add(f.getName());
			}

			final String settingsText = getString(R.string.menu_settings);

			fileList.add(0, settingsText);
			final String[] files = fileList
					.toArray(new String[fileList.size()]);

			final Dialog dialog = new Dialog(this);
			dialog.setTitle(R.string.osm_pick_file);
			dialog.setContentView(R.layout.filelist);
			ListView displayList = (ListView) dialog
					.findViewById(R.id.listViewFiles);

			displayList.setAdapter(new ArrayAdapter<String>(
					getApplicationContext(),
					android.R.layout.simple_list_item_single_choice, files));

			displayList.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> av, View v, int index,
						long arg) {

					dialog.dismiss();
					String chosenFileName = files[index];

					if (chosenFileName.equalsIgnoreCase(settingsText)) {
						startActivity(settingsIntent);
					} else {
						Utilities.ShowProgress(GpsMainActivity.this,
								getString(R.string.please_wait),
								getString(R.string.please_wait));
						List<File> files = new ArrayList<File>();
						files.add(new File(gpxFolder, chosenFileName));
						sender.UploadFile(files);
					}
				}
			});
			dialog.show();
		} else {
			Utilities.MsgBox(getString(R.string.sorry),
					getString(R.string.no_files_found), this);
		}
	}

	private void addAnnotationByName(final String annotationName) {
		Utilities.LogDebug("GpsMainActivity.AnnotationByName");

		if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml()
				&& !AppSettings.shouldLogToPlainText()) {
			return;
		}

		if (Session.getAchievedAnnotations().contains(annotationName)) {
			Utilities.MsgBox(getString(R.string.not_yet),
					getString(R.string.cant_add_description_until_next_point),
					GetActivity());

			return;

		}

		AlertDialog.Builder alert = new AlertDialog.Builder(
				GpsMainActivity.this);

		alert.setTitle(R.string.add_description);
		alert.setMessage(R.string.letters_numbers);

		// Set an EditText view to get user input
		final EditText input = new EditText(getApplicationContext());

		alert.setView(input);

		alert.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						final String desc = Utilities.CleanDescription(input
								.getText().toString());
						Annotate(annotationName, desc);
					}
				});
		alert.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Cancelled.
					}
				});

		alert.show();
	}

	/**
	 * Prompts user for input, then adds text to log file
	 */

	private void Annotate() {
		Utilities.LogDebug("GpsMainActivity.Annotate");

		if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml()) {
			return;
		}

		if (!Session.shoulAllowDescription()) {
			Utilities.MsgBox(getString(R.string.not_yet),
					getString(R.string.cant_add_description_until_next_point),
					GetActivity());

			return;

		}

		AlertDialog.Builder alert = new AlertDialog.Builder(
				GpsMainActivity.this);

		alert.setTitle(R.string.add_description);
		alert.setMessage(R.string.letters_numbers);

		// Set an EditText view to get user input
		final EditText input = new EditText(getApplicationContext());

		alert.setView(input);

		alert.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						final String desc = Utilities.CleanDescription(input
								.getText().toString());
						Annotate(desc);
					}
				});
		alert.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Cancelled.
					}
				});

		alert.show();
	}

	private void Annotate(String description) {
		Utilities.LogDebug("GpsMainActivity.Annotate(description)");

		List<ILogger> loggers = FileLoggerFactory.GetFileLoggers();

		for (ILogger logger : loggers) {
			try {
				logger.annotate(description, Session.getCurrentLocationInfo());
				SetStatus(getString(R.string.description_added));
				Session.setAllowDescription(false);
			} catch (Exception e) {
				SetStatus(getString(R.string.could_not_write_to_file));
			}
		}
	}

	private void Annotate(String name, String description) {
		Utilities.LogDebug("GpsMainActivity.Annotate(description)");

		List<ILogger> loggers = FileLoggerFactory.GetFileLoggers();

		for (ILogger logger : loggers) {
			try {
				logger.annotate(name, description,
						Session.getCurrentLocationInfo());
				SetStatus(getString(R.string.description_added));
				Session.addAchievedAnnodation(name);
				Session.setAllowDescription(false);
				Toast.makeText(GpsMainActivity.this, "Annotate " + name,
						Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				SetStatus(getString(R.string.could_not_write_to_file));
				Toast.makeText(GpsMainActivity.this,
						getString(R.string.could_not_write_to_file),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Clears the table, removes all values.
	 */
	public void ClearForm() {

		Utilities.LogDebug("GpsMainActivity.ClearForm");

		TextView tvLatitude = (TextView) findViewById(R.id.txtLatitude);
		TextView tvLongitude = (TextView) findViewById(R.id.txtLongitude);
		TextView tvDateTime = (TextView) findViewById(R.id.txtDateTimeAndProvider);

		TextView tvAltitude = (TextView) findViewById(R.id.txtAltitude);

		TextView txtSpeed = (TextView) findViewById(R.id.txtSpeed);

		TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
		TextView txtDirection = (TextView) findViewById(R.id.txtDirection);
		TextView txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
		TextView txtDistance = (TextView) findViewById(R.id.txtDistanceTravelled);

		tvLatitude.setText("");
		tvLongitude.setText("");
		tvDateTime.setText("");
		tvAltitude.setText("");
		txtSpeed.setText("");
		txtSatellites.setText("");
		txtDirection.setText("");
		txtAccuracy.setText("");
		txtDistance.setText("");
		Session.setPreviousLocationInfo(null);
		Session.setLocationHistory(new ArrayList<Location>());
		Session.setTotalTravelled(0d);

	}

	public void OnStopLogging() {
		Utilities.LogDebug("GpsMainActivity.OnStopLogging");
		SetMainButtonChecked(false);
	}

	/**
	 * Sets the message in the top status label.
	 * 
	 * @param message
	 *            The status message
	 */

	private void SetStatus(String message) {
		Utilities.LogDebug("GpsMainActivity.SetStatus: " + message); // TextView
		TextView tvStatus = (TextView) findViewById(R.id.textStatus); //
		tvStatus.setText(message);
		Utilities.LogInfo(message);
	}

	/**
	 * Sets the number of satellites in the satellite row in the table.
	 * 
	 * @param number
	 *            The number of satellites
	 */

	private void SetSatelliteInfo(int number) { //
		Session.setSatelliteCount(number); //
		TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites); //
		txtSatellites.setText(String.valueOf(number));
	}

	/**
	 * Given a location fix, processes it and displays it in the table on the
	 * form.
	 * 
	 * @param loc
	 *            Location information
	 */

	private void DisplayLocationInfo(Location loc) {
		Utilities.LogDebug("GpsMainActivity.DisplayLocationInfo");
		try {

			if (loc == null) {
				return;
			}

			TextView tvLatitude = (TextView) findViewById(R.id.txtLatitude);
			TextView tvLongitude = (TextView) findViewById(R.id.txtLongitude);
			TextView tvDateTime = (TextView) findViewById(R.id.txtDateTimeAndProvider);

			TextView tvAltitude = (TextView) findViewById(R.id.txtAltitude);

			TextView txtSpeed = (TextView) findViewById(R.id.txtSpeed);

			TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
			TextView txtDirection = (TextView) findViewById(R.id.txtDirection);
			TextView txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
			TextView txtTravelled = (TextView) findViewById(R.id.txtDistanceTravelled);
			String providerName = loc.getProvider();

			if (providerName.equalsIgnoreCase("gps")) {
				providerName = getString(R.string.providername_gps);
			} else {
				providerName = getString(R.string.providername_celltower);
			}

			tvDateTime.setText(new Date(Session.getLatestTimeStamp())
					.toLocaleString()
					+ getString(R.string.providername_using, providerName));
			tvLatitude.setText(String.valueOf(loc.getLatitude()));
			tvLongitude.setText(String.valueOf(loc.getLongitude()));

			if (loc.hasAltitude()) {

				double altitude = loc.getAltitude();

				if (AppSettings.shouldUseImperial()) {
					tvAltitude
							.setText(String.valueOf(Utilities
									.MetersToFeet(altitude))
									+ getString(R.string.feet));
				} else {
					tvAltitude.setText(String.valueOf(altitude)
							+ getString(R.string.meters));
				}

			} else {
				tvAltitude.setText(R.string.not_applicable);
			}

			if (loc.hasSpeed()) {

				float speed = loc.getSpeed();
				String unit;
				if (AppSettings.shouldUseImperial()) {
					if (speed > 1.47) {
						speed = speed * 0.6818f;
						unit = getString(R.string.miles_per_hour);

					} else {
						speed = Utilities.MetersToFeet(speed);
						unit = getString(R.string.feet_per_second);
					}
				} else {
					if (speed > 0.277) {
						speed = speed * 3.6f;
						unit = getString(R.string.kilometers_per_hour);
					} else {
						unit = getString(R.string.meters_per_second);
					}
				}

				txtSpeed.setText(String.valueOf(speed) + unit);

			} else {
				txtSpeed.setText(R.string.not_applicable);
			}

			if (loc.hasBearing()) {

				float bearingDegrees = loc.getBearing();
				String direction;

				direction = Utilities.GetBearingDescription(bearingDegrees,
						getApplicationContext());

				txtDirection.setText(direction + "("
						+ String.valueOf(Math.round(bearingDegrees))
						+ getString(R.string.degree_symbol) + ")");
			} else {
				txtDirection.setText(R.string.not_applicable);
			}

			if (!Session.isUsingGps()) {
				txtSatellites.setText(R.string.not_applicable);
				Session.setSatelliteCount(0);
			}

			if (loc.hasAccuracy()) {

				float accuracy = loc.getAccuracy();

				if (AppSettings.shouldUseImperial()) {
					txtAccuracy.setText(getString(R.string.accuracy_within,
							String.valueOf(Utilities.MetersToFeet(accuracy)),
							getString(R.string.feet)));

				} else {
					txtAccuracy.setText(getString(R.string.accuracy_within,
							String.valueOf(accuracy),
							getString(R.string.meters)));
				}

			} else {
				txtAccuracy.setText(R.string.not_applicable);
			}

			String distanceUnit;
			double distanceValue = Session.getTotalTravelled();
			if (AppSettings.shouldUseImperial()) {
				distanceUnit = getString(R.string.feet);
				distanceValue = Utilities.MetersToFeet(distanceValue); // When
																		// it
																		// passes
																		// more
																		// than
																		// 1
				// kilometer, convert to miles.
				if (distanceValue > 3281) {
					distanceUnit = getString(R.string.miles);
					distanceValue = distanceValue / 5280;
				}
			} else {
				distanceUnit = getString(R.string.meters);
				if (distanceValue > 1000) {
					distanceUnit = getString(R.string.kilometers);
					distanceValue = distanceValue / 1000;
				}
			}

			txtTravelled.setText(String.valueOf(Math.round(distanceValue))
					+ " " + distanceUnit + " (" + Session.getNumLegs()
					+ " points)");

		} catch (Exception ex) {
			SetStatus(getString(R.string.error_displaying, ex.getMessage()));
		}

	}

	public void OnLocationUpdate(Location loc) {
		Utilities.LogDebug("GpsMainActivity.OnLocationUpdate");

		DisplayLocationInfo(loc);
		ShowPreferencesSummary();
		SetMainButtonChecked(true);

		if (Session.isSinglePointMode()) {
			loggingService.StopLogging();
			SetMainButtonEnabled(true);
			Session.setSinglePointMode(false);
		}

	}

	public void OnSatelliteCount(int count) {
		// SetSatelliteInfo(count);

	}

	public void onFileName(String newFileName) {
		if (newFileName == null || newFileName.length() <= 0) {
			return;
		}

		TextView txtFilename = (TextView) findViewById(R.id.txtFileName);

		if (AppSettings.shouldLogToGpx() || AppSettings.shouldLogToKml()) {

			txtFilename.setText(getString(
					R.string.summary_current_filename_format,
					Session.getCurrentFileName()));
		} else {
			txtFilename.setText("");
		}

	}

	public void OnStatusMessage(String message) {
		SetStatus(message);
	}

	public void OnFatalMessage(String message) {
		Utilities.MsgBox(getString(R.string.sorry), message, this);
	}

	public Activity GetActivity() {
		return this;
	}

	// @Override
	public void OnComplete() {
		Utilities.HideProgress();
	}

	// @Override
	public void OnFailure() {
		Utilities.HideProgress();
	}
}
