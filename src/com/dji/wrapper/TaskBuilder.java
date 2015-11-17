package com.dji.wrapper;

import java.util.ArrayList;

import android.location.Location;
import android.util.Log;

import com.my.fly.utilities.DegPoint;
import com.my.fly.utilities.GeoPoint;
import com.my.fly.utilities.Mbr;
import com.my.fly.utilities.MrcPoint;
import com.my.fly.utilities.Utilities;
import com.my.fly.utilities.WayPoint;

import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJIGroundStationFinishAction;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJIGroundStationMovingMode;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJIGroundStationPathMode;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationOnWayPointAction;

public class TaskBuilder
{
	protected static double baseDiagonalLength = Math.sqrt(9.0 * 9.0 + 16.0 * 16.0);////Image is 16/9. Image base diagonal
	
	protected static int GetTurnMode(float angle1, float angle2) 
	{
		float normalizedAngle = angle2 - angle1;
		if (normalizedAngle < 0)
			normalizedAngle = 360.0f + normalizedAngle;
		
		return normalizedAngle > 180.0 ? 0 : 1;// 1 - anti-clockwise turning; 0 - clockwise turning.
	}
	
	public static void BuildSequientialRoute(DJIGroundStationTask gsTask, Route route, boolean useViewPoint)
	{
		gsTask.RemoveAllWaypoint();
		route.isMapping = false;

		for (int i = 0; i < route.wayPoints.size(); i++)
		{
			WayPoint wp = route.wayPoints.get(i);
			DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(wp.coord.Lat, wp.coord.Lon);
			gsWayPoint.action.actionRepeat = 1;
			gsWayPoint.altitude = (float) wp.Alt;
			gsWayPoint.heading = (short) Utilities.ConvertHeadingToYaw(wp.Heading);
			gsWayPoint.dampingDistance = 1.5f;
			gsWayPoint.actionTimeout = 999;

			if (route.wayPoints.size() > 1)
			{
				if (i < (route.wayPoints.size() - 1))
					gsWayPoint.turnMode = GetTurnMode(route.wayPoints.get(i + 1).Heading, wp.Heading);
				else
					gsWayPoint.turnMode = GetTurnMode(wp.Heading, route.wayPoints.get(i - 1).Heading);
			}
			
			gsWayPoint.hasAction = true;

			if (useViewPoint)
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, wp.CamAngle);// 0 - -89

			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Simple_Shot, 1);

			gsTask.addWaypoint(gsWayPoint);
		}

		gsTask.movingSpeed = (route.length / route.mappingWayPoints.size()) > 30.0 ? 10.0f : 5.0f;
		gsTask.remoteControlSpeed = 10.0f; 
		gsTask.finishAction = DJIGroundStationFinishAction.None;
		gsTask.movingMode = DJIGroundStationMovingMode.GSHeadingUsingWaypointHeading;
		gsTask.pathMode = DJIGroundStationPathMode.Point_To_Point;
		gsTask.wayPointCount = gsTask.getAllWaypoint().size();
	}

	public static void BuildMyHomeRoute(DJIGroundStationTask gsTask, DegPoint lastPosition, DegPoint userPosition)
	{
		gsTask.RemoveAllWaypoint();

		float[] result = new float[3];
		Location.distanceBetween(lastPosition.Lat, lastPosition.Lon, userPosition.Lat, userPosition.Lon, result);
	}

	public static double GetViewRadius(int fov, double altitude)
	{
		if (fov == 0)
			return 0;

		double halfFov = (double) fov / 2.0;
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
			mbr.Adjust(route.wayPoints.get(i).coord.Lon, route.wayPoints.get(i).coord.Lat);
		}

		if (route.mappingAltitude == 0.0)
			route.mappingAltitude = (float) 50.0f;
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

		double stepInMetersX = 9.0 * ((viewRadius * 2.0) / baseDiagonalLength);
		double stepInMetersY = stepInMetersX - stepInMetersX / 4.0;
		double stepsX = Math.round(widthInMeters / stepInMetersX);
		double stepsY = Math.round(heightInMeters / stepInMetersY);
		double stepInDegX = Utilities.MetersToDeg(stepInMetersX);
		double stepInDegY = Utilities.MetersToDeg(stepInMetersY);
		int speed = viewRadius > 10 ? 10 : 2;

		route.mbrMapping.Reset();
		
		DegPoint leftTop = new DegPoint(mbr.Ymax, mbr.Xmin); 
		if (stepsX > stepsY)
			HorizontalMapping(gsTask, route, leftTop, stepsX, stepsY, speed, mappingAlt, -89, stepInDegX, stepInDegY);
		else
			VerticalMapping(gsTask, route, leftTop, stepsX, stepsY, speed, mappingAlt, -89, stepInDegX, stepInDegY);

		gsTask.movingSpeed = (route.length / route.mappingWayPoints.size()) > 30.0 ? 10.0f : 5.0f;
		gsTask.remoteControlSpeed = 10.0f; 
		gsTask.finishAction = DJIGroundStationFinishAction.None;
		gsTask.movingMode = DJIGroundStationMovingMode.GSHeadingUsingWaypointHeading;
		gsTask.pathMode = DJIGroundStationPathMode.Point_To_Point;
		gsTask.wayPointCount = gsTask.getAllWaypoint().size();
	}

	protected static DJIGroundStationWaypoint CreateMappingWayPoint(WayPoint wayPoint, int camAngle)
	{
		Log.i("Coord", "Wp " + wayPoint.coord.Lat + " " + wayPoint.coord.Lon);
		DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(wayPoint.coord.Lat, wayPoint.coord.Lon);
		gsWayPoint.action.actionRepeat = 1;
		gsWayPoint.altitude = wayPoint.Alt;
		gsWayPoint.heading = (short) Utilities.ConvertHeadingToYaw(wayPoint.Heading);
		gsWayPoint.dampingDistance = 1.0f;
		gsWayPoint.turnMode = 0;
		gsWayPoint.hasAction = true;
		gsWayPoint.actionTimeout = 999;
		gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, (camAngle > 0 ? -camAngle : camAngle));
		gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Simple_Shot, 1);

		return gsWayPoint;
	}

	protected static void HorizontalMapping(DJIGroundStationTask gsTask, Route route, DegPoint cur, double width, double height, int speed, double mappingAlt, int camAngle, double stepH, double stepV)
	{
		int heading = 270;

		for (int j = 0; j <= height; j++)
		{
			heading = 90;
			double f = Math.cos(Utilities.DegToRad(cur.Lat));
			for (int i = 0; i <= width; i++)
			{
				WayPoint wayPoint = new WayPoint();
				wayPoint.coord = new DegPoint(cur.Lat, cur.Lon);
				wayPoint.Alt = (int) mappingAlt;
				wayPoint.Heading = heading;
				route.mappingWayPoints.add(wayPoint);
				route.mbrMapping.Adjust(wayPoint.coord.Lon, wayPoint.coord.Lat);
				
				gsTask.addWaypoint(CreateMappingWayPoint(wayPoint, camAngle));

				cur.Lon += stepH / f;
			}

			cur.Lat -= stepV;
			stepH *= -1.0;
			cur.Lon += stepH / Math.cos(Utilities.DegToRad(cur.Lat));
		}
	}

	protected static void VerticalMapping(DJIGroundStationTask gsTask, Route route, DegPoint cur, double width, double height, int speed, double mappingAlt, int camAngle, double stepH, double stepV)
	{
		int heading = 180;

		for (int j = 0; j <= width; j++)
		{
			heading = 0;

			for (int i = 0; i <= height; i++)
			{
				WayPoint wayPoint = new WayPoint();
				wayPoint.coord = new DegPoint(cur.Lat, cur.Lon);
				wayPoint.Alt = (int) mappingAlt;
				wayPoint.Heading = heading;
				wayPoint.HoverTime = 0;
				route.mappingWayPoints.add(wayPoint);
				route.mbrMapping.Adjust(wayPoint.coord.Lon, wayPoint.coord.Lat);
				
				gsTask.addWaypoint(CreateMappingWayPoint(wayPoint, camAngle));

				cur.Lat -= stepV;
			}

			cur.Lon += stepH / Math.cos(Utilities.DegToRad(cur.Lat));
			stepV *= -1.0;
			cur.Lat -= stepV;
		}
	}
}
