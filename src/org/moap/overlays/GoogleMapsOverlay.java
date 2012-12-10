package org.moap.overlays;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class GoogleMapsOverlay extends Overlay {

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
