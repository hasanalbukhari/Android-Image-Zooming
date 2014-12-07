package com.example.imagezoom;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends Activity {

	public Bitmap bm = null;
	public BitmapRegionDecoder bitmapRegionDecoder;
	public ImageView imageView;
	
	public int touchState, width, height;
	public int newHeight, newWidth, currentX, currentY, left, top;
	public float dist0, distCurrent, distLast, scale, stepScale, maxScale, startScale, tempScale;
	
	BitmapFactory.Options bounds;
	
	final int IDLE = 0, TOUCH = 1, PINCH = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		init();
		getImageSpecs("map.png");
	}
	
	public void init()
	{
		maxScale = 5.0f;
		
		DisplayMetrics dm = getResources().getDisplayMetrics();
		width = (int) (dm.widthPixels);
		height = (int) (dm.heightPixels);
		
		imageView = (ImageView)findViewById(R.id.imageView);
		((FrameLayout)findViewById(R.id.imageFrameLayout)).setOnTouchListener(MyOnTouchListener);
		
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)imageView.getLayoutParams();
		lp.width = width;
		lp.height = height;
		lp.gravity = Gravity.CENTER;
		imageView.setLayoutParams(lp);
	}
	
	public void getImageSpecs(String res_id)
	{
		InputStream is = null;
		bitmapRegionDecoder = null;
		
		try {
			is = getAssets().open(res_id); // get image stream from assets. only the stream, no mem usage
			if (bitmapRegionDecoder != null) // this is only if you want to use the decoder to open another image in activity.
				bitmapRegionDecoder.recycle(); // clear memory from decoder.

			bitmapRegionDecoder = BitmapRegionDecoder.newInstance(is, false);

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		bounds = new BitmapFactory.Options();
		bounds.inJustDecodeBounds = true; // only specs needed. no image yet!
		BitmapFactory.decodeStream(is, null, bounds); // get image specs.
		
		try {
			is.close(); // close stream no longer needed.
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		newWidth = bounds.outWidth;
		newHeight = bounds.outHeight;
		
		dist0 = 1.0f;
		distCurrent = 1.0f;
		startScale = 1.0f;
		
		scale = startScale;
		stepScale = distCurrent/dist0;

		left = 0;
		top = 0;
		
		drawMatrix();
		touchState = IDLE;
	}
	
	private void drawMatrix()
	{
		tempScale = scale * stepScale;
		if (tempScale<startScale)
			tempScale = startScale;
		if (tempScale>maxScale)
			tempScale = maxScale;
		
		float currentWidth = newWidth;
		float currentHeight = newHeight;
		float displayScale = (float)width/height;
		if (displayScale<1.0f)
		{
			newWidth = (int) (bounds.outWidth / tempScale);
			newHeight = Math.min((int)(newWidth / displayScale), bounds.outHeight);
		}
		else
		{
			newHeight = (int) (bounds.outHeight / tempScale);
			newWidth = Math.min((int)(newHeight / displayScale), bounds.outWidth);
		}
		
		int maxLeftPadding = (bounds.outWidth-newWidth);
		int maxTopPadding = (bounds.outHeight-newHeight);
		
		left += (int)((currentWidth-newWidth)/2.0f);
		top += (int)((currentHeight-newHeight)/2.0f);
		
		left = Math.min(maxLeftPadding, Math.max(0, left));
		top = Math.min(maxTopPadding, Math.max(0, top));
		
        if (bm != null)
		{
			imageView.setImageBitmap(null);
			bm.recycle();
			bm = null;
		}
        
        Rect pRect = new Rect(left, top, left + newWidth, top + newHeight);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        int inSampleSize = 1;
        if (tempScale <= 2.75) // you can map scale with quality better than this.
        	inSampleSize = 2;

        bounds.inSampleSize = inSampleSize; // takes binary steps only. 1, 2, 4, 8. loaded image part quality
        bm = bitmapRegionDecoder.decodeRegion(pRect, bounds);
		imageView.setImageBitmap(bm);
	}
	
	OnTouchListener MyOnTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			float distx, disty;
			switch(event.getAction() & MotionEvent.ACTION_MASK){   
				case MotionEvent.ACTION_DOWN:
					//A pressed gesture has started, the motion contains the initial starting location.
					touchState = TOUCH;
					currentX = (int) event.getRawX();
		            currentY = (int) event.getRawY();
				break;
				
				case MotionEvent.ACTION_POINTER_DOWN:
					//A non-primary pointer has gone down.
					touchState = PINCH;
					//Get the distance when the second pointer touch
					distx = event.getX(0) - event.getX(1);
					disty = event.getY(0) - event.getY(1);
					
					dist0 = (float) Math.sqrt(distx * distx + disty * disty);
					distLast = dist0;
				break;
				
				case MotionEvent.ACTION_MOVE:
					//A change has happened during a press gesture (between ACTION_DOWN and ACTION_UP).
					if(touchState == PINCH){
						//Get the current distance
						distx = event.getX(0) - event.getX(1);
						disty = event.getY(0) - event.getY(1);
						distCurrent = (float) Math.sqrt(distx * distx + disty * disty);
						if (Math.abs(distCurrent-distLast) >= 35)
						{
							stepScale = distCurrent/dist0;
							distLast = distCurrent;
							drawMatrix();
						}
					}
					else
					{
						if (currentX==-1 && currentY==-1)
						{
							currentX = (int) event.getRawX();
							currentY = (int) event.getRawY();
						}
						else
						{
							int x2 = (int) event.getRawX();
				            int y2 = (int) event.getRawY();
				            int dx = (currentX - x2);
				            int dy = (currentY - y2);
				            left += dx;
				            top += dy;
				            currentX = x2;
				            currentY = y2;
				            drawMatrix();
						}
					}
				break;
					
				case MotionEvent.ACTION_UP:
					//A pressed gesture has finished.
					
					touchState = IDLE;
				break;
				
				case MotionEvent.ACTION_POINTER_UP:
					//A non-primary pointer has gone up.
					if (touchState == PINCH)
					{
						scale *= stepScale;
						if (scale<startScale)
							scale = startScale;
						if (scale>maxScale)
							scale = maxScale;
						currentX = -1;
						currentY = -1;
						stepScale = 1.0f;
					}
					touchState = TOUCH;
				break;
			}
			return true;
		}
	};
}
