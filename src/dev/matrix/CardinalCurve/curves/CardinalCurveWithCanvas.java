package dev.matrix.CardinalCurve.curves;

import android.graphics.*;
import android.os.SystemClock;

public class CardinalCurveWithCanvas {
	private static final float TOLERANCE = 5f;

	private int mSegments = -10;
	private float mTension = .5f;
	private float mMinWidth = 3;
	private float mMaxWidth = 20;
	private WidthType mWidthType = WidthType.FasterThinner;
	private Paint mPaint;
	private Bitmap mBitmap;
	private Canvas mCanvas;
	private PointF mTensionVector1 = new PointF();
	private PointF mTensionVector2 = new PointF();
	private RingBuffer<CurvePoint> mPoints = new RingBuffer<CurvePoint>(4);

	public CardinalCurveWithCanvas(int width, int height) {
		mPaint = new Paint();
		mPaint.setColor(Color.BLACK);

		mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);
	}

	public void resize(int width, int height) {
		Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap, width, height, true);
		mBitmap.recycle();
		mBitmap = bitmap;
		mCanvas = new Canvas(mBitmap);
	}

	public Paint getPaint() {
		return mPaint;
	}

	public void setPaint(Paint paint) {
		mPaint = paint;
	}

	public void setSegments(int segments) {
		mSegments = segments;
	}

	public void setTension(float tension) {
		mTension = tension;
	}

	public void setWidth(float minWidth, float maxWidth) {
		mMinWidth = minWidth;
		mMaxWidth = maxWidth;
	}

	public void setWidthType(WidthType widthType) {
		mWidthType = widthType;
	}

	public boolean addPoint(float x, float y) {
		return addPointInternal(x, y) && renderPoints();
	}

	public void clearPoints() {
		mPoints.clear();
	}

	public void clearCanvas() {
		clearCanvas(Color.TRANSPARENT);
	}

	public void clearCanvas(int color) {
		mBitmap.eraseColor(color);
	}

	public void draw(Canvas canvas) {
		canvas.drawBitmap(mBitmap, 0, 0, null);
	}

	private boolean addPointInternal(float x, float y) {
		CurvePoint prevPoint;
		if (mPoints.getSize() > 0) {
			prevPoint = mPoints.getLast();
			if (Math.abs(prevPoint.x - x) < TOLERANCE && Math.abs(prevPoint.y - y) < TOLERANCE) {
				return false;
			}
		} else {
			prevPoint = null;
		}

		CurvePoint point;
		if (mPoints.isFilled()) {
			point = mPoints.getFirst();
		} else {
			point = new CurvePoint();
		}
		point.x = x;
		point.y = y;
		point.time = SystemClock.elapsedRealtime();

		if (prevPoint != null) {
			point.width = mMaxWidth / mWidthType.apply(point.velocity(prevPoint));
			point.width = Math.max(point.width, mMinWidth);
			point.width = Math.min(point.width, mMaxWidth);
			if (mPoints.getSize() == 1) {
				prevPoint.width = point.width;
			}
		} else {
			point.width = mMaxWidth;
		}

		mPoints.add(point);
		return true;
	}

	private boolean renderPoints() {
		if (mPoints.getSize() < 4) {
			return false;
		}

		float lastX = 0;
		float lastY = 0;

		mPaint.setStrokeCap(Paint.Cap.ROUND);

		CurvePoint pn = mPoints.get(0);
		CurvePoint p1 = mPoints.get(1);
		CurvePoint p2 = mPoints.get(2);
		CurvePoint pp = mPoints.get(3);

		mTensionVector1.x = (p2.x - pn.x) * mTension;
		mTensionVector1.y = (p2.y - pn.y) * mTension;
		mTensionVector2.x = (pp.x - p1.x) * mTension;
		mTensionVector2.y = (pp.y - p1.y) * mTension;

		int segmentsCount;
		if (mSegments < 0) {
			float dx = p2.x - p1.x;
			float dy = p2.y - p1.y;
			segmentsCount = (int) Math.max(mSegments, Math.abs(Math.sqrt(dx * dx + dy * dy)) * (1 / (float) -mSegments) + 0.5) + 1;
		} else {
			segmentsCount = mSegments;
		}

		for (int index = 0; index < segmentsCount; ++index) {
			float progress = index / (float) (segmentsCount - 1);

			float pow2 = (float) Math.pow(progress, 2);
			float pow3 = pow2 * progress;
			float pow23 = pow2 * 3;
			float pow32 = pow3 * 2;

			float c1 = pow32 - pow23 + 1;
			float c2 = pow23 - pow32;
			float c3 = pow3 - 2 * pow2 + progress;
			float c4 = pow3 - pow2;

			float x = c1 * p1.x + c2 * p2.x + c3 * mTensionVector1.x + c4 * mTensionVector2.x;
			float y = c1 * p1.y + c2 * p2.y + c3 * mTensionVector1.y + c4 * mTensionVector2.y;

			if (index > 0) {
				mPaint.setStrokeWidth(p1.width + (p2.width - p1.width) * progress);
				mCanvas.drawLine(lastX, lastY, x, y, mPaint);
			}
			lastX = x;
			lastY = y;
		}
		return true;
	}

	static class CurvePoint {
		public float x;
		public float y;
		public long time;
		public float width;

		public float distance(CurvePoint point) {
			float dx = x - point.x;
			float dy = y - point.y;
			return (float) Math.sqrt(dx * dx + dy * dy);
		}

		public float velocity(CurvePoint point) {
			return distance(point) / Math.abs(time - point.time);
		}
	}

	public enum WidthType {
		FasterThinner {
			@Override
			float apply(float val) {
				return val;
			}
		},
		FasterThicker {
			@Override
			float apply(float val) {
				return 1 / val;
			}
		};

		abstract float apply(float val);
	}
}
