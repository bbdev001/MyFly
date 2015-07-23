package com.my.fly;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import geolife.android.navigationsystem.NavmiiControl;

public class RotationDetector
{
	public RotationDetector(View view, NavmiiControl navigationSystem)
	{
		this.view = view;
		this.navigationSystem = navigationSystem;
		ViewConfiguration config = ViewConfiguration.get(view.getContext());
		edgeSlop = config.getScaledEdgeSlop();
	}

	protected void handleStartProgressEvent(int actionCode, MotionEvent event)
	{
		switch (actionCode)
		{
			case MotionEvent.ACTION_POINTER_DOWN:
				resetState();
				prevEvent = MotionEvent.obtain(event);

				updateStateByEvent(event);

				sloppyGesture = isSloppyGesture(event);
				if (!sloppyGesture)
				{
					gestureInProgress = onRotateBegin();
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (!sloppyGesture)
				{
					break;
				}

				sloppyGesture = isSloppyGesture(event);
				if (!sloppyGesture)
				{
					gestureInProgress = onRotateBegin();
				}
				break;

			case MotionEvent.ACTION_POINTER_UP:
				if (!sloppyGesture)
				{
					break;
				}
				break;
		}
	}

	protected void handleInProgressEvent(int actionCode, MotionEvent event)
	{
		switch (actionCode)
		{
			case MotionEvent.ACTION_POINTER_UP:
				updateStateByEvent(event);

				if (!sloppyGesture)
				{
					onRotateEnd();
				}

				resetState();
				break;

			case MotionEvent.ACTION_CANCEL:
				if (!sloppyGesture)
				{
					onRotateEnd();
				}

				resetState();
				break;

			case MotionEvent.ACTION_MOVE:
				updateStateByEvent(event);

				if (currPressure / prevPressure > PRESSURE_THRESHOLD)
				{
					final boolean updatePrevious = onRotate();
					if (updatePrevious)
					{
						prevEvent.recycle();
						prevEvent = MotionEvent.obtain(event);
					}
				}
				break;
		}
	}

	protected void updateStateByEvent(MotionEvent curr)
	{
		final MotionEvent prev = prevEvent;

		// Reset mCurrEvent
		if (currEvent != null)
		{
			currEvent.recycle();
			currEvent = null;
		}
		currEvent = MotionEvent.obtain(curr);

		// Pressure
		currPressure = curr.getPressure(curr.getActionIndex());
		prevPressure = prev.getPressure(prev.getActionIndex());

		// Previous
		final float px0 = prev.getX(0);
		final float py0 = prev.getY(0);
		final float px1 = prev.getX(1);
		final float py1 = prev.getY(1);
		final float pvx = px1 - px0;
		final float pvy = py1 - py0;
		prevFingerDiffX = pvx;
		prevFingerDiffY = pvy;

		// Current
		final float cx0 = curr.getX(0);
		final float cy0 = curr.getY(0);
		final float cx1 = curr.getX(1);
		final float cy1 = curr.getY(1);
		final float cvx = cx1 - cx0;
		final float cvy = cy1 - cy0;
		currFingerDiffX = cvx;
		currFingerDiffY = cvy;
	}

	public float getRotationDegreesDelta()
	{
		double diffRadians = Math.atan2(prevFingerDiffY, prevFingerDiffX) - Math.atan2(currFingerDiffY, currFingerDiffX);
		return (float) (diffRadians * 180 / Math.PI);
	}

	protected void resetState()
	{
		if (prevEvent != null)
		{
			prevEvent.recycle();
			prevEvent = null;
		}
		if (currEvent != null)
		{
			currEvent.recycle();
			currEvent = null;
		}
		gestureInProgress = false;
		sloppyGesture = false;
	}

	protected float getRawX(MotionEvent event, int pointerIndex)
	{
		float offset = event.getX() - event.getRawX();
		if (pointerIndex < event.getPointerCount())
		{
			return event.getX(pointerIndex) + offset;
		}
		return 0f;
	}

	protected float getRawY(MotionEvent event, int pointerIndex)
	{
		float offset = event.getY() - event.getRawY();
		if (pointerIndex < event.getPointerCount())
		{
			return event.getY(pointerIndex) + offset;
		}
		return 0f;
	}

	protected boolean isSloppyGesture(MotionEvent event)
	{
		rightSlopEdge = view.getWidth() - edgeSlop;
		bottomSlopEdge = view.getHeight() - edgeSlop;

		final float rightSlop = rightSlopEdge;
		final float bottomSlop = bottomSlopEdge;

		final float x0 = event.getRawX();
		final float y0 = event.getRawY();
		final float x1 = getRawX(event, 1);
		final float y1 = getRawY(event, 1);

		boolean p0sloppy = x0 < edgeSlop || y0 < edgeSlop || x0 > rightSlop || y0 > bottomSlop;
		boolean p1sloppy = x1 < edgeSlop || y1 < edgeSlop || x1 > rightSlop || y1 > bottomSlop;

		if (p0sloppy && p1sloppy)
		{
			return true;
		}
		else if (p0sloppy)
		{
			return true;
		}
		else if (p1sloppy)
		{
			return true;
		}
		return false;
	}

	public boolean onTouchEvent(MotionEvent event)
	{
		final int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
		if (!gestureInProgress)
		{
			handleStartProgressEvent(actionCode, event);
		}
		else
		{
			handleInProgressEvent(actionCode, event);
		}
		return true;
	}

	public boolean onRotateBegin()
	{
		doRotation = true;
		rotationDegree = 0.0f;
		numUpdates = 0;
		return true;
	}

	public boolean onRotate()
	{
		if (!doRotation)
			return false;

		rotationDegree += getRotationDegreesDelta();

		if (numUpdates++ >= updateRate)
		{
			numUpdates = 0;
			applyRotation();
		}
		return true;
	}

	public void onRotateEnd()
	{
		rotationDegree += getRotationDegreesDelta();
		applyRotation();
		doRotation = false;
	}

	public boolean isInProgress()
	{
		return gestureInProgress;
	}

	private void applyRotation()
	{
		navigationSystem.SetMapRotation(navigationSystem.GetMapRotation() - rotationDegree);
		rotationDegree = 0.0f;
	}

	protected static final float PRESSURE_THRESHOLD = 0.67f;

	private final NavmiiControl navigationSystem;

	private final View view;

	private boolean doRotation = false;
	private boolean sloppyGesture;

	private final float edgeSlop;
	private float rightSlopEdge;
	private float bottomSlopEdge;

	protected float prevFingerDiffX;
	protected float prevFingerDiffY;
	protected float currFingerDiffX;
	protected float currFingerDiffY;

	protected boolean gestureInProgress;

	protected MotionEvent prevEvent;
	protected MotionEvent currEvent;

	protected float currPressure;
	protected float prevPressure;

	private float rotationDegree = 0.0f;

	private int numUpdates;

	private static final int updateRate = 5;
}
