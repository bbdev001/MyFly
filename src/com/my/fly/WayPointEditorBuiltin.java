package com.my.fly;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class WayPointEditorBuiltin
{
	private SeekBar tracker = null;
	private LinearLayout wayPointFields = null;
	
	public WayPointEditorBuiltin(Activity parent)
	{
		tracker = (SeekBar) parent.findViewById(R.id.tracker);
		wayPointFields = (LinearLayout) parent.findViewById(R.id.wayPointFields);
		
		Hide();
	}
	
	public void Show()
	{
		tracker.setVisibility(View.VISIBLE);
		wayPointFields.setVisibility(View.VISIBLE);
	}

	public void Hide()
	{
		tracker.setVisibility(View.INVISIBLE);
		wayPointFields.setVisibility(View.INVISIBLE);
	}
}
