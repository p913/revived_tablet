package ru.revivedtablet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ImageUtils {
    private static int MAX_BITMAP_HEIGHT = 4000;
    private static int MAX_BITMAP_WIDTH = 4000;

    public static final int URGE_FILL_COLOR = 0x50000000;
    public static final int URGE_TEXT_SIZE_NORMAL = 20;
    public static final int URGE_TEXT_SIZE_BIG = 30;
    public static final int URGE_TEXT_SIZE_SMALL = 12;
    public static final float URGE_TEXT_SCALEX = 0.7F;
    public static final int URGE_PADDING = 10;
    public static final int URGE_PADDING_SMALL = URGE_PADDING / 2;
    public static final int URGE_PADDING_BIG = URGE_PADDING * 2;
    public static final int URGE_WIDGET_HEIGHT = 96;

    public static final String THUMBNAIL_EXTENSION = ".thmbn";
    public static final String PICTURE_EXTENSION = ".jpeg";

    public enum PreferedBoundMode {
        EQUAL_OR_LESS, //результат должен помещаться строго в заданные рамки
        EQUAL_OR_GREAT,  //результат может превышать заданные рамки или по вертикали, или по горизонтали, но должен полностью заполнять их
        EQUAL_WITH_BLUR    //результат должен строго соответсвовать заданным рамкам, по краям возможно размытие
    }

    public static Bitmap decodeSampledBitmap(String fileName, int reqWidth, int reqHeight,
                                             PreferedBoundMode mode) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        //Log.d("inSampleSize", String.valueOf(options.inSampleSize));

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap loaded = BitmapFactory.decodeFile(fileName, options);
        if (loaded != null) {
            Bitmap scaled;
            int newHeight = (int)((double)options.outHeight * reqWidth / options.outWidth);
            int newWidth = (int)((double)options.outWidth * reqHeight / options.outHeight);
            if (mode == PreferedBoundMode.EQUAL_OR_GREAT) {
                if (newHeight > reqHeight && newHeight < MAX_BITMAP_HEIGHT)
                    scaled = Bitmap.createScaledBitmap(loaded, reqWidth, newHeight, true);
                else if (newWidth >= reqWidth && newWidth < MAX_BITMAP_WIDTH)
                    scaled = Bitmap.createScaledBitmap(loaded, newWidth, reqHeight, true);
                else
                    return null;
            } else {
                if (newHeight > reqHeight && newWidth < MAX_BITMAP_WIDTH)
                    scaled = Bitmap.createScaledBitmap(loaded, newWidth, reqHeight, true);
                else if (newHeight < MAX_BITMAP_HEIGHT)
                    scaled = Bitmap.createScaledBitmap(loaded, reqWidth, newHeight, true);
                else
                    return null;
            }

            //Log.d("Scaled.h", String.valueOf(scaled.getHeight()));
            //Log.d("Scaled.w", String.valueOf(scaled.getWidth()));

            if (scaled.getWidth() < reqWidth && mode == PreferedBoundMode.EQUAL_WITH_BLUR)
                scaled = horizontalWideAndBlur(scaled, reqWidth);
            else if (scaled.getHeight() < reqHeight && mode == PreferedBoundMode.EQUAL_WITH_BLUR)
            scaled = verticalWideAndBlur(scaled, reqHeight);

            if (loaded != scaled) //because createScaledBitmap did not create new bitmap if requested size == original size
                loaded.recycle();
            scaled.setDensity(Bitmap.DENSITY_NONE);
            return scaled;
        }
        else
            return null;
    }

    public static Bitmap horizontalWideAndBlur(Bitmap src, int reqWidth) {
        Bitmap tmp = Bitmap.createBitmap(reqWidth, src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tmp);
        int w = (reqWidth - src.getWidth()) / 2;
        c.drawBitmap(src, w, 0, null);
        if (w < src.getWidth()) {
            Matrix m = new Matrix();
            m.preScale(-1, 1);
            Bitmap mirrored = Bitmap.createBitmap(src, 0, 0, w, src.getHeight(), m, false);
            mirrored = fastblur(mirrored, 1F, 10);
            c.drawBitmap(mirrored, 0, 0, null);
            mirrored = Bitmap.createBitmap(src, src.getWidth() - w, 0, w, src.getHeight(), m, false);
            mirrored = fastblur(mirrored, 1F, 10);
            c.drawBitmap(mirrored, w + src.getWidth(), 0, null);
        }

        return tmp;
    }

    public static Bitmap verticalWideAndBlur(Bitmap src, int reqHeight) {
        Bitmap tmp = Bitmap.createBitmap(src.getWidth(), reqHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tmp);
        int h = (reqHeight - src.getHeight()) / 2;
        c.drawBitmap(src, 0, h, null);
        if (h < src.getHeight()) {
            Matrix m = new Matrix();
            m.preScale(1, -1);
            Bitmap mirrored = Bitmap.createBitmap(src, 0, 0, src.getWidth(), h, m, false);
            mirrored = fastblur(mirrored, 1F, 10);
            c.drawBitmap(mirrored, 0, 0, null);
            mirrored = Bitmap.createBitmap(src, 0, src.getHeight() - h, src.getWidth(), h, m, false);
            mirrored = fastblur(mirrored, 1F, 10);
            c.drawBitmap(mirrored, 0, h + src.getHeight(), null);
        }

        return tmp;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap createEmptyPicture(int width, int height) {
        Bitmap empty = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(empty);

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(20);
        textPaint.setColor(Color.WHITE);

        Paint fillPaint = new Paint();
        fillPaint.setColor(0xff0099cc);

        c.drawRect(0, 0, empty.getWidth(), empty.getHeight(), fillPaint);

        String ipAddr = null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (!intf.isLoopback() && intf.isUp())
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        String ip = inetAddress.getHostAddress();
                        if (ip.indexOf(':') < 0 /*ip4 only*/) {
                            ipAddr = ip;
                            break;
                        }
                    }
            }
        } catch (SocketException ex) {
        }

        if (ipAddr != null) {
            String text = "Откройте в браузере http://" + ipAddr + ":8000/ для загрузки фотографий";
            c.drawText(text, empty.getWidth() / 2, empty.getHeight() / 5, textPaint);
            text = "Upload pictures on http://" + ipAddr + ":8000/";
            c.drawText(text, empty.getWidth() / 2, empty.getHeight() * 4 / 5, textPaint);
        }

        return empty;
    }

    /**
     * Stack Blur v1.0 from
     * http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
     * Java Author: Mario Klingemann <mario at quasimondo.com>
     * http://incubator.quasimondo.com
     *
     * created Feburary 29, 2004
     * Android port : Yahel Bouaziz <yahel at kayenko.com>
     * http://www.kayenko.com
     * ported april 5th, 2012
     *
     * This is a compromise between Gaussian Blur and Box blur
     * It creates much better looking blurs than Box Blur, but is
     * 7x faster than my Gaussian Blur implementation.
     *
     * I called it Stack Blur because this describes best how this
     * filter works internally: it creates a kind of moving stack
     * of colors whilst scanning through the image. Thereby it
     * just has to add one new block of color to the right side
     * of the stack and remove the leftmost color. The remaining
     * colors on the topmost layer of the stack are either added on
     * or reduced by one, depending on if they are on the right or
     * on the left side of the stack.
     *
     * If you are using this algorithm in your code please add
     * the following line:
     * Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
     */

    public static Bitmap fastblur(Bitmap sentBitmap, float scale, int radius) {

        int width = Math.round(sentBitmap.getWidth() * scale);
        int height = Math.round(sentBitmap.getHeight() * scale);
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false);

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }
}
