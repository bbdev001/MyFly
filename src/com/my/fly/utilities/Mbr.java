package com.my.fly.utilities;

public class Mbr
{
	public double Xmin;
	public double Xmax;
	public double Ymin;
	public double Ymax;

	public Mbr()
	{
		Reset();
	}

	public void Adjust(double x, double y)
	{
		Xmin = Math.min(x, Xmin);
		Xmax = Math.max(x, Xmax);
		Ymin = Math.min(y, Ymin);
		Ymax = Math.max(y, Ymax);
	}

	public void Reset()
	{
		Xmin = Double.MAX_VALUE;
		Xmax = Double.MIN_VALUE;
		Ymin = Double.MAX_VALUE;
		Ymax = Double.MIN_VALUE;		
	}
	
	public double GetCenterX()
	{
		return (Xmin + Xmax) / 2.0;
	}

	public double GetCenterY()
	{
		return (Ymin + Ymax) / 2.0;
	}	
}
