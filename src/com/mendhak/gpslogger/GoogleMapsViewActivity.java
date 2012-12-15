package com.mendhak.gpslogger;

import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.joda.time.DateTime;
import org.moap.chart.SpeedOverTimeChart;
import org.moap.overlays.CustomOverlay;
import org.moap.overlays.GoogleMapsOverlay;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import arida.ufc.br.moap.core.beans.LatLonPoint;
import arida.ufc.br.moap.core.beans.Trajectory;
import arida.ufc.br.moap.core.imp.Parameters;
import arida.ufc.br.moap.datamodelapi.imp.TrajectoryModelImpl;
import arida.ufc.br.moap.datamodelapi.spi.ITrajectoryModel;
import arida.ufc.br.moap.importer.csv.imp.RawTrajectoryCSVImporter;
import arida.ufc.br.moap.importer.spi.ITrajectoryImporter;
import arida.ufc.br.moapgpstracker.R;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.mendhak.gpslogger.common.Utilities;

public class GoogleMapsViewActivity extends MapActivity {

	private boolean routeDisplayed = false;
	private ITrajectoryModel<LatLonPoint, DateTime> model;
	private final int MAX = 3;
	private final int[] colors = { Color.RED, Color.BLUE, Color.GREEN };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_google_maps_view);

		try {
			
			importData();
			
			Utilities.LogDebug("GoogleMapsView.onCreate - Importing has succeeded");

			MapView mapView = (MapView) findViewById(R.id.map_view);
			mapView.setBuiltInZoomControls(true);
			mapView.displayZoomControls(true);
			mapView.setClickable(true);

			List<Overlay> overlayList = mapView.getOverlays();
			overlayList.clear();

			
			if (model.getTrajectoryCount() > 0) {
				Utilities
						.LogDebug("GoogleMapsView.onCreate - It has trajectories");

				int idx = 0;
				for (Trajectory<LatLonPoint, DateTime> traj : model
						.getTrajectories()) {

					if (idx < MAX) {
						int color = colors[idx % MAX];
//						ItemizedOverlay<OverlayItem> itemized_overlay = new ItemizedOverlay<OverlayItem>();
						GoogleMapsOverlay googleMapsOverlay = new GoogleMapsOverlay(
								color);
						
						Drawable default_icon = getResources().getDrawable(R.drawable.ic_checkin_marker);
						default_icon.setBounds(0, 0, default_icon.getIntrinsicWidth(), default_icon.getIntrinsicHeight());
						
						Drawable comment_icon = getResources().getDrawable(R.drawable.ic_comment);
						comment_icon.setBounds(0, 0, comment_icon.getIntrinsicWidth(), comment_icon.getIntrinsicHeight());
						
						CustomOverlay custom_overlay = new CustomOverlay(default_icon);
						
						List<LatLonPoint> points = traj.getPoints();

						for (int i = 0; i < points.size(); i++) {
							LatLonPoint p = points.get(i);
							GeoPoint geoPoint = new GeoPoint(
									Utilities.convertCoordinates(p
											.getLatitude()),
									Utilities.convertCoordinates(p
											.getLongitude()));
							
							String checkin_annotation = (String) p.getAnnotations().getAnnotation("checkin").getValue();
							String comment_annotation = (String) p.getAnnotations().getAnnotation("comment").getValue();
							
							OverlayItem oi = new OverlayItem(geoPoint, "", "");
							
							if(checkin_annotation != null){
								Log.d("GoogleMapsViewAcitivity", "Annotation - Check-in");
								
								OverlayItem itemized_oi = new OverlayItem(geoPoint, checkin_annotation, "Check-in");
								custom_overlay.addOverlayItem(itemized_oi);
							}
							else if(comment_annotation != null ){
								Log.d("GoogleMapsViewAcitivity", "Annotation - Comment");
								
								OverlayItem itemized_oi = new OverlayItem(geoPoint, comment_annotation, "Comment");
								oi.setMarker(comment_icon);
								custom_overlay.addOverlayItem(itemized_oi);
							}
							
							googleMapsOverlay.addOverlayItem(oi);

						}
						/*
						 * Get last point for centering the visualization
						 */
						if (points.size() > 0) {
							GeoPoint geoPoint = new GeoPoint(
									Utilities.convertCoordinates(points.get(
											points.size() - 1).getLatitude()),
									Utilities.convertCoordinates(points.get(
											points.size() - 1).getLongitude()));
							mapView.getController().setCenter(geoPoint);

						}
						
						// Add overlay
						overlayList.add(googleMapsOverlay);
						
						// Add itemized overlay
//						if(custom_overlay.size() > 0){
//							overlayList.add(custom_overlay);
//						}

					} else {
						break;
					}
					idx++;

				}
				Utilities.LogDebug("GoogleMapsView.onCreate - Adding it to Overlay");

				mapView.invalidate();
				
				
				
				

			}
		} catch (Exception e) {
			Utilities.LogError("GoogleMapsView.onCreate", e);
		}
		
		this.model = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_google_maps_view, menu);

		return true;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return this.routeDisplayed;
	}

	

	/**
	 * Called when one of the menu items is selected.
	 */

	public boolean onOptionsItemSelected(MenuItem item) {

		int itemId = item.getItemId();
		Utilities.LogInfo("Option item selected - "
				+ String.valueOf(item.getTitle()));

		switch (itemId) {
		case R.id.mnu_chart:
			onChartMenu();
			break;
		}

		return true;
	}

	private void onChartMenu() {
		
		importData();
		
		SpeedOverTimeChart analysis = new SpeedOverTimeChart(model);
		
		final XYMultipleSeriesDataset dataset = analysis.createDataset();
		final XYMultipleSeriesRenderer renderer = analysis.createRenderer();

		if (dataset != null && renderer != null) {
			try {
				Log.d("GoogleMapsViewActivity", "Creating chart intent");
				Intent intent = ChartFactory.getTimeChartIntent(this, dataset,
						renderer, "HH:mm");

				startActivity(intent);
			} catch (Exception ex) {
				Log.e("GoogleMapsViewActivity","ERROR CHART", ex);
			}

		} else {
			Utilities.LogDebug(String.format("Dataset - %s\nRenderer - ",
					dataset.toString(), renderer.toString()));
			Log.e("GoogleMapsViewActivity","ERROR CHART: "+String.format("Dataset - %s\nRenderer - ",
					dataset.toString(), renderer.toString()));
		}
	}

	
	
	@SuppressWarnings("unchecked")
	private void importData(){
		
		/*
		 * Get Data From the other Intent
		 */
		Bundle extras = getIntent().getExtras();
		String[] array = extras
				.getStringArray("arida.ufc.br.moap.TrajectoryView");
		
		/*
		 * Trajectory Model from Moap
		 */
		model = new TrajectoryModelImpl<LatLonPoint, DateTime>();
		/*
		 * Importer from CSV
		 */
		ITrajectoryImporter importer = new RawTrajectoryCSVImporter();
		/*
		 * Creating parameters
		 */
		for (String s : array) {
			Parameters params = new Parameters();
			params.addParam(RawTrajectoryCSVImporter.PARAMETER_FILE,
					Environment.getExternalStorageDirectory()
							.getAbsolutePath()
							+ "/"
							+ "MoapGpsTracker/"
							+ s);

			importer.buildImport(model, params);
		}
	}

}
