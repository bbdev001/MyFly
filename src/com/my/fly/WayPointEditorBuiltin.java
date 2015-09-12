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
	private long wayPointIndex = -1;
	protected EditText altitude;
	protected EditText heading;
	protected EditText camAngle;
	protected int currentId = 0;
	protected WayPoint oldValue = new WayPoint();
	protected boolean isMapping = false;
	private OnSavedListener onSaved = null;
	protected OnDeletedListener onDeleted = null;
	protected OnHeadingChangedListener onHeadingChanged = null;
	protected WayPointEditorBuiltin self = null;
	
	public interface OnSavedListener
	{
		public void OnSaved(WayPoint wayPoint, long wayPointIndex);
	}

	public interface OnDeletedListener
	{
		public void OnDeleted(long wayPointIndex, boolean isMapping);
	}
	
	public interface OnHeadingChangedListener
	{
		public void OnHeadingChanged(long wayPointIndex, int heading);
	}
	
	public WayPointEditorBuiltin(Activity parent, OnSavedListener onSaved, OnDeletedListener onDeleted, OnHeadingChangedListener onHeadingChanged)
	{
		self = this;
		
		this.onSaved = onSaved;
		this.onDeleted = onDeleted;
		this.onHeadingChanged = onHeadingChanged;
		
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
				self.onSaved.OnSaved(wayPoint, self.wayPointIndex);
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
							self.onHeadingChanged.OnHeadingChanged(wayPointIndex, wayPoint.Heading);
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
	
	public void SetWayPoint(WayPoint point, long pointIndex, boolean isMapping)
	{
		this.isMapping = isMapping;
		this.wayPointIndex = pointIndex;
		this.wayPoint = point;
		this.wayPoint.CopyTo(oldValue);

		altitude.setText(Integer.toString(wayPoint.Alt));
		heading.setText(Integer.toString(wayPoint.Heading));
		camAngle.setText(Integer.toString(wayPoint.CamAngle));
		
		altitude.setFocusableInTouchMode(true);
		altitude.requestFocus();
	}
	
	public void Show()
	{
		wayPointFields.setVisibility(View.VISIBLE);
		tracker.setVisibility(View.VISIBLE);
		
		if (isMapping)
		{
			heading.setVisibility(View.INVISIBLE);
			camAngle.setVisibility(View.INVISIBLE);
		}
		else
		{
			heading.setVisibility(View.VISIBLE);
			camAngle.setVisibility(View.VISIBLE);	
		}
	}

	public void Hide()
	{
		tracker.setVisibility(View.INVISIBLE);
		wayPointFields.setVisibility(View.INVISIBLE);
	}
}
