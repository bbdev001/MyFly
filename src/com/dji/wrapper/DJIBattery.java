package com.dji.wrapper;

import android.content.Context;
import android.os.Handler;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.Battery.DJIBatteryProperty;
import dji.sdk.api.Camera.DJIPhantomCamera;
import dji.sdk.interfaces.DJIBatteryUpdateInfoCallBack;

public class DJIBattery
{
	public static final String TAG = "DJIBattary";
	private Handler uiHandler = null;
	private dji.sdk.api.Battery.DJIBattery object = null;
	private DJIDroneType droneType = null;

	public DJIBattery(DJIDroneType droneType, Context context, Handler handler)
	{
		uiHandler = handler;
		object = DJIDrone.getDjiBattery();
		this.droneType = droneType;
	}
	
	public void Connect(int interval)
	{
		object.setBatteryUpdateInfoCallBack(new DJIBatteryUpdateInfoCallBack()
		{
			@Override
			public void onResult(DJIBatteryProperty state)
			{
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.BATTERY_STATUS, state));
			}
		});
		
		StartUpdateTimer(interval);
	}
	
	public void Disconnect()
	{
		object.setBatteryUpdateInfoCallBack(null);
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
