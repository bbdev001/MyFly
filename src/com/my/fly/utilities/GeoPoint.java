package com.my.fly.utilities;

public class GeoPoint
{
	public double Lat;
	public double Lon;

	public GeoPoint()
	{
		Lat = 0.0;
		Lon = 0.0;
	}

	public GeoPoint(double lat, double lon)
	{
		Lat = lat;
		Lon = lon;
	}

	public void CopyTo(GeoPoint point)
	{
		point.Lat = Lat;
		point.Lon = Lon;
	}
	
	public GeoPoint Clone()
	{
		return new GeoPoint(Lat, Lon);
	}
	
	public void Sub(GeoPoint point)
	{
		Lat -= point.Lat;
		Lon -= point.Lon;
	}

	public void Add(GeoPoint point)
	{
		Lat += point.Lat;
		Lon += point.Lon;
	}
	
	public void ToMeters()
	{
		double EarthsRadius = 6378137.0;
		Lat *= (EarthsRadius / 2.0);
		Lon *= EarthsRadius;
	}
	
	public void FromMeters()
	{
		double EarthsRadius = 6378137.0;
		Lat /= (EarthsRadius / 2.0);
		Lon /= EarthsRadius;
	}

}
