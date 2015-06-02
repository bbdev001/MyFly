package com.dji.wrapper;

import java.util.ArrayList;

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
			gsWayPoint.heading = 179;//wp.Heading;
			gsWayPoint.speed = (float) wp.Speed;
			gsWayPoint.maxReachTime = (short) wp.MaxReachTime;
			gsWayPoint.stayTime = (short) wp.HoverTime;
			gsWayPoint.turnMode = wp.HoverTime > 0 ? 0 : 2;
			gsWayPoint.hasAction = true;

			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, 45/*this.ConvertHeading(wp.Heading)*/);
			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Yaw, 90);
			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, -45);
			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Simple_Shot, 1);
			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Stay, wp.HoverTime * 10);
            
			gsTask.addWaypoint(gsWayPoint);
		}
	}
	
	public static void BuildMappingRoute(DJIGroundStationTask gsTask, ArrayList<WayPoint> wayPoints, ArrayList<WayPoint> mappingWayPoints)
	{
		gsTask.RemoveAllWaypoint();
		mappingWayPoints.clear();
	}
}
