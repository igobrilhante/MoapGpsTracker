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

package com.mendhak.gpslogger.loggers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Session;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLoggerFactory
{
    public static List<ILogger> GetFileLoggers()
    {
        File gpxFolder = new File(Environment.getExternalStorageDirectory(), "MoapGpsTracker");
        if (!gpxFolder.exists())
        {
            gpxFolder.mkdirs();
        }

        List<ILogger> loggers = new ArrayList<ILogger>();
        final String prefix = Session.getCurrentFileName();
        if (AppSettings.shouldLogToGpx())
        {
            File gpxFile = new File(gpxFolder.getPath(), prefix  + ".gpx");
            loggers.add(new Gpx10FileLogger(Session.getUserName(),gpxFile, AppSettings.shouldUseSatelliteTime(), Session.shouldAddNewTrackSegment(), Session.getSatelliteCount()));
        }

        if (AppSettings.shouldLogToKml())
        {
            File kmlFile = new File(gpxFolder.getPath(), prefix + ".kml");
            loggers.add(new Kml22FileLogger(kmlFile, AppSettings.shouldUseSatelliteTime(), Session.shouldAddNewTrackSegment()));
        }

        if (AppSettings.shouldLogToPlainText())
        {
            File file = new File(gpxFolder.getPath(), prefix  + ".txt");
            loggers.add(new PlainTextFileLogger(Session.getUserName(),file, AppSettings.shouldUseSatelliteTime()));
        }

        if (AppSettings.shouldLogToOpenGTS())
        {
            loggers.add(new OpenGTSLogger(AppSettings.shouldUseSatelliteTime()));
        }
        
        if(AppSettings.shouldLogToServer()){
        	SharedPreferences prefs = AppSettings.getSharedPreferences();
        	SharedPreferences defaultPrefs = AppSettings.getDefaultSharedPreferences();
        	
        	String token = prefs.getString("user.gpstrackerserver.token", null);
        	String name = defaultPrefs.getString("server_login_key", null);
        	
        	if(!(token == null || name == null))
        	loggers.add(new ServerLogger(token,name));
        }
        
//        File file = new File(gpxFolder.getPath(), prefix  + ".csv");
//        loggers.add(new CsvLogger(Session.getUserName(),file, AppSettings.shouldUseSatelliteTime()));

        return loggers;
    }
}
