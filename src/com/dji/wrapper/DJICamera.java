package com.dji.wrapper;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
//import android.os.Vibrator;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef.DJIDroneType;
import dji.sdk.api.DJIError;
import dji.sdk.api.Camera.DJICameraFileNamePushInfo;
import dji.sdk.api.Camera.DJICameraPlaybackState;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraCaptureMode;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraMode;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraPhotoFormatType;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraPhotoQualityType;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraPhotoRatioType;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraPhotoSizeType;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraPreviewResolustionType;
import dji.sdk.api.Camera.DJICameraSettingsTypeDef.CameraVisionType;
import dji.sdk.api.Camera.DJICameraTypeDef;
import dji.sdk.api.Camera.DJIPhantomCamera;
import dji.sdk.api.Gimbal.DJIGimbalRotation;
import dji.sdk.interfaces.DJICameraFileNameInfoCallBack;
import dji.sdk.interfaces.DJICameraPlayBackStateCallBack;
import dji.sdk.interfaces.DJIExecuteResultCallback;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;
import dji.sdk.widget.DjiGLSurfaceView;

public class DJICamera
{
	public static final String TAG = "DJICamera";
	private DjiGLSurfaceView mDjiGLSurfaceView = null;
	private DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = null;
	// private Vibrator vibro = null;
	private int pitchSpeed = 150;
	private Handler uiHandler = null;
	private dji.sdk.api.Camera.DJICamera object = null;
	private DJIDroneType droneType = null;

	public class CameraFileInfo
	{
		public DJICameraTypeDef.CameraFileNamePushType Type;
		public String Path = "";
		public String Name = "";
		
		public CameraFileInfo(DJICameraTypeDef.CameraFileNamePushType type, String path, String name)
		{
			Type = type;
			Path = path;
			Name = name;
		}
	};
	
	
	public DJICamera(DJIDroneType droneType, Context context, Handler handler)
	{
		// vibro = (Vibrator)
		// context.getSystemService(Context.VIBRATOR_SERVICE);
		this.droneType = droneType;
		uiHandler = handler;
		object = DJIDrone.getDjiCamera();
	}

	public void Connect(DjiGLSurfaceView surface, int updateInterval)
	{
		mDjiGLSurfaceView = surface;
		mDjiGLSurfaceView.start();
		mDjiGLSurfaceView.setStreamType(CameraPreviewResolustionType.Resolution_Type_640x480_15fps);
		mDjiGLSurfaceView.invalidate();
		
		object.setReceivedVideoDataCallBack(new DJIReceivedVideoDataCallBack()
		{
			@Override
			public void onResult(byte[] videoBuffer, int size)
			{
				try
				{
					mDjiGLSurfaceView.setDataToDecoder(videoBuffer, size);
				}
				catch(Exception e)
				{
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "Camera " + e.getMessage()));	
				}
			}
		});

		object.setDjiCameraFileNameInfoCallBack(new DJICameraFileNameInfoCallBack()
		{
			@Override
			public void onResult(final DJICameraFileNamePushInfo Info)
			{
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.CAMERA_FILE_INFO, new CameraFileInfo(Info.type, Info.filePath, Info.fileName)));
			}
		});

		object.setDJICameraPlayBackStateCallBack(new DJICameraPlayBackStateCallBack()
		{
			@Override
			public void onResult(DJICameraPlaybackState mState)
			{
				Log.d(TAG, "Play back");
			}
		});

		object.setCameraPhotoQuality(CameraPhotoQualityType.Camera_Photo_Quality_Normal , new DJIExecuteResultCallback()
		{
			@Override 
			public void onResult(DJIError result)
			{
				if (result.errorCode != DJIError.RESULT_OK)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "Camera quality error " + result.errorCode));
			}
		});

		object.setCameraPhotoFormat(CameraPhotoFormatType.Camera_Photo_JPEG , new DJIExecuteResultCallback()
		{
			@Override 
			public void onResult(DJIError result)
			{
					if (result.errorCode != DJIError.RESULT_OK)
						uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "Camera format error " + result.errorCode));				
			}
		});
		
		/*object.setCameraPhotoSizeAndRatio(CameraPhotoSizeType.Camera_Photo_Size_4384x2922, CameraPhotoRatioType.Camera_Photo_Ratio_4_3, new DJIExecuteResultCallback()
		{
			@Override 
			public void onResult(DJIError result)
			{
					if (result.errorCode != DJIError.RESULT_OK)
						uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "Camera size error " + result.errorCode));								
			}
		});*/
		
		StartUpdateTimer(updateInterval);
	}

	public DjiGLSurfaceView GetSurfaceView()
	{
		return mDjiGLSurfaceView;
	}

	public void Disconnect()
	{
		object.setReceivedVideoDataCallBack(null);
		object.setDjiCameraFileNameInfoCallBack(null);
		object.setDJICameraPlayBackStateCallBack(null);
		StopUpdateTimer();
	}

	private boolean checkCameraStatus = false;

	public void StartUpdateTimer(int interval)
	{
		CameraMode camMode = droneType == DJIDroneType.DJIDrone_Vision ? CameraMode.Camera_Camera_Mode : CameraMode.Camera_Capture_Mode;
		object.setCameraMode(camMode, new DJIExecuteResultCallback()
		{
			@Override
			public void onResult(DJIError state)
			{
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "Camera " + state.errorCode + " " + DJIError.getErrorDescriptionByErrcode(state.errorCode)));
			}
		});
		
		object.startUpdateTimer(interval);
		checkCameraStatus = true;

		new Thread()
		{
			@Override
			public void run()
			{
				while (checkCameraStatus)
				{
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.CHECK_CAMERA_STATE, ""));
					DJIWrapper.Sleep(500);
				}
			}
		}.start();
	}

	public void StopUpdateTimer()
	{
		checkCameraStatus = false;
		object.stopUpdateTimer();
	}

	public boolean IsConnectedOk()
	{
		return object.getCameraConnectIsOk();
	}

	public void PitchUpStart()
	{
		DJIGimbalRotation pitch = null;
		if (DJIDrone.getDjiCamera().getVisionType() == CameraVisionType.Camera_Type_Plus)
			pitch = new DJIGimbalRotation(true, true, false, pitchSpeed);
		else
			pitch = new DJIGimbalRotation(true, true, false, pitchSpeed / 5);

		DJIDrone.getDjiGimbal().updateGimbalAttitude(pitch, null, null);
	}

	public void PitchDownStart()
	{
		DJIGimbalRotation pitch = null;
		if (DJIDrone.getDjiCamera().getVisionType() == CameraVisionType.Camera_Type_Plus)
			pitch = new DJIGimbalRotation(true, false, false, pitchSpeed);
		else
			pitch = new DJIGimbalRotation(true, false, false, pitchSpeed / 5);

		DJIDrone.getDjiGimbal().updateGimbalAttitude(pitch, null, null);
	}

	public void PitchStop()
	{
		DJIDrone.getDjiGimbal().updateGimbalAttitude(new DJIGimbalRotation(false, false, false, 0), null, null);
	}

	public void TakePhoto()
	{
		object.startTakePhoto(CameraCaptureMode.Camera_Single_Capture, new DJIExecuteResultCallback()
		{
			@Override
			public void onResult(DJIError err)
			{
				uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.CAMERA_TAKE_PHOTO, "Camera " + err.errorCode + " " + DJIError.getErrorDescriptionByErrcode(err.errorCode)));
			}
		});
	}

	public void StopTakePhoto()
	{
		object.stopTakePhoto(new DJIExecuteResultCallback()
		{
			@Override
			public void onResult(DJIError err)
			{
				if (err.errorCode == DJIError.RESULT_OK || err.errorCode == DJIError.RESULT_SUCCEED)
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.CAMERA_TAKE_PHOTO_DONE, DJIError.getErrorDescriptionByErrcode(err.errorCode)));
				else
					uiHandler.sendMessage(uiHandler.obtainMessage(DJIWrapper.ERROR_MESSAGE, "Camera " + err.errorCode + " " + DJIError.getErrorDescriptionByErrcode(err.errorCode)));
			}
		});
	}
}
