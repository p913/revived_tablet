package ru.revivedtablet.widget;


import android.graphics.Rect;

public abstract class PlacedInLineWidget implements Widget {
    private int line;

    private static long creationOrderCounter;

    protected InvalidateBroker invalidateBroker;

    private long creationOrder;

    public PlacedInLineWidget(int line) {
        if (line <= 0)
            this.line = 0;
        else if (line >= 3)
            this.line = 3;
        else
            this.line = line;

        creationOrder = creationOrderCounter++;
    }

    public int getLine() {
        return line;
    }

    @Override
    public void setInvalidateBroker(InvalidateBroker broker) {
        invalidateBroker = broker;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }


    public long getCreationOrder() {
        return creationOrder;
    }

    /**
     * @return ширина виджета, вычисляет виджет сам
     */
    public abstract int getWidth();

    /**
     * @return высота виджета, вычисляет виджет сам
     */
    public abstract int getHeight();

    /**
     * Установить область, в которой должен быть отрисован widget.
     * Актуально для виджетов, которые размещаются владельцем (View).
     * Для виджетов типа погоды, часов, батарейки
     */
    public abstract void setDrawRect(Rect rect);

    /**
     * @return область, в которой widget будет отрисован.
     */
    public abstract Rect getDrawRect();


}
