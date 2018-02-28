
package ru.revivedtablet.widget;

import android.graphics.Canvas;

public interface Widget {
	
	/**
	 * Отрисовать содержимое
	 * @param canvas
	 */
	void draw(Canvas canvas);
	
	/**
	 * Установить интерфейс, через который виджет будет информировать "подложку", о том,
	 * что содержимое виджета изменилось и требуется перерисовка 
	 * @param broker
	 */
	void setInvalidateBroker(InvalidateBroker broker);
	
	/**
	 * Called in MainUI thread
	 */
	void pause();

	/**
	 * Called in MainUI thread
	 */
	void resume();


	/**
	 * 
	 */
	interface InvalidateBroker {
		void invalidate();
	}; 
}
