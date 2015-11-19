package com.my.fly.utilities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Utilities
{
	public static DegPoint EarthToMap(DegPoint o, DegPoint p)
	{
		DegPoint res = new DegPoint();
			
		res.Lon = Math.round(((p.Lon - o.Lon) * ((double)Math.cos(DegToRad(o.Lat)))));
		res.Lat = p.Lat - o.Lat;
			
		return res;
	}


	public static DegPoint MapToEarth(DegPoint o, DegPoint p)
	{
		DegPoint res = new DegPoint();

		double fCos = Math.cos(DegToRad(o.Lat));
		res.Lon = Math.round(p.Lon / fCos + o.Lon);
		res.Lat = p.Lat + o.Lat;
			
		return res;
	}
	
	protected static double y2lat(double a)
	{
		return RadToDeg(2.0 * Math.atan(Math.exp(DegToRad(a))) - Math.PI / 2.0);
	}

	protected static double lat2y(double a)
	{
		double arg = Math.tan(Math.PI / 4 + DegToRad(a) / 2.0);
		return RadToDeg(Math.log(Math.abs(arg) > 0.0000001 ? arg : 0.0000001));
	}
	
	public static void ToMercator(DegPoint deg, MrcPoint mrc)
	{
		mrc.Lon = deg.Lon;
		mrc.Lat = Utilities.lat2y(deg.Lat);
	}
	
	public static void ToDegrees(MrcPoint mrc, DegPoint deg)
	{
		deg.Lon = mrc.Lon;
		deg.Lat = y2lat(mrc.Lat);
	}
	
	public static double DegToRad(double deg)
	{
		return (deg * Math.PI / 180.0);
	}
	
	public static double RadToDeg(double rad)
	{
		return (180.0 / Math.PI * rad);
	}
	
	public static void Rotate(ScreenPoint point, ScreenPoint centerPoint, double angle)
	{
		double tx = 0, ty = 0, dx = 0, dy = 0;
		double tst = 0, tst1 = 0;
		double rad = Utilities.DegToRad(angle);
		double lcos = Math.cos(rad), lsin = Math.sin(rad);

		tx = point.dX - centerPoint.dX;
		ty = point.dY - centerPoint.dY;

		tst = tx * lcos;
		tst1 = ty * lsin;
		dx = tst - tst1;

		tst = ty * lcos;
		tst1 = tx * lsin;
		dy = tst + tst1;

		point.Set(dx + centerPoint.dX, dy + centerPoint.dY);
	}
	
	public static void DrawTextWithBorder(String text, float x, float y, int textColor, int borderColor, int textWidth, int borderWidth, Canvas canvas, Paint paint)
	{
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(borderWidth);
		paint.setColor(borderColor);
		canvas.drawText(text, x, y, paint);
		
		paint.setStyle(Paint.Style.FILL);
		paint.setStrokeWidth(textWidth);	
		paint.setColor(textColor);			
		canvas.drawText(text, x, y, paint);	
	}
	
	public static double DegToMeters(double value)
	{
		double k = 40000000.0 / 360.0;	
		return value * k;
	}
	
	public static double MetersToDeg(double value)
	{
		double k = 360.0 / 40000000.0;	
		return value * k;
	}
	
	public static double ConvertYawToHeading(double yaw)
	{
		double heading = yaw;

		if (heading < 0)
			heading = 360 + heading;

		return heading;
	}
	
	public static double ConvertHeadingToYaw(double heading)
	{
		double yaw = 0.0;

		if (heading > 180)
			yaw = heading - 360;
		else
			yaw = heading;

		return yaw;
	}
	
	public static long CombineLong(int a, int b)
	{
		long r = (((long)a) << 32) | b;
		
		return r;
	}
	
	public static int ConvertCoordToInt(double v)
	{
		return (int) (v * 100000.0);
	}
}
