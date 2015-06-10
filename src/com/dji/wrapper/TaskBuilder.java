package com.dji.wrapper;

import java.util.ArrayList;

import android.location.Location;

import com.my.fly.utilities.DegPoint;
import com.my.fly.utilities.GeoPoint;
import com.my.fly.utilities.Mbr;
import com.my.fly.utilities.MrcPoint;
import com.my.fly.utilities.Utilities;
import com.my.fly.utilities.WayPoint;

import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationOnWayPointAction;

public class TaskBuilder
{
	public static void BuildSequientialRoute(DJIGroundStationTask gsTask, Route route)
	{
		gsTask.RemoveAllWaypoint();
		route.isMapping = false;
		
		for (int i = 0; i < route.wayPoints.size(); i++)
		{
			WayPoint wp = route.wayPoints.get(i);
			DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(wp.coord.Lat, wp.coord.Lon, 4, 1);
			gsWayPoint.altitude = (float) wp.Alt;
			gsWayPoint.heading = 0;
			gsWayPoint.speed = (float) wp.Speed;
			gsWayPoint.maxReachTime = (short) wp.MaxReachTime;
			gsWayPoint.stayTime = (short) (wp.HoverTime * 10);
			gsWayPoint.turnMode = wp.HoverTime > 0 ? 0 : 2;
			gsWayPoint.hasAction = true;

			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, (int)DJIWrapper.ConvertHeadingToYaw(wp.Heading));
			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, 45);
			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Simple_Shot, 1);
			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Stay, wp.HoverTime * 10);
            
			gsTask.addWaypoint(gsWayPoint);
		}
	}
	
	public static void BuildMyHomeRoute(DJIGroundStationTask gsTask, DegPoint lastPosition, DegPoint userPosition)
	{
		gsTask.RemoveAllWaypoint();
		
		//Move top
		DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(lastPosition.Lat, lastPosition.Lon);
		gsWayPoint.altitude = 45.0f;
		gsWayPoint.heading = 0.0f;
		gsWayPoint.speed = 0.0f;
		gsWayPoint.maxReachTime = 0;
		gsWayPoint.stayTime = 1;
		gsWayPoint.turnMode = 0;
		gsWayPoint.hasAction = false;
		gsTask.addWaypoint(gsWayPoint);

		//Move to home
		gsWayPoint = new DJIGroundStationWaypoint(userPosition.Lat, userPosition.Lon);
		gsWayPoint.altitude = 45.0f;
		gsWayPoint.heading = 0.0f;
		gsWayPoint.speed = 10.0f;
		gsWayPoint.maxReachTime = 0;
		gsWayPoint.stayTime = 1;
		gsWayPoint.turnMode = 0;
		gsWayPoint.hasAction = false;
		gsTask.addWaypoint(gsWayPoint);

		//Move down
		gsWayPoint = new DJIGroundStationWaypoint(userPosition.Lat, userPosition.Lon);
		gsWayPoint.altitude = 3.0f;
		gsWayPoint.heading = 0.0f;
		gsWayPoint.speed = 1.0f;
		gsWayPoint.maxReachTime = 0;
		gsWayPoint.stayTime = 1;
		gsWayPoint.turnMode = 0;
		gsWayPoint.hasAction = false;
		gsTask.addWaypoint(gsWayPoint);
	}
	
	public static double GetViewRadius(int fov, double altitude)
	{
		if (fov == 0)
			return 0;
		
		double halfFov = (double)fov / 2.0;
		double y = 180.0 - (halfFov + 90.0);
		return altitude * (Math.sin(Utilities.DegToRad(halfFov)) / Math.sin(Utilities.DegToRad(y)));
	}
	
	public static void BuildMappingRoute(DJIGroundStationTask gsTask, Route route)
	{
		gsTask.RemoveAllWaypoint();
		route.mappingWayPoints.clear();
		route.isMapping = true;

		Mbr mbr = new Mbr();
		double mappingAlt = 0.0;
		for (int i = 0; i < route.wayPoints.size(); i++)
		{
			mbr.Adjust(route.wayPoints.get(i).coord.Lon , route.wayPoints.get(i).coord.Lat);
			
			if (route.wayPoints.get(i).Alt > mappingAlt)
				mappingAlt = route.wayPoints.get(i).Alt;
		}
		
		if (route.mappingAltitude == 0.0)		
			route.mappingAltitude = (float)mappingAlt;
		else
			mappingAlt = route.mappingAltitude;

		if (mappingAlt == 0.0)
			return;
			
		double viewRadius = GetViewRadius(DJIWrapper.CAMERA_FOV, mappingAlt);
		float[] distance = new float[3];
		Location.distanceBetween(mbr.Ymin, mbr.Xmin, mbr.Ymin, mbr.Xmax, distance);
		double widthInMeters = distance[0]; 
		Location.distanceBetween(mbr.Ymin, mbr.Xmin, mbr.Ymax, mbr.Xmin, distance);
		double heightInMeters = distance[0];

		double stepH = viewRadius * 2.0;
		double stepV = viewRadius * 2.0;				
		double width = Math.ceil(widthInMeters / stepV);
		double height = Math.ceil(heightInMeters / stepH);
		int heading = 270;
		int speed = viewRadius > 10 ? 10 : 2;
		
		MrcPoint cur = new DegPoint(mbr.GetCenterY(), mbr.GetCenterX()).ToMercator();
		
		if (width > 1 || height > 1)
		{
			cur.Lon -= Utilities.MetersToDeg(widthInMeters / 2.0);
			cur.Lat += Utilities.MetersToDeg(heightInMeters / 2.0);
		}
		
		for (int j = 0; j < height; j++)
		{
			if ((j % 2) == 0)
				heading = 90;
			else
				heading = 270;
						
			for (int i = 0; i < width; i++)
			{
				MrcPoint coord = new MrcPoint(cur.Lat, cur.Lon);

				WayPoint wayPoint = new WayPoint();
				wayPoint.coord = coord.ToDegrees();
				wayPoint.Alt = (int)mappingAlt;
				wayPoint.Speed = speed;
				wayPoint.Heading = heading;
				wayPoint.HoverTime = 1;
				route.mappingWayPoints.add(wayPoint);
				
				DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(wayPoint.coord.Lat, wayPoint.coord.Lon, 4, 1);
				gsWayPoint.altitude = wayPoint.Alt;
				gsWayPoint.heading = 0;
				gsWayPoint.speed = wayPoint.Speed;
				gsWayPoint.maxReachTime = 0;
				gsWayPoint.stayTime = (short)(wayPoint.HoverTime * 10);
				gsWayPoint.turnMode = 0;
				gsWayPoint.hasAction = true;
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, (int)DJIWrapper.ConvertHeadingToYaw(wayPoint.Heading));
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, -90);
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Simple_Shot, 1);
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Stay, wayPoint.HoverTime * 10);		
				gsTask.addWaypoint(gsWayPoint);
				
				cur.Lon += Utilities.MetersToDeg(stepH);
			}

			cur.Lat -= Utilities.MetersToDeg(stepV);			
			stepH *= -1.0;
			cur.Lon += Utilities.MetersToDeg(stepH);
		}			
	}
}
