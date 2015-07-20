package com.my.fly.utilities;

import android.location.Location;
import android.util.Log;

public class DegPoint extends GeoPoint
{
	public DegPoint(double lat, double lon)
	{
		super(lat, lon);
	}
	
	public DegPoint()
	{
		super();
	}

	public double DistanceTo(DegPoint point)
	{
		float[] distance = new float[3];

		Location.distanceBetween(Lat, Lon, point.Lat, point.Lon, distance);
						
		return distance[0];
	}
	
	public MrcPoint ToMercator()
	{
		return new MrcPoint(Utilities.lat2y(Lat), Lon);
	}
	
	public int AzimutToPoint(DegPoint viewPoint)
	{
		MrcPoint a = ToMercator();
		MrcPoint b = viewPoint.ToMercator();
		
		double dx = b.Lon - a.Lon;
		double dy = a.Lat - b.Lat;
		double angleYaw = Utilities.RadToDeg(Math.atan2(dy, dx));
		double angle = Utilities.ConvertYawToHeading(angleYaw) + 90.0;
		if (angle >= 360)
			angle -= 360;
		//Log.e("angle", "angle " + (int)angle + " yaw " + (int)angleYaw);
		return (int)Math.round(angle);
	}
}

