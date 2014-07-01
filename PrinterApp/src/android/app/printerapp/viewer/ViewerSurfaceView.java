package android.app.printerapp.viewer;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ViewerSurfaceView extends GLSurfaceView{
	//View Modes
	public static final int NORMAL = 0;
	public static final int XRAY = 1;
	public static final int TRANSPARENT = 2;
	public static final int LAYERS = 3;
	
	ViewerRenderer mRenderer;
	
	//Touch
	private final float TOUCH_SCALE_FACTOR_ROTATION = 90.0f / 320;  //180.0f / 320;
	private float mPreviousX;
	private float mPreviousY;
	
   // zoom rate (larger > 1.0f > smaller)
	private float pinchScale = 1.0f;

	private PointF pinchStartPoint = new PointF();
	private float pinchStartY = 0.0f;
	private float pinchStartZ = 0.0f;
	private float pinchStartDistance = 0.0f;

	// for touch event handling
	private static final int TOUCH_NONE = 0;
	private static final int TOUCH_DRAG = 1;
	private static final int TOUCH_ZOOM = 2;
	private static final int TOUCH_LONG_PRESS = 3;
	private int touchMode = TOUCH_NONE;
	
	private static final int MIN_CLICK_DURATION = 1000;
	private long mStartClickTime;
	private boolean mLongClickActive = false;
	
	//for buttons pressed
	public static final int ROTATION_MODE =0;
	public static final int TRANSLATION_MODE = 1;
	public static final int LIGHT_MODE = 2;
	
	private int mMovementMode = 0;
	
	//Edition mode
	private boolean mEdition = false;


	
	public ViewerSurfaceView(Context context) {
	    super(context);
	}
	public ViewerSurfaceView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	}
	
	public ViewerSurfaceView(Context context, DataStorage data, int state, boolean doSnapshot, boolean stl) {
		super(context);
		
		// Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
      
		mRenderer = new ViewerRenderer (data, context, state, doSnapshot, stl);
		setRenderer(mRenderer);
		
		// Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	public void configViewMode (int state) {
		switch (state) {
		case (ViewerSurfaceView.NORMAL):
			setXray(false);
			setTransparent(false);
			break;			
		case (ViewerSurfaceView.XRAY):
			setXray(true);
			setTransparent(false);
			break;
		case (ViewerSurfaceView.TRANSPARENT):
			setXray(false);
			setTransparent(true);
			break;
		}
		
		requestRender();
	}
	
	
	public void showBackWitboxFace () {
		if (mRenderer.getShowBackWitboxFace()) mRenderer.showBackWitboxFace(false);
		else mRenderer.showBackWitboxFace(true);	
		requestRender();		
	}
	
	public void showRightWitboxFace () {
		if (mRenderer.getShowRightWitboxFace()) mRenderer.showRightWitboxFace(false);
		else mRenderer.showRightWitboxFace(true);
		requestRender();		
	}
	
	public void showLeftWitboxFace () {
		if (mRenderer.getShowLeftWitboxFace()) mRenderer.showLeftWitboxFace(false);
		else mRenderer.showLeftWitboxFace(true);
		requestRender();		
	}
	
	public void showDownWitboxFace () {
		if (mRenderer.getShowDownWitboxFace()) mRenderer.showDownWitboxFace(false);
		else mRenderer.showDownWitboxFace(true);
		requestRender();		
	}
	
	public void setTransparent (boolean trans) {
		mRenderer.setTransparent(trans);
	}
	
	public void setXray (boolean xray) {
		mRenderer.setXray(xray);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
        float y = event.getY();
        
        float normalizedX = (event.getX() / (float) mRenderer.getWidthScreen()) * 2 - 1;
		float normalizedY = -((event.getY() / (float) mRenderer.getHeightScreen()) * 2 - 1);
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			// starts pinch
			case MotionEvent.ACTION_POINTER_DOWN:
				if (event.getPointerCount() >= 2) {
					pinchStartDistance = getPinchDistance(event);
					pinchStartY = mRenderer.getCameraPosY();
					pinchStartZ = mRenderer.getCameraPosZ();
					if (pinchStartDistance > 50f) {
						getPinchCenterPoint(event, pinchStartPoint);
						mPreviousX = pinchStartPoint.x;
						mPreviousY = pinchStartPoint.y;
						touchMode = TOUCH_ZOOM;
					}
				}
				break;
				
			case MotionEvent.ACTION_DOWN:				
				if (mEdition) 
					if (!mRenderer.touchPoint(normalizedX, normalizedY)) {
						mEdition= false;	
						requestRender();
					}
								
				
				if (touchMode == TOUCH_NONE && event.getPointerCount() == 1) {
					touchMode = TOUCH_DRAG;
					mPreviousX = event.getX();
					mPreviousY = event.getY();
				}
								
				mStartClickTime = event.getEventTime();
				mLongClickActive = true;
				
				break;
			
			case MotionEvent.ACTION_MOVE:				
					long clickDuration = event.getEventTime() - mStartClickTime;
					if (clickDuration >= MIN_CLICK_DURATION && mLongClickActive) touchMode = TOUCH_LONG_PRESS;
					
					if (touchMode == TOUCH_ZOOM && pinchStartDistance > 0) {
						// on pinch
						PointF pt = new PointF();
						getPinchCenterPoint(event, pt);
						
						mPreviousX = pt.x;
						mPreviousY = pt.y;
										
						pinchScale = getPinchDistance(event) / pinchStartDistance;
						mRenderer.setCameraPosY(pinchStartY / pinchScale);
						mRenderer.setCameraPosZ(pinchStartZ / pinchScale);

						requestRender();

						
					}else if (touchMode == TOUCH_DRAG) {
						float dx = x - mPreviousX;
				        float dy = y - mPreviousY;
				        
				        
				        mPreviousX = x;
					    mPreviousY = y;
						switch (mMovementMode) {
						case ROTATION_MODE:
							doRotation (dx,dy);
							break;
						case TRANSLATION_MODE:
							doTranslation (dx,dy);
							break;
						case LIGHT_MODE:
			                if (y < getHeight() * 3/4) {
			                    dy = 1;
			                } else {
			                	dy = -1;
			                }

			                if (x < getWidth() / 2) {
			                	dx = -1;
			                } else {
			                	dx = 1;
			                }
							doLight (dx, dy);
							break;
						}
						
					} else if (touchMode == TOUCH_LONG_PRESS && !mEdition && mLongClickActive) {
						mLongClickActive = false;
						normalizedX = (event.getX() / (float) mRenderer.getWidthScreen()) * 2 - 1;
						normalizedY = -((event.getY() / (float) mRenderer.getHeightScreen()) * 2 - 1);
						if (mRenderer.touchPoint(normalizedX, normalizedY)) mEdition = true;		
					} 
					
					requestRender();
								    
	                break;
			
			// end pinch
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				if (touchMode == TOUCH_ZOOM) {				
					pinchScale = 1.0f;
					pinchStartPoint.x = 0.0f;
					pinchStartPoint.y = 0.0f;
				}
				touchMode = TOUCH_NONE;
				break;				
		}
		return true;
	}
	
	private void doRotation (float dx, float dy) {              
        mRenderer.setSceneAngleX(dx*TOUCH_SCALE_FACTOR_ROTATION);
        mRenderer.setSceneAngleY(dy*TOUCH_SCALE_FACTOR_ROTATION);		
	} 
	
	private void doTranslation(float dx, float dy) {
		mRenderer.setCenterX(-dx);
		mRenderer.setCenterZ(dy); //
	}
	
	private void doLight (float dx, float dy) {              
        mRenderer.setLightVector(dx, dy);		
	} 
	
	public void setMovementMode (int mode) {
		mMovementMode = mode;
	}
	
	private float getPinchDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	
	/**
	 * 
	 * @param event
	 * @param pt pinched point
	 */
	private void getPinchCenterPoint(MotionEvent event, PointF pt) {
		pt.x = (event.getX(0) + event.getX(1)) * 0.5f;
		pt.y = (event.getY(0) + event.getY(1)) * 0.5f;
	}
}

