package com.my.fly.utilities;

public class MrcPoint extends GeoPoint
{
	public MrcPoint(double lat, double lon)
	{
		super(lat, lon);
	}
	
	public MrcPoint()
	{
		super();
	}

	public DegPoint ToDegrees()
	{
		return new DegPoint(Utilities.y2lat(Lat), Lon);
	}
	
	public double DistanceTo(MrcPoint point)
	{
		double dx = Lon - point.Lon;
		double dy = Lat - point.Lat;
		
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public MrcPoint Clone()
	{
		return new MrcPoint(Lat, Lon);
	}
}
