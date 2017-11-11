package org.tomweatherhead.mandelbrot;

import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
//import android.graphics.RectF;
//import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.RelativeLayout;
//import android.widget.Button;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 *
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
class MandelbrotView extends SurfaceView implements SurfaceHolder.Callback {
    class MandelbrotThread extends Thread {
        /*
         * State-tracking constants
         */
        public static final int STATE_DONE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;

        private static final String KEY_VIEW_LEFT = "viewLeft";
        private static final String KEY_VIEW_TOP = "viewTop";
        private static final String KEY_VIEW_WIDTH = "viewWidth";
        private static final String KEY_VIEW_HEIGHT = "viewHeight";
        private static final String KEY_ZOOM_EXPONENT = "zoomExponent";
        //private static final String KEY_ = "";

        /*
         * Member (state) fields
         */
        private Bitmap mMandelbrotBitmap;
    	private Canvas bitmapCanvas;
    	private boolean setToDefaultView = true;
    	
    	private Context mContext;
    	
        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode = STATE_READY;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        // **** Global Variable Declarations ****
        private int canvasWidthInPixels = 1;
        private int canvasHeightInPixels = 1;
        private double defaultViewLeft = -2.25;
        private double defaultViewTop = 1.5;
        private double defaultViewWidth = 3.0;
        private double defaultViewHeight = 3.0;
        private double viewLeft = 0.0;
        private double viewTop = 0.0;
        private double viewWidth = 0.0;
        private double viewHeight = 0.0;
        private int currentCanvasLeftInPixels = 0;
        private int currentCanvasTopInPixels = 0;
        private int currentCanvasWidthInPixels = 0;
        private double currentWidth = 0.0;
        private ArrayList<Paint> palette = new ArrayList<Paint>();
        private int maxRendersPerCall = 512;
        private int zoomExponent = 0;
        
        public MandelbrotThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            mMandelbrotBitmap = Bitmap.createBitmap(canvasWidthInPixels, canvasHeightInPixels, Bitmap.Config.ARGB_8888);
        	bitmapCanvas = new Canvas(mMandelbrotBitmap);
            
            constructPalette();
        }

        // **** Start of pasted Javascript code ****

        private void fillSquare(int left, int top, int width, int colourIndex) {
        	bitmapCanvas.drawRect(left, top, left + width, top + width, palette.get(colourIndex));
        }

        private void calculateAndFillSquare(double cr, double ci,
        		int canvasSquareLeft, int canvasSquareTop, int canvasSquareWidth)
        {
            int maxNumIterations = palette.size() - 1;
            double zr = cr;
            double zi = ci;
            int i = 0;

            for (; i < maxNumIterations; ++i)
            {
                double zr2 = zr * zr;
                double zi2 = zi * zi;

                if (zr2 + zi2 >= 4.0)
                {
                    break;
                }

                double tempzr = zr2 - zi2 + cr;

                zi = 2.0 * zr * zi + ci;
                zr = tempzr;
            }

            fillSquare(canvasSquareLeft, canvasSquareTop, canvasSquareWidth, i);
        }

        private void renderLoop() {
            int nextCanvasWidthInPixels = currentCanvasWidthInPixels / 2;
            double nextWidth = currentWidth / 2.0;

            for (int renderNum = 0; renderNum < maxRendersPerCall; ++renderNum)
            {
                double cr = currentCanvasLeftInPixels * viewWidth / canvasWidthInPixels + viewLeft;
                double ci = viewTop - currentCanvasTopInPixels * viewHeight / canvasHeightInPixels;

                // TAW 2011/05/30 : This next call should be unnecessary if the "progressive scan" algorithm is working properly.
                //calculateAndFillSquare(cxt, cr, ci,
                //    currentCanvasLeftInPixels, currentCanvasTopInPixels,
                //    nextCanvasWidthInPixels);

                calculateAndFillSquare(cr + nextWidth, ci,
                    currentCanvasLeftInPixels + nextCanvasWidthInPixels, currentCanvasTopInPixels,
                    nextCanvasWidthInPixels);
                calculateAndFillSquare(cr, ci - nextWidth,
                    currentCanvasLeftInPixels, currentCanvasTopInPixels + nextCanvasWidthInPixels,
                    nextCanvasWidthInPixels);
                calculateAndFillSquare(cr + nextWidth, ci - nextWidth,
                    currentCanvasLeftInPixels + nextCanvasWidthInPixels, currentCanvasTopInPixels + nextCanvasWidthInPixels,
                    nextCanvasWidthInPixels);

                currentCanvasLeftInPixels += currentCanvasWidthInPixels;

                if (currentCanvasLeftInPixels >= canvasWidthInPixels)
                {
                    currentCanvasLeftInPixels = 0;
                    currentCanvasTopInPixels += currentCanvasWidthInPixels;

                    if (currentCanvasTopInPixels >= canvasHeightInPixels)
                    {
                        currentCanvasTopInPixels = 0;
                        currentCanvasWidthInPixels = nextCanvasWidthInPixels;
                        currentWidth = nextWidth;

                        if (currentCanvasWidthInPixels <= 1)
                        {
                            // Rendering is complete.
                            setState(STATE_DONE);
                        } else {
                        	nextCanvasWidthInPixels /= 2;
                        	nextWidth /= 2.0;
                        	//break;  // Allow the display of the rendered image at this level of chunkiness.
                        }
                        
                        return;
                    }
                }
            }
        }

        private void constructPalette()
        {
            palette.clear();

            Paint paint = null;
            
            for (int i = 0; i <= 255; i += 5)
            {
            	paint = new Paint();
                paint.setARGB(255, 255, i, 0); 		// Red to Yellow
                palette.add(paint);
                
            	paint = new Paint();
                paint.setARGB(255, 0, 255, i);   	// Green to Cyan/Aqua
                palette.add(paint);

            	paint = new Paint();
                paint.setARGB(255, i, 0, 255);   	// Blue to Magenta/Fuchsia
                palette.add(paint);
            }

        	paint = new Paint();
            paint.setARGB(255, 0, 0, 0);     // Pixels within the Mandelbrot Set are coloured Black.
            palette.add(paint);
        }

        private void renderView()
        {
        	
        	if (setToDefaultView) {
        		setToDefaultView = false;

            	if (!constrainView(defaultViewLeft, defaultViewTop, defaultViewWidth, defaultViewHeight, 0)) {
                	setState(STATE_DONE);	// The default (home) view has already been rendered and is visible. 
            		return;
            	}
        	}

        	setState(STATE_RUNNING);
        	
            calculateAndFillSquare(viewLeft, viewTop, 0, 0, canvasWidthInPixels);

            currentCanvasLeftInPixels = 0;
            currentCanvasTopInPixels = 0;
            currentCanvasWidthInPixels = canvasWidthInPixels;
            currentWidth = viewWidth;
            renderLoop();
        }

        private boolean constrainView(double newViewLeft, double newViewTop,
        		double newViewWidth, double newViewHeight, int newZoomExponent) {

            if (newViewWidth > defaultViewWidth) {
                newViewWidth = defaultViewWidth;
            }

            if (newViewHeight > defaultViewHeight) {
                newViewHeight = defaultViewHeight;
            }

            if (newViewLeft < defaultViewLeft) {
                newViewLeft = defaultViewLeft;
            }

            double newViewRight = newViewLeft + newViewWidth;
            double defaultViewRight = defaultViewLeft + defaultViewWidth;

            if (newViewRight > defaultViewRight) {
                newViewLeft = defaultViewRight - newViewWidth;
            }

            if (newViewTop > defaultViewTop) {
                newViewTop = defaultViewTop;
            }

            double newViewBottom = newViewTop - newViewHeight;
            double defaultViewBottom = defaultViewTop - defaultViewHeight;

            if (newViewBottom < defaultViewBottom) {
                newViewTop = defaultViewBottom + newViewHeight;
            }

            if (newViewLeft == viewLeft && newViewTop == viewTop && newViewWidth == viewWidth && newViewHeight == viewHeight) {
                return false;
            }

            if (newZoomExponent < 0) {
                newZoomExponent = 0;
            }

            viewLeft = newViewLeft;
            viewTop = newViewTop;
            viewWidth = newViewWidth;
            viewHeight = newViewHeight;
            zoomExponent = newZoomExponent;

            return true;
        }

        private void onCanvasClick(int x, int y) {
            double cr = (double)x * viewWidth / (double)canvasWidthInPixels + viewLeft;
            double ci = viewTop - (double)y * viewHeight / (double)canvasHeightInPixels;

            double newViewWidth = viewWidth / 2.0;
            double newViewHeight = viewHeight / 2.0;

            //if (newViewWidth <= 0.0 || newViewHeight <= 0.0) {
            if (zoomExponent >= 50) {
            	setState(STATE_DONE, mContext.getResources().getText(R.string.message_epsilon));
                return;
            }

            double newViewLeft = cr - newViewWidth / 2.0;
            double newViewTop = ci + newViewHeight / 2.0;

            if (constrainView(newViewLeft, newViewTop, newViewWidth, newViewHeight, zoomExponent + 1)) {
            	setState(STATE_READY);
            }
        }

        public void goHome() {
            synchronized (mSurfaceHolder) {
            	setToDefaultView = true;
            	setState(STATE_READY);
            }
        }
        
        public void zoomOut() {
            synchronized (mSurfaceHolder) {
            	double cr = viewLeft + viewWidth / 2.0;
            	double ci = viewTop - viewHeight / 2.0;

            	double newViewWidth = viewWidth * 2.0;
            	double newViewHeight = viewHeight * 2.0;
            	double newViewLeft = cr - newViewWidth / 2.0;
            	double newViewTop = ci + newViewHeight / 2.0;

            	if (constrainView(newViewLeft, newViewTop, newViewWidth, newViewHeight, zoomExponent - 1)) {
            		setState(STATE_READY);
            	}
            }
        }
        
        // **** End of pasted Javascript code ****

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) {
                	setState(STATE_PAUSE /*, mContext.getResources().getText(R.string.message_paused) */ );
                }
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            setState(STATE_RUNNING);
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         *
         * @return Bundle with this view's state
         */
        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
                    map.putDouble(KEY_VIEW_LEFT, Double.valueOf(viewLeft));
                    map.putDouble(KEY_VIEW_TOP, Double.valueOf(viewTop));
                    map.putDouble(KEY_VIEW_WIDTH, Double.valueOf(viewWidth));
                    map.putDouble(KEY_VIEW_HEIGHT, Double.valueOf(viewHeight));
                    map.putInt(KEY_ZOOM_EXPONENT, Integer.valueOf(zoomExponent));
                }
            }
            return map;
        }

        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         *
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
                setState(STATE_PAUSE);
        		setToDefaultView = false;
        		viewLeft = savedState.getDouble(KEY_VIEW_LEFT);
        		viewTop = savedState.getDouble(KEY_VIEW_TOP);
        		viewWidth = savedState.getDouble(KEY_VIEW_WIDTH);
        		viewHeight = savedState.getDouble(KEY_VIEW_HEIGHT);
        		zoomExponent = savedState.getInt(KEY_ZOOM_EXPONENT);
            }
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
          
                    	if (mMode == STATE_READY) {
                    		renderView();
                    	} else if (mMode == STATE_RUNNING) {
                    		renderLoop();
                    	}

                    	doDraw(c);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @see #setState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
            	CharSequence message = null;
            	
            	if (mode == STATE_PAUSE) {
            		message = mContext.getResources().getText(R.string.message_paused);
            	}
            	
                setState(mode, message);
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setState(int mode, CharSequence message) {
            /*
             * This method optionally can cause a text message to be displayed
             * to the user when the mode changes. Since the View that actually
             * renders that text is part of the main View hierarchy and not
             * owned by this thread, we can't touch the state of that View.
             * Instead we use a Message + Handler to relay commands to the main
             * thread, which updates the user-text View.
             */
            synchronized (mSurfaceHolder) {
                mMode = mode;

                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                Resources res = mContext.getResources();
                CharSequence str = "";

                if (message != null) {
                    str = message;
                } else {
                	str = res.getText(R.string.message_zoom) + " " + Integer.toString(zoomExponent);
                }
                
                b.putString("text", str.toString());
                b.putInt("viz", View.VISIBLE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
            	/*
            	int constraint = width;
            	
            	if (height < constraint) {
            		constraint = height;
            	}
            	
            	int powerOf2 = 1;
            	
            	while (powerOf2 * 2 <= constraint) {
            		powerOf2 *= 2;
            	}
            	*/

                canvasWidthInPixels = width;	//powerOf2;
                canvasHeightInPixels = height;	//powerOf2;
                
                mMandelbrotBitmap = Bitmap.createScaledBitmap(
                		mMandelbrotBitmap, canvasWidthInPixels, canvasHeightInPixels, false);
            	bitmapCanvas = new Canvas(mMandelbrotBitmap);

                // Restart the rendering.
                setState(STATE_READY);
            }
        }

        /**
         * Handles a key-down event.
         *
         * @param keyCode the key that was pressed
         * @param msg the original event object
         * @return true
         */
        boolean doKeyDown(int keyCode, KeyEvent msg) {
            synchronized (mSurfaceHolder) {
                boolean okStart = false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) okStart = true;
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) okStart = true;
                if (keyCode == KeyEvent.KEYCODE_S) okStart = true;

                if (mMode == STATE_PAUSE && okStart) {
                    // paused -> running
                    unpause();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_H) {		// Home (default view)
                	goHome();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_O) {		// Zoom out
                	zoomOut();
                    return true;
                } else if (mMode == STATE_RUNNING) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_P) {
                        pause();
                        return true;
                    }
                }

                return false;
            }
        }

        /**
         * Draws the ship, fuel/speed bars, and background to the provided
         * Canvas.
         */
        private void doDraw(Canvas canvas) {
            canvas.drawBitmap(mMandelbrotBitmap, 0, 0, null);
        }

        public void doTouchDown(int x, int y) {
        	
        	if (x < 0 || x >= canvasWidthInPixels || y < 0 || y >= canvasHeightInPixels) {
        		return;
        	}
        	
            synchronized (mSurfaceHolder) {
            	onCanvasClick(x, y);
            }
        }
    }

    /** Pointer to the text view to display "Paused.." etc. */
    private TextView mStatusText = null;

    private int lastStatusTextHeight = 0;
    private int lastHomeButtonHeight = 0;

    private ViewGroup mMainLayout;
    
    /** The thread that actually draws the animation */
    private MandelbrotThread thread;

    @SuppressLint("HandlerLeak")
	public MandelbrotView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new MandelbrotThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
            }
        });

        setFocusable(true); // make sure we get key events
    }

    /**
     * Fetches the animation thread corresponding to this LunarView.
     *
     * @return the animation thread
     */
    public MandelbrotThread getThread() {
        return thread;
    }

    /**
     * Standard override to get key-press events.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        return thread.doKeyDown(keyCode, msg);
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
        	thread.pause();
        }
    }

    /**
     * Installs a pointer to the text view used for messages.
     */
    public void setTextView(TextView textView) {
        mStatusText = textView;
    }

    public void updateStatusTextHeight() {
    	int h = mStatusText.getHeight();
    	
    	if (h != lastStatusTextHeight) {
    		lastStatusTextHeight = h;
    		mMainLayout.invalidate();
    	}
    }
    
    public void updateHomeButtonHeight(int h) {
    	
    	if (h != lastHomeButtonHeight) {
    		lastHomeButtonHeight = h;
    		mMainLayout.invalidate();
    	}
    }
    
    public void setMainLayout(ViewGroup mainLayout) {
    	mMainLayout = mainLayout;
    }
    
    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    public void doTouchDown(int x, int y) {
    	int location[] = new int[2];
    	
    	getLocationOnScreen(location);
    	
    	// Subtract the view's offset from x and y.
    	thread.doTouchDown(x - location[0], y - location[1]);
    }

    @Override 
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	/*
    	 * Ensure that the MandelbrotView does not push the TextView or
    	 * the RelativeLayout containing the buttons off of the bottom of the screen.
    	 * To do this, we must measure the heights of those two other items,
    	 * and then subtract those heights from parentHeight below.
    	 * Also, account for the top margin of the MandelbrotView.
    	 */
    	LayoutParams lp = (LayoutParams) getLayoutParams();
    	int topMargin = lp.topMargin;
    	int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
    	int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
    	
    	parentHeight -= topMargin + lastStatusTextHeight + lastHomeButtonHeight;

  		int constraint = parentWidth;
	
  		if (parentHeight < constraint) {
       		constraint = parentHeight;
       	}
   	
       	int powerOf2 = 1;
   	
       	while (powerOf2 * 2 <= constraint) {
       		powerOf2 *= 2;
       	}

       	this.setMeasuredDimension(powerOf2, powerOf2);
    }
}
