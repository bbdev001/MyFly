package com.dji.wrapper;

import java.util.ArrayList;

import com.my.fly.utilities.DegPoint;
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
			DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(wp.coord.Lat, wp.coord.Lon, 3, 1);
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
	
	public static void BuildMappingRoute(DJIGroundStationTask gsTask, ArrayList<WayPoint> wayPoints, ArrayList<WayPoint> mappingWayPoints)
	{
		gsTask.RemoveAllWaypoint();
		mappingWayPoints.clear();
	}
}
