package com.mendhak.gpslogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
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
import com.google.android.maps.Projection;
import com.mendhak.gpslogger.common.Utilities;

public class GoogleMapsViewActivity extends MapActivity {

	private boolean routeDisplayed = false;
	private List<OverlayItem> listOverLayItem;
	private ITrajectoryModel<LatLonPoint, DateTime> model;
	private final int MAX = 3;
	private final int[] colors = { Color.RED, Color.BLUE, Color.GREEN };
	private final PointStyle[] style = { PointStyle.SQUARE, PointStyle.CIRCLE,
			PointStyle.TRIANGLE };

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_google_maps_view);

		try {
			/*
			 * Get Data From the other Intent
			 */
			Bundle extras = getIntent().getExtras();
			String[] array = extras
					.getStringArray("arida.ufc.br.moap.TrajectoryView");

			/*
			 * Trajectory Model from Moap
			 */
			this.model = new TrajectoryModelImpl<LatLonPoint, DateTime>();
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
						GoogleMapsOverlay googleMapsOverlay = new GoogleMapsOverlay(
								color);
						List<LatLonPoint> points = traj.getPoints();

						for (int i = 0; i < points.size(); i++) {
							LatLonPoint p = points.get(i);
							GeoPoint geoPoint = new GeoPoint(
									Utilities.convertCoordinates(p
											.getLatitude()),
									Utilities.convertCoordinates(p
											.getLongitude()));
							OverlayItem oi = new OverlayItem(geoPoint, "", "");

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

	private class GoogleMapsOverlay extends Overlay {

		private List<OverlayItem> items;
		private int color;

		public GoogleMapsOverlay(int color) {
			items = new ArrayList<OverlayItem>();
			this.color = color;
		}

		public void addOverlayItem(OverlayItem item) {
			this.items.add(item);
		}

		public boolean draw(Canvas canvas, MapView mapv, boolean shadow,
				long when) {

			int size = items.size();
			Utilities.LogDebug("GoogleMapsView.Overlay - Size: " + size);

			Projection projection = mapv.getProjection();

			if (shadow == false) {

				// Line Paint
				Paint paint = new Paint();
				paint.setDither(true);
				paint.setColor(this.color);
				paint.setAlpha(120);
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				paint.setStrokeJoin(Paint.Join.ROUND);
				paint.setStrokeCap(Paint.Cap.ROUND);
				paint.setStrokeWidth(10);

				// Point Paint
				Paint whitePaint = new Paint();
				whitePaint.setDither(true);
				whitePaint.setColor(Color.WHITE);
				whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
				whitePaint.setStrokeJoin(Paint.Join.ROUND);
				whitePaint.setStrokeCap(Paint.Cap.ROUND);
				whitePaint.setStrokeWidth(10);
				
				// Point Paint
				Paint pointPaint = new Paint();
				pointPaint.setDither(true);
				pointPaint.setColor(this.color);
				pointPaint.setStyle(Paint.Style.FILL_AND_STROKE);
				pointPaint.setStrokeJoin(Paint.Join.ROUND);
				pointPaint.setStrokeCap(Paint.Cap.ROUND);
				pointPaint.setStrokeWidth(10);

				// Accuracy Paint
				Paint accuracyPaint = new Paint();
				accuracyPaint.setDither(true);
				accuracyPaint.setColor(this.color);
				accuracyPaint.setAlpha(50);
				accuracyPaint.setStyle(Paint.Style.FILL_AND_STROKE);
				accuracyPaint.setStrokeJoin(Paint.Join.ROUND);
				accuracyPaint.setStrokeCap(Paint.Cap.ROUND);
				accuracyPaint.setStrokeWidth(10);

				/*
				 * Nothing to do
				 */
				if (size == 0) {
					return true;
				}
				/*
				 * Draw one single point
				 */
				if (size == 1) {
					OverlayItem oi = this.items.get(0);
					mapv.getController().setCenter(oi.getPoint());

					Point p = new Point();
					projection.toPixels(oi.getPoint(), p);

					canvas.drawCircle(p.x, p.y, 30f, accuracyPaint);
					canvas.drawCircle(p.x, p.y, 5f, pointPaint);
					canvas.drawCircle(p.x, p.y, 2.5f, whitePaint);

				}
				/*
				 * Draw Paths
				 */
				else {

					// Draw linestrings
					Path mainPath = new Path();
					for (int i = 1; i < size; i++) {
						GeoPoint previousPoint = items.get(i - 1).getPoint();
						GeoPoint currentPoint = items.get(i).getPoint();

						Point p1 = new Point();
						Point p2 = new Point();
						Path path = new Path();

						projection.toPixels(previousPoint, p1);
						projection.toPixels(currentPoint, p2);

						path.moveTo(p2.x, p2.y);
						path.lineTo(p1.x, p1.y);

						mainPath.addPath(path);

					}
					
					canvas.drawPath(mainPath, paint);
					
					// // Draw the last position
					OverlayItem oi = this.items.get(this.items.size() - 1);

					Point p = new Point();
					projection.toPixels(oi.getPoint(), p);

					canvas.drawCircle(p.x, p.y, 30f, accuracyPaint);
					canvas.drawCircle(p.x, p.y, 5f, pointPaint);
					canvas.drawCircle(p.x, p.y, 1.5f, whitePaint);
					

					

				}

			}

			return super.draw(canvas, mapv, shadow, when);
		}
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
		
		XYMultipleSeriesDataset dataset = createDataset();
		XYMultipleSeriesRenderer renderer = createRenderer();

		if (dataset != null && renderer != null) {
			try {
				Intent intent = ChartFactory.getTimeChartIntent(this, dataset, renderer, "HH:mm");
//				Intent intent = ChartFactory.getLineChartIntent(this, dataset,renderer);
				
				startActivity(intent);
			} catch (Exception ex) {
				Utilities.LogError("ERROR CHART", ex);
			}

		} else {
			Utilities.LogDebug(String.format("Dataset - %s\nRenderer - ",
					dataset.toString(), renderer.toString()));
		}
	}

	private XYMultipleSeriesDataset createDataset() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

		for (Trajectory<LatLonPoint, DateTime> traj : this.model
				.getTrajectories()) {
			double speed = 0.0;
			XYSeries series = new XYSeries("Traj - " + traj.getId());
			TimeSeries timeSeries = new TimeSeries("Traj - " + traj.getId());
			series.add(0, speed);
			int i = traj.getPointCount();
			// Insert data into the series
			for (int j = 1; j < i; j++) {
				LatLonPoint previousPoint = traj.getPoint(j - 1);
				LatLonPoint point = traj.getPoint(j);

				DateTime time = traj.getTime(j);
				DateTime previousTime = traj.getTime(j - 1);

				double distance = Utilities.CalculateDistance(
						point.getLatitude(), point.getLongitude(),
						previousPoint.getLatitude(),
						previousPoint.getLongitude()) / 1000;
				Duration duration = new Duration(previousTime, time);
				Utilities
						.LogDebug(String.format("%s - %s", previousTime, time));
				double mili = duration.getMillis();
				double hours = mili / (1000 * 60 * 60);

				speed = distance / hours;

				Utilities.LogDebug(String.format("Speed = %s / %s = %s",
						distance, hours, speed));

				series.add(j, speed);
				timeSeries.add(new Date(time.getMillisOfDay()), speed);
			}
			dataset.addSeries(timeSeries);

		}

		return dataset;
	}

	/*
	 * Create Renderer
	 */
	private XYMultipleSeriesRenderer createRenderer() {

		int size = this.model.getTrajectoryCount();
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		
		renderer.setYTitle("Speed");
		renderer.setXTitle("Hour");
		renderer.setAxisTitleTextSize(30);
//		renderer.setChartTitleTextSize(40);
		renderer.setLabelsTextSize(30);
		renderer.setLegendTextSize(20);
		renderer.setPointSize(5f);
		renderer.setShowGridX(true);
		renderer.setShowGridY(true);
		renderer.setZoomButtonsVisible(true);
		
		renderer.setMargins(new int[] { 10, 60, 80, 0 });
		for (int i = 0; i < size; i++) {
			int idx = i % MAX;
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(colors[idx]);
			r.setPointStyle(style[idx]);
			r.setFillBelowLine(false);
			// r.setFillBelowLineColor(colors[idx]);
			
			r.setFillPoints(true);
			renderer.addSeriesRenderer(r);
		}

		renderer.setAxesColor(Color.DKGRAY);
		renderer.setLabelsColor(Color.LTGRAY);
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.WHITE);
		return renderer;
	}

}
