package com.dji.wrapper;

import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;

import com.dji.wrapper.DJIGroundStation.SpeedLimits;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef.GroundStationResult;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;

public class Joystik
{
	private Vibrator vibro = null;
	private float yaw = 0;
	private int throttle = 0;
	private float pitch = 0;
	private float roll = 0;
	private int vobroTime = 20;
	private int vibroMultiplierDefault = 4;
	private float maxControlValue = 1000.0f;
	private float speedLimit = maxControlValue * ((float) SpeedLimits.SpeedLimitMiddle) / 100.0f;
	//private float yawSpeedLimit = 0.5f;
	private Handler uiHandler = null;

	public Joystik(Handler handler, Vibrator vibrator)
	{
		uiHandler = handler;
		this.vibro = vibrator;
	}

	public void RotLeftTop()
	{
		throttle = 1;
		yaw = -speedLimit;
		SendControl();
	}

	public void Top()
	{
		throttle = 1;
		yaw = 0;
		SendControl();
	}

	public void RotRightTop()
	{
		throttle = 1;
		yaw = speedLimit;
		SendControl();
	}

	public void RotLeft()
	{
		throttle = 0;
		yaw = -speedLimit;
		SendControl();
	}

	public void RotRight()
	{
		throttle = 0;
		yaw = speedLimit;
		SendControl();
	}

	public void RotLeftBottom()
	{
		throttle = 2;
		yaw = -speedLimit;
		SendControl();
	}

	public void Bottom()
	{
		throttle = 2;
		yaw = 0;
		SendControl();
	}

	public void RotRightBottom()
	{
		throttle = 2;
		yaw = speedLimit;
		SendControl();
	}

	public void StrafeLeftForward()
	{
		pitch = speedLimit;
		roll = -speedLimit;
		SendControl();
	}

	public void Forward()
	{
		pitch = speedLimit;
		roll = 0;
		SendControl();
	}

	public void StrafeRightForward()
	{
		pitch = speedLimit;
		roll = speedLimit;
		SendControl();
	}

	public void StrafeLeft()
	{
		pitch = 0;
		roll = -speedLimit;
		SendControl();
	}

	public void StrafeRight()
	{
		pitch = 0;
		roll = speedLimit;
		SendControl();
	}

	public void StrafeLeftBackward()
	{
		pitch = -speedLimit;
		roll = -speedLimit;
		SendControl();
	}

	public void Backward()
	{
		pitch = -speedLimit;
		roll = 0;
		SendControl();
	}

	public void StrafeRightBackward()
	{
		pitch = -speedLimit;
		roll = speedLimit;
		SendControl();
	}

	public void StopRightJoystik()
	{
		pitch = 0;
		roll = 0;

		SendControl();
	}

	public void StopLeftJoystik()
	{
		yaw = 0;
		throttle = 0;

		SendControl();
	}

	public void SetSpeedLimit(int speedLimitPercentOfMax)
	{
		if (speedLimitPercentOfMax > 100)
			speedLimitPercentOfMax = 100;
		else if (speedLimitPercentOfMax < 0)
			speedLimitPercentOfMax = 0;
		
		speedLimit = maxControlValue * ((float) speedLimitPercentOfMax / 100.0f);
		VibroController vibroController = new VibroController(vibro);
		vibroController.Vibrate(vobroTime, vobroTime * vibroMultiplierDefault);
	}

	protected boolean isControlThreadStarted = false;

	protected int controllCount = 0;
	protected void SendControl()
	{
		DJIDrone.getDjiGroundStation().setAircraftJoystick((int)Math.round(yaw), (int)Math.round(pitch), (int)Math.round(roll), (int)throttle, new DJIGroundStationExecuteCallBack()
		{
			@Override
			public void onResult(GroundStationResult result)
			{
				if (result != GroundStationResult.GS_Result_Success)
				{
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.INFO_MESSAGE, result.toString()));
					Log.e("Joystik", result.toString());
				}
			}
		});
	}
}
