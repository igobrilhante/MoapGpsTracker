package com.mendhak.gpslogger.loggers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import com.mendhak.gpslogger.common.Utilities;

import android.location.Location;

public class CsvLogger implements ILogger {

	   private File file;
	    private boolean useSatelliteTime;
	    protected final String name = "CSV";
	   private String user;

	    public CsvLogger(String user,File file, boolean useSatelliteTime)
	    {
	        this.file = file;
	        this.user = user;
	        this.useSatelliteTime = useSatelliteTime;
	    }

//	    @Override
	    public void write(Location loc) throws Exception
	    {
	        if (!file.exists())
	        {
	            file.createNewFile();

	            FileOutputStream writer = new FileOutputStream(file, true);
	            BufferedOutputStream output = new BufferedOutputStream(writer);
	            String header = "user,time,lat,lon,elevation,accuracy,bearing,speed\n";
	            output.write(header.getBytes());
	            output.flush();
	            output.close();
	        }

	        FileOutputStream writer = new FileOutputStream(file, true);
	        BufferedOutputStream output = new BufferedOutputStream(writer);

	        Date now;

	        if (useSatelliteTime)
	        {
	            now = new Date(loc.getTime());
	        }
	        else
	        {
	            now = new Date();
	        }

	        String dateTimeString = Utilities.GetIsoDateTime(now);

	        String outputString = String.format("%s,%f,%f,%f,%f,%f,%f\n", 
	        		this.user,
	        		dateTimeString,
	                loc.getLatitude(),
	                loc.getLongitude(),
	                loc.getAltitude(),
	                loc.getAccuracy(),
	                loc.getBearing(),
	                loc.getSpeed());

	        output.write(outputString.getBytes());
	        output.flush();
	        output.close();
	    }

//	    @Override
	    public void annotate(String description, Location loc) throws Exception
	    {
	        // TODO Auto-generated method stub

	    }

//	    @Override
	    public String getName()
	    {
	        return name;
	    }

		public void annotate(String name, String description, Location loc)
				throws Exception {
			// TODO Auto-generated method stub
			
		}

}
