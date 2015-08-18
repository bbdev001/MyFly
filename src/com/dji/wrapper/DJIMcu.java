package com.dji.wrapper;

import com.my.fly.R;

import android.content.Context;
import android.os.Handler;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.api.MainController.DJIMainControllerTypeDef.DJIMcErrorType;
import dji.sdk.interfaces.DJIMcuErrorCallBack;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;

public class DJIMcu
{
	private Handler uiHandler = null;
	private Context context = null;
	private DJIMainControllerSystemState mcuState = null;
	private DJIDroneType droneType = null;
	private dji.sdk.api.MainController.DJIMainController object = null;
	
	public DJIMcu(DJIDroneType droneType, Context context, Handler handler)
	{
		this.context = context;
		uiHandler = handler;
		this.droneType = droneType;
		object = DJIDrone.getDjiMainController();
	}

	public void Connect(int updateInterval)
	{
		object.setMcuErrorCallBack(new DJIMcuErrorCallBack()
		{
			@Override
			public void onError(DJIMcErrorType error)
			{
				if (error != DJIMcErrorType.Mc_No_Error)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "MCU " + GetErrorDescriptionByErrorCode(error)));
			}
		});
		
		object.setMcuUpdateStateCallBack(new DJIMcuUpdateStateCallBack()
		{
			@Override
			public void onResult(DJIMainControllerSystemState state)
			{
				mcuState = state;
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.MCU_STATUS, state));
			}
		});

		StartUpdateTimer(updateInterval);
	}

	public void Disconnect()
	{
		object.stopUpdateTimer();	
		object.setMcuUpdateStateCallBack(null);
		object.setMcuErrorCallBack(null);
	}
	
	public boolean IsFlying()
	{
		if (mcuState == null)
			return false;

		return mcuState.isFlying;
	}

	public void StartUpdateTimer(int interval)
	{
		object.startUpdateTimer(interval);
	}

	public void StopUpdateTimer()
	{
		object.stopUpdateTimer();
	}

	private String GetErrorDescriptionByErrorCode(DJIMcErrorType errCode)
	{
		String result = "";

		if (errCode == DJIMcErrorType.Mc_Config_Error)
		{
			result = context.getString(R.string.MCU_CONFIG_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_SerialNum_Error)
		{
			result = context.getString(R.string.MCU_SERIALNUM_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Imu_Error)
		{
			result = context.getString(R.string.MCU_IMU_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_X1_Error)
		{
			result = context.getString(R.string.MCU_X1_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_X2_Error)
		{
			result = context.getString(R.string.MCU_X2_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Pmu_Error)
		{
			result = context.getString(R.string.MCU_PMU_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Transmitter_Error)
		{
			result = context.getString(R.string.MCU_TRANSMITTER_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Sensor_Error)
		{
			result = context.getString(R.string.MCU_SENSOR_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Compass_Error)
		{
			result = context.getString(R.string.MCU_COMPASS_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Imu_Calibration_Error)
		{
			result = context.getString(R.string.MCU_IMU_CALIBRATION_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Compass_Calibration_Error)
		{
			result = context.getString(R.string.MCU_COMPASS_CALIBRATION_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Transmitter_Calibration_Error)
		{
			result = context.getString(R.string.MCU_TRANSMITTER_CALIBRATION_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Invalid_Battery_Error)
		{
			result = context.getString(R.string.MCU_INVALID_BATTERY_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Invalid_Battery_Communication_Error)
		{
			result = context.getString(R.string.MCU_INVALID_BATTERY_COMMUNICATION_ERROR);
		}
		else if (errCode == DJIMcErrorType.Mc_Unknown_Error)
		{
			result = context.getString(R.string.MCU_UNKOWN_ERROR);
		}

		return result;
	}
}

