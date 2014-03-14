package dev.matrix.CardinalCurve;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import dev.matrix.CardinalCurve.curves.CardinalCurveWithCanvas;

public class MySignature extends View {

	private CardinalCurveWithCanvas mCurve;

	public MySignature(Context context) {
		super(context);

		setWillNotDraw(false);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (mCurve != null) {
			mCurve.resize(w, h);
		} else {
			mCurve = new CardinalCurveWithCanvas(w, h);
			mCurve.setWidth(3, 20);
			mCurve.clearCanvas(Color.WHITE);
			mCurve.getPaint().setAntiAlias(true);
			mCurve.getPaint().setColor(Color.BLUE);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		mCurve.draw(canvas);
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
				mCurve.clearPoints();
				mCurve.clearCanvas(Color.WHITE);
				invalidate();
				break;
		}
		return true;
	}
}
