package com.dji.wrapper;

import android.content.Context;
import android.os.Handler;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.RemoteController.DJIRemoteControllerAttitude;
import dji.sdk.interfaces.DJIRemoteControllerUpdateAttitudeCallBack;

public class DJIRemoteController
{
	public static final String TAG = "DJIRemoteController";
	private Handler uiHandler = null;
	private dji.sdk.api.RemoteController.DJIRemoteController object = null;
	private DJIDroneType droneType = null;

	public DJIRemoteController(DJIDroneType droneType, Context context, Handler handler)
	{
		uiHandler = handler;
		object = DJIDrone.getDjiRemoteController();
		this.droneType = droneType;
	}
	
	public void Connect(int interval)
	{
		object.setRemoteControllerUpdateAttitudeCallBack(new DJIRemoteControllerUpdateAttitudeCallBack()
		{
			@Override
			public void onResult(DJIRemoteControllerAttitude state)
			{
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.REMOTE_CONTROLLER_STATE, state));
				
			}
		});
		
		StartUpdateTimer(interval);
	}
	
	public void Disconnect()
	{
		object.setRemoteControllerUpdateAttitudeCallBack(null);
		StopUpdateTimer();
	}
	
	public void StartUpdateTimer(int interval)
	{
		object.startUpdateTimer(interval);
	}

	public void StopUpdateTimer()
	{
		object.stopUpdateTimer();
	}
}
