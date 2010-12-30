package com.malkomalko.helpers;

import android.graphics.Canvas;
import android.graphics.Paint;

public class GraphicsHelper {
	
	public static void rect(Canvas canvas, int color, int stroke,
			float x, float y, float width, float height)
	{
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setStrokeWidth(stroke);
		float leftx = x;
		float topy = y;
		float rightx = x + width;
		float bottomy = y + height;
		canvas.drawRect(leftx, topy, rightx, bottomy, paint);
	}
	
	public static void circle(Canvas canvas, int color, int stroke,
		float x, float y, float radius)
	{
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setStrokeWidth(stroke);
		canvas.drawCircle(x, y, radius, paint);
	}
	
}
