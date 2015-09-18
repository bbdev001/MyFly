package com.my.fly;

import java.util.HashMap;

import com.my.fly.utilities.WayPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class WayPointEditorBuiltin
{
	private SeekBar tracker = null;
	private LinearLayout wayPointFields = null;
	private WayPoint wayPoint = new WayPoint();
	private long wayPointIndex = -1;
	protected EditText altitude;
	protected EditText heading;
	protected EditText camAngle;
	protected TextView altLabel;
	protected TextView headLabel;
	protected TextView camLabel;
	protected Button cancel;
	protected Button delete;
	protected int currentId = 0;
	protected WayPoint oldValue = new WayPoint();
	protected boolean isMapping = false;
	private OnSavedListener onSaved = null;
	protected OnDeletedListener onDeleted = null;
	protected OnHeadingChangedListener onHeadingChanged = null;
	protected WayPointEditorBuiltin self = null;
	protected Activity parent = null;

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

		this.parent = parent;
		this.onSaved = onSaved;
		this.onDeleted = onDeleted;
		this.onHeadingChanged = onHeadingChanged;

		tracker = (SeekBar) parent.findViewById(R.id.tracker);
		wayPointFields = (LinearLayout) parent.findViewById(R.id.wayPointFields);

		altitude = (EditText) parent.findViewById(R.id.altitude);
		heading = (EditText) parent.findViewById(R.id.heading);
		camAngle = (EditText) parent.findViewById(R.id.camAngle);
		altLabel = (TextView) parent.findViewById(R.id.altitudeCaption);
		headLabel = (TextView) parent.findViewById(R.id.headingCaption);
		camLabel = (TextView) parent.findViewById(R.id.camAngleCaption);
		cancel = (Button) parent.findViewById(R.id.btnCancel);
		delete = (Button) parent.findViewById(R.id.btnDelete);

		cancel.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				oldValue.CopyTo(wayPoint);
				self.SetWayPoint(wayPoint, wayPointIndex, isMapping);
				self.onSaved.OnSaved(wayPoint, wayPointIndex);
			}
		});

		delete.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(self.parent);

				alertDialog.setTitle(self.parent.getString(R.string.ConfirmDelete));
				alertDialog.setMessage(self.parent.getString(R.string.AreYouSure));
				alertDialog.setPositiveButton(self.parent.getString(R.string.Yes), new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
						self.onDeleted.OnDeleted(wayPointIndex, isMapping);
						self.Hide();
					}
				});

				// Setting Negative "NO" Button
				alertDialog.setNegativeButton(self.parent.getString(R.string.No), null);

				// Showing Alert Message
				alertDialog.show();
			}
		});

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
					switch (currentId)
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
							camAngle.setText(Integer.toString(progress));
							wayPoint.CamAngle = -progress;
							break;
					}
				}
			}
		});

		View.OnLongClickListener onLongClickListener = new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				currentId = v.getId();
				String caption = "";
				
				switch (currentId)
				{
					case R.id.altitude:
						caption = self.parent.getString(R.string.AltitudeM);
						break;
					case R.id.heading:
						caption = self.parent.getString(R.string.HeadingD);
						break;
					case R.id.camAngle:
						caption = self.parent.getString(R.string.CamAngleD);
						break;
				}
				
				String data = ((EditText)v).getText().toString();				 
				final InputBox dialog = new InputBox(self.parent, self.parent.getString(R.string.EnterValue), caption, data, true, new InputBox.OnDialogClosedListener()
				{
					public void OnClosed(boolean isCancel, String result)
					{
						if (!isCancel)
						{
							int intResult = Integer.parseInt(result);
							switch (currentId)
							{
								case R.id.altitude:
									wayPoint.Alt = intResult;
									altitude.setText(result);
									break;
								case R.id.heading:
									wayPoint.Heading = intResult;
									heading.setText(result);
									self.onHeadingChanged.OnHeadingChanged(wayPointIndex, wayPoint.Heading);
									break;
								case R.id.camAngle:
									wayPoint.CamAngle = -intResult;
									camAngle.setText(result);
									break;
							}
							
							self.onSaved.OnSaved(wayPoint, self.wayPointIndex);
						}
					}
				});

				dialog.show();
				return false;
			}
		};

		View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener()
		{
			
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				if (!hasFocus)
					return;
				
				currentId = v.getId();
				
				switch(currentId)
				{
						case R.id.altitude:
							tracker.setMax(300);
							tracker.setProgress(wayPoint.Alt);
							break;
						case R.id.heading:
							tracker.setMax(360);
							tracker.setProgress(wayPoint.Heading);
							break;
						case R.id.camAngle:
							tracker.setMax(89);
							tracker.setProgress(Math.abs(wayPoint.CamAngle));
							break;				
				}
				
				tracker.setVisibility(View.VISIBLE);
			}
		};
		
		altitude.setOnLongClickListener(onLongClickListener);
		altitude.setOnFocusChangeListener(onFocusChangeListener);

		heading.setOnLongClickListener(onLongClickListener);
		heading.setOnFocusChangeListener(onFocusChangeListener);

		camAngle.setOnLongClickListener(onLongClickListener);
		camAngle.setOnFocusChangeListener(onFocusChangeListener);

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
		camAngle.setText(Integer.toString(Math.abs(wayPoint.CamAngle)));

		tracker.setMax(300);
		tracker.setProgress(wayPoint.Alt);
		currentId = R.id.altitude;
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
			headLabel.setVisibility(View.INVISIBLE);
			camLabel.setVisibility(View.INVISIBLE);
			delete.setVisibility(View.INVISIBLE);
		}
		else
		{
			heading.setVisibility(View.VISIBLE);
			camAngle.setVisibility(View.VISIBLE);
			headLabel.setVisibility(View.VISIBLE);
			camLabel.setVisibility(View.VISIBLE);
			delete.setVisibility(View.VISIBLE);
		}
	}

	public void Hide()
	{
		tracker.setVisibility(View.INVISIBLE);
		wayPointFields.setVisibility(View.INVISIBLE);
	}
}
