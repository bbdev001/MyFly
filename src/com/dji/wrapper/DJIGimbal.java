package com.dji.wrapper;

import com.my.fly.R;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.Gimbal.DJIGimbalAttitude;
import dji.sdk.interfaces.DJIGimbalErrorCallBack;
import dji.sdk.interfaces.DJIGimbalUpdateAttitudeCallBack;
import android.content.Context;
import android.os.Handler;

public class DJIGimbal
{
	public static final String TAG = "DJIBattary";
	private Handler uiHandler = null;
	private dji.sdk.api.Gimbal.DJIGimbal object = null;
	private DJIDroneType droneType = null;

	public DJIGimbal(DJIDroneType droneType, Context context, Handler handler)
	{
		uiHandler = handler;
		object = DJIDrone.getDjiGimbal();
		this.droneType = droneType;
	}
	
	public void Connect(int interval)
	{
		object.setGimbalUpdateAttitudeCallBack(new DJIGimbalUpdateAttitudeCallBack()
		{
            @Override
            public void onResult(DJIGimbalAttitude attitude) 
            {
                uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.GIMBAL_STATUS, attitude));
            }
            
        });
		
		object.setGimbalErrorCallBack(new DJIGimbalErrorCallBack()
		{
            @Override
            public void onError(int error)
            {
            	if (error != 0)
            		uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.INFO_MESSAGE, R.string.GimbalError + " " + error));
            }
        });
		
		StartUpdateTimer(interval);
	}
	
	public void Disconnect()
	{
		object.setGimbalUpdateAttitudeCallBack(null);
		object.setGimbalErrorCallBack(null);
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

