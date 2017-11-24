package org.landroo.ui;

public interface UIInterface 
{
	// fire down event
	public void onDown(float x, float y);
	// fire up event
	public void onUp(float x, float y);
	// fire tap event
	public void onTap(float x, float y);
	// fire hold event	
	public void onHold(float x, float y);
	// fire move event	
	public void onMove(float x, float y);
	// fire swipe event
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2);
	// fire double tap event
	public void onDoubleTap(float x, float y);
	// fire zoom event
	public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff);
	// fire rotate event
	public void onRotate(int mode, float x, float y, float angle);
	// fire change event
	public void onFingerChange();
}
