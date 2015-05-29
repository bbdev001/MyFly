package com.dji.wrapper;

import android.os.Vibrator;

public class VibroController
{
	private Vibrator vibro = null;
	
	public VibroController(Vibrator vibro)
	{
		this.vibro = vibro;
	}
	
	private long lastVibroTime = 0;
	public void Vibrate(int vibroInterval, int delayInterval)
	{
		long currTime = System.currentTimeMillis();
		
		if ((currTime - lastVibroTime) > delayInterval)
		{
			lastVibroTime = currTime;
			vibro.vibrate(vibroInterval);
		}
	}
	
	public void Cancel()
	{
		vibro.cancel();
	}
}
