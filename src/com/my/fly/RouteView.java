package com.my.fly;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import com.dji.wrapper.DJIWrapper;
import com.my.fly.utilities.*;

import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationStatusPushType;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationWayPointExecutionState;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.Scroller;

public class RouteView extends View implements OnGestureListener, OnScaleGestureListener
{
	public int width = 0;
	public int height = 0;
	public static final String TAG = "RouteView";
	private float scale = 1.0f;
	private float gestureScale = 1.0f;
	private Context context = null;
	private Paint paint;
	private Paint textPaint;
	private ArrayList<MrcPoint> route = new ArrayList<MrcPoint>();
	private ArrayList<WayPoint> routeWP = new ArrayList<WayPoint>();
	private static final float LINE_WIDTH = 10.0f;
	private Mbr mbr = new Mbr();
	private GestureDetector gestureDetector = null;
	private ScaleGestureDetector scaleGestureDetector = null;
	private ScreenPoint scrollOffset = new ScreenPoint(); 
	private ScreenPoint scrCenter = new ScreenPoint();
	private String routeName = "";
	private MrcPoint homePosition = new MrcPoint();
	private MrcPoint dronePosition = new MrcPoint();
	private MrcPoint droneUserPosition = new MrcPoint();
	private double droneSpeed = 0.0;
	private double droneAlt = 0.0;
	private double droneDistance = 0.0;
	private double droneRemainFlyTime = 0.0;
	private double dronePowerLevel = 0.0;
	private double dronePitch = 0.0;
	private double droneRoll = 0.0;
	private double droneYaw = 0.0;
	private String missionType = "";
	private String missionState = "";
	private int missionTargetWP = 0;
	private int missionReachedWP = 0;
	private int selectedPointIndex = -1;
	protected boolean isShowPress = false;

	public interface OnWayPointSelected
	{
		public void onWayPointSelected(int wayPointId);
	}
	
	ArrayList<OnWayPointSelected> wayPointSelectedListeners = new ArrayList<OnWayPointSelected>();
	
	public RouteView(Context c, AttributeSet attrs)
	{
		super(c, attrs);
		context = c;

		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setTextSize(32);
				
		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setColor(Color.BLACK);
		textPaint.setTextSize(32);
				
		width = (int)this.GetWidth();
		height = (int)this.GetHeight();
		
		gestureDetector = new GestureDetector(context, this);
		scaleGestureDetector = new ScaleGestureDetector(context, this);
	}

	public void AddOnWayPointSelectedListener(OnWayPointSelected listener)
	{
		wayPointSelectedListeners.add(listener);
	}
	
	public void Reset()
	{
		gestureScale = 1.0f;
		scrCenter = new ScreenPoint(width / 2.0f, height / 2.0f);
		
		if (height < width)
			scale = (float)((height - LINE_WIDTH * 4.0f) / (mbr.Ymax - mbr.Ymin));
		else
			scale = (float)((width - LINE_WIDTH * 4.0f) / (mbr.Xmax - mbr.Xmin));
	}
	
	public void SetHomePosition(DegPoint position)
	{
		Utilities.ToMercator(position, homePosition);
	}
	
	public void SetDronePosition(DegPoint position, double alt, double speed, double distance, double remainFlyTime, double powerLevel, double pitch, double roll, double yaw)
	{
		Utilities.ToMercator(position, dronePosition);
		droneAlt = alt;
		droneSpeed = speed;
		droneDistance = distance;
		droneRemainFlyTime = remainFlyTime;
		dronePitch = pitch;
		droneRoll = roll;
		droneYaw = yaw;
		
		invalidate();
	}
	
	public void SetDroneUserPosition(DegPoint position)
	{
		Utilities.ToMercator(position, droneUserPosition);
		
		invalidate();
	}
	
	// override onSizeChanged
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		Log.i(TAG, "onSizeChanged " + w + " " + h + " " + scale); 
		super.onSizeChanged(w, h, oldw, oldh);

		width = w;
		height = h;

		Reset();
	}

	public void SetRoute(ArrayList<WayPoint> wayPoints, String routeName)
	{
		Log.i(TAG, "SetRoute " + width + " " + height + " " + scale);
		routeWP.clear();
		route.clear();
		mbr.Reset();
		this.routeName = routeName;
		
		for (int i = 0; i < wayPoints.size(); i++)
		{
			MrcPoint mrcPoint = wayPoints.get(i).coord.ToMercator(); 
			mbr.Adjust(mrcPoint.Lon, mrcPoint.Lat);
			route.add(mrcPoint);
			routeWP.add(wayPoints.get(i));
		}
		
		mapCenter.Lat = mbr.GetCenterY();
		mapCenter.Lon = mbr.GetCenterX();
		DegPoint defPoint = mapCenter.ToDegrees();
		this.SetDronePosition(defPoint, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		this.SetHomePosition(defPoint);
		this.SetDroneUserPosition(defPoint);
		
		if (width == 0 || height == 0)
			return;
				
		Reset();
		invalidate();
	}

	private MrcPoint mapCenter = new MrcPoint();
	private NumberFormat formatter = new DecimalFormat("#.#");
	private ScreenPoint p = new ScreenPoint();
	private ScreenPoint p1 = new ScreenPoint();
	
	
	@Override
	protected void onDraw(Canvas canvas)
	{
		//Log.i(TAG, "onDraw " + width + " " + height + " " + scale);
		super.onDraw(canvas);
		
		if (width == 0 || height == 0)
			return;
			
		scale *= gestureScale;
		
		mapCenter.Lat = mbr.GetCenterY();
		mapCenter.Lon = mbr.GetCenterX();
		scrCenter.Add(scrollOffset);
	
		canvas.drawColor(Color.WHITE);
		paint.setStrokeWidth(LINE_WIDTH);

		//Route
		paint.setColor(Color.RED);
		for (int i = 1; i < route.size(); i++)
		{
			p.FromMercator(route.get(i - 1), mapCenter, scrCenter, scale);
			p1.FromMercator(route.get(i), mapCenter, scrCenter, scale);
			canvas.drawLine(p.X, p.Y, p1.X, p1.Y, paint);
		}

		
		for (int i = 0; i < route.size(); i++)
		{	
			p.FromMercator(route.get(i), mapCenter, scrCenter, scale);
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(Color.BLUE);
			canvas.drawCircle(p.X, p.Y, LINE_WIDTH, paint);
			paint.setStyle(Paint.Style.STROKE);
			
			if (routeWP.get(i).HoverTime > 0)
			{
				p.CopyTo(p1); p1.SetY(p1.dY - 30);
				Utilities.Rotate(p1, p, routeWP.get(i).Heading);		
				paint.setStrokeWidth(LINE_WIDTH);
				canvas.drawLine(p.X, p.Y, p1.X, p1.Y, paint);			
			}
		
			Utilities.DrawTextWithBorder(Integer.toString(i) + ": " + formatter.format(routeWP.get(i).Alt) + "m", p.X, p.Y,
				 Color.GRAY, Color.WHITE, 1, 3, canvas, textPaint);
		}

		//Home
		p.FromMercator(homePosition, mapCenter, scrCenter, scale);
		paint.setColor(Color.GRAY);
		paint.setStrokeWidth(LINE_WIDTH * 4.0f);	
		canvas.drawPoint(p.X, p.Y, paint);
		
		//Drone
		p.FromMercator(dronePosition, mapCenter, scrCenter, scale);
		paint.setColor(Color.GRAY);
		paint.setStrokeWidth(LINE_WIDTH * 4.0f);	
		canvas.drawPoint(p.X, p.Y, paint);
		
		paint.setColor(Color.MAGENTA);
		paint.setStrokeWidth(LINE_WIDTH * 3.0f);	
		canvas.drawPoint(p.X, p.Y, paint);

		//User
		p.FromMercator(droneUserPosition, mapCenter, scrCenter, scale);
		paint.setColor(Color.GREEN);
		paint.setStrokeWidth(LINE_WIDTH * 2.0f);	
		canvas.drawPoint(p.X, p.Y, paint);

		//Selected point;
		if (selectedPointIndex >= 0 && route.size() > selectedPointIndex)
		{
			p.FromMercator(route.get(selectedPointIndex), mapCenter, scrCenter, scale);
			paint.setStyle(Paint.Style.FILL);			
			paint.setColor(Color.GREEN);
			canvas.drawCircle(p.X, p.Y, LINE_WIDTH, paint);
			paint.setStyle(Paint.Style.STROKE);
		}

		//Info
		float lineHeight = 42.0f;
		float linePos = 0.0f;
		
		linePos += lineHeight;
		Utilities.DrawTextWithBorder("Name: " + routeName, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;
		Utilities.DrawTextWithBorder("Power level: " + formatter.format(dronePowerLevel) + "%", 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;
		Utilities.DrawTextWithBorder("Remain fly time: " + formatter.format(droneRemainFlyTime) + "min", 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;
		Utilities.DrawTextWithBorder("Distance: " + formatter.format(droneDistance) + "m", 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;
		Utilities.DrawTextWithBorder("Altitude: " + formatter.format(droneAlt) + "m", 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;		
		Utilities.DrawTextWithBorder("Speed: " + formatter.format(droneSpeed) + "ms", 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;
		Utilities.DrawTextWithBorder("Pitch: " + formatter.format(dronePitch), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;		
		Utilities.DrawTextWithBorder("Roll: " + formatter.format(droneRoll), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;		
		Utilities.DrawTextWithBorder("Yaw: " + formatter.format(droneYaw), 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;				
		Utilities.DrawTextWithBorder("GS Type: " + missionType, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;
		Utilities.DrawTextWithBorder("GS State: " + missionState, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;		
		Utilities.DrawTextWithBorder("Target WP: " + missionTargetWP, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;
		Utilities.DrawTextWithBorder("Reached WP: " + missionReachedWP, 10.0f, linePos, Color.BLACK, Color.WHITE, 1, 3, canvas, textPaint); linePos += lineHeight;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		switch(event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
			{
				selectedPointIndex = GetNearestPointIndex(event.getX(), event.getY());
				break;
			}
			case MotionEvent.ACTION_MOVE:
			{
				if (selectedPointIndex >= 0 && isShowPress)
				{
					scrollOffset.Set(0.0, 0.0);
					p.Set(event.getX(), event.getY());
					p.ToMercator(route.get(selectedPointIndex), mapCenter, scrCenter, scale);
					invalidate();
					return true;
				}
				
				break;
			}
			case MotionEvent.ACTION_UP:
			{
				isShowPress = false;
				//scrollOffset.Set(0.0, 0.0);
				break;
			}
		}
		
		gestureDetector.onTouchEvent(event);
		scaleGestureDetector.onTouchEvent(event);
		//test
		return true;
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

	@Override
	public boolean onDown(MotionEvent e)
	{
		//Log.e(TAG, "onDown");	
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e)
	{
		isShowPress = true;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e)
	{
		scrollOffset.Set(0.0, 0.0);
		if (selectedPointIndex < 0)
			return false;
		
		for (OnWayPointSelected listener:wayPointSelectedListeners)
		{
			listener.onWayPointSelected(selectedPointIndex);
		}
		
		invalidate();
		
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		scrollOffset.Set(-distanceX, -distanceY);

		invalidate();
		
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e)
	{
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		return true;
	}
	
	@Override
    public void computeScroll()
    {

    }

	@Override
	public boolean onScale(ScaleGestureDetector detector)
	{
		gestureScale = detector.getScaleFactor();
		
		postInvalidate();
		
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector)
	{
		gestureScale = 1.0f;
		
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector)
	{
		gestureScale = 1.0f;
	}
	
	protected int GetNearestPointIndex(float x, float y)
	{
		int result = -1;
		double minValue = Double.MAX_VALUE;
		ScreenPoint p = new ScreenPoint(x, y);
		
		for (int i = 0; i < route.size(); i++)
		{
			p1.FromMercator(route.get(i), mapCenter, scrCenter, scale);
			double d = p.DistanceTo(p1);
			if (d < 50 && d < minValue)
			{
				result = i;
				minValue = d;
			}
		}
		
		Log.e(TAG, "Index " + result + " x " + x + " y " + y + " distance " + minValue + " px");

		return result;
	}

	public void SetWayPoint(int wayPointId, WayPoint wayPoint)
	{
		routeWP.set(wayPointId, wayPoint);
		invalidate();
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

	public void SetMode(int mode)
	{
		// TODO Auto-generated method stub
		
	}

	public void SetSattCount(int satelliteCount)
	{
		// TODO Auto-generated method stub
		
	}
}

