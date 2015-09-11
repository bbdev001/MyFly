package com.my.fly;

import java.util.HashMap;

import com.my.fly.utilities.WayPoint;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class WayPointEditorBuiltin
{
	private SeekBar tracker = null;
	private LinearLayout wayPointFields = null;
	private WayPoint wayPoint = new WayPoint();
	protected EditText altitude;
	protected EditText heading;
	protected EditText camAngle;
	protected int currentId = 0;
	
	public WayPointEditorBuiltin(Activity parent)
	{
		tracker = (SeekBar) parent.findViewById(R.id.tracker);
		wayPointFields = (LinearLayout) parent.findViewById(R.id.wayPointFields);
		
		altitude = (EditText) parent.findViewById(R.id.altitude);
		heading = (EditText) parent.findViewById(R.id.heading);
		camAngle = (EditText) parent.findViewById(R.id.camAngle);
		
		tracker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
	
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				if (fromUser)
				{
					switch(currentId)
					{
						case R.id.altitude:
							altitude.setText(Integer.toString(progress));
							wayPoint.Alt = progress;
							break;
						case R.id.heading:
							heading.setText(Integer.toString(progress));
							wayPoint.Heading = progress; 
							break;
						case R.id.camAngle:
							progress = -progress;
							camAngle.setText(Integer.toString(progress));
							wayPoint.CamAngle = progress;
							break;
					}
				}
			}
		});
		
		altitude.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if (hasFocus)
				{
					tracker.setMax(300);
					tracker.setProgress(wayPoint.Alt);
					currentId = R.id.altitude;
					tracker.setVisibility(View.VISIBLE);
				}
			}
		});
		
		heading.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if (hasFocus)
				{
					tracker.setMax(360);
					tracker.setProgress(wayPoint.Heading);
					currentId = R.id.heading;
					tracker.setVisibility(View.VISIBLE);
				}
			}
		});
		
		camAngle.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if (hasFocus)
				{
					tracker.setMax(89);
					tracker.setProgress(Math.abs(wayPoint.CamAngle));
					currentId = R.id.camAngle;
					tracker.setVisibility(View.VISIBLE);
				}
			}
		});
		
		Hide();
	}
	
	public void SetWayPoint(WayPoint wayPoint)
	{
		this.wayPoint = wayPoint;
		
		altitude.setText(Integer.toString(wayPoint.Alt));
		heading.setText(Integer.toString(wayPoint.Heading));
		camAngle.setText(Integer.toString(wayPoint.CamAngle));
	}
	
	public void Show()
	{
		wayPointFields.setVisibility(View.VISIBLE);
	}

	public void Hide()
	{
		tracker.setVisibility(View.INVISIBLE);
		wayPointFields.setVisibility(View.INVISIBLE);
	}
}
