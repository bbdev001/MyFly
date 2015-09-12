package com.my.fly.utilities;

public class WayPoint
{
	public DegPoint coord;
	public int Alt;
	public int Heading;
	public int HoverTime;
	public int Action;
	public int CamAngle;

	public WayPoint()
	{
		LoadDefaultValues();
	}

	public WayPoint(WayPoint other)
	{
		coord = new DegPoint();
		
		coord.Lon = other.coord.Lon;
		coord.Lat = other.coord.Lat;
		
		Alt = other.Alt;
		Heading = other.Heading;
		HoverTime = other.HoverTime;
		Action = other.Action;
		CamAngle = other.CamAngle;
	}
	
	public void CopyTo(WayPoint other)
	{
		other.coord.Lon = coord.Lon;
		other.coord.Lat = coord.Lat;
		
		other.Alt = Alt;
		other.Heading = Heading;
		other.HoverTime = HoverTime;
		other.Action = Action;
		other.CamAngle = CamAngle;
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
			/*Speed =*/ Integer.parseInt(fields[6]);
		
		if (fields.length > 7)
			/*MaxReachTime =*/ Integer.parseInt(fields[7]);
		
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
		CamAngle = -45;		
	}
	
	@Override
	public String toString()
	{
		String result = coord.Lat + "," + coord.Lon + "," + Alt + "," + Heading + "," + HoverTime + "," + Action + "," + 0 + "," + 0 + "," + CamAngle;
		
		return result;
	}
}
