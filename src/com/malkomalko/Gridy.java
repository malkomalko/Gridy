package com.malkomalko;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;
import org.puredata.core.utils.PdUtils;

import com.malkomalko.helpers.GraphicsHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class Gridy extends Activity {
	
	float width;
	float height;
	Canvas canvas;
	Panel panel;
	Handler handler;
	int currentStep = 0;
	private Timer metro;
	boolean loopStarted = false;
	private static final int SAMPLE_RATE = 11025;
	private String patch;
	private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
	
	int[][][] grid = new int[8][8][8];
    int[] colors   = { 0xFF556270, 0xFF4ECDC4, 0xFFC7F464, 0xFFFF6B6B,
    				   0xFFC44D58, 0xFFE97F02, 0xFFF8CA00, 0xFF8A9B0F };
    int[] scale = { 48, 50, 52, 53, 55, 57, 59, 60 };
    
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
        	WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        width  = (float)getWindowManager().getDefaultDisplay().getWidth();
    	height = (float)getWindowManager().getDefaultDisplay().getHeight();
    	
        gestureDetector = new GestureDetector(new GridGestures());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
        panel = new Panel(this);
        panel.setOnTouchListener(gestureListener);
        
    	setContentView(panel);
    	initPd();
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		if (AudioParameters.suggestSampleRate() < SAMPLE_RATE) {
			post("required sample rate not available; exiting");
			finish();
			return;
		}
		int nOut = Math.min(AudioParameters.suggestOutputChannels(), 2);
		if (nOut == 0) {
			post("audio output not available; exiting");
			finish();
			return;
		}
		try {
			PdAudio.initAudio(SAMPLE_RATE, 0, nOut, 1, true);
			PdAudio.startAudio(this);
			if (!loopStarted) startLoop();
		} catch (IOException e) {
			Log.e("Gridy", e.toString());
		}
	}
	
	@Override
	protected void onPause() {
		PdAudio.stopAudio();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		cleanup();
		super.onDestroy();
	}
	
	@Override
	public void finish() {
		cleanup();
		super.finish();
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
    	gestureDetector.onTouchEvent(event);
    	return true;
    }
	
	private void cleanup() {
		PdAudio.stopAudio();
		PdUtils.closePatch(patch);
		PdBase.release();
	}
	
	private void post(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Gridy:" + msg, Toast.LENGTH_LONG).show();
			}
		});
	}
    
    private void initPd() {
		File dir = getFilesDir();
		File patchFile = new File(dir, "gridy.pd");
		try {
			IoUtils.extractZipResource(getResources().openRawResource(R.raw.patch), dir, true);
			patch = PdUtils.openPatch(patchFile.getAbsolutePath());
		} catch (IOException e) {
			Log.e("Gridy", e.toString() + "; exiting now");
			finish();
		}
	}
	
    private void startLoop() {
    	loopStarted = true;
    	
    	metro = new Timer();
    	metro.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}
		}, 0, 350);
    }
    
    private void TimerMethod() {
		this.runOnUiThread(Timer_Tick);
	}

    
    final Runnable Timer_Tick = new Runnable() {
		public void run() {
			currentStep = (currentStep == 7) ? 0 : currentStep + 1;
			int playStep = (currentStep == 7) ? 0 : currentStep + 1;
			panel.invalidate();
	        
	        for (int row = 0; row <= 7; row++) {
				if (grid[0][playStep][row] == 1)
					PdBase.sendList("note", scale[7-row], 95, 1000, 1, 0, 0, 100);
			}
		}
	};
    
    class Panel extends View {
    	public Panel(Context context) {
            super(context);
        }
 
        @Override
        public void onDraw(Canvas _canvas) {
        	canvas = _canvas;
        	for (int row = 0; row <= 7; row++) {
				for (int col = 0; col <= 7; col++) {
					GraphicsHelper.rect(
						canvas, colors[row], grid[0][col][row],
						(((width/8)*col)+2),
						(((height/8)*row)+2),
						(width/8)-4,
						(height/8)-4);
				}
			}
        	GraphicsHelper.circle(canvas, 0xFFFFFFFF, 1, (((width/8)*currentStep)+((width/8)/2)), height/2, 5);
        }
    }
    
    class GridGestures extends SimpleOnGestureListener {
    	@Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			int row = (int)Math.abs(((height - e1.getY()) / (height / 8) - 8));
			
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    for (int col = 0; col <= 7; col++) { grid[0][col][row] = 0; }
        			panel.invalidate();
                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    for (int col = 0; col <= 7; col++) { grid[0][col][row] = 1; }
        			panel.invalidate();
                }
            } catch (Exception e) { }
            
            return false;
        }
        
        @Override
        public boolean onDown(MotionEvent event) {
        	float posX = event.getX();
			float posY = event.getY();
			int col = (int)Math.abs(((width - posX) / (width / 8) - 8));
			int row = (int)Math.abs(((height - posY) / (height / 8) - 8));
			
			grid[0][col][row] = grid[0][col][row] == 0 ? 1 : 0;
			panel.invalidate();
			
			return true;
        }
    }

}
