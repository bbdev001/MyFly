package com.my.fly;

import com.my.fly.utilities.WayPoint;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class WPEditor extends Dialog implements View.OnClickListener 
{
	public WayPoint wayPoint = new WayPoint();
	protected EditText altitude;
	protected EditText heading;
	protected EditText hoverTime;
	protected Spinner action;
	protected Spinner speed;

	public interface OnDialogClosedListener
	{
		public void OnClosed(boolean isCancel);
	}

	protected OnDialogClosedListener onClosedListener = null;
	protected Activity context = null;
	
	public WPEditor(Activity context, String title, WayPoint wayPoint, OnDialogClosedListener onClosedListener)
	{
		super(context);
		this.context = context;
		setContentView(R.layout.wp_editor);
		setTitle(title);
		
		int pos = 0;
		this.wayPoint = wayPoint;
		this.onClosedListener = onClosedListener;
		
		altitude = (EditText) findViewById(R.id.altitude);
		altitude.setText(Integer.toString(wayPoint.Alt));	
		
		heading = (EditText) findViewById(R.id.heading);
		heading.setText(Integer.toString(wayPoint.Heading));
		
		hoverTime = (EditText) findViewById(R.id.hoverTime);
		hoverTime.setText(Integer.toString(wayPoint.HoverTime));
		
		String[] dataAction = {"Nothing", "StartVideo", "StopVideo", "Start Photo", "Stop Photo"};
		ArrayAdapter<String> adapterAction = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, dataAction);
        adapterAction.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);			
		action = (Spinner) findViewById(R.id.action);
		action.setAdapter(adapterAction);
		action.setSelection(wayPoint.Action);

		String[] dataSpeed = {"2", "4", "6", "8", "10"};
		ArrayAdapter<String> adapterSpeed = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, dataSpeed);
        adapterSpeed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);			
		speed = (Spinner) findViewById(R.id.speed);
		speed.setAdapter(adapterSpeed);
		pos = adapterSpeed.getPosition(Integer.toString(wayPoint.Speed));
		if (pos > 0)
			speed.setSelection(pos);
		
		((Button)findViewById(R.id.btnOk)).setOnClickListener(this);
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);
		
		super.show();
	}

	@Override
	public void onClick(View v)
	{
		boolean result = false;
		if (v.getId() == R.id.btnOk)
		{
			wayPoint.Alt = Integer.parseInt(altitude.getText().toString());
			wayPoint.Heading = Integer.parseInt(heading.getText().toString());
			wayPoint.HoverTime = Integer.parseInt(hoverTime.getText().toString());
			wayPoint.Action = action.getSelectedItemPosition();
			wayPoint.Speed = Integer.parseInt(speed.getSelectedItem().toString());
		}
		else if (v.getId() == R.id.btnCancel)
			result = true;
		
		dismiss();
	
		onClosedListener.OnClosed(result);
	}
}
