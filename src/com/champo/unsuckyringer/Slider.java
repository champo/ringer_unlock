package com.champo.unsuckyringer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

public class Slider extends ImageView {

	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Bitmap thumbImage = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_normal);
	private final float thumbWidth = thumbImage.getWidth();
	private final float thumbHalfWidth = 0.5f * thumbWidth;
	private final float thumbHalfHeight = 0.5f * thumbImage.getHeight();
	private final float lineHeight = 0.3f * thumbHalfHeight;
	private final float padding = thumbHalfWidth;

	private boolean notifyWhileDragging = false;
	private OnSliderChangeListener listener;

	/**
	 * Default color of a {@link RangeSeekBar}, #FF33B5E5. This is also known as
	 * "Ice Cream Sandwich" blue.
	 */
	public static final int DEFAULT_COLOR = Color.argb(0xFF, 0x33, 0xB5, 0xE5);

	/**
	 * An invalid pointer id.
	 */
	public static final int INVALID_POINTER_ID = 255;

	// Localized constants from MotionEvent for compatibility
	// with API < 8 "Froyo".
	public static final int ACTION_POINTER_UP = 0x6,
			ACTION_POINTER_INDEX_MASK = 0x0000ff00,
			ACTION_POINTER_INDEX_SHIFT = 8;

	private float mDownMotionX;
	private int mActivePointerId = INVALID_POINTER_ID;

	/**
	 * On touch, this offset plus the scaled value from the position of the
	 * touch will form the progress value. Usually 0.
	 */
	float mTouchProgressOffset;

	private int mScaledTouchSlop;
	private boolean mIsDragging;

	private int maxValue;
	private int selectedValue;

	public Slider(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public Slider(final Context context, final AttributeSet attrs, final int defStyle) {
	    super(context, attrs, defStyle);
	    setup(10);
	}

	public Slider(final int maxValue, final Context context) throws IllegalArgumentException {
		super(context);
		setup(maxValue);
	}

	private void setup(final int maxValue) {
		this.maxValue = maxValue;

		// make Slider focusable. This solves focus handling issues in
		// case EditText widgets are being used along with the Slider
		// within ScollViews.
		setFocusable(true);
		setFocusableInTouchMode(true);
		init();
	}

	private final void init() {
		mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	public boolean isNotifyWhileDragging() {
		return notifyWhileDragging;
	}

	/**
	 * Should the widget notify the listener callback while the user is still
	 * dragging a thumb? Default is false.
	 *
	 * @param flag
	 */
	public void setNotifyWhileDragging(final boolean flag) {
		this.notifyWhileDragging = flag;
	}

	/**
	 * Registers given listener callback to notify about changed selected
	 * values.
	 *
	 * @param listener
	 *            The listener to notify about changed selected values.
	 */
	public void setSliderChangeListener(final OnSliderChangeListener listener) {
		this.listener = listener;
	}

	public void setMaxValue(final int maxValue) {
		this.maxValue = maxValue;
	}

	public int getSelectedValue() {
		return selectedValue;
	}

	public void setSelectedValue(final int selectedValue) {
		this.selectedValue = selectedValue;
		if (listener != null) {
			listener.onSliderValuesChanged(this, selectedValue);
		}
	}

	/**
	 * Handles thumb selection and movement. Notifies listener callback on
	 * certain events.
	 */
	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		if (!isEnabled()) {
			return false;
		}

		int pointerIndex;

		final int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {

		case MotionEvent.ACTION_DOWN:
			// Remember where the motion event started
			mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
			pointerIndex = event.findPointerIndex(mActivePointerId);
			mDownMotionX = event.getX(pointerIndex);

			setPressed(true);
			invalidate();
			onStartTrackingTouch();
			trackTouchEvent(event);
			attemptClaimDrag();

			break;
		case MotionEvent.ACTION_MOVE:

			if (mIsDragging) {
				invalidate();
				trackTouchEvent(event);
			} else {
				// Scroll to follow the motion event
				pointerIndex = event.findPointerIndex(mActivePointerId);
				final float x = event.getX(pointerIndex);

				if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
					setPressed(true);
					invalidate();
					onStartTrackingTouch();
					trackTouchEvent(event);
					attemptClaimDrag();
				}
			}

			if (notifyWhileDragging && listener != null) {
				listener.onSliderValuesChanged(this, selectedValue);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mIsDragging) {
				trackTouchEvent(event);
				onStopTrackingTouch();
				setPressed(false);
			} else {
				// Touch up when we never crossed the touch slop threshold
				// should be interpreted as a tap-seek to that location.
				onStartTrackingTouch();
				trackTouchEvent(event);
				onStopTrackingTouch();
			}

			invalidate();
			if (listener != null) {
				listener.onSliderValuesChanged(this, selectedValue);
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			final int index = event.getPointerCount() - 1;
			// final int index = ev.getActionIndex();
			mDownMotionX = event.getX(index);
			mActivePointerId = event.getPointerId(index);
			invalidate();
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(event);
			invalidate();
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mIsDragging) {
				onStopTrackingTouch();
				setPressed(false);
			}
			invalidate(); // see above explanation
			break;
		}
		return true;
	}

	private final void onSecondaryPointerUp(final MotionEvent ev) {
		final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose
			// a new active pointer and adjust accordingly.
			// TODO: Make this decision more intelligent.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mDownMotionX = ev.getX(newPointerIndex);
			mActivePointerId = ev.getPointerId(newPointerIndex);
		}
	}

	private final void trackTouchEvent(final MotionEvent event) {
		final int pointerIndex = event.findPointerIndex(mActivePointerId);
		final float x = event.getX(pointerIndex);

		setNormalizedValue(screenToNormalized(x));
	}

	private void setNormalizedValue(final double value) {
		selectedValue = (int) Math.round(value * maxValue);
	}

	/**
	 * Tries to claim the user's drag motion, and requests disallowing any
	 * ancestors from stealing events in the drag.
	 */
	private void attemptClaimDrag() {
		if (getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
	}

	/**
	 * This is called when the user has started touching this widget.
	 */
	void onStartTrackingTouch() {
		mIsDragging = true;
	}

	/**
	 * This is called when the user either releases his touch or the touch is
	 * canceled.
	 */
	void onStopTrackingTouch() {
		mIsDragging = false;
	}

	/**
	 * Ensures correct size of the widget.
	 */
	@Override
	protected synchronized void onMeasure(final int widthMeasureSpec,
			final int heightMeasureSpec) {
		int width = 200;
		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
			width = MeasureSpec.getSize(widthMeasureSpec);
		}
		int height = thumbImage.getHeight();
		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
			height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
		}
		setMeasuredDimension(width, height);
	}

	/**
	 * Draws the widget on the given canvas.
	 */
	@Override
	protected synchronized void onDraw(final Canvas canvas) {
		super.onDraw(canvas);

		// draw seek bar background line
		final RectF rect = new RectF(padding,
				0.5f * (getHeight() - lineHeight), getWidth() - padding,
				0.5f * (getHeight() + lineHeight));
		paint.setStyle(Style.FILL);
		paint.setColor(Color.GRAY);
		paint.setAntiAlias(true);
		canvas.drawRect(rect, paint);

		// draw seek bar active range line
		rect.left = normalizedToScreen(0);
		rect.right = normalizedToScreen(getNormalizedValue());

		// orange color
		paint.setColor(DEFAULT_COLOR);
		canvas.drawRect(rect, paint);

		// draw thumb
		drawThumb(normalizedToScreen(getNormalizedValue()), canvas);
	}

	private double getNormalizedValue() {
		return (double) selectedValue / (double) maxValue;
	}

	/**
	 * Overridden to save instance state when device orientation changes. This
	 * method is called automatically if you assign an id to the RangeSeekBar
	 * widget using the {@link #setId(int)} method. Other members of this class
	 * than the normalized min and max values don't need to be saved.
	 */
	@Override
	protected Parcelable onSaveInstanceState() {
		final Bundle bundle = new Bundle();
		bundle.putParcelable("SUPER", super.onSaveInstanceState());
		bundle.putInt("MAX", maxValue);
		bundle.putInt("SELECTED", selectedValue);
		return bundle;
	}

	/**
	 * Overridden to restore instance state when device orientation changes.
	 * This method is called automatically if you assign an id to the
	 * RangeSeekBar widget using the {@link #setId(int)} method.
	 */
	@Override
	protected void onRestoreInstanceState(final Parcelable parcel) {
		final Bundle bundle = (Bundle) parcel;
		super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
		maxValue = bundle.getInt("MAX");
		selectedValue = bundle.getInt("SELECTED");
	}

	/**
	 * Draws the "normal" resp. "pressed" thumb image on specified x-coordinate.
	 *
	 * @param screenCoord
	 *            The x-coordinate in screen space where to draw the image.
	 * @param pressed
	 *            Is the thumb currently in "pressed" state?
	 * @param canvas
	 *            The canvas to draw upon.
	 */
	private void drawThumb(final float screenCoord, final Canvas canvas) {
		canvas.drawBitmap(thumbImage, screenCoord
				- thumbHalfWidth,
				(0.5f * getHeight()) - thumbHalfHeight, paint);
	}

	/**
	 * Converts a normalized value into screen space.
	 *
	 * @param normalizedCoord
	 *            The normalized value to convert.
	 * @return The converted value in screen space.
	 */
	private float normalizedToScreen(final double normalizedCoord) {
		return (float) (padding + normalizedCoord * (getWidth() - 2 * padding));
	}

	/**
	 * Converts screen space x-coordinates into normalized values.
	 *
	 * @param screenCoord
	 *            The x-coordinate in screen space to convert.
	 * @return The normalized value.
	 */
	private double screenToNormalized(final float screenCoord) {
		int width = getWidth();
		if (width <= 2 * padding) {
			// prevent division by zero, simply return 0.
			return 0d;
		} else {
			double result = (screenCoord - padding) / (width - 2 * padding);
			return Math.min(1d, Math.max(0d, result));
		}
	}

	/**
	 * Callback listener interface to notify about changed value.
	 */
	public interface OnSliderChangeListener {

		public void onSliderValuesChanged(Slider slider, int value);
	}

}
