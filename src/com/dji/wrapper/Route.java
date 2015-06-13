package com.dji.wrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.location.Location;

import com.my.fly.utilities.WayPoint;

public class Route
{
	public ArrayList<WayPoint> wayPoints = new ArrayList<WayPoint>();
	public ArrayList<WayPoint> mappingWayPoints = new ArrayList<WayPoint>();
	public String name = "";
	public float mappingAltitude = 0.0f;
	public boolean isMapping = false;
	public float routeLength = 0.0f;
	
	public boolean LoadFromCSV(String basePath, String name)
	{
		BufferedReader br = null;
		wayPoints.clear();
		mappingAltitude = 0.0f;
		isMapping = false;
		routeLength = 0.0f;
		
		try
		{
			File file = new File(basePath + "/" + name + ".csv");

			br = new BufferedReader(new FileReader(file.getAbsoluteFile()));
			String line = "";

			while ((line = br.readLine()) != null)
			{
				WayPoint wayPoint = new WayPoint(line);
				
				if (wayPoints.size() > 1)
				{
					WayPoint prevPoint = wayPoints.get(wayPoints.size() - 1);
					float[] results = new float[3];
					Location.distanceBetween(prevPoint.coord.Lat, prevPoint.coord.Lon, wayPoint.coord.Lat, wayPoint.coord.Lon, results);
					routeLength += results[0];
				}
				
				wayPoints.add(wayPoint);				
			}

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
			mappingAltitude = (float)Double.parseDouble(br.readLine());		
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	public boolean SaveToCSV(String basePath, String name)
	{
		BufferedWriter bw = null;
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
		routeLength = 0.0f;
		
		ArrayList<WayPoint> points = GetWayPoints();
		for (int i = 0; i < points.size(); i++)
		{
			if (i > 0)
			{
				WayPoint p1 = points.get(i - 1);
				WayPoint p2 = points.get(i);
				float[] results = new float[3];
				Location.distanceBetween(p1.coord.Lat, p1.coord.Lon, p2.coord.Lat, p2.coord.Lon, results);
				routeLength += results[0];
			}
		}
	}
}
