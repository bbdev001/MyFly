package com.my.fly;

import geolife.android.navigationsystem.NavmiiControl;
import geolife.android.navigationsystem.NavmiiControl.MapCoord;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.dji.wrapper.DJIWrapper;
import com.dji.wrapper.Route;
import com.my.fly.utilities.*;

import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationStatusPushType;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationWayPointExecutionState;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.Scroller;

public class RouteView extends View
{
	private class MarkerInfo 
	{
		public Integer wayPointNumber;
		public WayPoint wayPoint;
		
		public MarkerInfo(int number, WayPoint wayPoint)
		{
			this.wayPointNumber = number;
			this.wayPoint = wayPoint;
		}
	};
	
	public int width = 0;
	public int height = 0;
	public static final String TAG = "RouteView";
	private float scale = 1.0f;
	private float gestureScale = 1.0f;
	private Context context = null;
	private Paint paint;
	private Paint textPaint;
	
	private Route route = new Route("");
	private static final float LINE_WIDTH = 10.0f;
	private NavmiiControl.MapCoord lastClickGeoPosition = new NavmiiControl.MapCoord();
	private ScaleGestureDetector scaleGestureDetector = null;
	private RotationDetector rotateDetector = null;
	private MapCoord homePosition = new MapCoord();
	private MapCoord dronePosition = new MapCoord();
	private MapCoord viewPoint = new MapCoord();
	private DegPoint droneUserPosition = new DegPoint();
	private Long droneMarkerId = (long)0;
	private Long homeMarkerId = (long)0;
	private Long viewPointMarkerId = (long)0;
	private HashMap<Long, MarkerInfo> wayPointMarkers = new HashMap<Long, MarkerInfo>();
		
	private double droneSpeed = 0.0;
	private double droneAlt = 0.0;
	private double droneDistance = 0.0;
	private double droneRemainFlyTime = 0.0;
	private double dronePowerLevel = 0.0;
	private double dronePitch = 0.0;
	private double droneRoll = 0.0;
	private double droneHeading = 0.0;
	private double gimbalPitch = 0.0;
	private double gimbalRoll = 0.0;
	private double gimbalYaw = 0.0;
	private String missionType = "";
	private String missionState = "";
	private int missionTargetWP = 0;
	private int missionReachedWP = 0;
	private int selectedPointIndex = -1;
	private NavmiiControl navigationSystem = null;
	private String resourcePath = "";
	private float droneZLevel = 1.0f;
	private float selectedWpZLevel = 0.9f;
	private float wpZLevel = 0.8f;
	private float routeLineZLevel = 0.7f;
	private Long routeLineId = NavmiiControl.INVALID_USER_ITEM_ID;
	private Mbr scrMbr = new Mbr();
	
	protected boolean isShowPress = false;

	public interface OnWayPointSelected
	{
		public void onWayPointSelected(int wayPointId);
	}

	public interface OnWayPointPositionChanged
	{
		public void onWayPointPositionChanged(int wayPointId, MrcPoint newCoord);
	}

	public interface OnWayPointPositionChangingDone
	{
		public void onWayPointPositionChangingDone(int wayPointId);
	}

	protected OnWayPointSelected wayPointSelected = null;
	protected OnWayPointPositionChanged wayPointPositionChanged = null;
	protected OnWayPointPositionChangingDone wayPointPositionChangingDone = null;

	protected int ConvertSPToPixels(int spValue)
	{
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                              spValue, getResources().getDisplayMetrics());
	}
	
	public RouteView(Context c, AttributeSet attrs)
	{
		super(c, attrs);
		context = c;
		
		int pixelSize = ConvertSPToPixels(10);
		
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setTextSize(pixelSize);

		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setColor(Color.BLACK);
		textPaint.setTextSize(pixelSize);

		width = (int) this.GetWidth();
		height = (int) this.GetHeight();
	}

	public void SetNavigationSystem(NavmiiControl navigationSystem, String resourcePath)
	{
		this.resourcePath = resourcePath;
		this.navigationSystem = navigationSystem;
		scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener(this.navigationSystem));
		rotateDetector = new RotationDetector(this, this.navigationSystem);		
	}

	public void AddOnWayPointSelectedListener(OnWayPointSelected listener)
	{
		wayPointSelected = listener;
	}

	public void AddOnWayPointPositionChangedListener(OnWayPointPositionChanged listener)
	{
		wayPointPositionChanged = listener;
	}

	public DegPoint TranslateScreenPointToGeoPoint(Point point)
	{
		MapCoord mapCoord = navigationSystem.GetPositionOnMap(point);	
		
		return new DegPoint(mapCoord.lat, mapCoord.lon);
	}
	
	public void SetHomePosition(DegPoint position)
	{
		position.CopyTo(homePosition);

		if (homeMarkerId != NavmiiControl.INVALID_USER_ITEM_ID)
			navigationSystem.SetMarkerPosition(homeMarkerId, homePosition);
		else
			homeMarkerId = navigationSystem.CreateMarkerOnMap(resourcePath + "/bmp/Flag_Finish.png", homePosition, 0.5f, 0.5f, false);
	}

	public void SetDronePosition(DegPoint position, double alt, double speed, double distance, double remainFlyTime, double powerLevel, double pitch, double roll, double heading)
	{
		position.CopyTo(dronePosition);

		droneAlt = alt;
		droneSpeed = speed;
		droneDistance = distance;
		droneRemainFlyTime = remainFlyTime;
		dronePitch = pitch;
		droneRoll = roll;
		droneHeading = heading;

		if (droneMarkerId != NavmiiControl.INVALID_USER_ITEM_ID)
		{
			navigationSystem.SetMarkerPosition(droneMarkerId, dronePosition);
			navigationSystem.SetMarkerHeading(droneMarkerId, (float)heading);	
		}
		else
			droneMarkerId = navigationSystem.CreateDirectedMarkerOnMap(resourcePath + "/bmp/arrowMagenta.png", dronePosition, (float)heading, 0.5f, 0.5f, false);
		
		navigationSystem.SetItemOnMapZLevel(droneMarkerId, droneZLevel);

		invalidate();
	}

	public void SetGimbalStatus(double pitch, double roll, double yaw)
	{
		gimbalPitch = pitch;
		gimbalRoll = roll;
		gimbalYaw = yaw;
	}

	public void SetDroneUserPosition(DegPoint position)
	{
		position.CopyTo(droneUserPosition);
	}

	// override onSizeChanged
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		Log.i(TAG, "onSizeChanged " + w + " " + h + " " + scale);
		super.onSizeChanged(w, h, oldw, oldh);

		width = w;
		height = h;
	}
	
	protected long AddWayPoint(WayPoint wayPoint, int id)
	{
		MapCoord wpCoord = wayPoint.coord.ToMapCoord();		
		long markerId = navigationSystem.CreateDirectedMarkerOnMap(resourcePath + "/bmp/arrowBlue.png", wpCoord, wayPoint.Heading, 0.5f, 0.5f, true);

		navigationSystem.InsertPointToPolyline(routeLineId, wayPointMarkers.size(), wpCoord);
				
		navigationSystem.SetItemOnMapZLevel(markerId, wpZLevel);
		wayPointMarkers.put(markerId, new MarkerInfo(id, wayPoint));
		
		return markerId;
	}
	
	public void SetRoute(Route route, boolean autoScale)
	{
		Log.i("RouteView", "Select route");
	
		this.route = route;
		
		for (Map.Entry<Long, MarkerInfo> entry: wayPointMarkers.entrySet()) 
		{ 
			Long key = entry.getKey(); 
			navigationSystem.DeleteItemOnMap(key);
		} 
							
		route.viewPoint.coord.CopyTo(viewPoint);
		
		if (viewPointMarkerId == NavmiiControl.INVALID_USER_ITEM_ID)
			viewPointMarkerId = navigationSystem.CreateMarkerOnMap(resourcePath + "/bmp/waypoint_1.png", viewPoint, 0.5f, 0.5f, true);
		else
			navigationSystem.SetMarkerPosition(viewPointMarkerId, viewPoint);			
	
		if (routeLineId != NavmiiControl.INVALID_USER_ITEM_ID)
			navigationSystem.DeleteItemOnMap(routeLineId);
			
		routeLineId = navigationSystem.CreatePolylineOnMap(0xFF0000, 5.0f, new NavmiiControl.MapCoord[0]);
		
		navigationSystem.SetItemOnMapZLevel(routeLineId, routeLineZLevel);	
		
		ArrayList<WayPoint> wayPoints = route.GetWayPoints();
		wayPointMarkers.clear();

		for (int i = 0; i < wayPoints.size(); i++)
		{
			WayPoint wayPoint = wayPoints.get(i);		

			AddWayPoint(wayPoint, i);	
		}

	
		if (autoScale)
		{
			if (route.mbr.IsEmpty())
			{
				mapCenter.Lat = droneUserPosition.Lat;
				mapCenter.Lon = droneUserPosition.Lon;
			}
			else
			{		
				mapCenter.Lat = route.mbr.GetCenterY();
				mapCenter.Lon = route.mbr.GetCenterX();
			}
		
			this.SetDronePosition(mapCenter, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
			this.SetHomePosition(mapCenter);
			this.SetDroneUserPosition(mapCenter);

			if (width == 0 || height == 0)
				return;

			scale = navigationSystem.GetMapZoom();		
			
			Point p = new Point();
			p.set(0, 0);
			MapCoord p1 = navigationSystem.GetPositionOnMap(p);
			
			p.set(width - 96, height - 96);
			MapCoord p2 = navigationSystem.GetPositionOnMap(p);
				
			scrMbr.Reset();
			scrMbr.Adjust(p1.lon, p1.lat);
			scrMbr.Adjust(p2.lon, p2.lat);
					

            float bestZoomBoost = Math.max((float)(route.mbr.GetWidth() / scrMbr.GetWidth()), (float)(route.mbr.GetHeight() / scrMbr.GetHeight()));

            if (bestZoomBoost >= 0.0f)
                scale *= bestZoomBoost;
		}
		else
		{
			if (route.mbr.IsEmpty())
			{
				mapCenter.Lat = droneUserPosition.Lat;
				mapCenter.Lon = droneUserPosition.Lon;
			}
		}

		navigationSystem.SetMapZoom(scale);
		mapCoord.lon = mapCenter.Lon;
		mapCoord.lat = mapCenter.Lat;
		navigationSystem.SetMapCenter(mapCoord);

		invalidate();
	}
	
	public void ChangeRoutePointPosition(int index, MapCoord point)
	{
		navigationSystem.SetPolylinePoint(routeLineId, index, point);
	}
	
	public int GetWayPointNumberByMarkerId(long markerId)
	{
		if (wayPointMarkers.size() == 0)
			return -1;
		
		MarkerInfo info = wayPointMarkers.get(markerId);
		if (info == null)
			return -1;
			
		return info.wayPointNumber;
	}
	
	protected long prevSelectedMarkerId = NavmiiControl.INVALID_USER_ITEM_ID;
	public boolean SelectWayPointByMarkerId(long markerId)
	{
		if (!wayPointMarkers.containsKey(markerId))
			return false;
			
		if (prevSelectedMarkerId != NavmiiControl.INVALID_USER_ITEM_ID)
		{
			navigationSystem.SetMarkerImage(prevSelectedMarkerId, resourcePath + "/bmp/arrowBlue.png", 0.5f, 0.5f);
			navigationSystem.SetItemOnMapZLevel(prevSelectedMarkerId, wpZLevel);
		}
					
		navigationSystem.SetMarkerImage(markerId, resourcePath + "/bmp/arrowGreen.png", 0.5f, 0.5f);
		navigationSystem.SetItemOnMapZLevel(markerId, selectedWpZLevel);
		
		prevSelectedMarkerId = markerId;
		
		return true;
	}
	
	private DegPoint mapCenter = new DegPoint();
	private NumberFormat formatter = new DecimalFormat("#.#");
	private MapCoord mapCoord = new MapCoord();

	@Override
	protected void onDraw(Canvas canvas)
	{
		// Log.i(TAG, "onDraw " + width + " " + height + " " + scale);
		super.onDraw(canvas);

		if (width == 0 || height == 0)
			return;
		
		//mapCoord.lon = mapCenter.Lon;
		//mapCoord.lat = mapCenter.Lat;
		//navigationSystem.SetMapCenter(mapCoord);
		
		canvas.setDensity(DisplayMetrics.DENSITY_LOW);
		canvas.drawColor(Color.TRANSPARENT);
		paint.setStrokeWidth(LINE_WIDTH);

		// Info
		float lineHeight = (float)ConvertSPToPixels(12);
		float linePos = 0.0f;

		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.RouteName) + ": " + route.name, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.LengthM) + ": " + formatter.format(route.length), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.EstFlightTime) + ": " + formatter.format(route.GetEstimatedFlightTime()), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.PowerlevelPerc) + ": " + formatter.format(dronePowerLevel), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.RemFlightTime) + ": " + formatter.format(droneRemainFlyTime), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.DistanceM) + ": " + formatter.format(droneDistance), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.AltitudeM) + ": " + formatter.format(droneAlt), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.SpeedMS) + ": " + formatter.format(droneSpeed), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.Pitch) + ": " + formatter.format(dronePitch), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.Roll) + ": " + formatter.format(droneRoll), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.HeadingD) + ": " + formatter.format(droneHeading), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.CamAngleD) + ": " + formatter.format(gimbalPitch), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.GSType) + " : " + missionType, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.GSMode) + ": " + missionState, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.TargetWP) + ": " + missionTargetWP, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
		Utilities.DrawTextWithBorder(context.getString(R.string.ReachedWP) + ": " + missionReachedWP, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint);
		linePos += lineHeight;
	}
	
	public void clearCanvas()
	{
		invalidate();
	}

	public float GetWidth()
	{
		return width;
	}

	public float GetHeight()
	{
		return height;
	}

	public void SetWayPointHeading(Long markerId, int heading)
	{
		navigationSystem.SetMarkerHeading(markerId, heading);
	}

	public void SetMissionFlightStatus(String type, int targetWayPointIndex, String currentState)
	{
		missionType = type;
		missionTargetWP = targetWayPointIndex;
		missionState = currentState;
		invalidate();
	}

	public void SetReachedWayPoint(int reachedWayPointIndex, String currentState)
	{
		missionReachedWP = reachedWayPointIndex;
		missionState = currentState;
		invalidate();
	}

	public void SetPowerPercent(int powerLevelPercent)
	{
		dronePowerLevel = powerLevelPercent;
		invalidate();
	}

	public void RefreshMarkers()
	{
		for (Map.Entry<Long, MarkerInfo> entry: wayPointMarkers.entrySet()) 
		{ 
			Long key = entry.getKey(); 
			navigationSystem.SetMarkerHeading(key, entry.getValue().wayPoint.Heading);
		} 
	}
	
	public void SetMode(int mode)
	{
		// TODO Auto-generated method stub

	}

	public void SetSattCount(int satelliteCount)
	{
		// TODO Auto-generated method stub

	}
	
	public void SetViewPoint(DegPoint point)
	{
		viewPoint.lon = point.Lon;
		viewPoint.lat = point.Lat;
		
		if (viewPointMarkerId == NavmiiControl.INVALID_USER_ITEM_ID)
			viewPointMarkerId = navigationSystem.CreateMarkerOnMap(resourcePath + "/bmp/waypoint_1.png", viewPoint, 0.5f, 0.5f, true);
		else
			navigationSystem.SetMarkerPosition(viewPointMarkerId, viewPoint);
	}
}
