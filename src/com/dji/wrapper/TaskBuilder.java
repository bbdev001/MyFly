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
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJIGroundStationFinishAction;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJIGroundStationMovingMode;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJIGroundStationPathMode;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationOnWayPointAction;

public class TaskBuilder
{
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
			gsWayPoint.heading = useViewPoint ? (short) Utilities.ConvertHeadingToYaw(wp.Heading) : 0;
			gsWayPoint.speed = (float) wp.Speed;
			gsWayPoint.dampingDistance = 1.0f;
			gsWayPoint.actionTimeout = 5;

			if (i < (route.wayPoints.size() - 1))
				gsWayPoint.turnMode = GetTurnMode(route.wayPoints.get(i + 1).Heading, wp.Heading);
			else
				gsWayPoint.turnMode = GetTurnMode(wp.Heading, route.wayPoints.get(i - 1).Heading);

			gsWayPoint.hasAction = true;

			if (useViewPoint)
				gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, wp.CamAngle);// 0 - -89

			gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Simple_Shot, 1);

			gsTask.addWaypoint(gsWayPoint);
		}

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

		float speed = 5.0f;
		if (result[0] > 15.0f)
			speed = 10.0f;

		// Move top
		DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(lastPosition.Lat + 10.0, lastPosition.Lon + 10.0);
		gsWayPoint.action.actionRepeat = 0;
		gsWayPoint.altitude = 45.0f;
		gsWayPoint.heading = 0;
		gsWayPoint.speed = speed;
		gsWayPoint.dampingDistance = 1.0f;
		gsWayPoint.actionTimeout = 0;
		gsWayPoint.turnMode = 1;
		gsWayPoint.hasAction = false;
		gsTask.addWaypoint(gsWayPoint);

		// Move to home
		gsWayPoint = new DJIGroundStationWaypoint(userPosition.Lat - 10.0, userPosition.Lon - 10.0);
		gsWayPoint.action.actionRepeat = 0;
		gsWayPoint.altitude = 45.0f;
		gsWayPoint.heading = 0;
		gsWayPoint.speed = speed;
		gsWayPoint.dampingDistance = 1.0f;
		gsWayPoint.actionTimeout = 0;
		gsWayPoint.turnMode = 1;
		gsWayPoint.hasAction = false;
		gsTask.addWaypoint(gsWayPoint);

		// Move down
		gsWayPoint = new DJIGroundStationWaypoint(userPosition.Lat, userPosition.Lon);
		gsWayPoint.action.actionRepeat = 0;
		gsWayPoint.altitude = 2.5f;
		gsWayPoint.heading = 0;
		gsWayPoint.speed = 2.0f;
		gsWayPoint.dampingDistance = 1.0f;
		gsWayPoint.actionTimeout = 0;
		gsWayPoint.turnMode = 1;
		gsWayPoint.hasAction = false;
		gsTask.addWaypoint(gsWayPoint);

		gsTask.finishAction = DJIGroundStationFinishAction.None;
		gsTask.movingMode = DJIGroundStationMovingMode.GSHeadingTowardNextWaypoint;
		gsTask.pathMode = DJIGroundStationPathMode.Point_To_Point;
		gsTask.wayPointCount = gsTask.getAllWaypoint().size();
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

			if (route.wayPoints.get(i).Alt > mappingAlt)
				mappingAlt = route.wayPoints.get(i).Alt;
		}

		if (route.mappingAltitude == 0.0)
			route.mappingAltitude = (float) mappingAlt;
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
		int speed = viewRadius > 10 ? 10 : 2;

		MrcPoint cur = new DegPoint(mbr.GetCenterY(), mbr.GetCenterX()).ToMercator();

		if (width > 1 || height > 1)
		{
			cur.Lon -= Utilities.MetersToDeg(widthInMeters / 2.0);
			cur.Lat += Utilities.MetersToDeg(heightInMeters / 2.0);
		}

		if (width > height)
			HorizontalMapping(gsTask, route, cur, width, height, speed, mappingAlt, -89, stepH, stepV);
		else
			VerticalMapping(gsTask, route, cur, width, height, speed, mappingAlt, -89, stepH, stepV);

		gsTask.finishAction = DJIGroundStationFinishAction.None;
		gsTask.movingMode = DJIGroundStationMovingMode.GSHeadingUsingWaypointHeading;
		gsTask.pathMode = DJIGroundStationPathMode.Point_To_Point;
		gsTask.wayPointCount = gsTask.getAllWaypoint().size();
	}

	protected static DJIGroundStationWaypoint CreateMappingWayPoint(WayPoint wayPoint, int camAngle)
	{
		DJIGroundStationWaypoint gsWayPoint = new DJIGroundStationWaypoint(wayPoint.coord.Lat, wayPoint.coord.Lon);
		gsWayPoint.action.actionRepeat = 1;
		gsWayPoint.altitude = wayPoint.Alt;
		gsWayPoint.heading = (short) Utilities.ConvertHeadingToYaw(wayPoint.Heading);
		gsWayPoint.speed = wayPoint.Speed;
		gsWayPoint.dampingDistance = 1.0f;
		gsWayPoint.actionTimeout = 5;
		gsWayPoint.turnMode = 0;
		gsWayPoint.hasAction = true;
		gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, (camAngle > 0 ? -camAngle : camAngle));
		gsWayPoint.addAction(GroundStationOnWayPointAction.Way_Point_Action_Simple_Shot, 1);

		return gsWayPoint;
	}

	protected static void HorizontalMapping(DJIGroundStationTask gsTask, Route route, MrcPoint cur, double width, double height, int speed, double mappingAlt, int camAngle, double stepH, double stepV)
	{
		int heading = 270;

		for (int j = 0; j < height; j++)
		{
			heading = 90;

			for (int i = 0; i < width; i++)
			{
				MrcPoint coord = new MrcPoint(cur.Lat, cur.Lon);

				WayPoint wayPoint = new WayPoint();
				wayPoint.coord = coord.ToDegrees();
				wayPoint.Alt = (int) mappingAlt;
				wayPoint.Speed = speed;
				wayPoint.Heading = heading;
				wayPoint.HoverTime = 1;
				route.mappingWayPoints.add(wayPoint);

				gsTask.addWaypoint(CreateMappingWayPoint(wayPoint, camAngle));

				cur.Lon += Utilities.MetersToDeg(stepH);
			}

			cur.Lat -= Utilities.MetersToDeg(stepV);
			stepH *= -1.0;
			cur.Lon += Utilities.MetersToDeg(stepH);
		}
	}

	protected static void VerticalMapping(DJIGroundStationTask gsTask, Route route, MrcPoint cur, double width, double height, int speed, double mappingAlt, int camAngle, double stepH, double stepV)
	{
		int heading = 180;

		for (int j = 0; j < width; j++)
		{
			heading = 0;

			for (int i = 0; i < height; i++)
			{
				MrcPoint coord = new MrcPoint(cur.Lat, cur.Lon);

				WayPoint wayPoint = new WayPoint();
				wayPoint.coord = coord.ToDegrees();
				wayPoint.Alt = (int) mappingAlt;
				wayPoint.Speed = speed;
				wayPoint.Heading = heading;
				wayPoint.HoverTime = 1;
				route.mappingWayPoints.add(wayPoint);

				gsTask.addWaypoint(CreateMappingWayPoint(wayPoint, camAngle));

				cur.Lat -= Utilities.MetersToDeg(stepV);
			}

			cur.Lon += Utilities.MetersToDeg(stepH);
			stepV *= -1.0;
			cur.Lat -= Utilities.MetersToDeg(stepV);
		}
	}
}
