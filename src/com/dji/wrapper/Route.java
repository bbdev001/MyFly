package com.dji.wrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.location.Location;
import android.util.Log;

import com.my.fly.utilities.DegPoint;
import com.my.fly.utilities.Mbr;
import com.my.fly.utilities.Utilities;
import com.my.fly.utilities.WayPoint;

public class Route
{
	protected ArrayList<WayPoint> wayPoints = new ArrayList<WayPoint>();
	protected ArrayList<WayPoint> mappingWayPoints = new ArrayList<WayPoint>();
	protected double droneBaseSpeed = 3.0;
	protected double takingPhotoTime = 3.0;
	protected double estimatedFlightTime = 0.0f;//Minutes
	public String name = "";
	public float mappingAltitude = 0.0f;
	public boolean isMapping = false;
	public boolean lookAtViewPoint = true;
	public float length = 0.0f;
	public Mbr mbr = new Mbr();
	public WayPoint viewPoint = new WayPoint();
	
	public Route(String name)
	{
		this.name = name;
	}
	
	public boolean LoadFromCSV(String basePath, String name)
	{
		BufferedReader br = null;
		wayPoints.clear();
		mappingAltitude = 0.0f;
		isMapping = false;
		
		mbr.Reset();
		
		this.name = name;
		
		try
		{
			File file = new File(basePath + "/" + name + ".csv");

			br = new BufferedReader(new FileReader(file.getAbsoluteFile()));
			String line = "";

			while ((line = br.readLine()) != null)
			{
				WayPoint wayPoint = new WayPoint(line);
				mbr.Adjust(wayPoint.coord.Lon, wayPoint.coord.Lat);
				wayPoints.add(wayPoint);
			}

			viewPoint.coord.Lon = mbr.GetCenterX();
			viewPoint.coord.Lat = mbr.GetCenterY();

			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
			
		try
		{
			File file = new File(basePath + "/" + name + ".info");
			
			br = new BufferedReader(new FileReader(file.getAbsoluteFile()));
			
			String line = new String();
			line = br.readLine();
			if (line != null)
				mappingAltitude = (float)Double.parseDouble(line);
			
			line = br.readLine();
			if (line != null)
				viewPoint.coord.Lat = Double.parseDouble(line);
			
			line = br.readLine();
			if (line != null)
				viewPoint.coord.Lon = Double.parseDouble(line);
			
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		RecalculateLength();
		
		return true;
	}
	
	public boolean SaveToCSV(String basePath, String name)
	{
		BufferedWriter bw = null;
		
		this.name = name;
		
		try
		{
			File file = new File(basePath + "/" + name + ".csv");

			if (file.exists())
				file.delete();

			bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			for (WayPoint wp : wayPoints)
			{
				bw.write(wp.toString());
				bw.newLine();
			}
			
			bw.flush();
			bw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}

		try
		{
			File file = new File(basePath + "/" + name + ".info");

			if (file.exists())
				file.delete();

			bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));

			bw.write(Double.toString(mappingAltitude));
			bw.newLine();

			bw.write(Double.toString(viewPoint.coord.Lat));
			bw.newLine();
			bw.write(Double.toString(viewPoint.coord.Lon));
			bw.newLine();
			
			bw.flush();
			bw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return true;	
	}
	
	public ArrayList<WayPoint> GetWayPoints()
	{
		if (isMapping)
			return mappingWayPoints;
		
		return wayPoints;
	}
	
	public void RecalculateLength()
	{
		length = 0.0f;

		mbr.Reset();
		ArrayList<WayPoint> points = GetWayPoints();
		for (int i = 0; i < points.size(); i++)
		{
			mbr.Adjust(points.get(i).coord.Lon, points.get(i).coord.Lat);
			
			if (i > 0)
			{
				WayPoint p1 = points.get(i - 1);
				WayPoint p2 = points.get(i);
				float[] results = new float[3];
				Location.distanceBetween(p1.coord.Lat, p1.coord.Lon, p2.coord.Lat, p2.coord.Lon, results);
				length += results[0];
			}
		}
		
		estimatedFlightTime = (length / droneBaseSpeed + (double)points.size() * takingPhotoTime) / 60.0; 
	}

	protected int GetCamAngle(WayPoint p1, WayPoint p2)
	{
		float[] results = new float[3];
		Location.distanceBetween(p1.coord.Lat, p1.coord.Lon, p2.coord.Lat, p2.coord.Lon, results);
		
		double dx = results[0];
		double dy = p1.Alt - 2;//Means that view point alt is 2m
		double angle = Utilities.RadToDeg(Math.atan2(-dy, dx));
		
		return (int)angle;
	}
	
	public void SetHeadingsToViewPoint()
	{
		ArrayList<WayPoint> points = wayPoints;
		for (int i = 0; i < points.size(); i++)
		{
			WayPoint wp = points.get(i);
			wp.Heading = wp.coord.AzimutToPoint(viewPoint.coord);
			//Log.e("angle", "wp" + i + " " + wp.Heading);
			wp.CamAngle = GetCamAngle(wp, viewPoint);
		}
	}
	
	public double GetEstimatedFlightTime()
	{
		return estimatedFlightTime;
	}
}
