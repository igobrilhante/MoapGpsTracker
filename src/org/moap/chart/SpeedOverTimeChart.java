package org.moap.chart;

import java.util.Date;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import android.graphics.Color;
import android.util.Log;
import arida.ufc.br.moap.core.beans.LatLonPoint;
import arida.ufc.br.moap.core.beans.Trajectory;
import arida.ufc.br.moap.datamodelapi.spi.ITrajectoryModel;

import com.mendhak.gpslogger.common.Utilities;

public class SpeedOverTimeChart {
	
	private final String TAG = "SpeedOverTimeChart";
	private ITrajectoryModel<LatLonPoint, DateTime> model;
	private final int MAX = 3;
	private final int[] colors = { Color.RED, Color.BLUE, Color.GREEN };
	private final PointStyle[] style = { PointStyle.SQUARE, PointStyle.CIRCLE,PointStyle.TRIANGLE };

	public SpeedOverTimeChart(ITrajectoryModel<LatLonPoint, DateTime> trajectory_model){
		this.model = trajectory_model;
	}
	
	public XYMultipleSeriesDataset createDataset() {
		Log.d(TAG, "Creating dataset - computing speed");
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		
		if(this.model!=null){
			Log.d(TAG, "Number of trajectories "+this.model.getTrajectoryCount());
			
			for (Trajectory<LatLonPoint, DateTime> traj : this.model.getTrajectories()) {
				
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

					// distance in kilometers
					double distance = Utilities.CalculateDistance(
							point.getLatitude(), point.getLongitude(),
							previousPoint.getLatitude(),
							previousPoint.getLongitude()) / 1000;
					
					Duration duration = new Duration(previousTime, time);
					Utilities
							.LogDebug(String.format("%s - %s", previousTime, time));
					double mili = duration.getMillis();
					double hours = mili / (1000 * 60 * 60); // convert mili to hours

					speed = distance / hours;

					series.add(j, speed);
					timeSeries.add(new Date(time.getMillisOfDay()), speed);
				}
				dataset.addSeries(timeSeries);

			}
		}
		
		return dataset;
	}

	/*
	 * Create Renderer
	 */
	public XYMultipleSeriesRenderer createRenderer() {

		int size = this.model.getTrajectoryCount();
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		
		renderer.setYTitle("Speed");
		renderer.setXTitle("Hour");
		renderer.setAxisTitleTextSize(30);
//		renderer.setChartTitleTextSize(40);
		renderer.setLabelsTextSize(30);
		
		renderer.setLegendTextSize(20);
		
		renderer.setPointSize(6f);
		
		renderer.setFitLegend(true);
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
			r.setFillBelowLineColor(colors[idx]);
			
			r.setLineWidth(2.5f);
			
			r.setFillPoints(true);
			renderer.addSeriesRenderer(r);
		}

		renderer.setAxesColor(Color.DKGRAY);
		renderer.setLabelsColor(Color.LTGRAY);
//		renderer.setApplyBackgroundColor(true);
//		renderer.setBackgroundColor(Color.WHITE);
		return renderer;
	}
}
