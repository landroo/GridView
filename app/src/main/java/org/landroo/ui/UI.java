package org.landroo.ui;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import android.view.MotionEvent;

// this class
// fire swipe event
// fire touch event
// fire double touch event
// fire hold event
// fire move event
// fire zoom event
// fire rotate event
public class UI 
{
	private static final String TAG = "UI";
	private static final float SWIPE_MIN_DISTANCE = 20;
    private static final float SWIPE_MAX_OFF_PATH = 250;
	private static final float SWIPE_THRESHOLD_VELOCITY = 20;
	private static final int TAP_TIME = 400;
	private static final int HOLD_TIME = 800;
	private static final int DOUBLE_TAP_TIME = 250;
	private static final int TAP_TRESHOLD = 5;
	
    private float lastX1 = 0;
    private float lastY1 = 0;
    private float lastX2 = 0;
    private float lastY2 = 0;
    private float moveX = 0;
    private float moveY = 0;
    private long lastTime = 0;
    private long dubleTime = 0;
    private int eventMode = 0;
    
    boolean isdouble = false;
    
    private Timer holdTimer = null;
    private long holdTime = 0;
    private boolean isHold = false;
    
    public class Finger
    {
    	int id;
    	int action;
    	float x;
    	float y;
    	
    	public void clear()
    	{
    		action = 0;
    		x = 0;
    		y = 0;
    	}
    }
    
    public Finger[] fingers = new Finger[10];
    
    private UIInterface callBack;
    
    public UI(UIInterface context)
    {
    	callBack = context;
    	
    	for(int i = 0; i < 10; i++) 
    		fingers[i] = new Finger();
    	
    	holdTimer = new Timer();
		holdTimer.scheduleAtFixedRate(new HoldTask(), 0, 1);
    }
    
    public boolean tapEvent(MotionEvent event)
    {
    	for(int i = 0; i < 10; i++) fingers[i].clear();
    	for(int id = 0; id < event.getPointerCount(); id++)
    	{
	        float x = event.getX(id);
	        float y = event.getY(id);
	        
	        isdouble = false;
	        // second finger
	        if(id == 1) isdouble = processSecTap(x, y, event.getAction(), id);
	        
	        // first finger
			if(id == 0 && !isdouble) processTap(x, y, event.getAction(), id);
			
			fingers[id].id = event.getPointerId(event.getActionIndex());
			fingers[id].action = event.getAction() & MotionEvent.ACTION_MASK;
			fingers[id].x = x;
			fingers[id].y = y;
    	}
    	
    	this.callBack.onFingerChange();
    	
        return false;
    }

    // process tap
    private boolean processTap(float x1, float y1, int action, int id)
    {
        float x2 = 0;
        float y2 = 0;
        long deltaTime = 0;
        long doubleDelta = 0;
		Date now = new Date();
		int eventType = 0;
		double distance = 0;
		float velocity = 0;
		
        switch(action) 
        {
        	case MotionEvent.ACTION_DOWN:
        		lastX1 = x1;
        		lastY1 = y1;
        		lastTime = now.getTime();
        		eventType = 1;
        		// always call down event
        		this.callBack.onDown(x1, y1);
        		break;
        	case MotionEvent.ACTION_MOVE:
        		moveX = x1;
        		moveY = y1;
        		// always call move event
       			this.callBack.onMove(x1, y1);
        		eventType = 2;
        		break;
        	case MotionEvent.ACTION_UP:
                deltaTime = now.getTime() - lastTime;
        		doubleDelta = now.getTime() - dubleTime;
        		x2 = lastX1;
        		y2 = lastY1;
        		eventType = 6;
        		// always call up event
        		this.callBack.onUp(x1, y1);
        		if(deltaTime < TAP_TIME)
                {
            		// call touch event
            		eventType = 3;
            		dubleTime = now.getTime();
                }
        		
                if(deltaTime > HOLD_TIME
                && x1 + TAP_TRESHOLD > lastX1 && x1 - TAP_TRESHOLD < lastX1
                && y1 + TAP_TRESHOLD > lastY1 && y1 - TAP_TRESHOLD < lastY1)
                {
            		// call hold event
                	eventType = 4;
                }

        		if(doubleDelta < DOUBLE_TAP_TIME 
        		&& x1 + TAP_TRESHOLD > lastX1 && x1 - TAP_TRESHOLD < lastX1
        		&& y1 + TAP_TRESHOLD > lastY1 && y1 - TAP_TRESHOLD < lastY1)
       			{
            		// call double touch event
        			eventType = 5;
        		}
        		break;
        }
        
        if(deltaTime > 0)
        {
            distance = Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
            velocity = (float)(distance / deltaTime) * 100;
            
            if (Math.abs(y1 - y2) < SWIPE_MAX_OFF_PATH)
            {
                if(x2 - x1 > SWIPE_MIN_DISTANCE && velocity > SWIPE_THRESHOLD_VELOCITY) 
                {
                    // call right to left swipe event
                	eventType = 7;
                }
                else if (x1 - x2 > SWIPE_MIN_DISTANCE && velocity > SWIPE_THRESHOLD_VELOCITY) 
                {
                    // call left to right swipe event
                	eventType = 8;
                }
            }
            
            if (Math.abs(x1 - x2) < SWIPE_MAX_OFF_PATH)
            {
                if(y1 - y2 > SWIPE_MIN_DISTANCE && velocity > SWIPE_THRESHOLD_VELOCITY) 
                {
                	// call up to down swipe event
                	eventType = 9;
                }
                else if (y2 - y1 > SWIPE_MIN_DISTANCE && velocity > SWIPE_THRESHOLD_VELOCITY)   
                {
                	// call down to up swipe event
                	eventType = 10;
                }
            }
        }
        
        switch(eventType)
        {
        	case 0:		// ???
        		break;
        	case 1:		// on down
        		holdTime = 0;
        		isHold = true;
        		break;
        	case 2:		// on move
        		break;
        	case 3:		// tap
        		this.callBack.onTap(x1, y1);
        		isHold = false;
        		break;
        	case 4:		// hold
        		this.callBack.onHold(x1, y1);
        		break;
        	case 5:		// double tap
        		this.callBack.onDoubleTap(x1, y1);
        		break;
        	case 6:		// on up
        		isHold = false;
        		break;
        	case 7:		// right to left
        		this.callBack.onSwipe(1, velocity, x1, y1, x2, y2);
        		break;
        	case 8:		// left to right
        		this.callBack.onSwipe(2, velocity, x1, y1, x2, y2);
        		break;
        	case 9:		// up to down
            	this.callBack.onSwipe(3, velocity, x1, y1, x2, y2);        		
        		break;
        	case 10:	// down to up
        		this.callBack.onSwipe(4, velocity, x1, y1, x2, y2);
        		break;
        }
        
    	return true;
    }
    
    // process secound tap
    private boolean processSecTap(float x1, float y1, int action, int id)
    {
    	float x2 = moveX;
    	float y2 = moveY;
    	x2 = lastX1;
    	y2 = lastY1;
    	
    	lastX2 = x1;
    	lastY2 = y1;
    	
    	eventMode = 0;
    	
    	if(action == MotionEvent.ACTION_POINTER_1_DOWN
    	|| action == MotionEvent.ACTION_POINTER_2_DOWN)
    	{
    		eventMode = 1;
    	}
    	if(action == MotionEvent.ACTION_MOVE)
    	{
    		eventMode = 2;
    	}
    	if(action == MotionEvent.ACTION_POINTER_1_UP
    	|| action == MotionEvent.ACTION_POINTER_2_UP)
    	{
    		eventMode = 3;
    	}
    	
    	//Log.i("UI", "Mode: " + eventMode + " x=" + x1 + " y=" + y1);
    	
    	if(eventMode != 0)
    	{
	    	float rotate = (float)getAng(x2, y2, x1, y1);
	    	this.callBack.onRotate(eventMode, x1, y1, rotate);
	    	
			float distance = (float)getDist(x1, y1, x2, y2);
			this.callBack.onZoom(eventMode, x1, y1, distance,  x2 - x1, y2 - y1);

	        return true;
    	}
    		
    	return false;
    }

    // return the angle
    private double getAng(double x1, double y1, double x2, double y2)
    {
		double nDelX = x2 - x1;
		double nDelY = y2 - y1;
		double nDe = 0;

		if (nDelX != 0)
		{
			nDe = 2 * Math.PI;
			nDe = nDe + Math.atan(nDelY / nDelX);
			if (nDelX <= 0)
			{
				nDe = Math.PI;
				nDe = nDe + Math.atan(nDelY / nDelX);
			}
			else
			{
				if (nDelY >= 0)
				{
					nDe = 0;
					nDe = nDe + Math.atan(nDelY / nDelX);
				}
			}
		}
		else
		{
			if (nDelY == 0)
			{
				nDe = 0;
			}
			else
			{
				if (nDelY < 0)
				{
					nDe = Math.PI;
				}
				nDe = nDe + Math.PI / 2;
			}
		}
	
    	return nDe / Math.PI * 180;
	}

    // return the distance
	private double getDist(double x1, double y1, double x2, double y2)
	{
		double nDelX = x2 - x1;
		double nDelY = y2 - y1;
		
       	return  Math.sqrt(nDelY * nDelY + nDelX * nDelX);
    }
	
	class HoldTask extends TimerTask
	{
		public void run()
		{
			if(isHold)
			{
				holdTime++;
				if(holdTime == 1500)
					callBack.onHold(lastX1, lastY1);
			}
		}
	}
}
