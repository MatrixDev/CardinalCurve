package dev.matrix.CardinalCurve.curves;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

public class CardinalCurve {
	private static final float TOLERANCE = 5f;

	private int mSegments = 10;
	private float mTension = .5f;
	private float mMinWidth = 3;
	private float mMaxWidth = 20;
	private List<CurvePoint> mPoints = new ArrayList<CurvePoint>();
	private CardinalFunction mCardinalFunction = new CardinalFunction();

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

	public boolean addPoint(float x, float y) {
		CurvePoint prevPoint = null;
		if (!mPoints.isEmpty()) {
			prevPoint = mPoints.get(mPoints.size() - 1);
			if (Math.abs(prevPoint.x - x) < TOLERANCE && Math.abs(prevPoint.y - y) < TOLERANCE) {
				return false;
			}
		}

		CurvePoint point = new CurvePoint();
		point.x = x;
		point.y = y;
		point.time = SystemClock.elapsedRealtime();
		point.dstWidth = mMaxWidth;

		if (prevPoint != null) {
			point.srcWidth = mMaxWidth / point.velocity(prevPoint);
			point.srcWidth = Math.max(point.srcWidth, mMinWidth);
			point.srcWidth = Math.min(point.srcWidth, mMaxWidth);

			prevPoint.dstWidth = point.srcWidth;
		} else {
			point.srcWidth = mMaxWidth;
		}

		return mPoints.add(point);
	}

	public void clear() {
		mPoints.clear();
	}

	public void draw(Canvas canvas, Paint paint) {
		if (mPoints.size() < 2) {
			return;
		}

		float lastX = 0;
		float lastY = 0;

		paint.setAntiAlias(true);
		paint.setStrokeCap(Paint.Cap.ROUND);

		int size = mPoints.size() - 2;

		// can be optimized by drawing to bitmap directly
		for (int pointIndex = 0; pointIndex < size; ++pointIndex) {
			CurvePoint point = getPointSafe(pointIndex);

			if (point.segments == null) {
				mCardinalFunction.set(
						point,
						getPointSafe(pointIndex + 1),
						getPointSafe(pointIndex - 1),
						getPointSafe(pointIndex + 2)
				);

				point.segments = new CardinalSegment[mCardinalFunction.getSegmentsCount()];
				for (int segmentIndex = 0; segmentIndex < point.segments.length; segmentIndex++) {
					point.segments[segmentIndex] = mCardinalFunction.createSegment(segmentIndex);
				}
			}

			for (int segmentIndex = 0; segmentIndex < point.segments.length; ++segmentIndex) {
				CardinalSegment segment = point.segments[segmentIndex];
				if (pointIndex > 0 || segmentIndex > 0) {
					paint.setStrokeWidth(point.srcWidth + (point.dstWidth - point.srcWidth) * segment.progress);
					canvas.drawLine(lastX, lastY, segment.x, segment.y, paint);
				}
				lastX = segment.x;
				lastY = segment.y;
			}
		}
	}

	private CurvePoint getPointSafe(int index) {
		if (0 <= index && index < mPoints.size()) {
			return mPoints.get(index);
		}
		return (index < 0) ? mPoints.get(0) : mPoints.get(mPoints.size() - 1);
	}

	static class CurvePoint {
		public float x;
		public float y;
		public long time;
		public float srcWidth;
		public float dstWidth;
		public CardinalSegment[] segments;

		public float distance(CurvePoint point) {
			float dx = x - point.x;
			float dy = y - point.y;
			return (float) Math.sqrt(dx * dx + dy * dy);
		}

		public float velocity(CurvePoint point) {
			return distance(point) / Math.abs(time - point.time);
		}
	}

	static class CardinalSegment extends PointF {
		public float progress;
	}

	class CardinalFunction {
		private int segmentsCount;
		private CurvePoint p1;
		private CurvePoint p2;
		private PointF mTensionVector1 = new PointF();
		private PointF mTensionVector2 = new PointF();

		public void set(CurvePoint p1, CurvePoint p2, CurvePoint pn, CurvePoint pp) {
			this.p1 = p1;
			this.p2 = p2;

			mTensionVector1.x = (p2.x - pn.x) * mTension;
			mTensionVector1.y = (p2.y - pn.y) * mTension;
			mTensionVector2.x = (pp.x - p1.x) * mTension;
			mTensionVector2.y = (pp.y - p1.y) * mTension;

			if (mSegments < 0) {
				float dx = p2.x - p1.x;
				float dy = p2.y - p1.y;
				segmentsCount = (int) Math.max(mSegments, Math.abs(Math.sqrt(dx * dx + dy * dy)) * (1 / (float) -mSegments) + 0.5);
			} else {
				segmentsCount = mSegments;
			}
		}

		public int getSegmentsCount() {
			return segmentsCount;
		}

		public CardinalSegment createSegment(int index) {
			CardinalSegment segment = new CardinalSegment();
			segment.progress = index / (float) (segmentsCount - 1);

			float pow2 = (float) Math.pow(segment.progress, 2);
			float pow3 = pow2 * segment.progress;
			float pow23 = pow2 * 3;
			float pow32 = pow3 * 2;

			float c1 = pow32 - pow23 + 1;
			float c2 = pow23 - pow32;
			float c3 = pow3 - 2 * pow2 + segment.progress;
			float c4 = pow3 - pow2;

			segment.x = c1 * p1.x + c2 * p2.x + c3 * mTensionVector1.x + c4 * mTensionVector2.x;
			segment.y = c1 * p1.y + c2 * p2.y + c3 * mTensionVector1.y + c4 * mTensionVector2.y;
			return segment;
		}
	}
}
