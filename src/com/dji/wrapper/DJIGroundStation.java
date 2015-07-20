package com.dji.wrapper;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.GroundStation.DJIGroundStationExecutionPushInfo;
import dji.sdk.api.GroundStation.DJIGroundStationFlyingInfo;
import dji.sdk.api.GroundStation.DJIGroundStationMissionPushInfo;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJINavigationFlightControlCoordinateSystem;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationFlightMode;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationGoHomeResult;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationHoverResult;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationOneKeyFlyResult;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationResult;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationResumeResult;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationTakeOffResult;
import dji.sdk.api.GroundStation.DJIPhantomGroundStation;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIGroundStationExecutionPushInfoCallBack;
import dji.sdk.interfaces.DJIGroundStationFlyingInfoCallBack;
import dji.sdk.interfaces.DJIGroundStationGoHomeCallBack;
import dji.sdk.interfaces.DJIGroundStationHoverCallBack;
import dji.sdk.interfaces.DJIGroundStationMissionPushInfoCallBack;
import dji.sdk.interfaces.DJIGroundStationOneKeyFlyCallBack;
import dji.sdk.interfaces.DJIGroundStationResumeCallBack;
import dji.sdk.interfaces.DJIGroundStationTakeOffCallBack;

public class DJIGroundStation
{
	public final class SpeedLimits
	{
		public static final int SpeedLimitFast = 100;
		public static final int SpeedLimitMiddle = 50;
		public static final int SpeedLimitSlow = 25;
	}

	private Joystik joystik = null;
	private Vibrator vibro = null;
	private Handler uiHandler = null;
	private Context context = null;
	private dji.sdk.api.GroundStation.DJIGroundStation object = null;
	private DJIDroneType droneType = null;
	
	public DJIGroundStation(DJIDroneType droneType, Context context, Handler handler)
	{
		this.context = context;
		this.uiHandler = handler;
		this.droneType = droneType;
		
		object = DJIDrone.getDjiGroundStation();
		vibro = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
		joystik = new Joystik(handler, vibro);
	}

	private GroundStationFlightMode flightMode;

	public void Connect(int interval)
	{
		if (droneType == DJIDroneType.DJIDrone_Inspire1)
		{	
			//((dji.sdk.api.GroundStation.DJIInspireGroundStation)object).setHorizontalControlCoordinateSystem(DJIGroundStationTypeDef.DJINavigationFlightControlCoordinateSystem.Navigation_Flight_Control_Coordinate_System_Ground);
			//((dji.sdk.api.GroundStation.DJIInspireGroundStation)object).setYawControlCoordinateSystem(DJIGroundStationTypeDef.DJINavigationFlightControlCoordinateSystem.Navigation_Flight_Control_Coordinate_System_Ground);
		}		
	
		object.setGroundStationFlyingInfoCallBack(new DJIGroundStationFlyingInfoCallBack()
		{
			@Override
			public void onResult(DJIGroundStationFlyingInfo flyingInfo)
			{
				flightMode = flyingInfo.flightMode;
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_FLYING_STATUS, flyingInfo));
			}
		});

		object.setGroundStationMissionPushInfoCallBack(new DJIGroundStationMissionPushInfoCallBack()
		{
			@Override
			public void onResult(DJIGroundStationMissionPushInfo missionInfo)
			{
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_MISSION_STATUS, missionInfo));
			}
		});

		object.setGroundStationExecutionPushInfoCallBack(new DJIGroundStationExecutionPushInfoCallBack()
		{
			@Override
			public void onResult(DJIGroundStationExecutionPushInfo executionInfo)
			{
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_EXECUTION_STATUS, executionInfo));
			}
		});

		StartUpdateTimer(interval);
	}

	public void Disconnect()
	{
		StopUpdateTimer();
		object.setGroundStationFlyingInfoCallBack(null);
	}

	public void TakeOff()
	{
		Log.e("Ground station", "Take off");
		object.openGroundStation(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Successed)
				{
					String message = "Ground station opened";
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, message));

					object.oneKeyFly(new DJIGroundStationOneKeyFlyCallBack()
					{
						@Override
						public void onResult(GroundStationOneKeyFlyResult result)
						{
							String message;
							if (result == GroundStationOneKeyFlyResult.GS_One_Key_Fly_Successed)
							{
								message = "oneKeyFly success";
								DoHover();
							}
							else
								message = "oneKeyFly error " + result;

							uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, message));
						}
					});
				}
				else
				{
					String message = "Ground station does not open " + result;
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, message));
				}
			}
		});
	}

	private DJIGroundStationTask gsTask = null;

	private void GoOnRoute()
	{
		object.uploadGroundStationTask(gsTask, new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "Upload WP " + result.toString()));
				if (result == GroundStationResult.GS_Result_Successed)
				{
					object.startGroundStationTask(new DJIGroundStationTakeOffCallBack()
					{
						@Override
						public void onResult(GroundStationTakeOffResult result)
						{
							if (result == GroundStationTakeOffResult.GS_Takeoff_Successed)
								uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TASK_STARTED, ""));
							else
								uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "GS take off error " + result));
						}
					});
				}
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "GS task does not upload " + result));
			}
		});

	}
	
	public void StartTask(DJIGroundStationTask task)
	{
		gsTask = task;

		object.closeGroundStation(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result != GroundStationResult.GS_Result_Navigation_Is_Not_Open)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, result.toString()));
				
				object.openGroundStation(new DJIGroundStationExecuteCallBack()
				{
					@Override
					public void onResult(GroundStationResult result)
					{
						if (result == GroundStationResult.GS_Result_Successed)
						{
							uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "GS open " + result));
							GoOnRoute();
						}
						else
							uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "GS does not open " + result));
					}
				});
			}
		});		
	}

	public void StopTask()
	{
		object.closeGroundStation(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Successed)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TASK_ENDED, ""));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "GS closing error " + result));
			}
		});		
	}
	
	public void PauseTask()
	{
		object.pauseGroundStationTask(new DJIGroundStationHoverCallBack()
		{
			@Override
			public void onResult(GroundStationHoverResult result)
			{
				if (result == GroundStationHoverResult.GS_Hover_Successed)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TASK_PAUSED, ""));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "GS pause error " + result));
			}
		});
	}

	public void ResumeTask()
	{
		object.continueGroundStationTask(new DJIGroundStationResumeCallBack()
		{
			@Override
			public void onResult(GroundStationResumeResult result)
			{
				if (result == GroundStationResumeResult.GS_Resume_Successed)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TASK_RESUMED, ""));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "GS resume error " + result));
			}
		});
	}

	public void GoHome()
	{
		object.goHome(new DJIGroundStationGoHomeCallBack()
		{
			@Override
			public void onResult(GroundStationGoHomeResult result)
			{
				if (result == GroundStationGoHomeResult.GS_GoHome_Successed)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_GO_HOME, ""));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "GS go home error " + result));
			}
		});
	}
	
	private boolean waitForHover = false;

	public void DoHover()
	{
		if (waitForHover)
			return;

		waitForHover = true;
		new Thread()
		{
			@Override
			public void run()
			{
				while (waitForHover)
				{
					if (flightMode == GroundStationFlightMode.GS_Mode_Pause_1 || flightMode == GroundStationFlightMode.GS_Mode_Pause_2 || flightMode == GroundStationFlightMode.GS_Mode_Gps_Atti)
					{
						object.pauseGroundStationTask(new DJIGroundStationHoverCallBack()
						{
							@Override
							public void onResult(GroundStationHoverResult result)
							{
								if (result == GroundStationHoverResult.GS_Hover_Successed)
									uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "Hover done"));
								else
									uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, result.toString()));

								waitForHover = false;
							}
						});
					}

					DJIWrapper.Sleep(10);
				}
			}
		}.start();
	}

	public void StartUpdateTimer(int interval)
	{
		object.startUpdateTimer(interval);
	}

	public void StopUpdateTimer()
	{
		object.stopUpdateTimer();
	}

	public Joystik GetJoystik()
	{
		return joystik;
	}
}
