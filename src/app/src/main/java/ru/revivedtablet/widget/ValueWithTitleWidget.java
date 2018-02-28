package ru.revivedtablet.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.text.TextPaint;

import ru.revivedtablet.ImageUtils;

public class ValueWithTitleWidget extends PlacedInLineWidget {
	
	private TextPaint paintValue;
	private TextPaint paintTitle;
	private Paint paintFill;
	private Paint paintHistory;
	
	private Rect rect = new Rect(0, 0, 1, 1);
	
	private String title = "???";
	private String value = "???";

	private int width;
	private int height;

	public ValueWithTitleWidget(int line, String title, String value) {
		super(line);
		
		this.title = title;
		this.value = value;
		
		paintValue = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		paintValue.setTextSize(ImageUtils.URGE_TEXT_SIZE_BIG);
		paintValue.setColor(Color.WHITE);
		paintValue.setTextScaleX(ImageUtils.URGE_TEXT_SCALEX);
		
		paintTitle = new TextPaint(paintValue);
		paintTitle.setTextSize(ImageUtils.URGE_TEXT_SIZE_NORMAL);
		
		paintFill = new Paint();
		paintFill.setStyle(Style.FILL);
		paintFill.setColor(ImageUtils.URGE_FILL_COLOR);
		
		paintHistory = new Paint();
		paintHistory.setColor(Color.CYAN);
		paintHistory.setAntiAlias(true);
		
		calculateWidthAndHeight();
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.drawRect(rect, paintFill);
		canvas.drawText(title, ImageUtils.URGE_PADDING_SMALL + rect.left,
				rect.top + ImageUtils.URGE_PADDING_SMALL + ImageUtils.URGE_TEXT_SIZE_NORMAL, paintTitle);
		canvas.drawText(value, ImageUtils.URGE_PADDING_SMALL + rect.left,
				rect.top + ImageUtils.URGE_PADDING_SMALL * 2 + ImageUtils.URGE_TEXT_SIZE_NORMAL + ImageUtils.URGE_TEXT_SIZE_BIG, paintValue);
	}

	private void calculateWidthAndHeight() {
		Rect rt = new Rect(), rv = new Rect();

		paintTitle.getTextBounds(title, 0, title.length(), rt);
		paintValue.getTextBounds(value, 0, value.length(), rv);

		width = Math.max(rt.width(), rv.width()) + ImageUtils.URGE_PADDING_SMALL * 2;
		height = ImageUtils.URGE_PADDING_SMALL * 3 + ImageUtils.URGE_TEXT_SIZE_NORMAL + ImageUtils.URGE_TEXT_SIZE_BIG;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		if (invalidateBroker != null)
			invalidateBroker.invalidate();
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
        calculateWidthAndHeight();
		if (invalidateBroker != null)
			invalidateBroker.invalidate();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setDrawRect(Rect rect) {
		this.rect.set(rect);
	}

	@Override
	public Rect getDrawRect() {
		return rect;
	}
}
