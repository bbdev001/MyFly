package com.my.fly.utilities;

import geolife.android.navigationsystem.NavmiiControl.MapCoord;

public abstract class GeoPoint
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

	public void CopyTo(MapCoord point)
	{
		point.lat = Lat;
		point.lon = Lon;
	}
	

	public MapCoord ToMapCoord()
	{
		MapCoord mapCoord = new MapCoord();
		
		mapCoord.lon = Lon;
		mapCoord.lat = Lat;
		
		return mapCoord; 
	}
	
	public abstract GeoPoint Clone();

	
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
}
