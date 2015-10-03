package com.dji.wrapper;

import com.my.fly.R;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationStatusPushType;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationWayPointExecutionState;
import dji.sdk.interfaces.DJIGeneralListener;
import dji.sdk.widget.DjiGLSurfaceView;

public class DJIWrapper
{
	public static final int PERMISSION_STATUS = 0;
	public static final int INFO_MESSAGE = 1;
	public static final int CHECK_CAMERA_STATE = 2;
	public static final int BATTERY_STATUS = 3;
	public static final int MCU_STATUS = 4;
	public static final int GROUNDSTATION_FLYING_STATUS = 5;
	public static final int GROUNDSTATION_MISSION_STATUS = 6;
	public static final int GROUNDSTATION_EXECUTION_STATUS = 7;
	public static final int ROUTES_DOWNLOADED = 8;
	public static final int GROUNDSTATION_TASK_STARTED = 9;
	public static final int GROUNDSTATION_TASK_PAUSED = 10;
	public static final int GROUNDSTATION_TASK_RESUMED = 11;
	public static final int GROUNDSTATION_TASK_ENDED = 12;
	public static final int GROUNDSTATION_GO_HOME = 13;
	public static final int CAMERA_TAKE_PHOTO = 14;
	public static final int CAMERA_TAKE_PHOTO_DONE = 15;
	public static final int REMOTE_CONTROLLER_STATE = 16;
	public static final int CAMERA_FILE_INFO = 17;
	public static final int GIMBAL_STATUS = 18;
	public static final int GROUNDSTATION_TAKE_OFF_DONE = 19;
	public static final int TASK_ERROR_MESSAGE = 20;
	public static final int MEDIA_LIST = 21;
	
	public static final int NAVI_MODE_ATTITUDE = 0;
	public static final int NAVI_MODE_WAYPOINT = 1;
	public static final int EXECUTION_STATUS_UPLOAD_FINISH = 0;
	public static final int EXECUTION_STATUS_FINISH = 1;
	public static final int EXECUTION_STATUS_REACH_POINT = 2;
	public static final int CAMERA_FOV = 90;

	private static final String TAG = "DjiWrapper";

	private DJIDroneType droneType = DJIDroneType.DJIDrone_Inspire1;
	private com.dji.wrapper.DJICamera camera = null;
	private com.dji.wrapper.DJIMcu mcu = null;
	private com.dji.wrapper.DJIGroundStation groundStation = null;
	private com.dji.wrapper.DJIBattery battery = null;
	private com.dji.wrapper.DJIRemoteController remoteController = null;
	private com.dji.wrapper.DJIGimbal gimbal = null;
	private Context context = null;
	private Handler uiHandler = null;

	public boolean IsInited = false;

	public boolean InitSDK(DJIDroneType droneType, Context context, Handler.Callback handlerCallback)
	{
		Log.i(TAG, "Init SDK");

		boolean crashed = false;
		
		do
		{
			try
			{
				this.droneType = droneType;
				this.context = context;
				uiHandler = new Handler(handlerCallback);

				if (!DJIDrone.initWithType(context, droneType))
					return false;

				if (!DJIDrone.connectToDrone())
					return false;

				CheckPermission(context);

				camera = new DJICamera(droneType, context, uiHandler);
				mcu = new DJIMcu(droneType, context, uiHandler);
				groundStation = new DJIGroundStation(droneType, context, uiHandler);
				battery = new DJIBattery(droneType, context, uiHandler);
				gimbal = new DJIGimbal(droneType, context, uiHandler);
				remoteController = new DJIRemoteController(droneType, context, uiHandler);
			}
			catch (Exception e)
			{
				Log.e(TAG, e.toString());
				crashed = true;
			}
		}
		while (crashed);

		Log.i(TAG, "Init SDK done");
		return true;
	}

	public void ConnectDroneDevices(DjiGLSurfaceView cameraSurface)
	{
		if (IsInited)
		{
			if (cameraSurface != null)
				GetCamera().Connect(cameraSurface, 250);

			if (droneType == DJIDroneType.DJIDrone_Inspire1 || droneType == DJIDroneType.DJIDrone_Phantom3_Professional)
				GetRemoteController().Connect(250);

			GetMcu().Connect(250);
			GetGroundStation().Connect(250);
			GetGimbal().Connect(250);
			GetBattery().Connect(2000);
		}
	}

	public void DisconnectDroneDevices()
	{
		if (droneType == DJIDroneType.DJIDrone_Inspire1 || droneType == DJIDroneType.DJIDrone_Phantom3_Professional)
			GetRemoteController().Disconnect();

		GetCamera().Disconnect();
		GetMcu().Disconnect();
		GetGroundStation().Disconnect();
		GetGimbal().Disconnect();
		GetBattery().Disconnect();
	}

	public int GetPermissionErrorResult = 0;
	public String GetPermissionErrorResultMessage = "";

	protected void CheckPermission(Context appContext)
	{
		IsInited = false;
		context = appContext;

		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					DJIDrone.checkPermission(context, new DJIGeneralListener()
					{
						@Override
						public void onGetPermissionResult(int result)
						{
							IsInited = true;
							GetPermissionErrorResult = result;
							GetPermissionErrorResultMessage = GetPermissionErrorText(result);
							uiHandler.sendMessage(uiHandler.obtainMessage(PERMISSION_STATUS, result));
							Log.e(TAG, DJIError.getCheckPermissionErrorDescription(result));
						}
					});
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};

		t.start();
	}

	public DJICamera GetCamera()
	{
		return camera;
	}

	public DJIMcu GetMcu()
	{
		return mcu;
	}

	public DJIGroundStation GetGroundStation()
	{
		return groundStation;
	}

	public DJIGimbal GetGimbal()
	{
		return gimbal;
	}

	public DJIBattery GetBattery()
	{
		return battery;
	}

	public DJIRemoteController GetRemoteController()
	{
		return remoteController;
	}

	public void Destroy()
	{
		if (droneType == DJIDroneType.DJIDrone_Inspire1 || droneType == DJIDroneType.DJIDrone_Phantom3_Professional)
			remoteController.Disconnect();

		camera.Disconnect();
		mcu.Disconnect();
		groundStation.Disconnect();
		battery.Disconnect();

		DJIDrone.disconnectToDrone();

		Log.i(TAG, "Destroy");
	}

	public static void Sleep(int milliseconds)
	{
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	private String GetPermissionErrorText(int code)
	{
		String result = "";

		switch (code)
		{
			case 0:
				result = context.getString(R.string.ObtainPermissionSuccessfully);
				break;
			case -1:
				result = context.getString(R.string.CanNotConnectToInternet);
				break;
			case -2:
				result = context.getString(R.string.TheMetaDataIsInvalid);
				break;
			case -3:
				result = context.getString(R.string.JDKDoNotSupportAES256);
				break;
			case -4:
				result = context.getString(R.string.FailedToObtainDeviceID);
				break;
			case -5:
				result = context.getString(R.string.FailedToObtainDeviceID);
				break;
			case -6:
				result = context.getString(R.string.FailedToParseThePermissionData);
				break;
			case -7:
				result = context.getString(R.string.AppKeyIsForbidden);
				break;
			case -8:
				result = context.getString(R.string.TheActiveDeviceNumbersAreUpToMaximal);
				break;
			case -9:
				result = context.getString(R.string.ParseRequestURLError);
				break;
			case -10:
				result = context.getString(R.string.UnknownError);
				break;
		}

		return result;
	}

	public String GetMissionExecutionState(int state)
	{
		String result = "";
		GroundStationWayPointExecutionState execState = GroundStationWayPointExecutionState.find(state);
		switch (execState)
		{
			case Way_Point_Execution_Init:
				result = context.getString(R.string.Init);
				break;
			case Way_Point_Execution_Moving:
				result = context.getString(R.string.Moving);
				break;
			case Way_Point_Execution_Rotating:
				result = context.getString(R.string.Rotating);
				break;
			case Way_Point_Execution_Inaction:
				result = context.getString(R.string.Incation);
				break;
			case Way_Point_Execution_Reach_Pre_Action:
				result = context.getString(R.string.PreAction);
				break;
			case Way_Point_Execution_Reach_Post_Action:
				result = context.getString(R.string.PostAction);
				break;
		}

		return result;
	}

	public static String GetMissionType(GroundStationStatusPushType missionType)
	{
		String result = "";

		switch (missionType)
		{
			case Navi_Mode_Attitude:
				result = "Attitude";
				break;
			case Navi_Mode_Waypoint:
				result = "Way point";
				break;
			case Navi_Mode_Hotpoint:
				result = "Hot point";
				break;
			case Navi_Mode_FollowMe:
				result = "Follow me";
				break;
		}

		return result;
	}

	public static String GetFlightMode(DJIGroundStationTypeDef.GroundStationFlightMode mode)
	{
		String result = "";
		switch (mode)
		{
			case GS_Mode_Assited_Takeoff:
				result = "Assisted take off";
				break;
			case GS_Mode_Atti:
				result = "Atti";
				break;
			case GS_Mode_Atti_CL:
				result = "Atti Cl";
				break;
			case GS_Mode_Atti_Hover:
				result = "Atti hover";
				break;
			case GS_Mode_Atti_Landing:
				result = "Atti landing";
				break;
			case GS_Mode_Atti_Limited:
				result = "Atti limited";
				break;
			case GS_Mode_Auto_Takeoff:
				result = "Auto off";
				break;
			case GS_Mode_Blake:
				result = "Blake";
				break;
			case GS_Mode_Click_Go:
				result = "Click go";
				break;
			case GS_Mode_Gohome:
				result = "Go home";
				break;
			case GS_Mode_Gps_Atti:
				result = "GPS atti";
				break;
			case GS_Mode_GPS_Atti_Limited:
				result = "GPS atti limited";
				break;
			case GS_Mode_GPS_CL:
				result = "GPS CL";
				break;
			case GS_Mode_Gps_Cruise:
				result = "GPS Cruise";
				break;
			case GS_Mode_Home_Lock:
				result = "Home lock";
				break;
			case GS_Mode_Hot_Point:
				result = "Hot point";
				break;
			case GS_Mode_Hover:
				result = "Hover";
				break;
			case GS_Mode_Joystick:
				result = "Joystick";
				break;
			case GS_Mode_Landing:
				result = "Landing";
				break;
			case GS_Mode_Manual:
				result = "Manual";
				break;
			case GS_Mode_Navi_Go:
				result = "Navi go";
				break;
			case GS_Mode_Pause_1:
				result = "Pause 1";
				break;
			case GS_Mode_Pause_2:
				result = "Pause 2";
				break;
			case GS_Mode_Single:
				result = "Single";
				break;
			case GS_Mode_TakeOff:
				result = "Take off";
				break;
			case GS_Mode_Unknown:
				result = "Unknown";
				break;
			case GS_Mode_Waypoint:
				result = "Way point";
				break;
		}

		return result;
	}
}
