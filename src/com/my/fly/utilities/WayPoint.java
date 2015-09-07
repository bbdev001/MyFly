package com.my.fly.utilities;

public class WayPoint
{
	public DegPoint coord;
	public int Alt;
	public int Heading;
	public int HoverTime;
	public int Action;
	public int Speed;
	public int CamAngle;
	public int MaxReachTime;
	
	public WayPoint()
	{
		LoadDefaultValues();
	}

	public WayPoint(DegPoint point)
	{
		LoadDefaultValues();
		
		coord.Lon = point.Lon;
		coord.Lat = point.Lat;
	}

	public WayPoint(String value)
	{
		LoadDefaultValues();
		
		String[] fields = value.split(",");		
		coord = new DegPoint(Double.parseDouble(fields[0]), Double.parseDouble(fields[1]));
		Alt = Integer.parseInt(fields[2]);
		Heading = Integer.parseInt(fields[3]);
		HoverTime = Integer.parseInt(fields[4]);
		Action = Integer.parseInt(fields[5]);
		
		if (fields.length > 6)
			Speed = Integer.parseInt(fields[6]);
		
		if (fields.length > 7)
			MaxReachTime = Integer.parseInt(fields[7]);
		
		if (fields.length > 8)
			CamAngle = Integer.parseInt(fields[8]);
	}

	public void LoadDefaultValues()
	{
		coord = new DegPoint();
		Alt = 7;
		Heading = 0;
		HoverTime = 0;
		Action = 0;
		Speed = 10;
		MaxReachTime = 0;
		CamAngle = 45;		
	}
	
	@Override
	public String toString()
	{
		String result = coord.Lat + "," + coord.Lon + "," + Alt + "," + Heading + "," + HoverTime + "," + Action + "," + Speed + "," + MaxReachTime + "," + CamAngle;
		
		return result;
	}
}
