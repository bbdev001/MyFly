package com.dji.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;

import com.my.fly.R;

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
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationResult;
import dji.sdk.api.GroundStation.DJIPhantomGroundStation;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIGroundStationExecutionPushInfoCallBack;
import dji.sdk.interfaces.DJIGroundStationFlyingInfoCallBack;
import dji.sdk.interfaces.DJIGroundStationMissionPushInfoCallBack;

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
	private DJIGroundStationFlyingInfo flyingInfo = new DJIGroundStationFlyingInfo();
	private DJIGroundStation gsWrapper = null;
	private AtomicBoolean isAltAdjustmentNeeded = new AtomicBoolean(false);

	public DJIGroundStation(DJIDroneType droneType, Context context, Handler handler)
	{
		this.context = context;
		this.uiHandler = handler;
		this.droneType = droneType;

		gsWrapper = this;
		object = DJIDrone.getDjiGroundStation();
		vibro = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
		joystik = new Joystik(handler, vibro);
	}

	public void Connect(int interval)
	{
		object.setGroundStationFlyingInfoCallBack(new DJIGroundStationFlyingInfoCallBack()
		{
			@Override
			public void onResult(DJIGroundStationFlyingInfo flyingInfoParam)
			{
				flyingInfo = flyingInfoParam;
				flyingInfo.altitude /= 10.0f;

				if (isAltAdjustmentNeeded.get())
					AdjustAlt();

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

	public void TakeOff(float takeOffAlt)
	{
		this.takeOffAlt = takeOffAlt < 5.0f ? 5.0f : takeOffAlt;

		Log.e("Ground station", "Take off");
		object.openGroundStation(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Success)
				{
					String message = context.getString(R.string.GroundStationOpened);
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.INFO_MESSAGE, message));

					object.oneKeyFly(new DJIGroundStationExecuteCallBack()
					{
						@Override
						public void onResult(GroundStationResult result)
						{
							String message;
							if (result == GroundStationResult.GS_Result_Success)
							{
								message = context.getString(R.string.TakeOffDone);
								isAltAdjustmentNeeded.set(true);
							}
							else
								message = context.getString(R.string.TakeOffError) + " " + result;

							uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.TASK_ERROR_MESSAGE, message));
						}
					});
				}
				else
				{
					String message = context.getString(R.string.GroundStationDoesNotOpen) + " " + result;
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.TASK_ERROR_MESSAGE, message));
				}
			}
		});
	}

	private DJIGroundStationTask gsTask = null;

	public void GoOnRoute()
	{
		object.uploadGroundStationTask(gsTask, new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Success)
				{
					object.startGroundStationTask(new DJIGroundStationExecuteCallBack()
					{
						@Override
						public void onResult(GroundStationResult result)
						{
							if (result == GroundStationResult.GS_Result_Success)
								uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TASK_STARTED, ""));
							else
								uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.TASK_ERROR_MESSAGE, context.getString(R.string.TaskStartError) + " " + result));
						}
					});
				}
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.TASK_ERROR_MESSAGE, context.getString(R.string.TaskUploadError) + " " + result));
			}
		});

	}

	public void StartTask(DJIGroundStationTask task)
	{
		gsTask = task;
		DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Success)
				{
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.INFO_MESSAGE, context.getString(R.string.GroundStationOpened) + " " + result));
					GoOnRoute();
				}
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.TASK_ERROR_MESSAGE, context.getString(R.string.GroundStationDoesNotOpen) + " " + result));
			}
		});

	}

	public void StopTask()
	{
		DJIDrone.getDjiGroundStation().closeGroundStation(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Success)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TASK_ENDED, ""));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.TASK_ERROR_MESSAGE, context.getString(R.string.GroundStationClosingError) + " " + result));
			}
		});
	}

	public void PauseTask()
	{
		object.pauseGroundStationTask(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Success)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TASK_PAUSED, ""));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.INFO_MESSAGE, context.getString(R.string.GroundStationPausingError) + " " + result));
			}
		});
	}

	public void ResumeTask()
	{
		object.continueGroundStationTask(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Success)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TASK_RESUMED, ""));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.INFO_MESSAGE, context.getString(R.string.GroundStationResumingError) + " " + result));
			}
		});
	}

	public void GoHome()
	{
		object.goHome(new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result == GroundStationResult.GS_Result_Success)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_GO_HOME, ""));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.INFO_MESSAGE, context.getString(R.string.GroundStationGoHomeError) + " " + result));
			}
		});
	}

	private float takeOffAlt = 6.0f;

	public void AdjustAltitudeTo(float altitude)
	{
		takeOffAlt = altitude;
		isAltAdjustmentNeeded.set(true);
	}
	
	protected void AdjustAlt()
	{
		float altDelta = flyingInfo.altitude - takeOffAlt;
		
		if (altDelta > 0.0 && flyingInfo.altitude > 1.5)
			gsWrapper.GetJoystik().Bottom();
		else if (altDelta < takeOffAlt)
			gsWrapper.GetJoystik().Top();

		if (altDelta >= 0.0f && altDelta <= 1.0f)
		{
			isAltAdjustmentNeeded.set(false);
			gsWrapper.GetJoystik().StopLeftJoystik();
			uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GROUNDSTATION_TAKE_OFF_DONE, context.getString(R.string.AltAdjusted)));
		}
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
