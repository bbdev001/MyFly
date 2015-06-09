package com.my.fly.utilities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Utilities
{
	protected static double y2lat(double a)
	{
		return 180.0 / Math.PI * (2.0 * Math.atan(Math.exp(a * Math.PI / 180.0)) - Math.PI / 2.0);
	}

	protected static double lat2y(double a)
	{
		double arg = Math.tan(Math.PI / 4 + a * (Math.PI / 180.0) / 2.0);
		return 180.0 / Math.PI * Math.log(Math.abs(arg) > 0.0000001 ? arg : 0.0000001);
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
	
}
