package com.mendhak.gpslogger.loggers;

import java.util.List;

import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.text.style.BulletSpan;

public class LoggerService extends Service {

	public static final int WRITE = 0;
	public static final int ANNOTATION = 1;
	private IBinder binder = new LoggerBinder();
	private List<ILogger> loggers;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return this.binder;
	}

	/**
	 * Can be used from calling classes as the go-between for methods and
	 * properties.
	 */
	public class LoggerBinder extends Binder {
		public LoggerBinder getService() {
			Utilities.LogDebug("LoggerBinder.getService");
			return LoggerBinder.this;
		}
	}

	@Override
	public void onCreate() {
		Utilities.LogDebug("LoggerService.onCreate");

		this.loggers = FileLoggerFactory.GetFileLoggers();

		Utilities.LogInfo("LoggerService created");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Utilities.LogDebug("LoggerService.onStart");
		handleIntent(intent);

		stopSelf();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Utilities.LogDebug("LoggerService.onStartCommand");
		handleIntent(intent);

		stopSelf();

		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		Utilities.LogWarning("LoggerService is being destroyed by Android OS.");
		this.loggers = null;
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		Utilities.LogWarning("Android is low on memory.");
		super.onLowMemory();
	}

	private void handleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		Utilities.LogDebug("LoggerService HandleIntent - "+extras.toString());
		if (extras != null) {
			if (extras.containsKey("option") && extras.containsKey("loc")) {
				int option = extras.getInt("option");
				Utilities.LogDebug("LoggerService - Option " + option);
				switch (option) {
				case WRITE:
					write(extras);
					break;
				case ANNOTATION:
					annotation(extras);
					break;
				default:
					break;
				}

			}
		}
	}

	private void annotation(Bundle extras) {
		Location loc = (Location) extras.get("loc");
		String annotation_key = extras.getString("annotation_key");
		String annotation_value = extras.getString("annotation_value");
		Utilities.LogDebug("LoggerService - Annotation " + annotation_key);
		// Add annotation key
		Session.addAchievedAnnodation(annotation_key);

		for (ILogger logger : this.loggers) {
			try {
				logger.annotate(annotation_key, annotation_value, loc);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void write(Bundle extras) {

		Location loc = (Location) extras.get("loc");

		for (ILogger logger : this.loggers) {
			try {
				logger.write(loc);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
