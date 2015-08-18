package com.my.fly;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import com.dji.wrapper.*;

import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.GroundStation.DJIGroundStationFlyingInfo;
import dji.sdk.api.MainController.*;
import dji.sdk.widget.DjiGLSurfaceView;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
//import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnTouchListener, LocationListener, Handler.Callback
{
	private DJIWrapper djiWrapper = new DJIWrapper();
	private TextView errorMessages = null;
	private static final String TAG = "MainActivity";
	private DjiGLSurfaceView cameraSurface = null;
	private ScrollView scrollViewMessages = null;
	private TextView droneSpeed = null;
	private TextView droneAltitude = null;
	private TextView droneDistance = null;
	private LocationManager locationManager = null;
	//private Button autoStart = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_main);

		((ImageButton) findViewById(R.id.camTop)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.camBottom)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneRotLeftTop)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneTop)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneRotRightTop)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneRotLeft)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneRotRight)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneRotLeftBottom)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneBottom)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneRotRightBottom)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneStrafeLeftForward)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneForward)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneStrafeRightForward)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneStrafeLeft)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneStrafeRight)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneStrafeLeftBackward)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneBackward)).setOnTouchListener(this);
		((ImageButton) findViewById(R.id.droneStrafeRightBackward)).setOnTouchListener(this);
		((RadioGroup) findViewById(R.id.speedLimit)).check(R.id.speedLimitMiddle);

		View v = (View) findViewById(R.id.surfaceView_Rl_02);
		v.setBackgroundColor(Color.DKGRAY);

		//autoStart = (Button) findViewById(R.id.autoStart);
		droneSpeed = (TextView) findViewById(R.id.droneSpeed);
		droneAltitude = (TextView) findViewById(R.id.droneAltitude);
		droneDistance = (TextView) findViewById(R.id.droneDistance);
		scrollViewMessages = (ScrollView) findViewById(R.id.scrollViewMessages);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

		Display display = getWindowManager().getDefaultDisplay();
		Point scrSize = new Point();
		display.getSize(scrSize);

		cameraSurface = (DjiGLSurfaceView) findViewById(R.id.DjiSurfaceView_02);
		cameraSurface.getLayoutParams().height = scrSize.y;
		cameraSurface.getLayoutParams().width = (int) ((float) scrSize.y * 4.0f / 3.0f);
		errorMessages = (TextView) findViewById(R.id.errorMessages);

		AppendString("Connecting to drone");
		if (!djiWrapper.InitSDK(DJIDroneType.DJIDrone_Phantom3_Professional, getApplicationContext(), this))
			AppendString("Can't connect to the drone");
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what)
		{
			case DJIWrapper.PERMISSION_STATUS:
				AppendString(djiWrapper.GetPermissionErrorResultMessage);
				djiWrapper.ConnectDroneDevices(cameraSurface);
				break;
			case DJIWrapper.ERROR_MESSAGE:
				AppendString((String) msg.obj);
				break;
			case DJIWrapper.MCU_STATUS:
				ShowFlightStatus((DJIMainControllerSystemState) msg.obj);

				break;
			case DJIWrapper.GROUNDSTATION_FLYING_STATUS:
				DJIGroundStationFlyingInfo flyingInfo = (DJIGroundStationFlyingInfo) msg.obj;
				AppendString("GS_FLYING_STATE " + flyingInfo.flightMode.toString());
				break;
			default:
				break;
		}

		return false;
	}

	private NumberFormat formatter = new DecimalFormat("#.#");
	private void ShowFlightStatus(DJIMainControllerSystemState state)
	{
		try
		{
			Location lastLocation = GetOwnerLastPosition();			
			float[] distance = new float[3];

			if (lastLocation != null && (state.droneLocationLatitude != 0.0 || state.droneLocationLongitude != 0.0))
				Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(), state.droneLocationLatitude, state.droneLocationLongitude, distance);
			else
				distance[0] = 0.0f;

			Log.e(TAG, "MCU status s " + state.speed + "  h " + state.altitude + "  d " + distance[0]);
			droneSpeed.setText("s " + formatter.format(state.speed) + "ms");
			droneAltitude.setText("h " + formatter.format(state.altitude) + "m");
			droneDistance.setText("d " + formatter.format(distance[0]) + "m");
		}
		catch (Exception e)
		{
		}
	}

	public void onSpeedLimitChanged(View view)
	{
		boolean checked = ((RadioButton) view).isChecked();

		switch (view.getId())
		{
			case R.id.speedLimitFast:
			{
				if (checked)
					djiWrapper.GetGroundStation().GetJoystik().SetSpeedLimit(DJIGroundStation.SpeedLimits.SpeedLimitFast);

				break;
			}

			case R.id.speedLimitMiddle:
			{
				if (checked)
					djiWrapper.GetGroundStation().GetJoystik().SetSpeedLimit(DJIGroundStation.SpeedLimits.SpeedLimitMiddle);

				break;
			}

			case R.id.speedLimitSlow:
			{
				if (checked)
					djiWrapper.GetGroundStation().GetJoystik().SetSpeedLimit(DJIGroundStation.SpeedLimits.SpeedLimitSlow);

				break;
			}
		}
	}

	StringBuilder textBuilder = new StringBuilder();
	String prevMessage = "";
	void AppendString(String text)
	{
		try
		{
			if (text.length() == 0)
				return;

			if (prevMessage.equals(text))
				return;

			prevMessage = text;

			Log.e(TAG, text);
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
		}
		catch (Exception e)
		{
		}
	}

	@Override
	protected void onDestroy()
	{
		locationManager.removeUpdates(this);

		djiWrapper.Destroy();

		super.onDestroy();
	}

	public void OnTakeOff(View v)
	{
		if (!djiWrapper.GetMcu().IsFlying())
			djiWrapper.GetGroundStation().TakeOff();
		else
			djiWrapper.GetGroundStation().DoHover();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_OUTSIDE || event.getAction() == MotionEvent.ACTION_CANCEL)
		{
			switch (v.getId())
			{
				case R.id.camTop:
				case R.id.camBottom:
					djiWrapper.GetCamera().PitchStop();
					break;
				case R.id.droneRotLeftTop:
				case R.id.droneTop:
				case R.id.droneRotRightTop:
				case R.id.droneRotLeft:
				case R.id.droneRotRight:
				case R.id.droneRotLeftBottom:
				case R.id.droneBottom:
				case R.id.droneRotRightBottom:
					djiWrapper.GetGroundStation().GetJoystik().StopLeftJoystik();
					break;
				case R.id.droneStrafeLeftForward:
				case R.id.droneForward:
				case R.id.droneStrafeRightForward:
				case R.id.droneStrafeLeft:
				case R.id.droneStrafeRight:
				case R.id.droneStrafeLeftBackward:
				case R.id.droneBackward:
				case R.id.droneStrafeRightBackward:
					djiWrapper.GetGroundStation().GetJoystik().StopRightJoystik();
					break;
			}
		}
		else
		{
			Log.e(TAG, "Action=" + event.getAction());
			switch (v.getId())
			{
				case R.id.camTop:
					djiWrapper.GetCamera().PitchUpStart();
					break;
				case R.id.camBottom:
					djiWrapper.GetCamera().PitchDownStart();
					break;
				case R.id.droneRotLeftTop:
					djiWrapper.GetGroundStation().GetJoystik().RotLeftTop();
					break;
				case R.id.droneTop:
					djiWrapper.GetGroundStation().GetJoystik().Top();
					break;
				case R.id.droneRotRightTop:
					djiWrapper.GetGroundStation().GetJoystik().RotRightTop();
					break;
				case R.id.droneRotLeft:
					djiWrapper.GetGroundStation().GetJoystik().RotLeft();
					break;
				case R.id.droneRotRight:
					djiWrapper.GetGroundStation().GetJoystik().RotRight();
					break;
				case R.id.droneRotLeftBottom:
					djiWrapper.GetGroundStation().GetJoystik().RotLeftBottom();
					break;
				case R.id.droneBottom:
					djiWrapper.GetGroundStation().GetJoystik().Bottom();
					break;
				case R.id.droneRotRightBottom:
					djiWrapper.GetGroundStation().GetJoystik().RotRightBottom();
					break;
				case R.id.droneStrafeLeftForward:
					djiWrapper.GetGroundStation().GetJoystik().StrafeLeftForward();
					break;
				case R.id.droneForward:
					djiWrapper.GetGroundStation().GetJoystik().Forward();
					break;
				case R.id.droneStrafeRightForward:
					djiWrapper.GetGroundStation().GetJoystik().StrafeRightForward();
					break;
				case R.id.droneStrafeLeft:
					djiWrapper.GetGroundStation().GetJoystik().StrafeLeft();
					break;
				case R.id.droneStrafeRight:
					djiWrapper.GetGroundStation().GetJoystik().StrafeRight();
					break;
				case R.id.droneStrafeLeftBackward:
					djiWrapper.GetGroundStation().GetJoystik().StrafeLeftBackward();
					break;
				case R.id.droneBackward:
					djiWrapper.GetGroundStation().GetJoystik().Backward();
					break;
				case R.id.droneStrafeRightBackward:
					djiWrapper.GetGroundStation().GetJoystik().StrafeRightBackward();
					break;
			}
		}

		return false;
	}

	private int mapUserPathPoints = 3;
	private int zeroSpeedCount = 0;
	private ArrayList<Location> locations = new ArrayList<Location>();

	@Override
	public void onLocationChanged(Location location)
	{
		try
		{
			Log.e(TAG, "onLocationChanged");

			if (location.getSpeed() < 0.5)
				zeroSpeedCount++;
			else
				zeroSpeedCount = 0;

			if (locations.size() == 0)
			{
				locations.add(location);
				return;
			}

			if (zeroSpeedCount > 1)
			{
				FollowMe();
				return;
			}

			Location lastLocation = GetOwnerLastPosition();
			if (lastLocation == null)
				return;

			float distance = lastLocation.distanceTo(lastLocation);
			if (distance > 5)
				locations.add(location);

			Log.e(TAG, "locations count is " + locations.size());

			if (locations.size() > mapUserPathPoints)
			{
				locations.remove(0);
				FollowMe();
			}
		}
		catch (Exception e)
		{
		}
	}

	public void FollowMe()
	{
	}

	public Location GetOwnerLastPosition()
	{
		if (locations.size() == 0)
			return null;

		return (Location) locations.get(locations.size() - 1);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider)
	{
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		djiWrapper.DisconnectDroneDevices();
	}
	
	@Override 
	public void onResume()
	{
		super.onResume();
		
		djiWrapper.ConnectDroneDevices(cameraSurface);
	}
}
