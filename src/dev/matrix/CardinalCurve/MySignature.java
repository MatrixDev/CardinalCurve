package dev.matrix.CardinalCurve;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import dev.matrix.CardinalCurve.curves.CardinalCurve;

public class MySignature extends View {

	private Bitmap mBitmap;
	private Canvas mCanvas;
	private Paint mPaint = new Paint();
	private CardinalCurve mCurve = new CardinalCurve();

	public MySignature(Context context) {
		super(context);

		mCurve.setWidth(3, 20);
		mPaint.setColor(Color.BLUE);

		setWillNotDraw(false);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (mBitmap != null) {
			Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap, w, h, true);
			mBitmap.recycle();
			mBitmap = bitmap;
		} else {
			mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			mBitmap.eraseColor(Color.WHITE);
		}
		mCanvas = new Canvas(mBitmap);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(mBitmap, 0, 0, null);
		mCurve.draw(canvas, mPaint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				mCurve.addPoint(event.getX(), event.getY());
				invalidate();
				break;

			case MotionEvent.ACTION_UP:
				mCurve.addPoint(event.getX(), event.getY());
				mCurve.draw(mCanvas, mPaint);
				mCurve.clear();
				invalidate();
				break;
		}
		return true;
	}
}
