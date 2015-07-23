package com.my.fly;

import android.view.ScaleGestureDetector;
import geolife.android.navigationsystem.NavmiiControl;


public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
{
	public ScaleListener (NavmiiControl navigationSystem) {
		this.navigationSystem = navigationSystem;
	}
	
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
    	doZooming = true;
        scaleFactor = 1.0f;
        numUpdates = 0;
    	return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
    	if (!doZooming)
    		return false;

        scaleFactor *= detector.getScaleFactor();

        if (numUpdates++ >= updateRate) {
        	numUpdates = 0;
        	applyScale();
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    	applyScale();
    	doZooming = false;
    }

    private void applyScale() {
        float zoom = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
        scaleFactor = 1.0f;

        zoom = navigationSystem.GetMapZoom() / zoom;
        navigationSystem.SetMapZoom(zoom);
    }

    private final NavmiiControl navigationSystem;
    
    private boolean doZooming = false;
    
    private float scaleFactor;

    private int numUpdates;

    private static final int updateRate = 0;
}
