package com.my.fly.utilities;

public class ScreenPoint
{
	public double dX;
	public double dY;
	public float X;
	public float Y;
	
	public ScreenPoint()
	{
		dX = 0.0;
		dY = 0.0;
		X = 0.0f;
		Y = 0.0f;
	}

	public ScreenPoint(double x, double y)
	{
		dX = x;
		dY = y;
		X = (float)dX;
		Y = (float)dY;
	}

	public ScreenPoint(MrcPoint point, MrcPoint mapCenter, double scale)
	{
		FromMercator(point, mapCenter, scale);
	}
	
	public void CopyTo(ScreenPoint point)
	{
		point.X = X;
		point.Y = Y;
		point.dX = dX;
		point.dY = dY;
	}
	
	public ScreenPoint Clone()
	{
		return new ScreenPoint(X, Y);
	}
	
	public void Add(ScreenPoint point)
	{
		X += point.X;
		Y += point.Y;
		dX += point.dX;
		dY += point.dY;
	}

	public void Sub(ScreenPoint point)
	{
		X -= point.X;
		Y -= point.Y;
		dX -= point.dX;
		dY -= point.dY;		
	}

	public void Add(double x, double y)
	{
		X += (float)x;
		Y += (float)y;
		dX += x;
		dY += y;		
	}

	public void Sub(double x, double y)
	{
		X -= (float)x;
		Y -= (float)y;
		dX -= x;
		dY -= y;		
	}

	public void Set(double x, double y)
	{
		X = (float)x;
		Y = (float)y;
		dX = x;
		dY = y;		
	}

	public void SetX(double x)
	{
		X = (float)x;
		dX = x;
	}

	public void SetY(double y)
	{
		Y = (float)y;
		dY = y;		
	}
	
	public void FromMercator(MrcPoint point, MrcPoint mapCenter, double scale)
	{
		dX = (point.Lon - mapCenter.Lon) / scale;
		dY = (mapCenter.Lat - point.Lat) / scale;
		X = (float)dX;
		Y = (float)dY;
	}

	public void FromMercator(MrcPoint point, MrcPoint mapCenter, ScreenPoint scrCenter, double scale)
	{
		dX = (point.Lon - mapCenter.Lon) / scale;
		dY = (mapCenter.Lat - point.Lat) / scale;
		dX += scrCenter.dX;
		dY += scrCenter.dY;
		X = (float)dX;
		Y = (float)dY;
	}
	
	public void ToMercator(MrcPoint mrcPoint, MrcPoint mapCenter, double scale)
	{
		mrcPoint.Lon = dX * scale + mapCenter.Lon;
		mrcPoint.Lat = -dY * scale + mapCenter.Lat;
	}
	
	public void ToMercator(MrcPoint mrcPoint, MrcPoint mapCenter, ScreenPoint scrCenter, double scale)
	{
		mrcPoint.Lon = (scrCenter.dX - dX) * scale + mapCenter.Lon;
		mrcPoint.Lat = (dY - scrCenter.dY) * scale + mapCenter.Lat;
	}
	
	public double DistanceTo(ScreenPoint point)
	{
		double dx = dX - point.dX;
		double dy = dY - point.dY;
		
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public void Scale (double scale)
	{
		dX *= scale;
		dY *= scale;
		X = (float)dX;
		Y = (float)dY;		
	}	
}

