package org.tomweatherhead.mandelbrot;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.TextView;

import org.tomweatherhead.mandelbrot.MandelbrotView.MandelbrotThread;

/**
 * This is a simple LunarLander activity that houses a single LunarView. It
 * demonstrates...
 * <ul>
 * <li>animating by calling invalidate() from draw()
 * <li>loading and drawing resources
 * <li>handling onPause() in an animation
 * </ul>
 */
public class MandelbrotActivity extends Activity {
    private static final int MENU_HOME = 1;
    private static final int MENU_PAUSE = 2;
    private static final int MENU_RESUME = 3;
    private static final int MENU_ZOOM_OUT = 4;

    /** A handle to the thread that's actually running the animation. */
    private MandelbrotThread mMandelbrotThread;

    /** A handle to the View in which the game is running. */
    private MandelbrotView mMandelbrotView;

    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     *
     * To display the Options Menu when running in the emulator, press F2 or Page Up.
     * 
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_HOME, 0, R.string.menu_home);
        menu.add(0, MENU_ZOOM_OUT, 0, R.string.menu_zoom_out);
        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);

        return true;
    }

    /**
     * Invoked when the user selects an item from the Menu.
     *
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HOME:
            	mMandelbrotThread.goHome();
                return true;
            case MENU_ZOOM_OUT:
            	mMandelbrotThread.zoomOut();
                return true;
            case MENU_PAUSE:
            	mMandelbrotThread.pause();
                return true;
            case MENU_RESUME:
            	mMandelbrotThread.unpause();
                return true;
        }

        return false;
    }

    /**
     * Invoked when the Activity is created.
     *
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.main);

        // get handles to the LunarView from XML, and its LunarThread
        mMandelbrotView = (MandelbrotView) findViewById(R.id.mandelbrot);
        mMandelbrotThread = mMandelbrotView.getThread();

        final ViewGroup mainLayout = (ViewGroup) findViewById (R.id.main_layout);
        
        mMandelbrotView.setMainLayout(mainLayout);
        
        // give the MandelbrotView a handle to the TextView used for messages
        final TextView statusText = (TextView) findViewById(R.id.text);
        
        mMandelbrotView.setTextView(statusText);

        final Button homeButton = (Button) findViewById(R.id.home);
        
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	mMandelbrotThread.goHome();
            }
        });

        final Button zoomOutButton = (Button) findViewById(R.id.zoom_out);
        
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	mMandelbrotThread.zoomOut();
            }
        });

        ViewTreeObserver vto1 = statusText.getViewTreeObserver();
        
        vto1.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
            	mMandelbrotView.updateStatusTextHeight();
            }
        });
        
        ViewTreeObserver vto2 = homeButton.getViewTreeObserver();
        
        vto2.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
            	mMandelbrotView.updateHomeButtonHeight(homeButton.getHeight());
            }
        });
        
        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            mMandelbrotThread.setState(MandelbrotThread.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            mMandelbrotThread.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mMandelbrotView.getThread().pause(); // pause game when Activity pauses
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mMandelbrotThread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	
    	if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int)event.getX();
            int y = (int)event.getY();

            mMandelbrotView.doTouchDown(x, y);
    	}
    	
        // Let's try not stopping the propagation of this event.
        return false;
    }

}