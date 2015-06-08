package com.dji.wrapper;

import java.util.ArrayList;

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
	public static void BuildSequientialRoute(DJIGroundStationTask gsTask, ArrayList<WayPoint> wayPoints)
	{
		gsTask.RemoveAllWaypoint();
		for (int i = 0; i < wayPoints.size(); i++)
		{
			WayPoint wp = wayPoints.get(i);
			DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(wp.coord.Lat, wp.coord.Lon, 5, 1);
			gsWayPoint.altitude = (float) wp.Alt;
			gsWayPoint.heading = 0;//wp.Heading;
			gsWayPoint.speed = (float) wp.Speed;
			gsWayPoint.maxReachTime = (short) wp.MaxReachTime;
			gsWayPoint.stayTime = (short) wp.HoverTime;
			gsWayPoint.turnMode = wp.HoverTime > 0 ? 0 : 2;
			gsWayPoint.hasAction = true;

			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, wp.Heading);
			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Yaw, 90);
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
		
		//Shot shift
		gsWayPoint = new DJIGroundStationWaypoint(userPosition.Lat + 0.00002, userPosition.Lon + 0.00002);
		gsWayPoint.altitude = 2.5f;
		gsWayPoint.heading = 0.0f;
		gsWayPoint.speed = 1.0f;
		gsWayPoint.maxReachTime = 0;
		gsWayPoint.stayTime = 10;
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
	
	public static void BuildMappingRoute(DJIGroundStationTask gsTask, ArrayList<WayPoint> mappingWayPoints, ArrayList<WayPoint> wayPoints)
	{
		gsTask.RemoveAllWaypoint();
		mappingWayPoints.clear();

		Mbr mbr = new Mbr();
		double mappingAlt = 0.0;
		for (int i = 0; i < wayPoints.size(); i++)
		{
			MrcPoint mrcPoint = wayPoints.get(i).coord.ToMercator();
			mbr.Adjust(mrcPoint.Lon, mrcPoint.Lat);
			
			if (wayPoints.get(i).Alt > mappingAlt)
				mappingAlt = wayPoints.get(i).Alt;
		}
		
		mappingAlt *= 2.0;
		if (mappingAlt == 0.0)
			return;

		double viewRadius = GetViewRadius(DJIWrapper.CAMERA_FOV, mappingAlt);
		MrcPoint leftTop = new MrcPoint(mbr.Ymax, mbr.Xmin);
		MrcPoint rightBottom = new MrcPoint(mbr.Ymin, mbr.Xmax);
		
		leftTop.ToMeters();
		rightBottom.ToMeters();

		double width = Math.ceil((rightBottom.Lon - leftTop.Lon) / (viewRadius * 2.0));
		double height = Math.ceil((leftTop.Lat - rightBottom.Lat) / (viewRadius * 2.0));
		double stepH = viewRadius * 2.0;
		double stepV = viewRadius * 2.0;
		int heading = 270;
		int speed = viewRadius > 10 ? 10 : 2;
		
		MrcPoint cur = new MrcPoint(leftTop.Lat, leftTop.Lon);
		for (int j = 0; j < height; j++)
		{
			for (int i = 0; i < width; i++)
			{
				MrcPoint coord = new MrcPoint(cur.Lat, cur.Lon);
				coord.FromMeters();

				WayPoint wayPoint = new WayPoint();
				wayPoint.coord = coord.ToDegrees();
				wayPoint.Alt = (int)mappingAlt;
				wayPoint.Speed = speed;
				wayPoint.Heading = heading;
				wayPoint.HoverTime = 1;
				mappingWayPoints.add(wayPoint);
				
				DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(wayPoint.coord.Lat, wayPoint.coord.Lon, 3, 1);
				gsWayPoint.altitude = wayPoint.Alt;
				gsWayPoint.heading = wayPoint.Heading;
				gsWayPoint.speed = wayPoint.Speed;
				gsWayPoint.maxReachTime = 1;
				gsWayPoint.stayTime = (short)wayPoint.HoverTime;
				gsWayPoint.turnMode = 0;
				gsWayPoint.hasAction = true;
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, wayPoint.Heading);
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, 90);
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Simple_Shot, 1);		
				gsTask.addWaypoint(gsWayPoint);
				
				cur.Lon += stepH;
			}
			
			if ((j % 2) == 0)
				heading = 270;
			else
				heading = 180;

			cur.Lat += stepV;			
			stepH *= -1.0;
		}			
	}
}
