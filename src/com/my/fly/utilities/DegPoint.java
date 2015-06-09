package com.my.fly.utilities;

import android.location.Location;

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
}
