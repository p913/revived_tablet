package ru.revivedtablet.morpher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public interface ImageMorpher {
	
	/**
	 * @return Кол-во шагов, за которые делается полное преобразование
	 * из первой картинки во вторую. Первый шаг уже производит преобразование. 
	 * Последний шаг не должен рисовать конечную преобразованную картинку -
	 * это сделает на следующем шаге сам вью.  
	 */
	public int getStepCount();
	
	
	/**
	 * @param bmpBackground "Нижнее" изображение, из которого идет преобразование
	 * @param bmpForeground "Верхнее" изображение, в которое идет преобразование
	 * @param canvas 
	 * @param step Шаг от 1 до {@link getStepCount()} 
	 */
	public void drawStep(Bitmap bmpBackground, Bitmap bmpForeground, Rect rectBackground, Rect rectForeground, Canvas canvas, int step);
	
	/**
	 * Освободить выделенные ресурсы, которые были выделены в процессе выполнения шагов.
	 */
	public void release(); 
}
