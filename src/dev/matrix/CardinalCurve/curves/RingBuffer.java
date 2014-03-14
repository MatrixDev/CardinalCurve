package dev.matrix.CardinalCurve.curves;

@SuppressWarnings("unchecked")
public class RingBuffer<T> {
	private int mSize = 0;
	private int mOffset = 0;
	private Object[] mValues;

	public RingBuffer(int capacity) {
		mValues = new Object[capacity];
	}

	public int getSize() {
		return mSize;
	}

	public boolean isFilled() {
		return mSize == mValues.length;
	}

	public T getFirst() {
		return get(0);
	}

	public T getLast() {
		return get(mSize - 1);
	}

	public T get(int index) {
		return (T) mValues[(mOffset + index) % mValues.length];
	}

	public void add(T value) {
		int index = (mSize < mValues.length) ? mSize++ : mOffset++;
		mValues[index % mValues.length] = value;
	}

	public void clear() {
		for (int index = 0; index < mValues.length; ++index) {
			mValues[index] = null;
		}
		mSize = 0;
		mOffset = 0;
	}
}
