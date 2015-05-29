package com.my.fly.utilities;

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

	public MrcPoint ToMercator()
	{
		return new MrcPoint(Utilities.lat2y(Lat), Lon);
	}
}
